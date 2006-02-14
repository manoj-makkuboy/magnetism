package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.Hotness;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveXmppServer;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	static private final Logger logger = GlobalSetup.getLogger(MessengerGlueBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
		
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private MySpaceTracker mySpaceTracker;
		
	private Account accountFromUsername(String username) throws JabberUserNotFoundException {
		Guid guid;
		try {
			guid = Guid.parseJabberId(username);
		} catch (ParseException e) {
			throw new JabberUserNotFoundException("corrupt username");
		}
		Account account = accountSystem.lookupAccountByPersonId(guid.toString());
		if (account == null)
			throw new JabberUserNotFoundException("username does not exist");
		
		assert account.getOwner().getId().equals(username);
		
		return account;
	}
	
	private User userFromTrustedUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted username doesn't appear to exist: " + username);
		}
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		Account account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			return false;
		}
		
		assert account != null;
		
		return account.checkClientCookie(token, digest);
	}
	

	public long getJabberUserCount() {
		return accountSystem.getNumberOfActiveAccounts();
	}


	public void setName(String username, String name)
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


	public void setEmail(String username, String email) 
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public JabberUser loadUser(String username) throws JabberUserNotFoundException {
		
		Account account = accountFromUsername(username);
		
		PersonView view = identitySpider.getSystemView(account.getOwner(), PersonViewExtra.PRIMARY_EMAIL);
		
		JabberUser user = new JabberUser(username, account.getOwner().getNickname(), view.getEmail().getEmail());
	
		return user;
	}

	public String serverStartup(long timestamp) {
		logger.debug("Jabber server startup at " + new Date(timestamp));
		
		return LiveState.getInstance().createXmppServer().getServerIdentifier();
	}
	
	public void serverPing(String serverIdentifier) throws NoSuchServerException {
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		server.ping();
	}
	
	public void onUserAvailable(String serverIdentifier, String username) throws NoSuchServerException {
		logger.debug("Jabber user " + username + " now available");

		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);

		try {
			// account could be missing due to debug users or our own
			// send-notifications
			// user, i.e. any user on the jabber server that we don't know about
			Account account;
			try {
				account = accountFromUsername(username);
			} catch (JabberUserNotFoundException e) {
				logger.debug("username signed on that we don't know: " + username);
				return;
			}
			
			server.userAvailable(account.getOwner().getGuid());
			
			if (!account.getWasSentShareLinkTutorial()) {
				logger.debug("We have a new user!!!!! WOOOOOOOOOOOOHOOOOOOOOOOOOOOO send them tutorial!");
	
				postingBoard.doShareLinkTutorialPost(account.getOwner());
	
				account.setWasSentShareLinkTutorial(true);
			}
		} catch (RuntimeException e) {
			logger.error("Failed to do share link tutorial", e);
			throw e;
		}
	}

	public void onUserUnavailable(String serverIdentifier, String username) throws NoSuchServerException {
		logger.debug("Jabber user " + username + " now unavailable");
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		try {
			server.userUnavailable(Guid.parseJabberId(username));
		} catch (ParseException e) {
			logger.debug("Corrupt username passed to onUserUnavailable", e);
		}
	}

	public void onRoomUserAvailable(String serverIdentifier, String roomname, String username) throws NoSuchServerException  {
		logger.debug("Jabber user " + username + " has joined chatroom " + roomname);
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		try {
			server.postRoomUserAvailable(Guid.parseJabberId(roomname), Guid.parseJabberId(username));
		} catch (ParseException e) {
			logger.debug("Corrupt roomname or username passed to onUserUnavailable", e);
		}
	}

	public void onRoomUserUnavailable(String serverIdentifier, String roomname, String username) throws NoSuchServerException {
		logger.debug("Jabber user " + username + " has left chatroom " + roomname);
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		try {
			server.postRoomUserUnavailable(Guid.parseJabberId(roomname), Guid.parseJabberId(username));
		} catch (ParseException e) {
			logger.debug("Corrupt roomname or username passed to onUserUnavailable", e);
		}
	}
	
	public void onResourceConnected(String serverIdentifier, String username) throws NoSuchServerException {
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		try {
			server.resourceConnected(Guid.parseJabberId(username));
		} catch (ParseException e) {
			logger.debug("Corrupt username passed to onResourceConnected", e);
		}		
	}	
	
	public String getMySpaceName(String username) {
		User user = userFromTrustedUsername(username);
		return user.getAccount().getMySpaceName();
	}
	
	public void addMySpaceBlogComment(String username, long commentId, long posterId) {
		mySpaceTracker.addMySpaceBlogComment(userFromTrustedUsername(username), commentId, posterId);
	}	
	
	public List<MySpaceBlogCommentInfo> getMySpaceBlogComments(String username) {
		List<MySpaceBlogCommentInfo> ret = new ArrayList<MySpaceBlogCommentInfo>();
		for (MySpaceBlogComment cmt : mySpaceTracker.getRecentComments(userFromTrustedUsername(username))) {
			ret.add(new MySpaceBlogCommentInfo(cmt.getCommentId(), cmt.getPosterId()));
		}
		return ret;
	}
	
	private List<MySpaceContactInfo> userSetToContactList(Set<User> users) {
		List<MySpaceContactInfo> ret = new ArrayList<MySpaceContactInfo>();
		for (User user : users) {
			Account acct = user.getAccount();
			ret.add(new MySpaceContactInfo(acct.getMySpaceName(), acct.getMySpaceFriendId()));
		}
		return ret;		
	}
	
	public List<MySpaceContactInfo> getContactMySpaceNames(String username) {
		User requestingUser = userFromTrustedUsername(username);
		return userSetToContactList(identitySpider.getMySpaceContacts(new Viewpoint(requestingUser)));
	}
	
	public void notifyNewMySpaceContactComment(String username, String mySpaceContactName) {
		mySpaceTracker.notifyNewContactComment(userFromTrustedUsername(username), mySpaceContactName);
	}
	
	private User getUserFromUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("User does not exist: " + username, e);
		}
	}
	
	private Post getPostFromRoomName(User user, String roomName) throws NotFoundException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		return postingBoard.loadRawPost(viewpoint, Guid.parseTrustedJabberId(roomName));
	}
	
	public ChatRoomInfo getChatRoomInfo(String roomName, String initialUsername) {
		User initialUser = getUserFromUsername(initialUsername);				
		Post post;
		try {
			post = getPostFromRoomName(initialUser, roomName);
		} catch (NotFoundException e) {
			// FIXME in principle this should happen if the initialUser can't see the post, 
			// but right now there's no access controls in loadRawPost so it only happens
			// if something is broken
			return null;
		}
		 
		List<ChatRoomUser> allowedUsers = new ArrayList<ChatRoomUser>();
		
		User poster = post.getPoster();
		allowedUsers.add(new ChatRoomUser(poster.getGuid().toJabberId(null), poster.getVersion(), poster.getNickname()));
		
		// FIXME: This isn't really right; it doesn't handle public posts and
		// posts
		// where people join a group that it was sent to after the post was
		// sent. Public posts will need to be handled with a separate flag
		// in ChatRoomInfo.
		for (Resource recipient : post.getExpandedRecipients()) {
			User user = identitySpider.getUser(recipient);
			if (user != null && !user.equals(poster))
				allowedUsers.add(new ChatRoomUser(user.getGuid().toJabberId(null), user.getVersion(), user.getNickname()));
		}
		
		List<PostMessage> messages = postingBoard.getPostMessages(post);

		List<ChatRoomMessage> history = new ArrayList<ChatRoomMessage>();
		for (PostMessage postMessage : messages) {
			String username = postMessage.getFromUser().getGuid().toJabberId(null);
			ChatRoomMessage message;
			message = new ChatRoomMessage(username, postMessage.getMessageText(), postMessage.getTimestamp(), postMessage.getMessageSerial()); 
			history.add(message);
		}
		
		return new ChatRoomInfo(roomName, post.getTitle(), allowedUsers, history);
	}

	public Map<String,String> getPrefs(String username) {
		Map<String,String> prefs = new HashMap<String,String>();
		
		Account account;
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			logger.debug("Returning empty prefs for user we've never heard of");
			return prefs;
		}
		
		// right now we have only one pref
		
		prefs.put("musicSharingEnabled", Boolean.toString(!account.isDisabled() && account.isMusicSharingEnabled()));
		
		return prefs;
	}

	public Hotness getUserHotness(String username) {
		User user = userFromTrustedUsername(username);
		LiveState state = LiveState.getInstance();
		return state.getLiveUser(user.getGuid()).getHotness();
	}
}
