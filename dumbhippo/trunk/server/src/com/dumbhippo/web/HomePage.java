package com.dumbhippo.web;

import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class HomePage {
	static private final Log logger = GlobalSetup.getLog(HomePage.class);
	
	private SigninBean signin;
	
	private Configuration configuration;
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonInfo personInfo;
	private GroupSystem groupSystem;
	
	public HomePage() throws NamingException {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
	}
	
	public List<PostInfo> getReceivedPostInfos() {
		logger.debug("Getting received posts for " + signin.getUser().getId());
		return postBoard.getReceivedPostInfos(signin.getUser(), 0);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public void setSignin(SigninBean signin) {
		this.signin = signin;
	}

	public PersonInfo getPersonInfo() {
		if (personInfo == null)
			personInfo = new PersonInfo(identitySpider, signin.getUser(), signin.getUser());
		
		return personInfo;
	}
	
	public List<Group> getGroups() {
		return Group.sortedList(groupSystem.findGroups(signin.getUser()));
	}
	
	public List<PersonInfo> getContacts() {
		return PersonInfo.sortedList(identitySpider.getContactInfos(signin.getUser(), signin.getUser()));
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
}
