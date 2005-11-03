package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RedirectException;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(HttpMethodsBean.class);
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	@EJB
	private GroupSystem groupSystem;

	@EJB
	private InvitationSystem invitationSystem;
	
	private void startReturnObjectsXml(HttpResponseData contentType, XmlBuilder xml) {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		xml.appendStandaloneFragmentHeader();
		
		xml.append("<objects>");
	}
	
	private void endReturnObjectsXml(OutputStream out, XmlBuilder xml) throws IOException {
		xml.append("</objects>");
		
		out.write(xml.toString().getBytes());
	}
	
	private void returnPersonsXml(XmlBuilder xml, Set<PersonView> persons) {
		if (persons != null) {
			for (PersonView p : persons) {
				EmailResource email = p.getEmail();
				if (email != null) {
					xml.appendTextNode("person", null, "id", p.getPerson().getId(), "display", p.getHumanReadableName(),
							"email", email.getEmail());
				} else {
					xml.appendTextNode("person", null, "id", p.getPerson().getId(), "display", p.getHumanReadableName());
				}
			}
		}
	}
	
	private void returnGroupsXml(XmlBuilder xml, Viewpoint viewpoint, Set<Group> groups) {
		if (groups != null) {
			for (Group g : groups) {
				
				// FIXME with the right database query we can avoid getting *all* the members to 
				// display just a few of them
				
				StringBuilder sampleMembers = new StringBuilder();
				Set<Person> members = g.getMembers();
				logger.debug(members.size() + " members of " + g.getName());
				for (Person p : members) {
					if (sampleMembers.length() > PersonView.MAX_SHORT_NAME_LENGTH * 5) {
						sampleMembers.append(" ...");
						break;
					} 
					
					if (sampleMembers.length() > 0)
						sampleMembers.append(" ");
				
					PersonView member = identitySpider.getPersonView(viewpoint, p);
					String shortName = member.getHumanReadableShortName();
					sampleMembers.append(shortName);
				}
				
				xml.appendTextNode("group", null, "id", g.getId(), "display", g.getName(), "sampleMembers", sampleMembers.toString());
			}
		}
	}
	
	private void returnObjects(OutputStream out, HttpResponseData contentType, Viewpoint viewpoint, Set<PersonView> persons, Set<Group> groups) throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);
		
		returnPersonsXml(xml, persons);
		returnGroupsXml(xml, viewpoint, groups);
		
		endReturnObjectsXml(out, xml);
	}

	public void getContactsAndGroups(OutputStream out, HttpResponseData contentType, Person user) throws IOException {
		Viewpoint viewpoint = new Viewpoint(user);
		
		Set<PersonView> persons = identitySpider.getContacts(viewpoint, user);
		Set<Group> groups = groupSystem.findGroups(viewpoint, user);
		
		returnObjects(out, contentType, viewpoint, persons, groups);
	}
	
	public void doCreateOrGetContact(OutputStream out, HttpResponseData contentType, Person user,
			String email) throws IOException {

		XmlBuilder xml = new XmlBuilder();
		Viewpoint viewpoint = new Viewpoint(user);

		startReturnObjectsXml(contentType, xml);

		EmailResource resource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(user, resource);
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact);
		returnPersonsXml(xml, Collections.singleton(contactView));
		
		endReturnObjectsXml(out, xml);
	}
	
	static private Set<String> splitIdList(String list) {
		Set<String> ret;
		
		// string.split returns a single empty string if the string we split is length 0, unfortunately
		if (list.length() > 0) {
			ret = new HashSet<String>(Arrays.asList(list.split(",")));
		} else {
			ret = Collections.emptySet();
		}
		
		return ret;
	}
	
	public void doShareLink(Person user, String title, String url, String recipientIds, String description, boolean secret) throws ParseException, GuidNotFoundException {
		Set<String> recipientGuids = splitIdList(recipientIds);

		PostVisibility visibility = secret ? PostVisibility.RECIPIENTS_ONLY : PostVisibility.ANONYMOUSLY_PUBLIC;
		postingBoard.doLinkPost(user, visibility, title, description, url, recipientGuids);
	}

	
	public void doRenamePerson(Person user, String name) {
		FullName fullname = FullName.parseHumanString(name);
		user.setName(fullname);
		em.merge(user);
	}
	
	public void doCreateGroup(OutputStream out, HttpResponseData contentType, Person user, String name, String members) throws IOException, ParseException, GuidNotFoundException {
				
		Set<String> memberGuids = splitIdList(members);
		
		Set<Person> memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		
		Group group = groupSystem.createGroup(user, name);
		group.addMember(user);
		group.addMembers(memberPeople);
		
		Viewpoint viewpoint = new Viewpoint(user);
		returnObjects(out, contentType, viewpoint, null, Collections.singleton(group));
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType, Person user, String email) throws IOException {
		EmailResource emailResource = identitySpider.getEmail(email);
		Person contact = identitySpider.createContact(user, emailResource);
		Viewpoint viewpoint = new Viewpoint(user);
		PersonView contactView = identitySpider.getPersonView(viewpoint, contact);

		returnObjects(out, contentType, viewpoint, Collections.singleton(contactView), null);
	}
	
	public void doJoinGroup(Person user, String groupId) {
		try {
			Group group = identitySpider.lookupGuidString(Group.class, groupId);
			groupSystem.addMember(user, group, user);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doLeaveGroup(Person user, String groupId) {
		try {
			Group group = identitySpider.lookupGuidString(Group.class, groupId);
			groupSystem.removeMember(user, group, user);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doAddContactPerson(Person user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class, contactId);
			identitySpider.addContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}
	
	public void doRemoveContactPerson(Person user, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class, contactId);
			identitySpider.removeContactPerson(user, contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (IdentitySpider.GuidNotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void handleRedirect(Person user, String url, String postId, String inviteKey) throws RedirectException {
		
		Invitation invitation = null;
		
		if (user == null && inviteKey != null) {
			invitation = invitationSystem.lookupInvitationByKey(inviteKey);
		}
		
		// FIXME obviously we should redirect you to login and then come back...
		if (user == null && invitation == null) {
			throw new RedirectException("Do you need to <a href=\"/home\">log in</a>?");
		}

		Post post;
		try {
			post = identitySpider.lookupGuidString(Post.class, postId);
		} catch (ParseException e) {
			throw new RedirectException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")");
		} catch (GuidNotFoundException e) {
			throw new RedirectException("Which post did you come from? (post's ID was \"" + XmlBuilder.escape(postId) + "\")");
		}
		
		if (user != null) {
			
		}
		
		if (user != null) {
			postingBoard.postClickedBy(post, user);
		} else {
			logger.debug("not yet handling a merely-invited person hitting the redirect page");
		}
	}
}
