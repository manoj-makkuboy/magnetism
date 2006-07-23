package com.dumbhippo.persistence;

import javax.persistence.Entity;

import org.hibernate.annotations.Index;

/**
 * This table maps albumId to Yahoo song results
 */
@Entity
@org.hibernate.annotations.Table(name = "CachedYahooAlbumSongData", indexes={ 
		@Index(name="albumId_index", columnNames = { "albumId", "id" } ) 
})
public class CachedYahooAlbumSongData extends YahooSongResult {
	private static final long serialVersionUID = 1L;

}
