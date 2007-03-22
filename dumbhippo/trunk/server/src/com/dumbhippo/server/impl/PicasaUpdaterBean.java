package com.dumbhippo.server.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTaskFamily;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.PicasaUpdateStatus;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.PicasaUpdater;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.PicasaAlbum;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.CacheFactoryBean;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.services.caches.PicasaAlbumsCache;

@Stateless
public class PicasaUpdaterBean extends CachedExternalUpdaterBean<PicasaUpdateStatus> implements CachedExternalUpdater<PicasaUpdateStatus>, PicasaUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(PicasaUpdaterBean.class);
	
	@EJB
	private Notifier notifier;

	@WebServiceCache
	private PicasaAlbumsCache albumsCache;	
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)	
	public void doPeriodicUpdate(String username) {
		EJBUtil.assertNoTransaction();
		
		PicasaUpdater proxy = EJBUtil.defaultLookup(PicasaUpdater.class);
		
		List<? extends PicasaAlbum> albums = albumsCache.getSync(username, true);
		proxy.saveUpdatedStatus(username, albums);
	}
	
	//TODO: Identify the unique part of picasa urls instead of using the whole thing
	private String computeHash(List<? extends PicasaAlbum> albums) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= albums.size())
				break;
			sb.append(albums.get(i).getThumbnailHref());
		}
		return sb.toString();
	}	

	public boolean saveUpdatedStatus(String username, List<? extends PicasaAlbum> albums) {
		logger.debug("Saving new Picasa status for " + username + ": albums {}",
				albums);
		
		EJBUtil.assertHaveTransaction();
		
		PicasaUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(username);
		} catch (NotFoundException e) {
			updateStatus = new PicasaUpdateStatus(username);
			em.persist(updateStatus);
		}

		String hash = computeHash(albums);
		
		if (!updateStatus.getAlbumHash().equals(hash)) {
			logger.debug("Most recent albums changed '{}' -> '{}'",
					updateStatus.getAlbumHash(), hash);
			updateStatus.setAlbumHash(hash);
			notifier.onPicasaRecentAlbumsChanged(username, albums);
			return true;
		}
		return false;
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.PICASA;
	}

	@Override
	protected Query getCachedStatusQuery(String handle) {
		Query q = em.createQuery("SELECT updateStatus FROM PicasaUpdateStatus updateStatus " +
		"WHERE updateStatus.username = :username");
		q.setParameter("username", handle);
		return q;
	}

	@Override
	protected Class<? extends CachedExternalUpdater<PicasaUpdateStatus>> getUpdater() {
		return PicasaUpdater.class;
	}

	@Override
	protected PollingTaskFamilyType getTaskFamily() {
		return PollingTaskFamilyType.PICASA;
	}

	private static class PicasaTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicity() {
			return 20 * 60 * 1000; // 20 minutes
		}

		// Rough numbers, hopefully they're reasonable
		public long getMaxOutstanding() {
			return 10;
		}

		public long getMaxPerSecond() {
			return 5;
		}

		public String getName() {
			return PollingTaskFamilyType.PICASA.name();
		}
	}
	
	private static PollingTaskFamily family = new PicasaTaskFamily();
	
	private static class PicasaTask extends PollingTask {
		private String username;
		
		public PicasaTask(String username) {
			this.username = username;
		}

		@Override
		protected PollResult execute() throws Exception {
			boolean changed = false;
			PicasaUpdater proxy = EJBUtil.defaultLookup(PicasaUpdater.class);
			PicasaAlbumsCache cache = CacheFactoryBean.defaultLookup(PicasaAlbumsCache.class);
			
			List<? extends PicasaAlbum> albums = cache.getSync(username, true);
			changed = proxy.saveUpdatedStatus(username, albums);			
			return new PollResult(changed, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return username;
		}
	}	
	
	@Override
	protected PollingTask createPollingTask(String handle) {
		return new PicasaTask(handle);
	}
}