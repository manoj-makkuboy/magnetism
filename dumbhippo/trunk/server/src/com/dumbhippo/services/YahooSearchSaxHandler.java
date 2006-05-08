package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;

class YahooSearchSaxHandler extends EnumSaxHandler<YahooSearchSaxHandler.Element> {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSearchSaxHandler.class);
	
	// The enum names should match the xml element names (including case)
	enum Element {
		// General
		ResultSet,
		Result,
		Name,
		Thumbnail,
			Url,
			Width,
			Height,
		RelatedArtists,
		Artist,
		PopularSongs,
		Song,
		YahooMusicPage,
		
		// Album search 
		Title,
		Publisher,
		ReleaseDate,
		Tracks, 
		RelatedAlbums,
		Album, // element name for listing related albums
		
		// Song search
		Length,
		Track,
		
		// Download search
		Source,
		Format,
		Price,
		Channels,
		Restrictions,
		Quality,
		
		// Error
		Error,
		Message,
		
		IGNORED // an element we don't care about
	};	

	private class Result {
		private String id;
		private Map<Element,String> values;
		private String artistId;
		private String albumId;
		
		public Result(String id) {
			this.id = id;
			values = new EnumMap<Element,String>(Element.class);
		}
		
		public String getValue(Element e) {
			return values.get(e);
		}

		public int getValueInt(Element e) {
			String v = values.get(e);
			if (v == null)
				return -1;
			try {
				return Integer.parseInt(v);
			} catch (NumberFormatException e1) {
				return -1;
			}
		}
		
		public void setValue(Element e, String v) {
			values.put(e, v);
		}
		
		// this will be an album id if an album search was done
		public String getId() {
			return id;
		}

		// this will be an album id if a song search was done
		public String getAlbumId() {
			return albumId;
		}

		public void setAlbumId(String albumId) {
			this.albumId = albumId;
		}

		public String getArtistId() {
			return artistId;
		}

		public void setArtistId(String artistId) {
			this.artistId = artistId;
		}
	}
	
	private List<Result> results;
	private int totalResultsAvailable;
	private String errorMessage;
	
	YahooSearchSaxHandler() {
		super(Element.class, Element.IGNORED);
		results = new ArrayList<Result>();
		totalResultsAvailable = -1;
	}

	private Result currentResult() {
		// note that unlike the other current() things, 
		// "results" is not a stack, just a list 
		if (results.size() > 0)
			return results.get(results.size() - 1);
		else
			return null;
	}
	
	
	@Override
	protected void openElement(Element c) throws SAXException {
		// logger.debug("opening element " + c);
		if (c == Element.ResultSet) {
		    String totalResultsAvailableString = currentAttributes().getValue("totalResultsAvailable");
			try {
				totalResultsAvailable = Integer.parseInt(totalResultsAvailableString);
			} catch (NumberFormatException e1) {
			    totalResultsAvailable = -1;
				logger.error("numberFormatException when parsing value for totalResultsAvailable");
			}
		}
		if (c == Element.Result) {
			String id = currentAttributes().getValue("id");
			Result result = new Result(id);
			results.add(result);
		}
	}
	
	@Override
	protected void closeElement(Element c) throws SAXException {
		/*
		logger.debug("closing element " + c);
		
		Attributes a = currentAttributes();
		for (int i = 0; i < a.getLength(); ++i) {
			logger.debug("qName = " + a.getQName(i) + " localname = " + a.getLocalName(i) + " value = " + a.getValue(i));
		}
		*/
		
		if (parent() == Element.Result) {
			currentResult().setValue(c, getCurrentContent());
			
			if (c == Element.Artist)
				currentResult().setArtistId(currentAttributes().getValue("id"));
			else if (c == Element.Album)
				currentResult().setAlbumId(currentAttributes().getValue("id"));	
		} else if (parent() == Element.Thumbnail) {
			if (c == Element.Url || c == Element.Width || c == Element.Height)
				currentResult().setValue(c, getCurrentContent());
		} else if (c == Element.Message && parent() == Element.Error) {
			errorMessage = getCurrentContent();
			logger.debug("Error reply from Yahoo: {}", errorMessage);
			throw new ServiceException(true, "Yahoo! search failed: " + errorMessage);	
		}
	}
	
	/*
	@Override 
	public void endDocument() throws SAXException {
		logger.debug("End of yahoo document");
	}
	*/
	
	private YahooSongResult songFromResult(Result r, String artist, String album, String name) {
		// we want to make sure we return results that match supplied fields exactly, 
		// because otherwise yahoo will include results only subsets of which contain the 
		// supplied fields, for example search for album "Martina McBride: Greatest Hits"
		// returns both "Martina McBride: Greatest Hits" and 
		// "Sing Like Martina McBride - Greatest Hits"
		String resultAlbum = r.getValue(Element.Album);
		String resultArtist = r.getValue(Element.Artist);
		String resultName = r.getValue(Element.Title);
		if (!resultName.equals(name) || !resultAlbum.equals(album) || !resultArtist.equals(artist)) {
			return null;
		}	

		return songFromResult(r);		
	}

	private YahooSongResult songFromResult(Result r) {
		YahooSongResult song = new YahooSongResult();
		song.setLastUpdated(new Date());
		song.setSongId(r.getId());
		song.setName(r.getValue(Element.Title));
		song.setAlbumId(r.getAlbumId());
		song.setArtistId(r.getArtistId());
		song.setPublisher(r.getValue(Element.Publisher));
		song.setReleaseDate(r.getValue(Element.ReleaseDate));
		song.setDuration(r.getValueInt(Element.Length));
		song.setTrackNumber(r.getValueInt(Element.Track));
		return song;		
	}
	
	public List<YahooSongResult> getBestSongs(String artist, String album, String name) {
		// FIXME this could doubtless be more sophisticated ...
		// right now it just hopes the first two results are the 
		// good ones. you sometimes need two different song IDs 
		// to get all the download urls we care about (iTunes 
		// in particular)
		if (results.isEmpty()) {
			logger.debug("No song results were parsed");
			return Collections.emptyList();
		}

		List<YahooSongResult> list = new ArrayList<YahooSongResult>();
		Result r = results.get(0);
		YahooSongResult songResult = songFromResult(r, artist, album, name);
		boolean usedFirstResult = false;
		if (songResult != null) {
		    list.add(songResult);
		    usedFirstResult = true;
		}
		if (results.size() > 1) {
			r = results.get(1);
			// the rhapsody/yahoo/itunes/etc. good results 
			// tend to have these fields...
			if (!usedFirstResult || 
				(r.getAlbumId() != null &&
				 r.getValue(Element.ReleaseDate) != null &&
				 r.getValue(Element.Publisher) != null && 
				 !r.getId().equals(results.get(0).getId()))) {
				
				songResult = songFromResult(r, artist, album, name);
				if (songResult != null) {
				    list.add(songResult);
				}
			}
		}
		return list;
	}
	
	public List<YahooSongResult> getAlbumSongs() {
		List<YahooSongResult> list = new ArrayList<YahooSongResult>();
		
		if (results.isEmpty()) {
			logger.debug("No album songs results were parsed");
		}
		
		for (Result r : results) {
		    list.add(songFromResult(r));
		}
		return list;
	}
	
	private YahooAlbumResult albumFromResult(Result r) {
		YahooAlbumResult album = new YahooAlbumResult();
		album.setLastUpdated(new Date());
		album.setAlbumId(r.getId());
		album.setAlbum( r.getValue(Element.Title));
		album.setArtistId(r.getArtistId());
		album.setArtist(r.getValue(Element.Artist));
		album.setPublisher(r.getValue(Element.Publisher));
		album.setTracksNumber(r.getValueInt(Element.Tracks));
		album.setReleaseDate(r.getValue(Element.ReleaseDate));
		return album;		
	}
	
	public List<YahooAlbumResult> getBestAlbums() {
		logger.debug("total results available is {} when getting best albums " + totalResultsAvailable);
		List<YahooAlbumResult> list = new ArrayList<YahooAlbumResult>();
		
		if (results.isEmpty()) {
			logger.debug("No album results were parsed");
		}

		for (Result r : results) {
			YahooAlbumResult albumResult = albumFromResult(r);
		    list.add(albumResult);
		}
		//logger.debug("Got {} album results: {}", list.size(), list);
		return list;			
	}
	
	public int getTotalResultsAvailable() {
		return totalResultsAvailable;
	}
	
	private YahooSongDownloadResult downloadFromResult(Result r) {
		
		// need a known source and an url to be useful
		String sourceStr = r.getValue(Element.Source);
		SongDownloadSource source = SongDownloadSource.parseYahooSourceName(sourceStr);
		if (source == null) {
			//logger.debug("Ignoring download result from unknown source {}", sourceStr);
			return null;
		}
		String url = r.getValue(Element.Url);
		if (url == null) {
			//logger.debug("Ignoring download result with no url");
			return null;
		}
		
		YahooSongDownloadResult song = new YahooSongDownloadResult();
		song.setLastUpdated(new Date());
		song.setSource(source);
		song.setUrl(url);
		song.setFormats(r.getValue(Element.Format));
		song.setPrice(r.getValue(Element.Price));
		song.setRestrictions(r.getValue(Element.Restrictions));
		return song;		
	}
	
	public List<YahooSongDownloadResult> getBestDownloads() {
		if (results.isEmpty()) {
			logger.debug("No download results were parsed");
			return Collections.emptyList();
		}

		List<YahooSongDownloadResult> list = new ArrayList<YahooSongDownloadResult>();
		for (Result r : results) {
			YahooSongDownloadResult d = downloadFromResult(r);
			if (d != null)
				list.add(d);
		}
		//logger.debug("Got {} download results: {}", list.size(), list);
		return list;		
	}

	private YahooArtistResult artistFromResult(Result r) {
		YahooArtistResult artist = new YahooArtistResult();
		artist.setLastUpdated(new Date());
		artist.setArtistId(r.getId());
		artist.setArtist(r.getValue(Element.Name));
		artist.setYahooMusicPageUrl(r.getValue(Element.YahooMusicPage));
		artist.setSmallImageUrl(r.getValue(Element.Url));
		artist.setSmallImageWidth(r.getValueInt(Element.Width));
		artist.setSmallImageHeight(r.getValueInt(Element.Height));
		return artist;		
	}
	
	public List<YahooArtistResult> getBestArtists() {
		if (results.isEmpty()) {
			logger.debug("No artists were found");
			return Collections.emptyList();
		}
		
		List<YahooArtistResult> list = new ArrayList<YahooArtistResult>();
		for (Result r : results) {
			YahooArtistResult yahooArtist = artistFromResult(r);
			if (yahooArtist != null)
				list.add(yahooArtist);
		}
		
		return list;		
	}
}
