package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

public class ExpandedArtistView extends ArtistView {
	
	private List<AlbumView> albums;
	
	private String yahooMusicPageUrl;
	private String smallImageUrl;
	private int smallImageWidth;
	private int smallImageHeight;
	
	private int totalAlbumsByArtist;
	
	ExpandedArtistView() {
		super();
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
		this.totalAlbumsByArtist = 0;
	    albums = new ArrayList<AlbumView>();	
	}

	public ExpandedArtistView(String name) {
		super(name);
		this.smallImageWidth = -1;
		this.smallImageHeight = -1;
		this.totalAlbumsByArtist = 0;
	    albums = new ArrayList<AlbumView>();
    }
	
	public List<AlbumView> getAlbums() {
		return albums;
	}

	public void pageAlbums(Pageable<AlbumView> pageable) {
		pageable.setResults(albums);
	    pageable.setTotalCount(totalAlbumsByArtist);
	}
	
	public void addAlbum(AlbumView album) {
		// album.getArtist() should match this.getName()
		albums.add(album);
	}

	public String getYahooMusicPageUrl() {
		return yahooMusicPageUrl;
	}

	public void setYahooMusicPageUrl(String yahooMusicPageUrl) {
		this.yahooMusicPageUrl = yahooMusicPageUrl;
	}
	
	public String getSmallImageUrl() {
		return smallImageUrl;
	}

	public void setSmallImageUrl(String smallImageUrl) {
		this.smallImageUrl = smallImageUrl;
	}
	
	public int getSmallImageHeight() {
		return smallImageHeight;
	}

	public void setSmallImageHeight(int smallImageHeight) {
		this.smallImageHeight = smallImageHeight;
	}

	public int getSmallImageWidth() {
		return smallImageWidth;
	}

	public void setSmallImageWidth(int smallImageWidth) {
		this.smallImageWidth = smallImageWidth;
	}
	
	public int getTotalAlbumsByArtist() {
		return totalAlbumsByArtist;
	}

	public void setTotalAlbumsByArtist(int totalAlbumsByArtist) {
		this.totalAlbumsByArtist = totalAlbumsByArtist;
	}	
}

