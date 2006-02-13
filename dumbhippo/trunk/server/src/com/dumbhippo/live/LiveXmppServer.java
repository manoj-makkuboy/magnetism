package com.dumbhippo.live;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;

/**
 * Represents an instance of a connection of a Jabber server to the
 * application server. All presence information for that Jabber server
 * is proxied through here, so if we stop being pinged by the Jabber
 * server we can reliably mark all the users from that Jabber server
 * as no longer present. (Presence is reference counted inside
 * LiveState, so it works if we get a reconnection from a restarted
 * Jabber server before the old one times out.)
 * 
 * @author otaylor
 */
public class LiveXmppServer implements Ageable {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveXmppServer.class);

	private LiveState state;
	private Map<Guid, UserInfo> availableUsers;
	private int cacheAge;
	private String serverIdentifier;
	
	private static class UserInfo {
		Guid userId;
		Set<Guid> postRooms;
		
		UserInfo(Guid userId) {
			this.userId = userId;
		}
		
		void addPostRoom(Guid postId) {
			if (postRooms == null)
				postRooms = new HashSet<Guid>();
			
			postRooms.add(postId);
		}
		
		void removePostRoom(Guid postId) {
			if (postRooms != null)
				postRooms.remove(postId);
		}
		
		boolean containsPostRoom(Guid postId) {
			if (postRooms == null)
				return false;
			return postRooms.contains(postId);
		}
	}
	
	LiveXmppServer(LiveState state) {
		this.state = state;
		availableUsers = new HashMap<Guid, UserInfo>();		
		serverIdentifier = Guid.createNew().toString();
	}
	
	/**
	 * Get this server instances unique cookie
	 * 
	 * @return a unique cookie for this instance of the server;
	 *  the Jabber server passes this token back with subsequent
	 *  presence information or pings.
	 */
	public String getServerIdentifier() {
		return serverIdentifier;
	}
	
	/**
	 * Mark a user as available for this server. Called by the
	 * Jabber server glue code.
	 * 
	 * @param userId the user who is now present
	 */
	public void userAvailable(Guid userId) {
		synchronized (state) {
			if (!availableUsers.containsKey(userId)) {
				availableUsers.put(userId, new UserInfo(userId));
				state.userAvailable(userId);
			}
		}
	}
	
	/**
	 * Mark a user as unavailable for this server. Called by the
	 * Jabber server glue code.
	 * 
	 * @param userId the user who is no longer present
	 */
	public void userUnavailable(Guid userId) {
		synchronized (state) {
			UserInfo info = availableUsers.get(userId);
			if (info != null) {
				if (info.postRooms != null) {
					for (Guid postId : info.postRooms)
						state.postRoomUserUnavailable(postId, userId);
				}
				
				availableUsers.remove(userId);
				state.userUnavailable(userId);
			}
		}
	}
	
	/**
	 * Mark that a user has joined the chat room for a post. Must
	 * be called after userAvailable for that user. Called by the Jabber 
	 * server glue code.
	 * 
	 * @param postId the post ID for the chat room 
	 * @param userId the user who has joined the room
	 */
	public void postRoomUserAvailable(Guid postId, Guid userId) {
		UserInfo info = availableUsers.get(userId);
		if (info != null && !info.containsPostRoom(postId)) {
			info.addPostRoom(postId);
			state.postRoomUserAvailable(postId, userId);			
		} else {
			logger.warn("onPostRoomUserAvailable called for an unavailable user");
		}
	}

	/**
	 * Mark that a user has left the chat room for a post.
	 * Called by the Jabber server glue code.
	 * 
	 * @param postId the post ID for the chat room 
	 * @param userId the user who has joined the room
	 */
	public void postRoomUserUnavailable(Guid postId, Guid userId) {
		UserInfo info = availableUsers.get(userId);
		if (info != null && info.containsPostRoom(postId)) {
			info.removePostRoom(postId);
			state.postRoomUserUnavailable(postId, userId);
		}
	}
	
	/**
	 * Keeps an LiveXmppServer object alive, cached, and referencing
	 * it's users. This must be called within 
	 * LiveState.CLEANER_INTERVAL * LiveState.MAX_XMPP_SERVER_CACHE_AGE 
	 * or the object will be discarded. (Even if otherwise referenced,
	 * this is to avoid leaked references causing problems.)
	 */
	public void ping() {
		synchronized(state) {
			setCacheAge(0);
		}
	}	
	
	/********************************************************************/
	
	public int getCacheAge() {
		return cacheAge;
	}

	public void setCacheAge(int age) {
		this.cacheAge = age;
	}
	
	public void discard() {
		for (UserInfo info : availableUsers.values()) {
			if (info.postRooms != null) {
				for (Guid postId : info.postRooms)
					state.postRoomUserUnavailable(postId, info.userId);
			}
			
			state.userUnavailable(info.userId);
		}
		availableUsers = null;
	}
}
