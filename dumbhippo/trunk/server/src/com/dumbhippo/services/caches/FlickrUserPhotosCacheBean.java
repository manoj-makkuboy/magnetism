package com.dumbhippo.services.caches;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedFlickrUserPhoto;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class FlickrUserPhotosCacheBean extends AbstractListCacheBean<String,FlickrPhotoView,CachedFlickrUserPhoto> implements
		FlickrUserPhotosCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrUserPhotosCacheBean.class);

	// this is long, because we explicitly drop the cache if we think there 
	// might be a change... having an expiration here at all isn't needed in 
	// theory.
	static private final long FLICKR_USER_PHOTOS_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
	
	// how many photos we want to get for a user; we don't need to store them all,
	// just a few to display. Right now we show maybe 4 or 5, so ask for a couple 
	// extra.
	static private final int NUMBER_OF_SAMPLE_PHOTOS = 7; 
	
	public FlickrUserPhotosCacheBean() {
		super(Request.FLICKR_USER_PHOTOS, FlickrUserPhotosCache.class, FLICKR_USER_PHOTOS_EXPIRATION);
	}

	@Override
	public List<CachedFlickrUserPhoto> queryExisting(String key) {
		Query q = em.createQuery("SELECT photo FROM CachedFlickrUserPhoto photo WHERE photo.ownerId = :ownerId");
		q.setParameter("ownerId", key);
		
		List<CachedFlickrUserPhoto> results = TypeUtils.castList(CachedFlickrUserPhoto.class, q.getResultList());
		return results;
	}

	@Override
	public void setAllLastUpdatedToZero(String key) {
		EJBUtil.prepareUpdate(em, CachedFlickrUserPhoto.class);
		
		Query q = em.createQuery("UPDATE CachedFlickrUserPhoto c " + 
				" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
				" WHERE c.ownerId = :ownerId");
		q.setParameter("ownerId", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired", updated);
	}	
	
	@Override
	public FlickrPhotoView resultFromEntity(CachedFlickrUserPhoto entity) {
		return entity.toPhoto();
	}

	@Override
	public CachedFlickrUserPhoto entityFromResult(String key, FlickrPhotoView result) {
		return new CachedFlickrUserPhoto(key, result);
	}

	@Override
	protected List<FlickrPhotoView> fetchFromNetImpl(String key) {
		FlickrWebServices ws = new FlickrWebServices(REQUEST_TIMEOUT, config);
		FlickrPhotos photos = ws.lookupPublicPhotos(key, 1, NUMBER_OF_SAMPLE_PHOTOS);
		if (photos == null)
			return null;
		else
			return TypeUtils.castList(FlickrPhotoView.class, photos.getPhotos());
	}

	@Override
	public CachedFlickrUserPhoto newNoResultsMarker(String key) {
		return CachedFlickrUserPhoto.newNoResultsMarker(key);
	}
}
