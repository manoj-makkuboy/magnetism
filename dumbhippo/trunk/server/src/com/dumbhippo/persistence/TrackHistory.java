package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Every time you listen to a song, we add it to this history 
 * and update the lastUpdated. Your current song is the row
 * with the latest lastUpdated.
 * 
 * @author hp
 */
@Entity
@Table(name="TrackHistory", 
		   uniqueConstraints = 
		      {@UniqueConstraint(columnNames={"user_id","track_id"})}
	      )
	      
// We need to use native SQL when making queries across all 
// TrackHistory objects for a particular track, because of combinations
// of limitations of MySQL 4 and of HQL/EJBQL. You can't use aggregates
// in HAVING / ORDER BY clauses in MySQL 4 without naming them in
// in the SELECT clause, and you can't use named aggregates in those
// locations in HQL/EJBQL.	      
@SqlResultSetMapping(name="trackHistoryAggregateMapping", 
		entities={
			// We don't need to specify field by field mappings,
		    // using FieldResult since we return only one entity, and
		    // the default name for each returned field
		    
			@EntityResult(name="com.dumbhippo.persistence.Track")
		}
//      Doesn't work with our current Hibernate		
//		columns={
//			@ColumnResult(name="lastUpdated")
//		}
)
// Naming the queries allows us to keep them close to the result
// set mapping. We can also turn on caching for named queries,
// which could be helpful here, though we are't doing that yet.
@NamedNativeQueries({
	@NamedNativeQuery(name="trackHistoryMostPopularTracks",
		queryString=
			"SELECT  " +
			"  track.album as album, " +
			"  track.artist as artist, " +
			"  track.digest as digest, " +
			"  track.discIdentifier as discIdentifier, " +
			"  track.duration as duration, " +
			"  track.fileSize as fileSize, " +
			"  track.format as format, " +
			"  track.id as id, " +
			"  track.name as name, " +
			"  track.trackNumber as trackNumber, " + 
			"  track.type as type, " +
			"  track.url as url, " + 
			"  MAX(th.lastUpdated) as lastUpdated, " +
			"  SUM(th.timesPlayed) as timesPlayed " +
			"FROM TrackHistory th LEFT JOIN Track track on th.track_id = track.id " +
			"GROUP by th.track_id " +
			// probably a waste to tie-break by lastUpdated here, but fairly cheap
			"ORDER by timesPlayed DESC, lastUpdated DESC",
		resultSetMapping="trackHistoryAggregateMapping"
	),
	@NamedNativeQuery(name="trackHistoryOnePlayTracks",
			queryString=
				"SELECT  " +
				"  track.album as album, " +
				"  track.artist as artist, " +
				"  track.digest as digest, " +
				"  track.discIdentifier as discIdentifier, " +
				"  track.duration as duration, " +
				"  track.fileSize as fileSize, " +
				"  track.format as format, " +
				"  track.id as id, " +
				"  track.name as name, " +
				"  track.trackNumber as trackNumber, " + 
				"  track.type as type, " +
				"  track.url as url, " + 
				"  MAX(th.lastUpdated) as lastUpdated, " +
				"  SUM(th.timesPlayed) as timesPlayed " +
				"FROM TrackHistory th LEFT JOIN Track track on th.track_id = track.id " +
				"WHERE th.timesPlayed = 1 " + // Prefilter, if 1 track has > 1, total is > 1 
				"GROUP by th.track_id " +
				"HAVING timesPlayed = 1 " +
				"ORDER by lastUpdated DESC",
			resultSetMapping="trackHistoryAggregateMapping"
		)
	
})
public class TrackHistory extends DBUnique {

	private static final long serialVersionUID = 1L;

	private User user;
	private Track track;
	private long lastUpdated;
	private int timesPlayed;
	
	protected TrackHistory() {
		this.timesPlayed = 0;
	}
	
	public TrackHistory(User user, Track track) {
		this();
		this.user = user;
		this.track = track;
	}
	
	@Column(nullable=false)
	public Date getLastUpdated() {
		return new Date(lastUpdated);
	}
	
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated.getTime();
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Track getTrack() {
		return track;
	}
	public void setTrack(Track track) {
		this.track = track;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getUser() {
		return user;
	}
	protected void setUser(User user) {
		this.user = user;
	}

	@Column(nullable=false)
	public int getTimesPlayed() {
		return timesPlayed;
	}

	public void setTimesPlayed(int timesPlayed) {
		this.timesPlayed = timesPlayed;
	}
}
