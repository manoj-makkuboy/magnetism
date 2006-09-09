package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.SessionManagerListener;
import org.jivesoftware.wildfire.user.UserNotFoundException;

import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.util.EJBUtil;

public class PresenceMonitor implements SessionManagerListener {

	private Map<String, UserInfo> userInfo;
	private ExecutorService executor;
	
	private static class UserInfo {
		private int sessionCount;
	}

	public PresenceMonitor() {
		userInfo = new HashMap<String, UserInfo>();
		// It might be better to use a multiple-thread executor, since the work done when a
		// resource connects is potentially parallelizable. On the other hand, using
		// a single thread here will minimize server load during restart, which is a good
		// thing.
		executor = ThreadUtils.newSingleThreadExecutor("PresenceMonitor-notification");
	}

	public void shutdown() {
		executor.shutdown();
	}
	
	private void onSessionCountChange(ClientSession session, int increment) {
		int oldCount, newCount;
		final String user;
		
		try {
			user = session.getUsername();
		} catch (UserNotFoundException e) {
			Log.error("should not happen, couldn't look up username in " + PresenceMonitor.class.getCanonicalName(), e);
			// shouldn't happen afaik since we are only called on non-anon sessions, but don't throw from here
			return;
		}
		
		Guid userGuid;
		try {
			userGuid = Guid.parseJabberId(user);
		} catch (ParseException e) {
			Log.error("Can't parse username as a guid");
			return;
		}
		
		// TODO remove this later when we don't have a special admin user
		if (user.equals("admin"))
			return;
		
		Log.debug("User '" + user + "' session count incrementing by " + increment);
		
		synchronized (userInfo) {			
			UserInfo info = userInfo.get(user);
			if (info == null) {
				info = new UserInfo();
				userInfo.put(user, info);
			}
			
			oldCount = info.sessionCount;
			newCount = oldCount + increment;
			if (newCount < 0) {
				Log.error("Bug! decremented user session count below 0, old count " + oldCount + " increment " + increment);
				newCount = 0; // "fix it"
			}
			Log.debug("User " + user + " now has " + newCount + " sessions was " + oldCount);
		
			info.sessionCount = newCount;
			
			PresenceService presenceService = PresenceService.getInstance();
			
			// We queue this stuff so we aren't invoking some database operation in jboss
			// with the lock held and blocking all new clients in the meantime
			if (oldCount > 0 && newCount == 0) {
				presenceService.setLocalPresence("/users", userGuid, 0);
			} else if (oldCount == 0 && newCount > 0) {
				presenceService.setLocalPresence("/users", userGuid, 1);
			}
			
			// We potentially do lots of work when a resource connects, including sending
			// messages to that user, so we don't want to block the user connecting and possibly 
			// cause reentrancy issues.
			if (newCount > oldCount) {
				executor.execute(new Runnable() {
					public void run() {
						MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
						glue.onResourceConnected(user);
					}
				});
			}
		}
	}
	
	
	public void onClientSessionAvailable(ClientSession session) {
		Log.debug("Client session now available: " + session.getStreamID());
		onSessionCountChange(session, 1);
	}

	public void onClientSessionUnavailable(ClientSession session) {
		Log.debug("Client session now unavailable: " + session.getStreamID());
		onSessionCountChange(session, -1);
	}
}
