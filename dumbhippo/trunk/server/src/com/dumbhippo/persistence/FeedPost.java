package com.dumbhippo.persistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A Post that is a reflection of an entry in a RSS feed; the Post is
 * specific to both the particular sender (the GroupFeed) and the entry.
 * If multiple groups are subscribed to the same feed, a Post will be
 * created for each group.
 *  
 * @author otaylor
 */
@Entity
public class FeedPost extends Post {
	private static final long serialVersionUID = 1L;
	private GroupFeed feed;
	private FeedEntry entry;

	protected FeedPost() {
		super();
	}
	
	static private Set<Resource> makeExpandedRecipients(GroupFeed feed) {
		Set<Resource> expandedRecipients = new HashSet<Resource>();
		for (GroupMember member : feed.getGroup().getMembers()) {
			expandedRecipients.add(member.getMember());
		}
		
		return expandedRecipients;
	}
	
	public FeedPost(GroupFeed feed, FeedEntry entry, PostVisibility visibility) {
		super(null, visibility, entry.getTitle(), entry.getDescription(), null,
				  Collections.singleton(feed.getGroup()), makeExpandedRecipients(feed), 
				  Collections.singleton((Resource)entry.getLink()));
		
		this.feed = feed;
		this.entry = entry;
	}

	@ManyToOne
	@JoinColumn(nullable = false)
	public GroupFeed getFeed() {
		return feed;
	}

	public void setFeed(GroupFeed feed) {
		this.feed = feed;
	}

	@ManyToOne
	@JoinColumn(nullable = false)
	public FeedEntry getEntry() {
		return entry;
	}

	public void setEntry(FeedEntry entry) {
		this.entry = entry;
	}
}
