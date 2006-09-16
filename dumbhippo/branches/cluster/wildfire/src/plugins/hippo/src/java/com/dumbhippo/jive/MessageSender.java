package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.XmppMessageSenderProvider;

/**
 * This class implements sending messages to a particular user or list of users;
 * one use for this class is for to act as the "message sender provider" for the
 * session beans. But it is also useful for sending messages from the plugin
 * code which runs inside Wildfire, because it provides non-blocking ordered
 * delivery, something which is harder to get using the Wildfire internal
 * methods (as the hoops we jump through here show).
 * 
 * Non-blocking: A call to sendMessage will never block: the message is just
 *   queued for future delivery. 
 * 
 * Ordering: If one call to sendMessage completes before another starts, and 
 *   they share a common recipient, then the messages will be delivered to the
 *   recipients in that order.
 *   
 * Reliability: If the user is online when sendMessage() is called and stays
 *   online for sufficiently long after, they will get the message
 */
public class MessageSender implements XmppMessageSenderProvider {
	ExecutorService pool = ThreadUtils.newCachedThreadPool("MessageSender-pool");
	private Map<Guid, UserMessageQueue> userQueues = new HashMap<Guid, UserMessageQueue>();
	private JID defaultFrom = defaultFrom = new JID("admin", XMPPServer.getInstance().getServerInfo().getName(), null);
	static private MessageSender instance;
	
	public void start() {
		instance = this;
	}

	public void shutdown() {
		instance = null;
		pool.shutdown();
	}
	
	/**
	 * @return the static singleton instance of this class. May be null during startup
	 *  or shutdown, but otherwise will always exist.
	 */
	static public MessageSender getInstance() {
		return instance;
	}
	
	static class UserMessageQueue implements Runnable {
		Queue<Message> queue = new LinkedList<Message>();
		String node;
		
		public UserMessageQueue(String node) {
			this.node = node;
		}
		
		public synchronized void addMessage(ExecutorService pool, Message template) {
			boolean wasEmpty = queue.isEmpty();
			queue.add(template);
			
			if (wasEmpty)
				pool.execute(this);
		}
		
		public void run() {
			List<Message> toSend;
			
			synchronized(this) {
				toSend = new ArrayList<Message>(queue);
				queue.clear(); 
			}
			
			for (Message template : toSend) {
				Message message = template.createCopy();
				try {
					SessionManager.getInstance().userBroadcast(node, message);
				} catch (UnauthorizedException e) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * Send a message to a set of users.
	 * 
	 * @param to the users to send the message to
	 * @param template template for the messages we want to send out. It will be
	 *     copied and the recipient filled in.
	 */
	public void sendMessage(Set<Guid> to, Message template) {
		for (Guid guid : to) {
			String node = guid.toJabberId(null);
			
			// We want to avoid queueing messages for users not on this server,
			// since that will frequently be the vast majority of the recipients
			// that we are given
			if (SessionManager.getInstance().getSessionCount(node) == 0)
				continue;

			UserMessageQueue userQueue;

			synchronized (userQueues) {
				userQueue = userQueues.get(guid);
				if (userQueue == null) {
					userQueue = new UserMessageQueue(node);
					userQueues.put(guid, userQueue);
				}
			}
			
			userQueue.addMessage(pool, template);
		}
	}

	/**
	 * sendMessage variant that sends the message only to a single recipient
	 * 
	 * @param to the recipient
	 * @param template Message object to send
	 */
	public void sendMessage(Guid to, Message template) {
		sendMessage(Collections.singleton(to), template);
	}

	// This is the sendMessage() variant used by the Session beans to avoid
	// having to access Wildfire XMPP types. 
	public void sendMessage(Set<Guid> to, String payload) {
        Document payloadDocument;
        try {
            payloadDocument = DocumentHelper.parseText(payload);
        } catch (DocumentException e) {
            throw new RuntimeException("Couldn't parse payload as XML");
        }
        
        Element payloadElement = payloadDocument.getRootElement();
        payloadElement.detach();
        Message template = new Message();
        template.setFrom(defaultFrom);
        template.setType(Message.Type.headline);
        template.getElement().add(payloadElement);
        sendMessage(to, template);
	}
}
