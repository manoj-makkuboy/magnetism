package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.Parser;
import bsh.TokenMgrError;

import com.dumbhippo.BeanUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.RssBuilder;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveGroup;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupDescriptionChangedRevision;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupNameChangedRevision;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserNameChangedRevision;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.search.SearchSystem;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AmazonUpdater;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatSystem;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.NowPlayingThemeSystem;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.RevisionControl;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.server.SigninSystem;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.applications.AppinfoUploadView;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationView;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.TitleBlockView;
import com.dumbhippo.server.blocks.TitleDescriptionBlockView;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.ExternalAccountDMO;
import com.dumbhippo.server.dm.ExternalAccountKey;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.EntityView;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonWebServices;
import com.dumbhippo.services.FlickrUser;
import com.dumbhippo.services.FlickrWebServices;
import com.dumbhippo.services.LastFmWebServices;
import com.dumbhippo.services.MySpaceScraper;
import com.dumbhippo.services.TransientServiceException;
import com.dumbhippo.statistics.ColumnDescription;
import com.dumbhippo.statistics.ColumnMap;
import com.dumbhippo.statistics.Row;
import com.dumbhippo.statistics.StatisticsService;
import com.dumbhippo.statistics.StatisticsSet;
import com.dumbhippo.statistics.Timescale;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class HttpMethodsBean implements HttpMethods, Serializable {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup
			.getLogger(HttpMethodsBean.class);

	private static final long serialVersionUID = 0L;

	// how long to wait on the web services calls
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private ApplicationSystem applicationSystem;
	
	@EJB
	private ChatSystem chatSystem;

	@EJB
	private GroupSystem groupSystem;

	@EJB
	private SigninSystem signinSystem;

	@EJB
	private MusicSystem musicSystem;

	@EJB
	private NowPlayingThemeSystem nowPlayingSystem;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB 
	private WantsInSystem wantsInSystem;
	
	@EJB
	private ClaimVerifier claimVerifier;
	
	@EJB
	private Configuration config;
	
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private SearchSystem searchSystem;
	
	@EJB
	private SharedFileSystem sharedFileSystem;
	
	@EJB
	private Stacker stacker;
	
	@EJB
	private FacebookSystem facebookSystem;

	@EJB
	private FacebookTracker facebookTracker;
	
	@EJB
	private RevisionControl revisionControl;
	
	@EJB
	private PollingTaskPersistence pollingPersistence;
	
	@EJB 
	private FeedSystem feedSystem;
	
	@EJB
	private AmazonUpdater amazonUpdater;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	private void startReturnObjectsXml(HttpResponseData contentType,
			XmlBuilder xml) {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		xml.appendStandaloneFragmentHeader();

		xml.append("<objects>");
	}

	private void endReturnObjectsXml(OutputStream out, XmlBuilder xml)
			throws IOException {
		xml.append("</objects>");
		out.write(xml.getBytes());
	}

	private void returnPersonsXml(XmlBuilder xml, Viewpoint viewpoint,
			Collection<PersonView> persons) {
		if (persons != null) {
			for (PersonView p : persons) {

				StringBuilder sb = new StringBuilder();

				String emailsStr = null;
				Collection<EmailResource> emails = p.getAllEmails();

				for (EmailResource e : emails) {
					sb.append(e.getEmail());
					sb.append(",");
				}
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
					emailsStr = sb.toString();
				}

				sb.setLength(0);

				String aimsStr = null;
				Collection<AimResource> aims = p.getAllAims();
				for (AimResource a : aims) {
					sb.append(a.getScreenName());
					sb.append(",");
				}
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
					aimsStr = sb.toString();
				}

				EmailResource primaryEmail = p.getEmail();
				AimResource primaryAim = p.getAim();

				String hasAccount = p.getUser() != null ? "true" : "false";

				String display = p.getName();
				if (p.getUser() != null && viewpoint.isOfUser(p.getUser())) {
					display = display + " (myself)";
				}

				// Note: we do not want to use p.getIdentifyingGuid() for "id"
				// by default here, because there are places in javascript that 
				// depend on the contact id to be returned for the id field, 
				// and not the resource id that is returned by p.getIdentifyingGuid()
				// (at least groupinvitation.js for inviteeId is one such place)
				xml.appendTextNode("person", null, "id",
                        p.getPersonIdentifyingGuid() != null ? 
                        p.getPersonIdentifyingGuid().toString() : p.getIdentifyingGuid().toString(), 
						"contactId", p.getContact() != null ? p.getContact().getId() : "",
						"userId", p.getUser() != null ? p.getUser().getId()
								: "", "display", display, "hasAccount",
						hasAccount, "email",
						primaryEmail != null ? primaryEmail.getEmail() : null,
						"aim", primaryAim != null ? primaryAim.getScreenName()
								: null, "emails", emailsStr, "aims", aimsStr,
						"photoUrl", p.getPhotoUrl());
			}
		}
	}

	private void returnGroupsXml(XmlBuilder xml, Viewpoint viewpoint,
			Collection<Group> groups) {
		if (groups != null) {
			for (Group g : groups) {
				LiveGroup liveGroup = LiveState.getInstance().getLiveGroup(g.getGuid());
				
				xml.appendTextNode("group", null, "id", g.getId(), 
						"display", g.getName(), 
						"photoUrl", g.getPhotoUrl(),
						"memberCount", Integer.toString(liveGroup.getMemberCount()),
						"isPublic", Boolean.toString(g.isPublic()));
			}
		}
	}

	private void returnObjects(OutputStream out, HttpResponseData contentType,
			Viewpoint viewpoint, Collection<PersonView> persons, Collection<Group> groups)
			throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);

		if (persons != null)
			returnPersonsXml(xml, viewpoint, persons);
		if (groups != null)
			returnGroupsXml(xml, viewpoint, groups);
 
		endReturnObjectsXml(out, xml);
	}

	private void throwIfUrlNotHttp(URL url) throws XmlMethodException {
		if (!(url.getProtocol().equals("http") || url.getProtocol().equals("https")))
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "URL must be http or https: '" + url.toExternalForm() + "'");
	}
	
	// FIXME if we change doShareLink to be an "XMLMETHOD" then this can throw XmlMethodException directly
	// FIXME this method is deprecated; just make your method take an URL then use throwIfUrlNotHttp as required
	private URL parseUserEnteredUrl(String url, boolean httpOnly) throws MalformedURLException {
		url = url.trim();
		URL urlObject;
		try {
			urlObject = new URL(url);
		} catch (MalformedURLException e) {
			if (!url.startsWith("http://")) {
				// let users type just "example.com" instead of "http://example.com"
				return parseUserEnteredUrl("http://" + url, httpOnly);	
			} else {
				throw e;
			}
		}
		if (httpOnly && !(urlObject.getProtocol().equals("http") || urlObject.getProtocol().equals("https")))
			throw new MalformedURLException("Invalid protocol in url " + url);
		return urlObject;
	}
	
	public void getAddableContacts(OutputStream out,
			HttpResponseData contentType, UserViewpoint viewpoint, Group group, String inviteeId)
			throws IOException {
		Set<PersonView> persons = groupSystem.findAddableContacts(viewpoint, viewpoint.getViewer(), group);

		if (inviteeId != null && inviteeId.trim().length() > 0) {
			try {
				Person person = identitySpider.lookupGuidString(Person.class, inviteeId);
				PersonView personView = personViewer.getPersonView(viewpoint, person);
				persons.add(personView);
			} catch (ParseException e) {
				writeMessageReply(out, "getAddableContactsReply", "Supplied invitee was not found.");
				logger.warn("bad invitee guid", e);
				return;
			} catch (NotFoundException e) {
				writeMessageReply(out, "getAddableContactsReply", "Supplied invitee was not found.");
				logger.warn("no such invitee guid", e);
				return;
			}
		}
		returnObjects(out, contentType, viewpoint, persons, null);
	}

	public void getContactsAndGroups(OutputStream out,
			HttpResponseData contentType, UserViewpoint viewpoint) throws IOException {

		List<PersonView> persons = personViewer.getContacts(viewpoint, viewpoint.getViewer(),
				0, -1);
		// Add the user themself to the list of returned contacts (whether or not the
		// viewer is in their own contact list getContacts() strips it out.)
		persons.add(personViewer.getPersonView(viewpoint, viewpoint.getViewer()));
		Set<Group> groups = groupSystem.findRawGroups(viewpoint, viewpoint.getViewer());

		returnObjects(out, contentType, viewpoint, persons, groups);
	}

	public void doCreateOrGetContact(OutputStream out,
			HttpResponseData contentType, UserViewpoint viewpoint, EmailResource email)
			throws IOException {
		XmlBuilder xml = new XmlBuilder();

		startReturnObjectsXml(contentType, xml);

		Person contact = identitySpider.createContact(viewpoint.getViewer(), email);
		PersonView contactView = personViewer.getPersonView(viewpoint, contact);
		returnPersonsXml(xml, viewpoint, Collections.singleton(contactView));

		endReturnObjectsXml(out, xml);
	}

	static private Set<String> splitIdList(String list) {
		Set<String> ret;

		// string.split returns a single empty string if the string we split is
		// length 0, unfortunately
		if (list.length() > 0) {
			ret = new HashSet<String>(Arrays.asList(list.split(",")));
		} else {
			ret = Collections.emptySet();
		}

		return ret;
	}

	public void doShareLink(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String title, String url,
			String recipientIds, String description, boolean isPublic,
			String postInfoXml) throws ParseException, NotFoundException,
			SAXException, MalformedURLException, IOException {
		Set<String> recipientGuids = splitIdList(recipientIds);

		PostInfo info;
		if (postInfoXml != null)
			info = PostInfo.parse(postInfoXml);
		else
			info = null;

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		URL urlObject = parseUserEnteredUrl(url, true);

		Post post = postingBoard.doLinkPost(viewpoint.getViewer(), isPublic, title, description,
							urlObject, recipients, PostingBoard.InviteRecipients.DONT_INVITE, info);
		XmlBuilder xml = new XmlBuilder();
		xml.openElement("post", "id", post.getId());
		xml.closeElement();
		out.write(xml.getBytes());
	}

	public void doShareGroup(UserViewpoint viewpoint, Group group, String recipientIds,
			String description) throws ParseException, NotFoundException {

		Set<String> recipientGuids = splitIdList(recipientIds);

		// this is what can throw ParseException
		Set<GuidPersistable> recipients = identitySpider.lookupGuidStrings(
				GuidPersistable.class, recipientGuids);

		postingBoard.doShareGroupPost(viewpoint.getViewer(), group, null, description, recipients,
				PostingBoard.InviteRecipients.MUST_INVITE);
	}

	public void doRenamePerson(UserViewpoint viewpoint, String name) {
		name = name.trim();
		viewpoint.getViewer().setNickname(name);
		DataService.currentSessionRW().changed(UserDMO.class, viewpoint.getViewer().getGuid(), "name");
		revisionControl.persistRevision(new UserNameChangedRevision(viewpoint.getViewer(), new Date(), name));
	}

	public void doCreateGroup(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, String name, String members, boolean secret, boolean open, String description)
			throws IOException, ParseException, NotFoundException {
		Set<Person> memberPeople;
		
		if (members != null) {
			Set<String> memberGuids = splitIdList(members);
			memberPeople = identitySpider.lookupGuidStrings(Person.class, memberGuids);
		} else {
			memberPeople = Collections.emptySet();
		}

		Group group = 
			groupSystem.createGroup(viewpoint.getViewer(), name,
			 	                    secret ? GroupAccess.SECRET : open ? GroupAccess.PUBLIC : GroupAccess.PUBLIC_INVITE,
			 	                   	description);
		for (Person p : memberPeople)
			groupSystem.addMember(viewpoint.getViewer(), group, p);

		returnObjects(out, contentType, viewpoint, null, Collections
				.singleton(group));
	}

	public void doAddMembers(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, Group group, String memberIds) throws IOException,
			ParseException, NotFoundException {
		Set<String> memberGuids = splitIdList(memberIds);
        Set<Person> memberPeople = new HashSet<Person>();
        Set<Resource> memberResources = new HashSet<Resource>();
		
		for (String memberGuid : memberGuids) {
		    try {
		        memberPeople.add(identitySpider.lookupGuidString(Person.class, memberGuid));
		    } catch (ParseException e) {
			    throw new RuntimeException("Bad Guid", e);
	  	    } catch (NotFoundException e) {
			    // when the person that is being invited is not a user and
			    // is not a viewer's contact, what we'll get here is a
			    // Resource Guid
		        memberResources.add(identitySpider.lookupGuidString(Resource.class, memberGuid));	
	  	    }
		}
		    
		for (Person p : memberPeople)
			groupSystem.addMember(viewpoint.getViewer(), group, p);
		for (Resource r : memberResources)
			groupSystem.addMember(viewpoint.getViewer(), group, r);
		
		returnObjects(out, contentType, viewpoint, null, Collections
				.singleton(group));
	}

	public void doAddContact(OutputStream out, HttpResponseData contentType,
			UserViewpoint viewpoint, EmailResource email) throws IOException {
		Contact contact = identitySpider.createContact(viewpoint.getViewer(), email);
		PersonView contactView = personViewer.getPersonView(viewpoint, contact);

		returnObjects(out, contentType, viewpoint, Collections
				.singleton(contactView), null);
	}

	public void doJoinGroup(UserViewpoint viewpoint, Group group) {
		groupSystem.addMember(viewpoint.getViewer(), group, viewpoint.getViewer());
	}

	public void doLeaveGroup(UserViewpoint viewpoint, Group group) {
		groupSystem.removeMember(viewpoint.getViewer(), group, viewpoint.getViewer());
	}

	public void doSetGroupMembershipPolicy(UserViewpoint viewpoint, Group group, boolean open) {
		groupSystem.reviseGroupMembershipPolicy(viewpoint.getViewer(), group, open);		
	}
	
	public void doRenameGroup(UserViewpoint viewpoint, Group group, String name) {
		if (!groupSystem.canEditGroup(viewpoint, group))
			throw new RuntimeException("Only active members can edit a group");
					
		group.setName(name);
		revisionControl.persistRevision(new GroupNameChangedRevision(viewpoint.getViewer(), group, new Date(), name));
	}
	
	public void doSetGroupDescription(UserViewpoint viewpoint, Group group, String description) {
		if (!groupSystem.canEditGroup(viewpoint, group))
			throw new RuntimeException("Only active members can edit a group");
		
		description = description.trim();
		
		group.setDescription(description);
		revisionControl.persistRevision(new GroupDescriptionChangedRevision(viewpoint.getViewer(), group, new Date(), description));
	}
	
	public void doSetGroupStockPhoto(UserViewpoint viewpoint, Group group, String photo) {
		groupSystem.setStockPhoto(viewpoint, group, photo);
	}

	public void doAddContactPerson(UserViewpoint viewpoint, String contactId) {
		try {
			Person contact = identitySpider.lookupGuidString(Person.class,
					contactId);
			identitySpider.addContactPerson(viewpoint.getViewer(), contact);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		}
	}

	public void doRemoveContactObject(UserViewpoint viewpoint, String contactObjectId) {
		try {
			GuidPersistable object = 
				identitySpider.lookupGuidString(GuidPersistable.class, contactObjectId);
			if (object instanceof Person) {
			    identitySpider.removeContactPerson(viewpoint.getViewer(), (Person)object);
			} else if (object instanceof Resource) {
				identitySpider.removeContactResource(viewpoint.getViewer(), (Resource)object);				
			} else {
				throw new RuntimeException("GuidPersistable for " + contactObjectId + " is neither Person nor Resource");		
		    }
		} catch (NotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		}
	}

	public void doRemoveInvitedContact(UserViewpoint viewpoint, String resourceId) {		
		try {
		    Resource resource = identitySpider.lookupGuidString(Resource.class, resourceId);	
		    // remove groups we've invited this person to
		    // we are using SystemViewpoint, because we would not be able to SEE_GROUP 
		    // if have removed yourself from it, though still want to remove an invitation
		    for (GroupMember groupMember : groupSystem.findGroups(SystemViewpoint.getInstance(), resource)) {
		    	if (groupSystem.canRemoveInvitation(viewpoint.getViewer(), groupMember))
		    	    groupSystem.removeMember(viewpoint.getViewer(), groupMember);
		    }
		    invitationSystem.deleteInvitations(viewpoint, resource);
		    identitySpider.removeContactResource(viewpoint.getViewer(), resource);	
		} catch (NotFoundException e) {
			throw new RuntimeException("Guid not found", e);
		} catch (ParseException e) {
			throw new RuntimeException("Bad Guid", e);
		}    
	}
	
	public void doSendLoginLinkEmail(XmlBuilder xml, String address) throws IOException, HumanVisibleException, RetryException {
		signinSystem.sendSigninLinkEmail(address);
	}

	public void doSendClaimLinkEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException, RetryException {
		claimVerifier.sendClaimVerifierLink(viewpoint, viewpoint.getViewer(), address);
	}
	
	public void doSendClaimLinkAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException, RetryException {
		claimVerifier.sendClaimVerifierLink(viewpoint, viewpoint.getViewer(), address);
	}

	public void doRemoveClaimEmail(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		Resource resource;
		try {
			resource = identitySpider.lookupEmail(address);
		} catch (NotFoundException e) {
			return; // doesn't exist anyhow
		}
		identitySpider.removeVerifiedOwnershipClaim(viewpoint, viewpoint.getViewer(), resource);
	}
	
	public void doDisableFacebookSession(UserViewpoint viewpoint) throws IOException, HumanVisibleException {
		try {
		    FacebookAccount facebookAccount = facebookSystem.lookupFacebookAccount(viewpoint, viewpoint.getViewer());
		    facebookTracker.handleExpiredSessionKey(facebookAccount);
		} catch (NotFoundException e) {
			throw new RuntimeException("The viewer does not have a Facebook account registered on Mugshot.", e);
		}	
	}

	public void doRemoveClaimAim(UserViewpoint viewpoint, String address) throws IOException, HumanVisibleException {
		Resource resource;
		try {
			resource = identitySpider.lookupAim(address);
		} catch (NotFoundException e) {
			throw new HumanVisibleException("That AIM screen name isn't associated with any account");
		}
		identitySpider.removeVerifiedOwnershipClaim(viewpoint, viewpoint.getViewer(), resource);
	}
	
	public void doSetAccountDisabled(UserViewpoint viewpoint, boolean disabled)
			throws IOException, HumanVisibleException {
		identitySpider.setAccountDisabled(viewpoint.getViewer(), disabled);
	}

	public void doSetPassword(UserViewpoint viewpoint, String password) throws IOException,
			HumanVisibleException {
		password = password.trim();
		if (password.length() == 0) {
			password = null;
		}
		signinSystem.setPassword(viewpoint.getViewer(), password);
	}

	public void doSetMusicSharingEnabled(UserViewpoint viewpoint, boolean enabled)
			throws IOException {
		identitySpider.setMusicSharingEnabled(viewpoint.getViewer(), enabled);
	}
	
	public void doSetBio(UserViewpoint viewpoint, String bio) {
		identitySpider.setBio(viewpoint, viewpoint.getViewer(), bio);
	}

	public void doSetMusicBio(UserViewpoint viewpoint, String bio) {
		identitySpider.setMusicBio(viewpoint, viewpoint.getViewer(), bio);
	}
	
	private void returnTrackXml(XmlBuilder xml, TrackView tv) {
		xml.openElement("song");
		if (tv != null) {
			String image = tv.getSmallImageUrl();
			
			// flash embed needs an absolute url
			if (image != null && image.startsWith("/")) {
				String baseurl = config.getProperty(HippoProperty.BASEURL);
				image = baseurl + image;
			}
			xml.appendTextNode("image", image);
			xml.appendTextNode("title", tv.getName());
			xml.appendTextNode("artist", tv.getArtist());
			xml.appendTextNode("album", tv.getAlbum());
			xml.appendTextNode("stillPlaying", Boolean.toString(tv.isNowPlaying()));
		} else {
			xml.appendTextNode("title", "Song Title");
			xml.appendTextNode("artist", "Artist");
			xml.appendTextNode("album", "Album");
			xml.appendTextNode("stillPlaying", "false");
		}
		xml.closeElement();
	}
	
	public void getNowPlaying(OutputStream out, HttpResponseData contentType,
			String who, String theme) throws IOException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		User whoUser;
		try {
			whoUser = identitySpider.lookupGuidString(User.class, who);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}
		
		NowPlayingTheme themeObject;
		if (theme == null) {
			try {
				// this falls back to "any random theme" if user doesn't have one
				themeObject = nowPlayingSystem.getCurrentNowPlayingTheme(whoUser);
			} catch (NotFoundException e) {
				// happens only if no themes are in the system
				themeObject = null;
			}
		} else {
			try {
				themeObject = nowPlayingSystem.lookupNowPlayingTheme(theme);
			} catch (ParseException e) {
				throw new RuntimeException("bad theme argument", e);
			} catch (NotFoundException e) {
				throw new RuntimeException("bad theme argument", e);
			}
		}
		
		if (themeObject == null) {
			// create a non-persistent theme object just for this call; will have 
			// sane default values
			logger.debug("No now playing themes in system or invalid theme id, using a placeholder/temporary theme object");
			themeObject = new NowPlayingTheme(null, whoUser);
		}
		
		TrackView tv;
		try {
			// FIXME this is from the system viewpoint for now, but
			// should really be from an "anonymous" viewpoint
			tv = musicSystem.getCurrentTrackView(null, whoUser);
		} catch (NotFoundException e) {
			tv = null;
		}
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement("nowPlaying");
		
		returnTrackXml(xml, tv);
		
		if (themeObject != null) {
			xml.openElement("theme");
			String activeUrl = themeObject.getActiveImageRelativeUrl();
			xml.appendTextNode("activeImageUrl", activeUrl);
			String inactiveUrl = themeObject.getInactiveImageRelativeUrl();
			xml.appendTextNode("inactiveImageUrl", inactiveUrl);
			// Append degraded-mode (no alpha, lossy) images for Flash 7 ; nobody 
			// else should use these as they will be unreliable and suck.
			// Flash 7 can't load GIF or PNG, only JPEG.
			if (activeUrl != null)
				xml.appendTextNode("activeImageUrlFlash7", activeUrl + ".jpg");
			if (inactiveUrl != null)
				xml.appendTextNode("inactiveImageUrlFlash7", inactiveUrl + ".jpg");			
			xml.appendTextNode("text", null, "what", "album", "color", themeObject.getAlbumTextColor(),
					"fontSize", Integer.toString(themeObject.getAlbumTextFontSize()),
					"x", Integer.toString(themeObject.getAlbumTextX()),
					"y", Integer.toString(themeObject.getAlbumTextY()));
			xml.appendTextNode("text", null, "what", "artist", "color", themeObject.getArtistTextColor(),
					"fontSize", Integer.toString(themeObject.getArtistTextFontSize()),
					"x", Integer.toString(themeObject.getArtistTextX()),
					"y", Integer.toString(themeObject.getArtistTextY()));
			xml.appendTextNode("text", null, "what", "title", "color", themeObject.getTitleTextColor(),
					"fontSize", Integer.toString(themeObject.getTitleTextFontSize()),
					"x", Integer.toString(themeObject.getTitleTextX()),
					"y", Integer.toString(themeObject.getTitleTextY()));
			xml.appendTextNode("text", null, "what", "status", "color", themeObject.getStatusTextColor(),
					"fontSize", Integer.toString(themeObject.getStatusTextFontSize()),
					"x", Integer.toString(themeObject.getStatusTextX()),
					"y", Integer.toString(themeObject.getStatusTextY()));
			xml.appendTextNode("albumArt", null, "x", Integer.toString(themeObject.getAlbumArtX()),
					"y", Integer.toString(themeObject.getAlbumArtY()));
			xml.closeElement();
		}
		
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
	
	public void doCreateNewNowPlayingTheme(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String basedOn)
		throws IOException {
		NowPlayingTheme basedOnObject;
		if (basedOn != null) {
			try {
				basedOnObject = nowPlayingSystem.lookupNowPlayingTheme(basedOn);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			basedOnObject = null;
		}
		
		NowPlayingTheme theme = nowPlayingSystem.createNewNowPlayingTheme(viewpoint, basedOnObject);
		out.write(theme.getId().getBytes());
		out.flush();
	}
	
	public void doSetNowPlayingTheme(UserViewpoint viewpoint, String themeId) throws IOException {
		NowPlayingTheme theme;
		try {
			theme = nowPlayingSystem.lookupNowPlayingTheme(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		nowPlayingSystem.setCurrentNowPlayingTheme(viewpoint, viewpoint.getViewer(), theme);
	}
	
	public void doModifyNowPlayingTheme(UserViewpoint viewpoint, String themeId, String key, String value) throws IOException {
		NowPlayingTheme theme;
		try {
			theme = nowPlayingSystem.lookupNowPlayingTheme(themeId);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		final String[] blacklist = { "id", "guid", "creator", "basedOn", "creationDate" };
		for (String b : blacklist) {
			if (key.equals(b))
				throw new RuntimeException("property " + b + " can't be changed");
		}
		
		if (!viewpoint.isOfUser(theme.getCreator())) {
			throw new RuntimeException("can only modify your own themes");
		}
		
		BeanUtils.setValue(theme, key, value);
	}

	private void writeMessageReply(OutputStream out, String nodeName, String message) throws IOException {
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		xml.openElement(nodeName);
		xml.appendTextNode("message", message);
		xml.closeElement();
		out.write(xml.getBytes());
		out.flush();
	}
	
	public void doInviteSelf(OutputStream out, HttpResponseData contentType, String address, String promotion) throws IOException, RetryException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");

		String note = null;
		
		Character character;
		
		PromotionCode promotionCode = null;
		
		try {
			promotionCode = PromotionCode.check(promotion);
			switch (promotionCode) {
			case MUSIC_INVITE_PAGE_200602:
				// not valid at the moment
				character = null;
				// character = Character.MUSIC_GEEK;
				break;
			case GENERIC_LANDING_200606:
                // not valid at the moment
				character = null;
				// character = Character.MUGSHOT;
				break;
			case SUMMIT_LANDING_200606:
				// not valid at the moment
				character = null;
				// character = Character.MUGSHOT;
				break;
			case OPEN_SIGNUP_200609:
				character = Character.MUGSHOT;
				break;
			default:
				character = null;
				break;
			}
		
		} catch (NotFoundException e) {
			character = null;
		}
		
		if (character == null) {
			note = "The limited-time offer has expired!";
		} else {
			User inviter = accountSystem.getCharacter(character);
			
			try {
				if (!inviter.getAccount().canSendInvitations(1)) {
					wantsInSystem.addWantsIn(address);				    
					note = "Sorry, someone got there first! No more invitations available right now. We saved your address and will let you know when we have room for more.";
				} else {
					// this does NOT check whether the account has invitations left,
					// that's why we do it above.
					note = invitationSystem.sendEmailInvitation(new UserViewpoint(inviter), promotionCode, address,
								"Mugshot Invitation", "");
					if (note == null)
						note = "Your invitation is on its way (check your email)";
				}
			} catch (ValidationException e) {
			    // FIXME should switch this over to XmlMethod so we can display a custom error
				//   to the user; right now we validate in Javascript
				//   so this (and the resulting "Something went wrong! Reload the page and try again.")
				//   shouldn't get hit in normal operation.
			    throw new RuntimeException("Invalid email address", e);				
			}
		}
		
		if (note == null)
			throw new RuntimeException("bug! note was null in InviteSelf");
		
		//logger.debug("invite self message: '{}'", note);
		
		writeMessageReply(out, "inviteSelfReply", note);
	}
	
	public void doSendEmailInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, EmailResource address, String subject, String message, String suggestedGroupIds) throws IOException {
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		
		String note;
		try {
			note = invitationSystem.sendEmailInvitation(viewpoint, null, address, subject, message);
			
			Set<String> groupIdsSet = splitIdList(suggestedGroupIds);
			
			// this will try to findContact first, which should normally return an existing contact
			// because the viewer has just sent the invitation to the system to the invitee 
			Contact contact = identitySpider.createContact(viewpoint.getViewer(), address);
			
			for (String groupId : groupIdsSet) {
			    Group groupToSuggest = groupSystem.lookupGroupById(viewpoint, groupId);
			    groupSystem.addMember(viewpoint.getViewer(), groupToSuggest, contact);
			}
		} catch (NotFoundException e) {
				throw new RuntimeException("Group with a given id not found " + e);	
		}
		
		writeMessageReply(out, "sendEmailInvitationReply", note);
	}
	
	
	public void doInviteWantsIn(String countToInvite, String subject, String message, String suggestedGroupIds) throws IOException, RetryException {	
		logger.debug("Got into doInviteWantsIn");
		int countToInviteValue = Integer.parseInt(countToInvite);
		
		String note = null;
		
		User inviter = accountSystem.getMugshotCharacter();
		
		if (!inviter.getAccount().canSendInvitations(countToInviteValue)) {
            logger.debug("Mugshot character does not have enough invitations to invite {} people.", countToInviteValue);
            return;
		}

		/* Mapping the email strings to EmailResource can produce RetryException, causing us
		 * to need to retry the transaction. Since we can't send out emails again for the
		 * other recipients, we need to do the mapping first before starting to send out
		 * invitations.
		 */
        List<WantsIn> wantsInList = wantsInSystem.getWantsInWithoutInvites(countToInviteValue);
		final Map<WantsIn, EmailResource> resourcesToInvite = new HashMap<WantsIn, EmailResource>();
		for (WantsIn wantsIn : wantsInList) {
			try {
			    resourcesToInvite.put(wantsIn, identitySpider.getEmail(wantsIn.getAddress()));
			} catch (ValidationException e) {
				logger.warn("Tried to invite WantsIn with invalid email address", e);
			}
		}

		for (WantsIn wantsIn : wantsInList) {  
			EmailResource invitee = resourcesToInvite.get(wantsIn);
			if (invitee == null)
				continue;
			
			note = invitationSystem.sendEmailInvitation(new UserViewpoint(inviter), null, invitee,
			                                            subject, message);
		    if (note == null) {
                logger.debug("Invitation for {} is on its way", wantsIn.getAddress());
                wantsIn.setInvitationSent(true);
		    } else {
			    logger.debug("Trying to send an invitation to {} produced the following note: {}", wantsIn.getAddress(), note);
			    if (note.contains(InvitationSystem.INVITATION_SUCCESS_STRING)) {
                    wantsIn.setInvitationSent(true);				    	
			    }
		    }
			    
			Set<String> groupIdsSet = splitIdList(suggestedGroupIds);
			
			// this will try to findContact first, which should normally return an existing contact
			// because Mugshot has already sent the invitation to the system to the invitee 
			Contact contact = identitySpider.createContact(inviter, invitee);

			try {
			    for (String groupId : groupIdsSet) {
			        Group groupToSuggest = groupSystem.lookupGroupById(new UserViewpoint(inviter), groupId);
			        groupSystem.addMember(inviter, groupToSuggest, contact);
			    }
			} catch (NotFoundException e) {
				throw new RuntimeException("Group with a given id not found " + e);
			}	    	
        }
	}
	
	public void doSendGroupInvitation(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, Group group, String inviteeId, EmailResource inviteeAddress, String subject, String message) throws IOException
	{
		if (contentType != HttpResponseData.XML)
			throw new IllegalArgumentException("only support XML replies");
		Person person;
		PostingBoard.InviteRecipients inviteRecipients;
		
		if (inviteeId != null) {
			try {
				person = identitySpider.lookupGuidString(Person.class, inviteeId);
				inviteRecipients = PostingBoard.InviteRecipients.DONT_INVITE;
			} catch (ParseException e) {
				throw new RuntimeException("bad invitee guid", e);
			} catch (NotFoundException e) {
				throw new RuntimeException("no such invitee guid", e);
			}
			
		} else if (inviteeAddress != null) {
			// If the recipient isn't yet a Mugshot member, make sure we can invite
			// them to the system before sending out an email; the resulting email
			// is very confusing if it is an invitation; this check is to produce
			// a nice message back to the user; we do another check when actually
			// sending out the invitation to prevent race conditions
			User user = identitySpider.getUser(inviteeAddress);
			if (user == null && viewpoint.getViewer().getAccount().getInvitations() == 0) {
				String note = "Sorry, " + inviteeAddress + " isn't a Mugshot member yet";
				writeMessageReply(out, "sendGroupInvitationReply", note);
			
				return;
			}

			person = identitySpider.createContact(viewpoint.getViewer(), inviteeAddress);
			inviteRecipients = PostingBoard.InviteRecipients.MUST_INVITE;
		} else {
			throw new RuntimeException("inviteeId and inviteeAddress can't both be null");
		}
			
		GuidPersistable recipient = person;
		Set<GuidPersistable> recipients = Collections.singleton(recipient);
		try {
			postingBoard.doShareGroupPost(viewpoint.getViewer(), group, subject, message, recipients, inviteRecipients);
		} catch (NotFoundException e) {
			throw new RuntimeException("doShareGroup unxpectedly couldn't find contact recipient");
		}
		
		// let's find out if we were inviting to the group or inviting to follow the group
		boolean adderCanAdd = groupSystem.canAddMembers(viewpoint.getViewer(), group);
		
		PersonView personView = personViewer.getPersonView(viewpoint, person);

		String note;
		if (adderCanAdd) {
		    note = personView.getName() + " has been invited to the group " + group.getName();
		} else {
			note = personView.getName() + " has been invited to follow the group " + group.getName();
		}
		
		writeMessageReply(out, "sendGroupInvitationReply", note);
	}

	public void doSuggestGroups(UserViewpoint viewpoint, EmailResource address, String suggestedGroupIds, String desuggestedGroupIds) {
		Set<String> suggestedGroupIdsSet = splitIdList(suggestedGroupIds);
		Set<String> desuggestedGroupIdsSet = splitIdList(desuggestedGroupIds);
		
		// this will try to findContact first, which should normally return an existing contact
		// if the viewer has already sent the invitation to the system to the invitee 
		Contact contact = identitySpider.createContact(viewpoint.getViewer(), address);

		try {
		    for (String groupId : suggestedGroupIdsSet) {
		        Group groupToSuggest = groupSystem.lookupGroupById(viewpoint, groupId);
		        groupSystem.addMember(viewpoint.getViewer(), groupToSuggest, contact);
		    }
		    
		    for (String groupId : desuggestedGroupIdsSet) {
		    	Group groupToDesuggest = groupSystem.lookupGroupById(viewpoint, groupId);
		        groupSystem.removeMember(viewpoint.getViewer(), groupToDesuggest, contact);
		    }		    
		    
		} catch (NotFoundException e) {
			throw new RuntimeException("Group with a given id not found " + e);
		}	    	
	}
	
	public void doSendRepairEmail(UserViewpoint viewpoint, String userId) throws RetryException
	{
		if (!identitySpider.isAdministrator(viewpoint.getViewer())) {
			throw new RuntimeException("Only administrators can send repair links");
		}
		
		User user;
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}
		
		try {
			signinSystem.sendRepairLink(user);
		} catch (HumanVisibleException e) {
			throw new RuntimeException("Error sending repair link", e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void doReindexAll(UserViewpoint viewpoint) 
	{
		if (!identitySpider.isAdministrator(viewpoint.getViewer())) {
			throw new RuntimeException("Only administrators can recreate the search indices");
		}
		
		searchSystem.reindexAll(null);
	}
	
	public void getRandomBio(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint) throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");

		out.write(StringUtils.getBytes("I was born in the year 1903, in a shack."));
		out.flush();
	}

	public void getRandomMusicBio(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint) throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");
	
		out.write(StringUtils.getBytes("Polka makes me perky!"));
		out.flush();
	}
	
	public void doSetStockPhoto(UserViewpoint viewpoint, String photo) {
		identitySpider.setStockPhoto(viewpoint, viewpoint.getViewer(), photo);
	}
	
	public void getUserPhoto(OutputStream out, HttpResponseData contentType, String userId, String size)
		throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");
		
		User user;
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}

		int sizeValue = Integer.parseInt(size);
		switch (sizeValue) {
		case Configuration.SHOT_SMALL_SIZE:
		case Configuration.SHOT_MEDIUM_SIZE:
		case Configuration.SHOT_LARGE_SIZE:
			break;
		default:
			throw new RuntimeException("invalid photo size");
		}
		
		String url = EntityView.sizePhoto(user.getPhotoUrl(), sizeValue);
		
		out.write(StringUtils.getBytes(url));
		out.flush();
	}
	
	public void getGroupPhoto(OutputStream out, HttpResponseData contentType, Group group, String size)
		throws IOException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");
		
		int sizeValue = Integer.parseInt(size);
		switch (sizeValue) {
		case Configuration.SHOT_SMALL_SIZE:
		case Configuration.SHOT_MEDIUM_SIZE:
		case Configuration.SHOT_LARGE_SIZE:
			break;
		default:
			throw new RuntimeException("invalid photo size");
		}
		
		String url = EntityView.sizePhoto(group.getPhotoUrl(), sizeValue);
		
		out.write(StringUtils.getBytes(url));
		out.flush();
	}
	
	public void doAcceptTerms(UserViewpoint viewpoint) {
		viewpoint.getViewer().getAccount().setHasAcceptedTerms(true);
	}
	
	public void doSetNeedsDownload(UserViewpoint viewpoint, boolean needsDownload) {
		viewpoint.getViewer().getAccount().setNeedsDownload(needsDownload);
	}
	
	public void doSetAdminDisabled(UserViewpoint viewpoint, String userId, boolean disabled) {
		if (!identitySpider.isAdministrator(viewpoint.getViewer())) {
			throw new RuntimeException("Only administrators can administratively disable/enable accounts");
		}
		
		User user;
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
		} catch (ParseException e) {
			throw new RuntimeException("bad guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("no such person", e);
		}
		
		identitySpider.setAccountAdminDisabled(user, disabled);
	}
	
	public void doSetNewFeatures(UserViewpoint viewpoint, boolean newFeaturesFlag) {
		config.setProperty(HippoProperty.NEW_FEATURES.getKey(), Boolean.valueOf(newFeaturesFlag).toString());
	}
	
	private void writeException(XmlBuilder xml, StringWriter clientOut, Throwable t) throws IOException {
		xml.openElement("result", "type", "exception");
		xml.appendTextNode("output", clientOut.toString());		
		xml.appendTextNode("message", t.getMessage());
		StringWriter buf = new StringWriter();
		t.printStackTrace(new PrintWriter(buf));
		xml.appendTextNode("trace", buf.toString());
		xml.closeElement();
	}
	
	private void writeSuccess(XmlBuilder xml, StringWriter clientOut, Object result) throws IOException {
		xml.openElement("result", "type", "success");
		xml.appendTextNode("retval", result != null ? result.toString() : "null", "class", result != null ? result.getClass().getCanonicalName() : "null");
		if (result != null) {
			xml.openElement("retvalReflection");
			for (Method m : result.getClass().getMethods()) {
				xml.openElement("method", "name", m.getName(), "return", m.getReturnType().getSimpleName());
				for (Class<?> param : m.getParameterTypes()) {
					xml.appendTextNode("param", param.getSimpleName());
				}
				xml.closeElement();
			}
			xml.closeElement();
		}
		xml.appendTextNode("output", clientOut.toString());
		xml.closeElement();
	}
	
	public class Server implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public Object getLiveState() {
			return LiveState.getInstance();
		}
		
		public User getUser(String id) throws NotFoundException{
			Guid guid;
			try {
				guid = new Guid(id);
				return identitySpider.lookupGuid(User.class, guid); 				
			} catch (ParseException e) {
				return identitySpider.lookupUserByEmail(SystemViewpoint.getInstance(), id);
			}
		}
		
		public UserViewpoint getView(User u) {
			return new UserViewpoint(u);
		}
		
		public Object getEJB(String name) throws ClassNotFoundException, NamingException {
			try {
				return EJBUtil.uncheckedDynamicLookupLocal(name);
			} catch (NameNotFoundException e) {
				return EJBUtil.uncheckedDynamicLookupRemote(name);
			}
		}		
		
		public Object getMBean(Class<?> iface, String service) throws MalformedObjectNameException {
			MBeanServer server = MBeanServerLocator.locateJBoss();
			return MBeanProxyExt.create(iface, service, server);			
		}
	}
	
	private Interpreter makeInterpreter(PrintWriter out) {
		Interpreter bsh = new Interpreter();

		try {
			bsh.set("server", new Server());
			bsh.set("out", out);
			bsh.set("em", em);
			
			// This makes us override private/protected etc
			bsh.eval("setAccessibility(true);");
		
			// Some handy primitives
			bsh.eval("user(str) { return server.getUser(str); };");
			bsh.eval("view(u) { return server.getView(u); };");
			bsh.eval("ejb(str) { return server.getEJB(str); };");
			bsh.eval("mbean(c, i) { return server.getMBean(c, i); };");
			bsh.eval("guid(str) { return new com.dumbhippo.identity20.Guid(str); }");
			
			// Some default bindings
			bsh.eval("identitySpider = server.getEJB(\"IdentitySpider\");");
			bsh.eval("systemView = com.dumbhippo.server.views.SystemViewpoint.getInstance();");			
		} catch (EvalError e) {
			throw new RuntimeException(e);
		}
		
		return bsh;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void doAdminShellExec(XmlBuilder xml, UserViewpoint viewpoint, boolean parseOnly, boolean transaction, final String command) throws IOException, HumanVisibleException {
		StringWriter clientOut = new StringWriter();
		if (parseOnly) {
			Parser parser = new Parser(new StringReader(command));
			try {
				while (!parser.Line())
					;
				writeSuccess(xml, clientOut, null);
			} catch (bsh.ParseException e) {
				writeException(xml, clientOut, e);
			} catch (TokenMgrError e) {
				writeException(xml, clientOut, e);
			}
			return;
		}
		
		PrintWriter pw = new PrintWriter(clientOut);
		final Interpreter bsh = makeInterpreter(pw);
		pw.flush();
		
		Callable<Object> execution = new Callable<Object>() {
			public Object call() throws Exception {
				return bsh.eval(command);
			}
		};

		try {
			Object result;
			if (transaction) {
				result = TxUtils.runInTransaction(execution);
			} else {
				result = execution.call();
			}
			bsh.set("result", result);
			writeSuccess(xml, clientOut, result);
		} catch (EvalError e) {
			writeException(xml, clientOut, e);
		} catch (Exception e) {
			throw new RuntimeException(e);  // Shouldn't happen
		}
	}
	
	private Feed getFeedFromUserEnteredUrl(String url) throws XmlMethodException, RetryException {
		URL urlObject;
		try {
			urlObject = parseUserEnteredUrl(url, true);
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Invalid URL: " + e.getMessage());
		}
		return feedSystem.scrapeFeedFromUrl(urlObject);
	}
	
	public void getFeedDump(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint, String url) throws HumanVisibleException, IOException, RetryException {
		try {
			PrintStream printer = new PrintStream(out);						
			Feed feed = getFeedFromUserEnteredUrl(url);
			feedSystem.updateFeed(feed);
			
			printer.println("Link: " + feed.getSource().getUrl());
			printer.println("Title: " + feed.getTitle());
			printer.print("Last fetched: " + feed.getLastFetched());
			if (feed.getLastFetchSucceeded())
				printer.println(" (succeeded)");
			else
				printer.println(" (failed)");
			printer.println();
			
			List<FeedEntry> entries = feedSystem.getCurrentEntries(feed);
			for (FeedEntry entry : entries) {
				printer.println("Guid: " + entry.getEntryGuid());
				printer.println("Link: " + entry.getLink());
				printer.println("Title: " + entry.getTitle());
				printer.println("Date: " + entry.getDate());
				printer.println("Description: " + entry.getDescription());
				printer.println();
			}
			
			printer.flush();
			
		} catch (XmlMethodException e) {
			throw new HumanVisibleException(e.getCodeString() + ": " + e.getMessage());
		}
	}

	public void doFeedPreview(XmlBuilder xml, UserViewpoint viewpoint, String url) throws XmlMethodException, RetryException {		
		Feed feed = getFeedFromUserEnteredUrl(url);
		feedSystem.updateFeed(feed);

		// format deliberately kept a little bit similar to RSS
		// (element names title, link, item for example)
		
		xml.openElement("feedPreview");
		xml.appendTextNode("title", feed.getTitle());
		xml.appendTextNode("link", feed.getLink().getUrl());
		xml.appendTextNode("source", feed.getSource().getUrl());
				
		List<FeedEntry> entries = feedSystem.getCurrentEntries(feed);
		
		int count = 0;
		for (FeedEntry entry : entries) {
			if (count > 2)
				break;
			xml.openElement("item");
			xml.appendTextNode("title", entry.getTitle());
			xml.appendTextNode("link", entry.getLink().getUrl());
			xml.closeElement();
			++count;
		}
		// close feedPreview
		xml.closeElement();
	}
	
	public void doAddGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, Group group, String url) throws XmlMethodException, RetryException {		
		Feed feed = getFeedFromUserEnteredUrl(url);

		if (!groupSystem.canEditGroup(viewpoint, group))
			throw new XmlMethodException(XmlMethodErrorCode.FORBIDDEN, "Only active members can add feeds to a group");
							
		feedSystem.addGroupFeed(viewpoint.getViewer(), group, feed);
	}

	public void doRemoveGroupFeed(XmlBuilder xml, UserViewpoint viewpoint, Group group, URL url) throws XmlMethodException {		
		LinkResource link;
		try {
			link = identitySpider.lookupLink(url);
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "Feed not found: " + url);
		}
		Feed feed = feedSystem.getExistingFeed(link);
		
		if (!groupSystem.canEditGroup(viewpoint, group))
			throw new XmlMethodException(XmlMethodErrorCode.FORBIDDEN, "Only active members can remove feeds from a group");
							
		feedSystem.removeGroupFeed(viewpoint.getViewer(), group, feed);		
	}

	public void doSetRhapsodyHistoryFeed(XmlBuilder xml, UserViewpoint viewpoint, String urlOrIdStr) throws XmlMethodException, RetryException {
		String urlOrId = urlOrIdStr.trim();

		String rhapUserId = StringUtils.findParamValueInUrl(urlOrId, "rhapUserId");		
		if (rhapUserId == null) {
			if (urlOrId.startsWith("http://") || urlOrId.toLowerCase().contains(("rhapsody"))) {
				throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Rhapsody RSS URL should contain a rhapUserId param: " + urlOrId);
			} else {
				// we also want to handle the user entering only an id
				rhapUserId = urlOrId;
			}
		}
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.RHAPSODY);
		try {
			external.setHandleValidating(rhapUserId);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
				
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		Feed feed;		
		try {
		    feed = feedSystem.scrapeFeedFromUrl(new URL("http://feeds.rhapsody.com/user-track-history.rss?rhapUserId=" + StringUtils.urlEncode(external.getHandle()) + "&userName=I"));
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);
	}
	
	public void doSetNetflixFeedUrl(XmlBuilder xml, UserViewpoint viewpoint, String urlOrIdStr) throws XmlMethodException, RetryException {
		String urlOrId = urlOrIdStr.trim();

		String netflixUserId = StringUtils.findParamValueInUrl(urlOrId, "id");		
		if (netflixUserId == null) {
			if (urlOrId.startsWith("http://") || urlOrId.toLowerCase().contains(("netflix"))) {
			    throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Netflix RSS URL should contain an id param: " + urlOrId);
			} else {
				// we also want to handle a user entering only an id
				netflixUserId = urlOrId;
			}
		}
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.NETFLIX);
		try {
			external.setHandleValidating(netflixUserId);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
				
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		Feed feed;		
		try {
		    feed = feedSystem.scrapeFeedFromUrl(new URL("http://rss.netflix.com/AtHomeRSS?id=" + StringUtils.urlEncode(external.getHandle())));
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);			
	}
	
	private ExternalAccountType parseExternalAccountType(String type) throws XmlMethodException {
		try {
			return ExternalAccountType.valueOf(type);
		} catch (IllegalArgumentException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Unknown external account type: " + type);
		}
	}

	private String parseEmail(String email) throws XmlMethodException {
		try {
			email = EmailResource.canonicalize(email);
			return email;
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Not a valid email address: '" + email + "'");
		}		
	}
	
	public void doHateExternalAccount(XmlBuilder xml, UserViewpoint viewpoint, String type, String quip) throws XmlMethodException {
		
		// FIXME for any external account that has a feed associated with it, we could try to remove the feed and
		// the polling task for it; also for MySpace we could check if there is a polling task for checking on
		// a feed for a private profile and remove that task
		
		ExternalAccountType typeEnum = parseExternalAccountType(type);
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, typeEnum);
		externalAccountSystem.setSentiment(external, Sentiment.HATE);
		if (quip != null) {
			quip = quip.trim();
			if (quip.length() == 0)
				quip = null;
		}
		external.setQuip(quip);
		DataService.currentSessionRW().changed(ExternalAccountDMO.class, new ExternalAccountKey(external), "quip");
	}

	public void doRemoveExternalAccount(XmlBuilder xml, UserViewpoint viewpoint, String type) throws XmlMethodException {
		
		// FIXME for any external account that has a feed associated with it, we could try to remove the feed and
		// the polling task for it; also for MySpace we could check if there is a polling task for checking on
		// a feed for a private profile and remove that task
		
		ExternalAccountType typeEnum = parseExternalAccountType(type);
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, typeEnum);
		externalAccountSystem.setSentiment(external, Sentiment.INDIFFERENT);
	}

	public void doFindFlickrAccount(XmlBuilder xml, UserViewpoint viewpoint, String email) throws XmlMethodException {
		FlickrWebServices ws = new FlickrWebServices(8000, config);
		FlickrUser flickrUser = ws.lookupFlickrUserByEmail(email);
		if (flickrUser == null)
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "Flickr doesn't report a user with the email address '" + email + "'");
		xml.openElement("flickrUser");
		xml.appendTextNode("nsid", flickrUser.getId());
		xml.appendTextNode("username", flickrUser.getName());
		xml.closeElement();
	}

	public void doSetFlickrAccount(XmlBuilder xml, UserViewpoint viewpoint, String nsid, String email) throws XmlMethodException {
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.FLICKR);		

		email = parseEmail(email);
		
		try {
			external.setHandleValidating(nsid);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		external.setExtra(email);
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
	}
	
	public void doSetMySpaceName(XmlBuilder xml, UserViewpoint viewpoint, String name) throws XmlMethodException, RetryException {
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.MYSPACE);		
		String friendId;		
		boolean isPrivate;
		try {
			Pair<String, Boolean> mySpaceInfoPair = MySpaceScraper.getFriendId(name);
			friendId = mySpaceInfoPair.getFirst();
			isPrivate = mySpaceInfoPair.getSecond();
			external.setExtraValidating(friendId);
			external.setHandleValidating(name);
			external.setFeeds(new HashSet<Feed>());
		} catch (TransientServiceException e) {
			logger.warn("Failed to get MySpace friend ID", e);
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "Couldn't verify MySpace name '" + name + "' right now - MySpace may have issues, try again in a few minutes");
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		
		// our check for whether the MySpace profile is set to private is not that definitive, so we might as well attempt
		// to get the feed in any case
		try {
		    Feed feed = feedSystem.scrapeFeedFromUrl(MySpaceScraper.getBlogURLFromFriendId(friendId));
			EJBUtil.forceInitialization(feed.getAccounts());		
			externalAccountSystem.setSentiment(external, Sentiment.LOVE);
			external.setFeed(feed);
			feed.getAccounts().add(external);	
		} catch (XmlMethodException e) {
			if (e.getCode() == XmlMethodErrorCode.INVALID_URL && isPrivate) {
				// the account must be set private	
				externalAccountSystem.setSentiment(external, Sentiment.LOVE);
				logger.debug("Creating a task for external account id {}", external.getId());
				pollingPersistence.createTaskIdempotent(PollingTaskFamilyType.MYSPACE, String.valueOf(external.getId()));
				xml.appendTextNode("message", "It looks like your MySpace account is set to private, so updates about your MySpace blog entries will not be available on Mugshot.");
			} else {
		        // rethrow
			    throw e;
			}
		}	
	}

	public void doSetYouTubeName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException {
		// Try to pull youtube name out of either a youtube profile url ("http://www.youtube.com/user/$username" || "http://www.youtube.com/profile?user=$username") or 
		// just try using the thing as a username directly
		String name = urlOrName.trim();
		int user = urlOrName.indexOf("/user/");
		if (user >= 0) {
			user += "/user/".length();
			name = urlOrName.substring(user);
		} else if ( (user = urlOrName.indexOf("/profile?user=")) >= 0) {
			user += "/profile?user=".length();
			name = urlOrName.substring(user);
		}
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.YOUTUBE);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		xml.appendTextNode("username", external.getHandle());
	}
	
	public void doSetLastFmName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException {
		String name = urlOrName.trim();
		String found = StringUtils.findPathElementAfter(name, "/user/");
		if (found != null)
			name = found;
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.LASTFM);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		
		try {
			LastFmWebServices.getTracksForUser(name);
		} catch (TransientServiceException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Can't retrieve your songs from last.fm");
		}
		
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		xml.appendTextNode("username", external.getHandle());
	}	
	
	public void doSetDeliciousName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException, RetryException {
		String name = urlOrName.trim();
		// del.icio.us urls are just "http://del.icio.us/myusername"
		
		String found = StringUtils.findLastPathElement(name);
		if (found != null)
			name = found;

		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.DELICIOUS);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		Feed feed;
		try {
			feed = feedSystem.scrapeFeedFromUrl(new URL("http://del.icio.us/rss/" + StringUtils.urlEncode(external.getHandle())));
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);
		
		xml.appendTextNode("username", external.getHandle());
	}
	
	public void doSetTwitterName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException, RetryException {
		String name = urlOrName.trim();
		
		// Twitter urls are just "http://twitter.com/myusername"
		
		String found = StringUtils.findLastPathElement(name);
		if (found != null)
			name = found;
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.TWITTER);
		String validatedHandle = null;
		try {
			validatedHandle = external.validateHandle(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		
		Feed feed;
		boolean feedFound;
		try {
			Pair<Feed, Boolean> twitterFeedPair = feedSystem.createFeedFromUrl(new URL("http://twitter.com/" + StringUtils.urlEncode(validatedHandle)), true);
			feed = twitterFeedPair.getFirst();
			feedFound = twitterFeedPair.getSecond();
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		
		try {
			external.setHandleValidating(validatedHandle);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);
		
		xml.appendTextNode("username", external.getHandle());
		
	    if (!feedFound) {	
		    xml.appendTextNode("message", "It looks like your Twitter updates are not public, if you want them to be available " +
		    		                      "on Mugshot, you should make them public in Twitter settings");
	    }
	}

	public void doSetDiggName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException, RetryException {
		String name = urlOrName.trim();
		
		// Digg urls are "http://digg.com/users/myusername/stuff"

		String found = StringUtils.findPathElementAfter(name, "/users/");
		if (found != null)
			name = found;

		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.DIGG);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		Feed feed;
		try {
			feed = feedSystem.scrapeFeedFromUrl(new URL("http://digg.com/users/" + StringUtils.urlEncode(external.getHandle()) + "/dugg"));
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);
		
		xml.appendTextNode("username", external.getHandle());
	}

	public void doSetRedditName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException, RetryException {
		String name = urlOrName.trim();

		// Reddit urls are "http://reddit.com/user/myusername"

		//logger.debug("name={}", name);
		
		String found = StringUtils.findPathElementAfter(name, "/user/");
		if (found != null)
			name = found;
		
		//logger.debug("found={}", found);
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.REDDIT);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		external.setFeeds(new HashSet<Feed>());
		
		Feed feed, likedFeed, dislikedFeed;
		boolean likedFeedFound, dislikedFeedFound;
		try {
			feed = feedSystem.scrapeFeedFromUrl(new URL("http://reddit.com/user/" + StringUtils.urlEncode(external.getHandle())));
            // we create feeds for likes and dislikes regardless of whether they are actually found
			Pair<Feed, Boolean> likedFeedPair = feedSystem.createFeedFromUrl(new URL("http://reddit.com/user/" + StringUtils.urlEncode(external.getHandle()) + "/liked.rss"), false);
			likedFeed = likedFeedPair.getFirst();
			likedFeedFound = likedFeedPair.getSecond();
			Pair<Feed, Boolean> dislikedFeedPair = feedSystem.createFeedFromUrl(new URL("http://reddit.com/user/" + StringUtils.urlEncode(external.getHandle()) + "/disliked.rss"), false);
			dislikedFeed = dislikedFeedPair.getFirst();
			dislikedFeedFound = dislikedFeedPair.getSecond(); 			
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		EJBUtil.forceInitialization(feed.getAccounts());		
		external.addFeed(feed);
		feed.getAccounts().add(external);		

		EJBUtil.forceInitialization(likedFeed.getAccounts());
		external.addFeed(likedFeed);
		likedFeed.getAccounts().add(external);				
               
		EJBUtil.forceInitialization(dislikedFeed.getAccounts());
		external.addFeed(dislikedFeed);
		dislikedFeed.getAccounts().add(external);				
		
		xml.appendTextNode("username", external.getHandle());
		
		// we should really always either find or not find both feeds
		if (likedFeedFound != dislikedFeedFound) {
		    logger.warn("likedFeedFound was {}, while dislikedFeedFound was {}", likedFeedFound, dislikedFeedFound);				
		}
		
	    if (!likedFeedFound || !dislikedFeedFound) {	
		    xml.appendTextNode("message", "It looks like your Reddit votes are not public, if you want them to be available " +
		    		                       "on Mugshot in addition to links you submit and comment on on Reddit, you can make " +
		    		                       "them public at http://reddit.com/prefs/options");
	    }
	}
	
	public void doSetLinkedInProfile(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException {
		// Try to pull linked in name out of either a linked in profile url ("http://www.linkedin.com/in/username") or 
		// just try using the thing as a username directly
		String name = urlOrName.trim();
		int i = urlOrName.indexOf("/in/");
		if (i >= 0) {
			i += "/in/".length();
			name = urlOrName.substring(i);
		}
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public profile URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.LINKED_IN);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		xml.appendTextNode("username", external.getHandle());
	}

	public void doSetPicasaName(XmlBuilder xml, UserViewpoint viewpoint, String urlOrName) throws XmlMethodException, RetryException {
		String name = urlOrName.trim();
		
		// Picasa public urls are http://picasaweb.google.com/username
		
		String found = StringUtils.findLastPathElement(name);
		if (found != null)
			name = found;
		
		if (name.startsWith("http://"))
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Enter your public gallery URL or just your username");
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.PICASA);
		try {
			external.setHandleValidating(name);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		try {
			@SuppressWarnings("unused") Feed feed = feedSystem.scrapeFeedFromUrl(new URL("http://picasaweb.google.com/" + StringUtils.urlEncode(external.getHandle())));
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage() + " (check that you have a public Picasa web gallery set up)");
		}
		/* We validate that the feed can be fetched, but the actual feed checking is done as a "web service" not as a feed (i.e. we don't update the Feed object
		 * and get FeedEntry for it)
		 */
		/* 
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);
		*/
		
		xml.appendTextNode("username", external.getHandle());
	}
	
	public void doSetAmazonUrl(XmlBuilder xml, UserViewpoint viewpoint, String urlOrUserIdStr) throws XmlMethodException {		
		String urlOrUserId = urlOrUserIdStr.trim();

		String amazonUserId = StringUtils.findPathElementAfter(urlOrUserId, "/profile/");
		if (amazonUserId == null) {
			if (urlOrUserId.startsWith("http://") || urlOrUserId.toLowerCase().contains(("amazon"))) {
			    throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Enter your public profile URL");
			} else {
				// we also want to handle a user entering only an id
				amazonUserId = urlOrUserId;
			}
		}
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.AMAZON);
		// TODO: we could make a LookupUserContent web request to Amazon before actually setting the handle 
		try {
			external.setHandleValidating(amazonUserId);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
				
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		// check how many reviews they have and warn them there will be no updates if there are too many
		AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);		
		int reviewCount = ws.getReviewsCount(amazonUserId);		
		int reviewCountWeCanGet = AmazonWebServices.MAX_AMAZON_REVIEW_PAGES_RETURNED * AmazonWebServices.AMAZON_REVIEWS_PER_PAGE;
		
		if (reviewCount >= reviewCountWeCanGet) { 
		    xml.appendTextNode("message", "You currently have " + reviewCount + " reviews on Amazon. " +
		    		                      "We will not be able to get updates about your reviews, because for now "+
		    		                      "we can only get the " + reviewCountWeCanGet + " oldest reviews.");	    
		} else if (reviewCount >= reviewCountWeCanGet - 10) {
		    xml.appendTextNode("message", "You currently have " + reviewCount + " reviews on Amazon. " +
                                          "We will not be able to get updates about your reviews once you reach " +
                                          reviewCountWeCanGet + " reviews because for now we can only get the " 
                                          + reviewCountWeCanGet + " oldest reviews.");		
		}
		
		xml.openElement("amazonDetails");
		
		for (Pair<String, String> link : amazonUpdater.getAmazonLinks(amazonUserId, true)) {
		    xml.openElement("link");
		    xml.appendTextNode("name", link.getFirst());
		    xml.appendTextNode("url", link.getSecond());
		    xml.closeElement();
		}
		
		xml.closeElement();
	}
	
	public void doSetWebsite(XmlBuilder xml, UserViewpoint viewpoint, URL url) throws XmlMethodException {
		// DO NOT cut and paste this block into similar external account methods. It's only here because
		// we don't use the "love hate" widget on /account for the website, and the javascript glue 
		// for the plain entries assumes this works.
		if (url == null) {
			doRemoveExternalAccount(xml, viewpoint, "WEBSITE");
			try {
				ExternalAccount external = externalAccountSystem.lookupExternalAccount(viewpoint, viewpoint.getViewer(), ExternalAccountType.WEBSITE);
				// otherwise the website url would keep "coming back" since there's no visual indication of hate/indifferent status
				external.setHandle(null);
			} catch (NotFoundException e) {
			}
			return;
		}
		
		throwIfUrlNotHttp(url);
		
		// the rest of this is more typical of a "set external account" http method
				
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.WEBSITE);
		try {
			external.setHandleValidating(url.toExternalForm());
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
	}

	public void doSetBlog(XmlBuilder xml, UserViewpoint viewpoint, URL url) throws XmlMethodException, RetryException {
		
		// DO NOT cut and paste this block into similar external account methods. It's only here because
		// we don't use the "love hate" widget on /account for the website, and the javascript glue 
		// for the plain entries assumes this works.
		if (url == null) {
			doRemoveExternalAccount(xml, viewpoint, "BLOG");
			try {
				ExternalAccount external = externalAccountSystem.lookupExternalAccount(viewpoint, viewpoint.getViewer(), ExternalAccountType.BLOG);
				// otherwise the blog url would keep "coming back" since there's no visual indication of hate/indifferent status
				external.setHandle(null);
			} catch (NotFoundException e) {
			}
			return;
		}
		
		throwIfUrlNotHttp(url);
		
		Feed feed = feedSystem.scrapeFeedFromUrl(url);
		EJBUtil.forceInitialization(feed.getAccounts());
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.BLOG);
		
		try {
			external.setHandleValidating(url.toExternalForm());
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}

		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		external.setFeed(feed);
		feed.getAccounts().add(external);
	}
	
	public void doSetGoogleReaderUrl(XmlBuilder xml, UserViewpoint viewpoint, String feedOrPageUrl) throws XmlMethodException, RetryException {
		feedOrPageUrl = feedOrPageUrl.trim();
		
		// the page url would be "http://www.google.com/reader/shared/10558901572126132384"
		// the feed would be "http://www.google.com/reader/public/atom/user/10558901572126132384/state/com.google/broadcast"
		
		String userId = null;
		if (feedOrPageUrl.startsWith("http://www.google.com/reader/shared/")) {
			userId = StringUtils.findLastPathElement(feedOrPageUrl);
		} else if (feedOrPageUrl.startsWith("http://www.google.com/reader/public/atom/user/")) {
			String s = feedOrPageUrl.substring("http://www.google.com/reader/public/atom/user/".length());
			int i = s.indexOf('/');
			if (i >= 0)
				userId = s.substring(0, i);
			if (userId.length() == 0 || !StringUtils.isAllNumbers(userId))
				userId = null;
		} 
		
		if (userId == null) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Give the URL of your Google Reader shared items page or feed");
		} else {
			logger.debug("Parsed google reader id '{}' from '{}'", userId, feedOrPageUrl);
		}
		
		ExternalAccount external = externalAccountSystem.getOrCreateExternalAccount(viewpoint, ExternalAccountType.GOOGLE_READER);
		try {
			external.setHandleValidating(userId);
		} catch (ValidationException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, e.getMessage());
		}
		
		externalAccountSystem.setSentiment(external, Sentiment.LOVE);
		
		Feed feed;
		try {
		    feed = feedSystem.scrapeFeedFromUrl(new URL("http://www.google.com/reader/public/atom/user/" +
		    		StringUtils.urlEncode(external.getHandle() + "/state/com.google/broadcast")));
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, e.getMessage());
		}
		EJBUtil.forceInitialization(feed.getAccounts());
		
		external.setFeed(feed);
		feed.getAccounts().add(external);
	}
	
	private StatisticsService getStatisticsService() throws XmlMethodException {
		// This probably should be using JNDI, or MX, or EJB injection, but the static
		// member variable in StatisticsService is sufficient for now and simple
		StatisticsService service = StatisticsService.getInstance();
		if (service == null)
			throw new XmlMethodException(XmlMethodErrorCode.NOT_READY, "Statistics Service isn't started");
		
		return service;
	}
		
	public void getStatisticsSets(XmlBuilder xml, UserViewpoint viewpoint, String filename) throws IOException, XmlMethodException {
		List<StatisticsSet> sets;
		if (filename == null) {
		    sets = getStatisticsService().listSets();
		} else {
			try {
				sets = Collections.singletonList(getStatisticsService().getSet(filename));
			} catch (NotFoundException e) {
				throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, e.getMessage());
			}
		}
		xml.openElement("statisticsSets");
		for (StatisticsSet set : sets) {			
			xml.openElement("statisticsSet");
			// Doing these as child nodes is a little weird to me; it would 
			// be easier on the javascript if they were attributes.
			xml.appendTextNode("current", set.isCurrent() ? "true" : "false");
			xml.appendTextNode("filename", set.getFilename());
			xml.appendTextNode("hostname", set.getHostName());
			xml.appendTextNode("startTime", Long.toString(set.getStartDate().getTime()));
			xml.appendTextNode("endTime", Long.toString(set.getEndDate().getTime()));
			xml.openElement("columns");
			for (ColumnDescription column : set.getColumns()) {
				xml.openElement("column", "id", column.getId(), "units", column.getUnits().name(), "type", column.getType().name());
				xml.appendTextNode("name", column.getName());
				xml.closeElement();
			}
			xml.closeElement();
			xml.closeElement();
			
		}
		xml.closeElement();
	}
	
	public void getStatistics(XmlBuilder xml, UserViewpoint viewpoint, String filename, String columns, String startString, String endString, String timescaleString) throws IOException, XmlMethodException {
		StatisticsSet set;

		try {
			if (filename != null)
				 set = getStatisticsService().getSet(filename);
			else
				set = getStatisticsService().getCurrentSet();
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, e.getMessage());
		}
		
		String[] columnNames = columns.split(",");
		// will maintain indexes of requested columns as they appear in the statistics set's columnMap 
		int[] columnIndexes = new int[columnNames.length];
		ColumnMap columnMap = set.getColumns();
		
		for (int i = 0; i < columnNames.length; i++) {
			String columnName = columnNames[i];
			try {
				columnIndexes[i] = columnMap.getIndex(columnName);
			} catch (NoSuchElementException e) {
				throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "Column '" + columnName + "' not found");
			}
		}
		
		Date start;
		if (startString != null) {
			try {
				start = new Date(Long.parseLong(startString));
			} catch (NumberFormatException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Bad start time '" + startString + "'");
			}
		} else {
			start = set.getStartDate();
		}
			
		Date end;
		if (endString != null) {
			try {
				end = new Date(Long.parseLong(endString));
			} catch (NumberFormatException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Bad end time '" + endString + "'");
			}			
		} else {
			end = set.getEndDate();
		}
		
		int timescaleSeconds;
		if (timescaleString != null) {
			try {
				timescaleSeconds = Integer.parseInt(timescaleString);
				
			} catch (NumberFormatException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Bad timescale seconds value '" + timescaleString + "'");
			}
		} else {
			// If the user doesn't specify a timescale, try to get about 200 points
			timescaleSeconds = (int)((end.getTime() - start.getTime()) / 1000L / 200L);
		}
		
		Timescale timescale = Timescale.get(timescaleSeconds);
		
		xml.openElement("statistics", 
				        "timescale", Integer.toString(timescale.getSeconds()),
				        "setStartTime", Long.toString(set.getStartDate().getTime()),
				        "setEndTime", Long.toString(set.getEndDate().getTime()));
		
		Iterator<Row> iterator = set.getIterator(start, end, timescale, columnIndexes);
		while (iterator.hasNext()) {
			Row row = iterator.next();
			xml.openElement("row", "time", Long.toString(row.getDate().getTime()));
			for (int i = 0; i < columnIndexes.length; i++) {
				if (i != 0)
					xml.append(',');
				xml.append(Long.toString(row.value(i)));
			}
			xml.closeElement();
		}
		
		xml.closeElement();
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
 	public void doDeleteFile(XmlBuilder xml, UserViewpoint viewpoint, Guid fileId) throws XmlMethodException {
		TxUtils.assertNoTransaction();
		try {
			sharedFileSystem.deleteFile(viewpoint, fileId);
		} catch (HumanVisibleException e) {
			throw new XmlMethodException(XmlMethodErrorCode.FAILED, e.getMessage());
		}
 	}

	public void getUserSummary(XmlBuilder xml, Viewpoint viewpoint, User who, boolean includeStack, boolean participantOnly) throws XmlMethodException {
		Set<ExternalAccountView> externalAccountViews = externalAccountSystem.getExternalAccountViews(viewpoint, who);
		
		List<BlockView> stack;
		if (includeStack) {
			Pageable<BlockView> pageable = new Pageable<BlockView>("stack");
			pageable.setPosition(0);
			pageable.setInitialPerPage(5);
			stacker.pageStack(viewpoint, who, pageable, participantOnly);
			stack = pageable.getResults();
		} else {
			stack = Collections.emptyList();
		}
		
		PersonView userView = personViewer.getPersonView(viewpoint, who);
		
		xml.openElement("userSummary",
				"who", who.getId(),
				"online", Boolean.toString(userView.isOnline()),
				"onlineIcon", userView.getOnlineIcon(),
				"photo", userView.getPhotoUrl(),
				"name", userView.getName(),
				"homeUrl", userView.getHomeUrl());
		
		xml.openElement("addresses");

		AimResource aim = userView.getAim();
		EmailResource email = userView.getEmail();
		
		if (aim != null)
			xml.appendEmptyNode("address", "type", "aim", "value", aim.getScreenName());
		
		if (email != null)
			xml.appendEmptyNode("address", "type", "email", "value", email.getEmail());
		
		xml.closeElement();
		
		xml.openElement("accounts");
		
		for (ExternalAccountView ea : externalAccountViews) {
			if (ea.getExternalAccount().isLovedAndEnabled())
				ea.writeToXmlBuilder(xml);
		}
		
		xml.closeElement();
		
		xml.openElement("stack");
		
		for (BlockView bv : stack) {
			bv.writeSummaryToXmlBuilder(xml);
		}
		
		xml.closeElement();
	}
	
	private static final String APPLICATIONS_NAMESPACE = "http://dumbhippo.com/protocol/applications";
	
	public void getPopularApplications(XmlBuilder xml, Viewpoint viewpoint, String category) throws XmlMethodException {
		Pageable<ApplicationView> pageable = new Pageable<ApplicationView>("applications");
		pageable.setPosition(0);
		pageable.setInitialPerPage(30);		
		ApplicationCategory cat = null;
		if (category != null) {
			for (ApplicationCategory c : ApplicationCategory.values()) {
				if (c.getDisplayName().equals(category)) {
					cat = c;
					break;
				}
			}
			if (cat == null)
				cat = ApplicationCategory.fromRaw(Collections.singleton(category));
		}
		applicationSystem.pagePopularApplications(null, 24, cat, pageable);
		// Keep in sync with ApplicationsIQHandler
		xml.openElement("topApplications", "xmlns", APPLICATIONS_NAMESPACE,
						"category", cat != null ? cat.getDisplayName() : null,
						"origCategory", category);
		for (ApplicationView application : pageable.getResults()) {
			application.writeToXmlBuilder(xml);		
		}		
		xml.closeElement();
	}
	
	public void getSearchApplications(XmlBuilder xml, Viewpoint viewpoint, String search) throws XmlMethodException {	
		Pageable<ApplicationView> pageable = new Pageable<ApplicationView>("applications");
		pageable.setPosition(0);
		pageable.setInitialPerPage(30);		
		applicationSystem.search(search, 24, null, pageable);
		xml.openElement("applications", "xmlns", APPLICATIONS_NAMESPACE);
		for (ApplicationView application : pageable.getResults()) {
			application.writeToXmlBuilder(xml);		
		}		
		xml.closeElement();			
	}
	
	public void getAllApplications(XmlBuilder xml, Viewpoint viewpoint) throws XmlMethodException {
		xml.openElement("applications", "xmlns", APPLICATIONS_NAMESPACE);		
		applicationSystem.writeAllApplicationsToXml(24, xml);
		xml.closeElement();			
	}

	public void getUserRSS(OutputStream out, HttpResponseData contentType, User who, boolean participantOnly) throws IOException, XmlMethodException {
		
		List<BlockView> stack;
		Pageable<BlockView> pageable = new Pageable<BlockView>("stack");
		pageable.setPosition(0);
		pageable.setInitialPerPage(5);
		stacker.pageStack(AnonymousViewpoint.getInstance(), who, pageable, participantOnly);
		stack = pageable.getResults();
		
		PersonView userView = personViewer.getPersonView(AnonymousViewpoint.getInstance(), who);
		
	    String baseUrlString = config.getProperty(HippoProperty.BASEURL);
	    
	    URL baseUrl = new URL(baseUrlString);
	    URL userPhotoUrl = new URL(baseUrl, userView.getPhotoUrl());
	    URL userHomeUrl = new URL(baseUrl, userView.getHomeUrl());
	    
	    String channelDescription = userView.getBioAsHtml();
	    if (channelDescription == null)
	    	channelDescription = "What " + userView.getName() + " is doing online";

	    RssBuilder rss = new RssBuilder(userView.getName(),
										userHomeUrl,
										channelDescription);
				
		rss.setChannelImage(userPhotoUrl,
				            userView.getName(),
				            userHomeUrl);

		for (BlockView bv : stack) {
			if (!(bv instanceof TitleBlockView))
				continue;

			String description = null;
			if (bv instanceof TitleDescriptionBlockView)
				description = ((TitleDescriptionBlockView) bv).getDescription();
			
			URL url;
			try {
				url = new URL(baseUrl, bv.getSummaryLink());
			} catch (MalformedURLException e) {
				logger.warn("Invalid summary link URL: {}", bv.getSummaryLink());
				continue;
			}
			
			String guid = baseUrlString + "/rss/user/" + userView.getUser().getId() + "/" + bv.getBlock().getId();
			
			rss.addItem(url,
						bv.getSummaryLinkText(),
						description,
						bv.getBlock().getTimestamp(),
						guid);
		}
		
		out.write(rss.getBytes());
	}

	public void doSetApplicationUsageEnabled(UserViewpoint viewpoint, boolean enabled) throws IOException, HumanVisibleException {
		identitySpider.setApplicationUsageEnabled(viewpoint.getViewer(), enabled);
	}
	
	public void doRevertApplication(XmlBuilder builder, UserViewpoint viewpoint, String applicationId, Guid version, String comment) throws XmlMethodException {
		applicationSystem.revertApplication(viewpoint, applicationId, version, comment);
	}

	public static final int NUM_APPLICATION_EDIT_ITEMS = 30;
	
	public void getApplicationEditRSS(OutputStream out, HttpResponseData contentType, Viewpoint viewpoint) throws IOException, XmlMethodException {
		List<AppinfoUploadView> uploads = applicationSystem.getUploadHistory(viewpoint, NUM_APPLICATION_EDIT_ITEMS);
		
	    String baseUrlString = config.getProperty(HippoProperty.BASEURL);
	    URL baseUrl = new URL(baseUrlString);
	    URL homeUrl = new URL(baseUrl, "/applications");

	    RssBuilder rss = new RssBuilder("Mugshot Application Edits",
										homeUrl,
										"Recent changes to the Mugshot application database");
				
		for (AppinfoUploadView upload : uploads) {
			Application application = upload.getUpload().getApplication();
			boolean initialUpload = upload.getUpload().isInitialUpload();
			URL url = new URL(baseUrl, "/application-history?id=" + application.getId() + "&version=" + upload.getUpload().getId());

			String title = (initialUpload ? "Application Upload: " : "Application Edit: ") + application.getName();
			
			XmlBuilder description = new XmlBuilder();
			description.openElement("p");
			description.append(initialUpload ? "Initial upload of " : "Edit to ");
			description.appendEscaped(application.getName());
			description.append(" by ");
			description.appendEscaped(upload.getUploader().getName());
			description.closeElement();
			String comment = upload.getUpload().getComment();
			if (comment != null && !"".equals("comment")) {
				description.openElement("p");
				description.appendEscaped(comment);
				description.closeElement();				
			}
			
			String guid = baseUrlString + "/rss/application-edit/" + upload.getUpload().getId();

			rss.addItem(url, title, description.toString(),
						upload.getUpload().getUploadDate(), guid);
		}
		
		out.write(rss.getBytes());
	}
	
	public void doAddChatMessage(XmlBuilder xml, UserViewpoint viewpoint, Guid chatId, String text, String sentimentString) throws XmlMethodException, RetryException {
		ChatRoomInfo info;
		User user = viewpoint.getViewer();
		
		Sentiment sentiment;
		try {
			sentiment = Sentiment.valueOf(sentimentString);
		} catch (IllegalArgumentException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "Bad value for sentiment");
		}
		
		try {
			info = chatSystem.getChatRoomInfo(chatId, false);
			if (!chatSystem.canJoinChat(info.getChatId(), info.getKind(), user.getGuid()))
				throw new NotFoundException("Chatroom not visible");
		} catch (NotFoundException e) {
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_ARGUMENT, "No such chatroom");
		}
		
		chatSystem.addChatRoomMessage(info.getChatId(), info.getKind(), user.getGuid(), text, sentiment, new Date());
	}
	
	public void getAimVerifyLink(OutputStream out, HttpResponseData contentType, UserViewpoint viewpoint) throws IOException, RetryException {
		if (contentType != HttpResponseData.TEXT)
			throw new IllegalArgumentException("only support TEXT replies");

		String token = claimVerifier.getAuthKey(viewpoint.getViewer(), null);
		String link = "aim:GoIM?screenname=" + config.getPropertyFatalIfUnset(HippoProperty.AIMBOT_NAME) 
			+ "&message=Hey+Bot!+Crunch+this:+" + token;
		
		out.write(StringUtils.getBytes(link));
		out.flush();
	}
}
