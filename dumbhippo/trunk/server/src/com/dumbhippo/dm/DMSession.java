package com.dumbhippo.dm;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.NotFoundException;

public class DMSession {
	static private Logger logger = GlobalSetup.getLogger(DMSession.class);

	private Map<Object, DMObject> sessionDMOs = new HashMap<Object, DMObject>();
	private DMCache cache;
	private DMViewpoint viewpoint;
	private EntityManager injectableEntityManager;
	
	protected DMSession(DMCache cache, DMViewpoint viewpoint) {
		this.cache = cache;
		this.viewpoint = viewpoint;
	}
	
	protected DMCache getCache() {
		return cache;
	}

	public DMViewpoint getViewpoint() {
		return viewpoint;
	}
	
	public <K, T extends DMObject<K>> T find(Class<T> clazz, K key) throws NotFoundException {
		@SuppressWarnings("unchecked")
		T result = (T)sessionDMOs.get(key);
		
		if (result == null) {
			logger.debug("Didn't find object for key {}, creating a new one", key);
			
			DMClass<T> dmClass = cache.getDMClass(clazz); 
			
			result = dmClass.createInstance(key, this);
			sessionDMOs.put(key, result);
		}
		
		return result;
	}
	
	public <K, T extends DMObject<K>> T findMustExist(Class<T> clazz, K key) {
		try {
			return find(clazz, key);
		} catch (NotFoundException e) {
			throw new RuntimeException("Entity unexpectedly missing, class=" + clazz.getName() + ", key=" + key);
		}
	}
	
	/**
	 * For use in generated code; this isn't part of the public interface 
	 * 
	 * @param <T>
	 * @param clazz
	 * @param t
	 */
	public <T extends DMObject<?>> void internalInit(Class<T> clazz, T t) {
		DMClass<T> dmClass = cache.getDMClass(clazz); 
		
		dmClass.processInjections(this, t);
		
		// FIXME: sort this out, or at least throw a specific unchecked exception
		//   If we init() immediately on objects not found in the cache we
		//   could cut down on having to expect lazy exceptions.
		try {
			t.init();
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Object getInjectableEntityManager() {
		if (injectableEntityManager == null)
			injectableEntityManager = cache.createInjectableEntityManager();
		
		return injectableEntityManager;
	}
}
