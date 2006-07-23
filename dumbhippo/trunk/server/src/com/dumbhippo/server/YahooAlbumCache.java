package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.services.YahooSongData;

@Local
public interface YahooAlbumCache {
	public List<YahooAlbumResult> getYahooAlbumResultsSync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude);
	
    public Future<List<YahooAlbumResult>> getYahooAlbumResultsAsync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude);
    
	public YahooAlbumResult getYahooAlbumSync(YahooSongData yahooSong) throws NotFoundException;
	
	public Future<YahooAlbumResult> getYahooAlbumAsync(YahooSongData yahooSong);	
}
