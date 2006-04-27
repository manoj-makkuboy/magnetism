package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksPersonPage extends AbstractPersonPage {
	
	static private final int INITIAL_RESULT_COUNT = 3;
	static private final int PAGING_RESULT_COUNT = 6;
	
	static private final Logger logger = GlobalSetup.getLogger(LinksPersonPage.class);
	
	private PostingBoard postBoard;
	
	private ListBean<PostView> favoritePosts;
	
	private int receivedPostsTotal = -1;
	private int receivedPostsIndex = 0;
	private ListBean<PostView> receivedPosts;
	private int sentPostsTotal = -1;	
	private int sentPostsIndex = 0;
	private ListBean<PostView> sentPosts;
	
	private Boolean notifyPublicShares;
	
	public LinksPersonPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public int getReceivedPostsTotal() {
		if (receivedPostsTotal == -1) {
			receivedPostsTotal = postBoard.getReceivedPostsCount(getUserSignin().getViewpoint(), getViewedUser());
		}
		return receivedPostsTotal;
	}
	
	public int getSentPostsTotal() {
		if (sentPostsTotal == -1) {
			sentPostsTotal = postBoard.getPostsForCount(getUserSignin().getViewpoint(), getViewedUser());
		}
		return sentPostsTotal;
	}	
	
	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getViewedUser());
			int count = receivedPostsIndex > 0 ? PAGING_RESULT_COUNT : INITIAL_RESULT_COUNT;
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(getUserSignin().getViewpoint(), getViewedUser(), receivedPostsIndex * count, count));
		}
		return receivedPosts;
	}
	
	public ListBean<PostView> getFavoritePosts() {
		if (favoritePosts == null) {
			logger.debug("Getting favorite posts for {}", getViewedUser());
			favoritePosts = new ListBean<PostView>(postBoard.getFavoritePosts(getSignin().getViewpoint(), getViewedUser(), 0, INITIAL_RESULT_COUNT));
		}
		return favoritePosts;
	}
	
	public ListBean<PostView> getSentPosts() {
		if (sentPosts == null) {
			logger.debug("Getting sent posts for {}", getViewedUser());
			int count = sentPostsIndex > 0 ? PAGING_RESULT_COUNT : INITIAL_RESULT_COUNT;			
			sentPosts = new ListBean<PostView>(postBoard.getPostsFor(getSignin().getViewpoint(), getViewedUser(), sentPostsIndex * count, count));
		}
		return sentPosts;
	}

	public int getReceivedPostsIndex() {
		return receivedPostsIndex;
	}

	public void setReceivedPostsIndex(int receivedPostsIndex) {
		this.receivedPostsIndex = receivedPostsIndex;
	}
	
	public int getSentPostsIndex() {
		return sentPostsIndex;
	}

	public void setSentPostsIndex(int idx) {
		this.sentPostsIndex = idx;
	}	
	
	public boolean getNotifyPublicShares() {
		if (notifyPublicShares == null)
			notifyPublicShares = identitySpider.getNotifyPublicShares(getViewedUser());
		return notifyPublicShares;
	}
}
