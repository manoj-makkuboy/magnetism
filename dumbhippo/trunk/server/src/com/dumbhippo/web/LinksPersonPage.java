package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksPersonPage extends AbstractPersonPage {
	
	@PagePositions
	private PagePositionsBean pagePositions;
	
	static private final Logger logger = GlobalSetup.getLogger(LinksPersonPage.class);
	
	private PostingBoard postBoard;
	
	private Pageable<PostView> favoritePosts;
	
	private Pageable<PostView> receivedPosts;
	private Pageable<PostView> sentPosts;
	
	private Boolean notifyPublicShares;
	
	public LinksPersonPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public Pageable<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getViewedUser());
			receivedPosts = pagePositions.createPageable("receivedPosts");
			postBoard.pageReceivedPosts(getUserSignin().getViewpoint(), getViewedUser(), receivedPosts);
		}
		return receivedPosts;
	}
	
	public Pageable<PostView> getFavoritePosts() {
		if (favoritePosts == null) {
			logger.debug("Getting favorite posts for {}", getViewedUser());
			favoritePosts = pagePositions.createPageable("favoritePosts");
			postBoard.pageFavoritePosts(getSignin().getViewpoint(), getViewedUser(), favoritePosts);
		}
		return favoritePosts;
	}
	
	public Pageable<PostView> getSentPosts() {
		if (sentPosts == null) {
			logger.debug("Getting sent posts for {}", getViewedUser());
			sentPosts = pagePositions.createPageable("sentPosts");
			postBoard.pagePostsFor(getSignin().getViewpoint(), getViewedUser(), sentPosts);
		}
		return sentPosts;
	}

	public boolean getNotifyPublicShares() {
		if (notifyPublicShares == null)
			notifyPublicShares = identitySpider.getNotifyPublicShares(getViewedUser());
		return notifyPublicShares;
	}
}
