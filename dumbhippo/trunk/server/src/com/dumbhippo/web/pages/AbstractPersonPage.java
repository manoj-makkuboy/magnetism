package com.dumbhippo.web.pages;

import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public abstract class AbstractPersonPage extends AbstractSigninOptionalPage {
	static private final Logger logger = GlobalSetup.getLogger(AbstractPersonPage.class);	
	
	private User viewedUser;
	private String viewedUserId;
	private boolean disabled;
	
	private GroupSystem groupSystem;
	private MusicSystem musicSystem;
	private PersonView viewedPerson;
	
	private ListBean<GroupView> groups;
	private ListBean<GroupView> invitedGroups;
	
	private boolean lookedUpCurrentTrack;
	private TrackView currentTrack;
	
	protected int totalContacts;
	
	protected AbstractPersonPage() {	
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		lookedUpCurrentTrack = false;
	}
	
	protected IdentitySpider getIdentitySpider() { 
		return identitySpider;
	}
	
	protected GroupSystem getGroupSystem() {
		return groupSystem;
	}
	
	protected MusicSystem getMusicSystem() {
		return musicSystem;
	}
 	
	public String getViewedUserId() {
		return viewedUserId;
	}
	
	public User getViewedUser() {
		return viewedUser;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	protected void setViewedUser(User user) {
		this.viewedUser = user;
		this.viewedUserId = user.getId();
		
		if (identitySpider.getAccountDisabled(user)) {
				this.disabled = true;
		}
		
		logger.debug("viewing person: {} disabled = {}", this.viewedUser, disabled);
	}
	
	public String getName() {
		return getViewedUser().getNickname();
	}

	public void setViewedUserId(String userId) {
		if (userId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			try {
				setViewedUser(identitySpider.lookupGuidString(User.class, userId));
			} catch (ParseException e) {
				logger.debug("bad userId as person parameter {}", userId);
			} catch (NotFoundException e) {
				logger.debug("bad userId as person parameter {}", userId);
			}
		}
	}

	public PersonView getViewedPerson() {
		if (viewedPerson == null)
			viewedPerson = identitySpider.getPersonView(getSignin().getViewpoint(), getViewedUser(), PersonViewExtra.ALL_RESOURCES);
		
		return viewedPerson;
	}
	
	public boolean isContact() {
		if (getSignin().isValid())
			return identitySpider.isContact(getSignin().getViewpoint(), getUserSignin().getUser(), getViewedUser());
		else
			return false;
	}
	
	public boolean isSelf() {
		if (getSignin().isValid() && getViewedUser() != null) {
			return getUserSignin().getUser().equals(getViewedUser());
		} else {
			return false;
		}
	}
	
	public boolean isValid() {
		return getViewedUser() != null;
	}
	
	// We don't show group's you haven't accepted the invitation for on your public page
	public ListBean<GroupView> getGroups() {
		if (groups == null) {
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getSignin().getViewpoint(), getViewedUser(), MembershipStatus.ACTIVE)));
		}
		return groups;
	}	
	
	public ListBean<GroupView> getInvitedGroups() {
		// Only the user can see their own invited groups
		if (!isSelf())
			return null;
		if (invitedGroups == null) {
			invitedGroups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getSignin().getViewpoint(), getViewedUser(), MembershipStatus.INVITED)));
		}
		return invitedGroups;
	}		
	
	public boolean isNewGroupInvites() {
		if (!isSelf())
			return false;
		Account acct = getViewedUser().getAccount();
		Date groupInvitationReceived = acct.getGroupInvitationReceived();
		Date lastSeenInvitation = acct.getLastSeenGroupInvitations();
		if (groupInvitationReceived == null)
			return false;
		if (lastSeenInvitation == null)
			return true;
		return groupInvitationReceived.compareTo(lastSeenInvitation) > 0;
	}
	
	/**
	 * Get a set of contacts of the viewed user that we want to display on the person page.
	 * 
	 * @return a list of PersonViews of a subset of contacts
	 */
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				identitySpider.getContacts(getSignin().getViewpoint(), getViewedUser(), 
						                   false, PersonViewExtra.INVITED_STATUS, 
						                   PersonViewExtra.PRIMARY_EMAIL, 
						                   PersonViewExtra.PRIMARY_AIM);		
			contacts = new ListBean<PersonView>(PersonView.sortedList(getSignin().getViewpoint(), getViewedUser(), mingledContacts));
			
			totalContacts = mingledContacts.size();
		}
		return contacts;
	}
	
	public void setTotalContacts(int totalContacts) {		
	    this.totalContacts = totalContacts;
	}
	
	public int getTotalContacts() {
		if (contacts == null) {
			getContacts();
		}
		
		return totalContacts;
	}
	
	public TrackView getCurrentTrack() {
		if (!lookedUpCurrentTrack) {
			try {
			lookedUpCurrentTrack = true;
				currentTrack = getMusicSystem().getCurrentTrackView(getSignin().getViewpoint(), getViewedUser());
			} catch (NotFoundException e) {
			}
		}
		return currentTrack;
	}
}
