package com.dumbhippo.live;

import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;

// Implementation of LivePostUpdater
@Stateless
public class LivePostUpdaterBean implements LivePostUpdater {
	@EJB
	PostingBoard postingBoard;
	
	@EJB
	MessageSender messageSender;
	
	public void initialize(LivePost livePost) {
		int totalViewers = postingBoard.getPostViewerCount(livePost.getGuid());
		livePost.setTotalViewerCount(totalViewers);
		List<PersonPostData> viewers = postingBoard.getPostViewers(null, livePost.getGuid(), LivePost.MAX_STORED_VIEWERS);
		for (PersonPostData viewerData : viewers) {
			livePost.addViewer(viewerData.getPerson().getGuid(), viewerData.getClickedDate());
		}
		PostView pv;
		try {
			pv = postingBoard.loadPost(new Viewpoint(null), livePost.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Post postObj = pv.getPost();
		List<PostMessage> messages = postingBoard.getRecentPostMessages(postObj, 60);
		livePost.setRecentMessageCount(messages.size());
		
		double logViewers = Math.E * Math.log(totalViewers);
		if (!((totalViewers > 127 && (totalViewers % 20) != 0) ||
			  (totalViewers >= 3 && totalViewers <= 5) ||
			  (Math.floor(logViewers) == Math.ceil(logViewers)))) {
			messageSender.sendLivePostChanged(livePost);
		}
	}
}
