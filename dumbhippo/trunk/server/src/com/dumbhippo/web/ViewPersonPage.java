package com.dumbhippo.web;

import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/**
 * Displays a list of posts from a person
 * 
 */

public class ViewPersonPage {
	static private final Log logger = GlobalSetup.getLog(ViewPersonPage.class);	
	
	private Person viewedPerson;
	private String viewedPersonId;

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private GroupSystem groupSystem;
	private PostingBoard postBoard;
	private PersonInfo personInfo;
	
	public ViewPersonPage() throws NamingException {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public List<PostInfo> getPostInfos() {
		assert viewedPerson != null;
		return postBoard.getPostInfosFor(viewedPerson, signin.getUser(), 0);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public String getViewedPersonId() {
		return viewedPersonId;
	}
	
	protected void setViewedPerson(Person person) {
		this.viewedPerson = person;
		this.viewedPersonId = person.getId();
		logger.debug("viewing person: " + this.viewedPersonId);
	}
	
	public String getName() {
		return viewedPerson.getName().toString();
	}

	public void setViewedPersonId(String personId) throws ParseException, GuidNotFoundException {
		if (personId == null) {
			logger.debug("no viewed person");
			return;
		} else {
			setViewedPerson(identitySpider.lookupGuidString(Person.class, personId));
		}
	}
	
	public PersonInfo getPersonInfo() {
		if (personInfo == null)
			personInfo = new PersonInfo(identitySpider, signin.getUser(), viewedPerson);
		
		return personInfo;
	}
	
	public boolean getIsContact() {
		if (signin.isValid())
			return identitySpider.isContact(signin.getUser(), viewedPerson);
		else
			return false;
	}
	
	public List<Group> getGroups() {
		// FIXME: We want to lock this down more and only show a subset of groups.
		
		return Group.sortedList(groupSystem.findGroups(viewedPerson));
	}
}
