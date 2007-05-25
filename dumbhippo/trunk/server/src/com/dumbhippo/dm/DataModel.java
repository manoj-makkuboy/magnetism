package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javassist.ClassClassPath;
import javassist.ClassPool;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.cache.Timestamper;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.DMStore;
import com.dumbhippo.dm.store.StoreClient;

public class DataModel {
	protected static final Logger logger = GlobalSetup.getLogger(DataModel.class);
	
	private DMSessionMap sessionMap = new DMSessionMapJTA();
	private EntityManagerFactory emf = null;
	private Map<Class, DMClassHolder> classes = new HashMap<Class, DMClassHolder>();
	private ClassPool classPool = new ClassPool();
	private DMStore store = new DMStore();
	private boolean completed = false;
	private ExecutorService notificationExecutor = ThreadUtils.newSingleThreadExecutor("DataModel-notification");

	private static DataModel instance = new DataModel();

	private DataModel() {
		classPool = new ClassPool();
	
		// FIXME. We actually want the class path to be the class path of the class loader
		// where the DMO's live. Something to fix when we add jar-file scanning to 
		// find DMOs.
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
	}
	
	public static DataModel getInstance() {
		return instance;
	}

	/**
	 * Public for use in tests which aren't using JTA transactions
	 * 
	 * @param sessionMap the session map
	 */
	public void setSessionMap(DMSessionMap sessionMap) {
		this.sessionMap = sessionMap;
	}
		
	/**
	 * Public for use in tests which aren't using container-managed entity-managers
	 * 
	 * @param sessionMap the session map
	 */
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}
	
	/**
	 * Add a DMO class to those managed by the cache. Eventually, we probably want
	 * to search for classes marked with @DMO rather than requiring manual 
	 * registration. After adding all classes, call completeDMClasses(). 
	 * 
	 * @param <T>
	 * @param clazz
	 */
	public <T extends DMObject> void addDMClass(Class<T> clazz) {
		if (completed)
			throw new IllegalStateException("completeDMClasses has already been callled");
		
		classes.put(clazz, new DMClassHolder<T>(this, clazz));
	}
	
	/**
	 * Do any necessary post-processing after all DMO classes managed by the cache
	 * have been added.
	 */
	public void completeDMClasses() {
		if (completed)
			throw new IllegalStateException("completeDMClasses has already been callled");

		completed = true;
		
		for (DMClassHolder classHolder : classes.values())
			classHolder.complete();
	}
	
	/**
	 * Creates an entity manager for use in injection into DMOs 
	 */
	public EntityManager createInjectableEntityManager() {
		return emf.createEntityManager();
	}
	
	public <T extends DMObject> DMClassHolder<T> getDMClass(Class<T> clazz) {
		@SuppressWarnings("unchecked")
		DMClassHolder<T> classHolder = classes.get(clazz);
		
		if (classHolder == null)
			throw new IllegalArgumentException("Class " + clazz.getName() + " is not bound as a DMO");
		
		return classHolder;
	}
	
	public ReadOnlySession initializeReadOnlySession(DMViewpoint viewpoint) {
		ReadOnlySession session = new ReadOnlySession(this, viewpoint);
		sessionMap.initCurrent(session);
		
		return session;
	}
	
	public ReadWriteSession initializeReadWriteSession(DMViewpoint viewpoint) {
		ReadWriteSession session = new ReadWriteSession(this, viewpoint);
		sessionMap.initCurrent(session);
		
		return session;
	}
	
	protected DMSession getCurrentSession() {
		DMSession session = sessionMap.getCurrent();
		if (session == null)
			throw new IllegalStateException("DM session wasn't initialized");

		return session;
	}
	
	public ClassPool getClassPool() {
		return classPool;
	}
	
	public DMStore getStore() {
		return store;
	}

	public long getTimestamp() {
		// FIXME: This doesn't fully work in a clustered configuration; we should use
		// timestamps/serials from the invalidation protocol instead.
		
		return Timestamper.next();
	}
	
	public void notifyAsync(ChangeNotificationSet notificationSet) {
		notificationExecutor.execute(notificationSet);
	}
	
	/**
	 * Testing hook, not fully thread safe.
	 */
	public void waitForAllNotifications() {
		ExecutorService oldExecutor = notificationExecutor;
		notificationExecutor = ThreadUtils.newSingleThreadExecutor("DataModel-notification");
		
		oldExecutor.shutdown();
		try {
			oldExecutor.awaitTermination(300, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Timed out waiting for pending notifications to clear");
		}
	}

	public void sendNotification(final ClientNotification clientNotification) {
		logger.debug("Sending notification to {}", clientNotification.getClient());
		
		sessionMap.runInTransaction(new Runnable() {
			public void run() {
				StoreClient client = clientNotification.getClient();
				
				long serial = client.allocateSerial(); 
				FetchVisitor visitor = client.beginNotification();
				
				ReadOnlySession session = initializeReadOnlySession(client.getViewpoint());
				
				clientNotification.visitNotification(session, visitor);
				client.endNotification(visitor, serial);
			}
		});
	}
}
