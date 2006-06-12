package com.dumbhippo.web.pages;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.web.WebEJBUtil;

public class AdminPage extends AbstractSigninRequiredPage {

	protected static final Logger logger = GlobalSetup.getLogger(AdminPage.class);

	private Configuration config;
	
	private LiveState liveState;
	
	private InvitationSystem invitationSystem;

	private IdentitySpider identitySpider;
	
	private Set<PersonView> availableLiveUsers;
	private Set<PersonView> cachedLiveUsers;
	private Set<LivePost> livePosts;
 	private List<PersonView> users;
	
	private int systemInvitations;
	private int totalInvitations;
	private long numberOfAccounts;
	
	public AdminPage() throws HumanVisibleException {
		super();
		liveState = LiveState.getInstance();		
		config = WebEJBUtil.defaultLookup(Configuration.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		String isAdminEnabled = config.getProperty(HippoProperty.ENABLE_ADMIN_CONSOLE);
		logger.debug("admin console enabled: {}", isAdminEnabled);
		if (!isAdminEnabled.equals("true"))
			throw new HumanVisibleException("Administrator console not enabled");
		
		systemInvitations = -1;
		totalInvitations = -1;
		numberOfAccounts = -1;
	}
	
	public boolean isValid() throws HumanVisibleException {
		PersonView person = getPerson();
		return identitySpider.isAdministrator(person.getUser());
	}
	
	private Set<PersonView> liveUserSetToPersonView(Set<LiveUser> lusers) {
		Set<PersonView> result = new HashSet<PersonView>();

		for (LiveUser luser : lusers) {
			User user = identitySpider.lookupUser(luser);
			result.add(identitySpider.getSystemView(user));					
		}
		return result;		
	}

	public Set<PersonView> getCachedLiveUsers() {
		if (cachedLiveUsers == null)
			cachedLiveUsers = liveUserSetToPersonView(liveState.getLiveUserCacheSnapshot());
		return cachedLiveUsers;
	}
	
	public int getCachedLiveUsersCount() {
		return getCachedLiveUsers().size();
	}
	
	public Set<PersonView> getAvailableLiveUsers() {
		if (availableLiveUsers == null)
			availableLiveUsers = liveUserSetToPersonView(liveState.getLiveUserAvailableSnapshot());
		return availableLiveUsers;
	}	

	public int getAvailableLiveUsersCount() {
		return getAvailableLiveUsers().size();
	}
	
	public Set<LivePost> getLivePosts() {
		if (livePosts == null)
			livePosts = liveState.getLivePostSnapshot();
		return livePosts;
	}
	
	public int getLivePostsCount() {
		return getLivePosts().size();
	}
	
	public int getSystemInvitations() {
		if (systemInvitations < 0)
			systemInvitations = invitationSystem.getSystemInvitationCount(getUserSignin().getViewpoint());
		return systemInvitations;
	}
	
	public int getUserInvitations() {
		return getTotalInvitations() - getSystemInvitations();
	}
	
	public int getTotalInvitations() {
		if (totalInvitations < 0)
			totalInvitations = invitationSystem.getTotalInvitationCount(getUserSignin().getViewpoint());
		return totalInvitations;
	}
	
	public long getNumberOfAccounts() {
		if (numberOfAccounts < 0)
			numberOfAccounts = identitySpider.getNumberOfActiveAccounts(getUserSignin().getViewpoint());
		return numberOfAccounts;
	}
 	
 	public List<PersonView> getAllUsers() {
 		if (users == null) {
 			Set<PersonView> userSet = identitySpider.getAllUsers(getUserSignin().getViewpoint()); 
 			users = PersonView.sortedList(userSet);
 		}
 		return users;
 	}
}
