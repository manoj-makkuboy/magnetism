package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Stateless
public class PostingBoardBean implements PostingBoard {

	private static final Log logger = GlobalSetup.getLog(PostingBoardBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;	
	
	@EJB
	private IdentitySpider identitySpider;	
	
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private MessageSender messageSender;

	@EJB
	private Configuration configuration;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	private void sendPostNotifications(Post post, Set<Person> expandedRecipients) {
		// FIXME I suspect this should be outside the transaction and asynchronous
		logger.debug("Sending out jabber/email notifications...");
		for (Person r : expandedRecipients) {
			messageSender.sendPostNotification(r, post);
		}
	}
	
	public Post doLinkPost(Person poster, PostVisibility visibility, String title, String text, String url, Set<String> recipientGuids) throws ParseException, GuidNotFoundException {
		Set<Resource> shared = (Collections.singleton((Resource) identitySpider.getLink(url)));
		
		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(GuidPersistable.class, recipientGuids);

		// for each recipient, if it's a group we want to explode it into persons
		// (but also keep the group itself), if it's a person we just add it
		
		Set<Person> personRecipients = new HashSet<Person>();
		Set<Group> groupRecipients = new HashSet<Group>();
		Set<Person> expandedRecipients = new HashSet<Person>();
		
		// sort into persons and groups
		for (GuidPersistable r : recipients) {
			if (r instanceof Person) {
				personRecipients.add((Person) r);
			} else if (r instanceof Group) {
				groupRecipients.add((Group) r);
			} else {
				// wtf
				throw new GuidNotFoundException(r.getId());
			}
		}
		
		// build expanded recipients
		expandedRecipients.addAll(personRecipients);
		for (Group g : groupRecipients) {
			Set<Person> members = g.getMembers();
			expandedRecipients.addAll(members);
		}
		
		// if this throws we shouldn't send out notifications, so do it first
		Post post = createPostViaProxy(poster, visibility, title, text, shared, personRecipients, groupRecipients, expandedRecipients);
		
		sendPostNotifications(post, expandedRecipients);

		return post;
	}
	
	public void doShareLinkTutorialPost(Person recipient) {

		Person poster = identitySpider.getTheMan();
		LinkResource link = identitySpider.getLink(configuration.getProperty(HippoProperty.BASEURL) + "/tutorial");
		Set<Group> emptyGroups = Collections.emptySet();
		Set<Person> recipientSet = Collections.singleton(recipient);

		Post post = createPostViaProxy(poster, PostVisibility.RECIPIENTS_ONLY, "What is this DumbHippo thing?",
				"Learn to use DumbHippo by visiting this link", Collections.singleton((Resource) link), recipientSet, emptyGroups, recipientSet);

		sendPostNotifications(post, recipientSet);
	}
	
	private Post createPostViaProxy(Person poster, PostVisibility visibility, String title, String text, Set<Resource> resources, Set<Person> personRecipients, Set<Group> groupRecipients, Set<Person> expandedRecipients) {
		PostingBoard proxy = (PostingBoard) ejbContext.lookup(PostingBoard.class.getCanonicalName());
		
		return proxy.createPost(poster, visibility, title, text, resources, personRecipients, groupRecipients, expandedRecipients);
	}
	
	// internal function that is public only because of TransactionAttribute; use createPostViaProxy
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Post createPost(Person poster, PostVisibility visibility, String title, String text, Set<Resource> resources, Set<Person> personRecipients, Set<Group> groupRecipients, Set<Person> expandedRecipients) {
		
		logger.debug("saving new Post");
		Post post = new Post(poster, visibility, title, text, personRecipients, groupRecipients, expandedRecipients, resources);
		em.persist(post);
	
		return post;
	}

	private PersonPostData getPersonPostData(Person viewer, Post post) {
		if (viewer == null)
			return null;
		
		Query q = em.createQuery("SELECT ppd FROM PersonPostData ppd " +
				                 "WHERE ppd.post = :post AND ppd.person = :viewer");
		q.setParameter("post", post);
		q.setParameter("viewer", viewer);
		try {
			return (PersonPostData) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	// Hibernate bug: I think we should be able to write
	// EXISTS (SELECT group FROM IN(post.groupRecipients) group WHERE
	//         :viewer MEMBER of group.MEMBERS)
	// according to the EJB3 persistance spec, but that results in
	// garbage SQL
	
	static final String CAN_VIEW = 
		" (post.visibility = " + PostVisibility.ATTRIBUTED_PUBLIC.ordinal() + " OR " + 
              ":viewer MEMBER OF post.personRecipients OR " +
              "EXISTS (SELECT g FROM Group g WHERE " +
                         "g MEMBER OF post.groupRecipients AND " + 
                         "(g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() + " OR " +
                           ":viewer MEMBER OF g.members)))";
	
	static final String ORDER_RECENT = " ORDER BY post.postDate DESC ";
	
	private List<PostInfo> getPostInfos(Query q, Person viewer, int max) {
		if (max > 0)
			q.setMaxResults(max);

		@SuppressWarnings("unchecked")		
		List<Post> posts = q.getResultList();	
		
		List<PostInfo> result = new ArrayList<PostInfo>();
		for (Post p : posts) {
			PersonPostData ppd = getPersonPostData(viewer, p);				
			result.add(new PostInfo(identitySpider, viewer, p, ppd));
		}
		
		return result;		
	}
	
	public List<PostInfo> getPostInfosFor(Person poster, Person viewer, int max) {
		Query q;
		q = em.createQuery("SELECT post FROM Post post " +
				           "WHERE post.poster = :poster AND " +
				           CAN_VIEW + ORDER_RECENT);
		
		q.setParameter("poster", poster);
		q.setParameter("viewer", viewer);
		
		return getPostInfos(q, viewer, max);
	}
	
	public List<PostInfo> getReceivedPostInfos(Person recipient, int max) {
		// There's an efficiency win here by specializing to the case where
		// viewer == recipient ... we know that posts are always visible
		// to the recipient
		
		Query q;
		q = em.createQuery("SELECT post FROM Post post " +
				           "WHERE :recipient MEMBER OF post.expandedRecipients " +
				           ORDER_RECENT);
		
		q.setParameter("recipient", recipient);

		return getPostInfos(q, recipient, max);
	}
	
	public List<PostInfo> getGroupPostInfos(Group recipient, Person viewer, int max) {
		Query q;
		q = em.createQuery("SELECT post FROM Post post " +
				           "WHERE :recipient MEMBER OF post.groupRecipients AND " +
				           CAN_VIEW + ORDER_RECENT);
		
		q.setParameter("recipient", recipient);
		q.setParameter("viewer", viewer);
		
		return getPostInfos(q, viewer, max);
	}
	
	public List<PostInfo> getContactPostInfos(Person viewer, boolean include_received, int max) {
		List<PostInfo> results = new ArrayList<PostInfo>();
		
		HippoAccount account = accountSystem.lookupAccountByPerson(viewer); 
		if (account == null)
			return results; // return an empty list
		
		String recipient_clause = include_received ? "" : "NOT :viewer MEMBER OF post.expandedRecipients AND "; 

		Query q;
		q = em.createQuery("SELECT post FROM HippoAccount account, Post post " + 
						   "WHERE account = :account AND " +
				               "post.poster MEMBER OF account.contacts AND " +
				                recipient_clause +
				                CAN_VIEW + ORDER_RECENT);
		
		q.setParameter("account", account);
		q.setParameter("viewer", viewer);		
		
		return getPostInfos(q, viewer, max);
	}
	
	public Post loadPost(Guid guid) {
		return em.find(Post.class, guid.toString());
	}
	
	public PostInfo loadPostInfo(Guid guid, Person viewer) {
		Post p =  em.find(Post.class, guid.toString());
		PersonPostData ppd = getPersonPostData(viewer, p);
		// FIXME access control check here, when used from post framer?
		return (new PostInfo(identitySpider, viewer, p, ppd));
	}

	public void postClickedBy(Post post, Person clicker) {
		logger.debug("Post " + post + " clicked by " + clicker);
		messageSender.sendPostClickedNotification(post, clicker);
		// FIXME should be unique...
		PersonPostData postData = new PersonPostData(clicker, post);
		em.persist(postData);
	}
}
