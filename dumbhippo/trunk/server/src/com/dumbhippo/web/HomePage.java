package com.dumbhippo.web;

import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class HomePage extends AbstractSigninRequiredPage {
	static private final Logger logger = GlobalSetup.getLogger(HomePage.class);
	static private final int MAX_RECEIVED_POSTS_SHOWN = 4;
	static private final int MAX_CONTACTS_SHOWN = 9;
	
	private PostingBoard postBoard;
	private GroupSystem groupSystem;

	private ListBean<PostView> receivedPosts;
	private ListBean<PostView> contactPosts;
	private ListBean<GroupView> groups;
	
	private int totalContacts;

	public HomePage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		totalContacts = 0;
	}

	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getUserSignin().getUser());
			// + 1 as a marker for whether there are more
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(getUserSignin().getViewpoint(), getUserSignin().getUser(), 0, MAX_RECEIVED_POSTS_SHOWN + 1));
		}
		return receivedPosts;
	}
	
	public ListBean<GroupView> getGroups() {
		if (groups == null) {
			groups = new ListBean<GroupView>(GroupView.sortedList(groupSystem.findGroups(getUserSignin().getViewpoint(), getUserSignin().getUser())));
		}
		return groups;
	}
	
	public ListBean<PostView> getContactPosts() {
		if (contactPosts == null) {
			contactPosts = new ListBean<PostView>(postBoard.getContactPosts(getUserSignin().getViewpoint(), getUserSignin().getUser(), false, 0, 0));
		}
		return contactPosts;
	}
	
	public int getMaxReceivedPostsShown() {
		return MAX_RECEIVED_POSTS_SHOWN;
	}
	
	/**
	 * Get a set of contacts of the signed in user that we want to display on user's homepage.
	 * 
	 * @return a list of PersonViews of a subset of contacts
	 */
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			Set<PersonView> mingledContacts = 
				identitySpider.getContacts(getUserSignin().getViewpoint(), getUserSignin().getUser(), 
						                   false, PersonViewExtra.INVITED_STATUS, 
						                   PersonViewExtra.PRIMARY_EMAIL, 
						                   PersonViewExtra.PRIMARY_AIM);
			contacts = new ListBean<PersonView>(PersonView.sortedList(mingledContacts,
					                                                  1, MAX_CONTACTS_SHOWN, 
					                                                  1, 1));
			
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
}
