package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.persistence.CachedYahooAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooAlbumCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooAlbumCacheBean extends AbstractCacheBean implements YahooAlbumCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooAlbumCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;

	
	static private class YahooAlbumSearchTask implements Callable<YahooAlbumData> {
		
		private String albumId;

		public YahooAlbumSearchTask(String albumId) {
			this.albumId = albumId;
		}
		
		public YahooAlbumData call() {
			logger.debug("Entering YahooAlbumSearchTask thread for albumId {}", albumId);				

			YahooAlbumCache cache = EJBUtil.defaultLookup(YahooAlbumCache.class);	
						
			YahooAlbumData data = cache.fetchFromNet(albumId);

			return cache.saveInCache(albumId, data);
		}
	}	
	
	public YahooAlbumData getSync(String albumId) {
		return getFutureResult(getAsync(albumId));
	}

	public Future<YahooAlbumData> getAsync(String albumId) {
		if (albumId == null) {
			throw new IllegalArgumentException("null albumId passed to YahooAlbumCache");
		}
		
		YahooAlbumData result = checkCache(albumId);
		if (result != null) {
			if (result.getAlbum() == null) // no result marker
				result = null;
			return new KnownFuture<YahooAlbumData>(result);
		}
				
		FutureTask<YahooAlbumData> futureAlbum =
			new FutureTask<YahooAlbumData>(new YahooAlbumSearchTask(albumId));
		getThreadPool().execute(futureAlbum);
		return futureAlbum;		

	}

	private CachedYahooAlbumData albumResultQuery(String albumId) {
		Query q;
		
		q = em.createQuery("FROM CachedYahooAlbumData album WHERE album.albumId = :albumId");
		q.setParameter("albumId", albumId);
		
		try {
			return (CachedYahooAlbumData) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	public YahooAlbumData checkCache(String albumId) {
		CachedYahooAlbumData result = albumResultQuery(albumId);

		if (result != null) {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + YAHOO_EXPIRATION_TIMEOUT) < now)) {
				result = null;
			}
		}

		if (result != null) {
			logger.debug("Have cached yahoo album result for albumId {}: {}",
					albumId, result);
			return result.toData();
		} else {
			return null;
		}
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public YahooAlbumData fetchFromNet(String albumId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		YahooAlbumData data = ws.lookupAlbum(albumId);
		logger.debug("Fetched album data for id {}: {}", albumId, data);
		return data;
	}

	// null data means to save a negative result
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public YahooAlbumData saveInCache(final String albumId, final YahooAlbumData data) {
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<YahooAlbumData>() {
				public YahooAlbumData call() {
					CachedYahooAlbumData r = albumResultQuery(albumId);
					if (r == null) {
						// data is allowed to be null which saves the negative result row
						// in the db
						r = new CachedYahooAlbumData();
						r.updateData(data);
						em.persist(r);
					} else {
						if (data != null) // don't ever save a negative result once we have data at some point
							r.updateData(data);
					}
					r.setLastUpdated(new Date());
					
					logger.debug("Saved new yahoo album data id {}: {}", 
						     albumId, r);
					
					if (r.isNoResultsMarker())
						return null;
					else
						return r.toData();
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			throw new RuntimeException(e); // not reached
		}
	}	
}
