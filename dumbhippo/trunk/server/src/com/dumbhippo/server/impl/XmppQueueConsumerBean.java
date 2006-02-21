package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.EJB;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventChatMessage;
import com.dumbhippo.xmppcom.XmppEventMusicChanged;
import com.dumbhippo.xmppcom.XmppEventPrimingTracks;

@MessageDriven(activateConfig =
{
  @ActivationConfigProperty(propertyName="destinationType",
    propertyValue="javax.jms.Queue"),
  @ActivationConfigProperty(propertyName="destination",
    propertyValue="queue/" + XmppEvent.QUEUE)
})
public class XmppQueueConsumerBean implements MessageListener {

	static private final Logger logger = GlobalSetup.getLogger(XmppQueueConsumerBean.class);

	@EJB
	private MusicSystemInternal musicSystem;
	
	@EJB
	private PostingBoard postingBoard;

	@EJB
	IdentitySpider identitySpider;
	
	public void onMessage(Message message) {
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in {}: {}", XmppEvent.QUEUE, obj);
				
				if (obj instanceof XmppEventMusicChanged) {
					XmppEventMusicChanged event = (XmppEventMusicChanged) obj;
					processMusicChangedEvent(event);
				} else if (obj instanceof XmppEventChatMessage) {
					XmppEventChatMessage event = (XmppEventChatMessage) obj;
					processChatMessageEvent(event);					
				} else if (obj instanceof XmppEventPrimingTracks) {
					XmppEventPrimingTracks event = (XmppEventPrimingTracks) obj;
					processPrimingTracksEvent(event);
				} else {
					logger.warn("Got unknown object: {}", obj);
				}
			} else {
				logger.warn("Got unknown jms message: {}", message);
			}
		} catch (JMSException e) {
			logger.warn("JMS exception in xmpp queue consumer", e);
		} catch (Exception e) {
			logger.warn("Exception processing Xmpp JMS message", e);
		}
	}

	private void processMusicChangedEvent(XmppEventMusicChanged event) {
		User user = getUserFromUsername(event.getJabberId());
		musicSystem.setCurrentTrack(user, event.getProperties());
	}
	
	private void processPrimingTracksEvent(XmppEventPrimingTracks event) {
		User user = getUserFromUsername(event.getJabberId());
		if (identitySpider.getMusicSharingPrimed(user)) {
			// at log .info, since it isn't really a problem, but if it happened a lot we'd 
			// want to investigate why
			logger.info("Ignoring priming data for music sharing, already primed");
			return;
		}
		List<Map<String,String>> tracks = event.getTracks();
		// the tracks are in order from most to least highly-ranked, we want to 
		// timestamp the most highly-ranked one as most recent, so do this backward
		tracks = new ArrayList<Map<String,String>>(tracks);
		Collections.reverse(tracks);
		for (Map<String,String> properties : tracks) {
			musicSystem.addHistoricalTrack(user, properties);
		}
		// don't do this again
		identitySpider.setMusicSharingPrimed(user, true);
		logger.debug("Primed user with {} tracks", tracks.size());
	}
	
	private User getUserFromUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted username " + username + " did not exist for some reason");
		}
	}
	
	private Post getPostFromRoomName(User user, String roomName) {
		Viewpoint viewpoint = new Viewpoint(user);
		try {
			return postingBoard.loadRawPost(viewpoint, Guid.parseTrustedJabberId(roomName));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted room name did not exist" + roomName, e);
		}
	}
	
	
	public void processChatMessageEvent(XmppEventChatMessage event) {
		User fromUser = getUserFromUsername(event.getFromUsername());
		Post post = getPostFromRoomName(fromUser, event.getRoomName());		
		postingBoard.addPostMessage(post, fromUser, event.getText(), event.getTimestamp(), event.getSerial());
	}
}
