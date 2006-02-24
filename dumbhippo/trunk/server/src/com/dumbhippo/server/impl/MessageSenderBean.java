package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.RandomToken;
import com.dumbhippo.live.Hotness;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LivePostBoard;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

/**
 * Send out messages when events happen (for now, when a link is shared).
 * 
 * Can use Jabber for account holders and email etc. in other situations.
 * 
 * The way this class works is that it has inner classes that work as delegates.
 * The outer class just picks the right delegate to send a particular thing
 * in a particular context.
 * 
 * @author hp
 * 
 */
@Stateless
public class MessageSenderBean implements MessageSender {
	static private final Logger logger = GlobalSetup.getLogger(MessageSenderBean.class);

	// Injected beans, some are logically used by delegates but we can't 
	// inject into the delegate objects.
	
	@EJB
	private Configuration config;

	@EJB
	private Mailer mailer;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private InvitationSystem invitationSystem;

	@EJB
	private NoMailSystem noMail;
	
	@EJB 
	private LivePostBoard livePostBoard;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	// Our delegates
	
	private XMPPSender xmppSender;
	private EmailSender emailSender;
	
	private static class LinkExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "link";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private String senderName;
		private Guid senderGuid;
		private String senderPhotoUrl;
		
		private Set<PersonView> recipients;
		
		private String url;
		private String postInfo;

		private Guid guid;

		private String title;
		
		private String description;

		private Set<String> groupRecipients;
		
		private Set<PersonView> viewers;
		
		private String timeout;

		public LinkExtension() {
		}
		
		public void setSenderName(String senderName) {
			this.senderName = senderName;
		}
		public void setSenderGuid(Guid senderGuid) {
			this.senderGuid = senderGuid;
		}
		public void setPostId(Guid postId) {
			this.guid = postId;
		}
		public void setRecipients(Set<PersonView> recipients) {
			this.recipients = recipients;
		}
		public void setGroupRecipients(Set<String> groupRecipients) {
			this.groupRecipients = groupRecipients;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public void setViewers(Set<PersonView> viewers) {
			this.viewers = viewers;
		}
		
		private static void appendPersonViews(XmlBuilder builder, String outerNode, String nodeName, Set<PersonView> views) {
			builder.openElement(outerNode);
			for (PersonView recipient : views) {
				Account acct = recipient.getAccount();
				if (acct != null)
					builder.appendTextNode(nodeName, recipient.getName(), "id", acct.getId());
				else
					builder.appendTextNode(nodeName, recipient.getName());
			}
			builder.closeElement();
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("link", "id", guid.toString(), "xmlns", NAMESPACE, "href", url);
			builder.appendTextNode("senderName", senderName, "isCache", "true");
			builder.appendTextNode("senderGuid", senderGuid.toString());
			builder.appendTextNode("senderPhotoUrl", senderPhotoUrl);
			builder.appendTextNode("title", title);
			builder.appendTextNode("description", description);
			builder.appendTextNode("postInfo", postInfo);
			if (timeout != null)
				builder.appendTextNode("timeout", timeout);			
			appendPersonViews(builder, "recipients", "recipient", recipients);
			builder.openElement("groupRecipients");
			for (String recipient : groupRecipients) {
				builder.appendTextNode("recipient", recipient);
			}
			builder.closeElement();
			if (viewers != null) {
				appendPersonViews(builder, "viewers", "viewer", viewers);			
			}
			builder.closeElement();
			return builder.toString();
		}
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String getPostInfo() {
			return postInfo;
		}

		public void setPostInfo(String postInfo) {
			this.postInfo = postInfo;
		}

		public void setSenderPhotoUrl(String senderPhotoUrl) {
			this.senderPhotoUrl = senderPhotoUrl;
		}

		public void setTimeout(String timeout) {
			this.timeout = timeout;
		}
	}
	
	private static class LinkClickedExtension implements PacketExtension {

		private static final String ELEMENT_NAME = "linkClicked";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/linkshare";
		
		private Guid swarmerGuid;
		private String clickerName;

		private Guid guid;

		private String title;
		
		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("linkClicked", "xmlns", NAMESPACE, "id", guid.toString(), "swarmerId", swarmerGuid.toString());
			builder.appendTextNode("swarmerName", clickerName, "isCache", "true");	
			builder.appendTextNode("postTitle", title, "isCache", "true");
			builder.closeElement();
			return builder.toString();
		}
		public LinkClickedExtension(Guid swarmerGuid, String clickerName, Guid postId, String title) {
			this.swarmerGuid = swarmerGuid;
			this.clickerName = clickerName;
			this.guid = postId;
			this.title = title;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}
	}	
	
	private static class MySpaceNameChangedExtension implements PacketExtension {
		private static final String ELEMENT_NAME = "mySpaceNameChanged";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/myspace";
		
		private String mySpaceName;

		public MySpaceNameChangedExtension(String newMySpaceName) {
			mySpaceName = newMySpaceName;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("mySpaceNameChanged", "xmlns", NAMESPACE, "name", mySpaceName);
			builder.closeElement();
			return builder.toString();
		}
	}
	
	private static class MySpaceContactCommentExtension implements PacketExtension {
		private static final String ELEMENT_NAME = "mySpaceContactComment";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/myspace";
		
		public MySpaceContactCommentExtension() {
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement("mySpaceContactComment", "xmlns", NAMESPACE);
			builder.closeElement();
			return builder.toString();
		}
	}
	
	private static class HotnessChangedExtension implements PacketExtension {
		private static final String ELEMENT_NAME = "hotness";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/hotness";
		
		private Hotness hotness;
		
		public HotnessChangedExtension(Hotness hotness) {
			this.hotness = hotness;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement(ELEMENT_NAME, "xmlns", NAMESPACE, "value", hotness.name());
			builder.closeElement();
			return builder.toString();
		}
	}
	
	private static class ActivePostsChangedExtension implements PacketExtension {
		private static final String ELEMENT_NAME = "activePostsChanged";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/liveposts";
		
		List<String> livePosts;
		
		public ActivePostsChangedExtension(List<String> livePosts) {
			this.livePosts = livePosts;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement(ELEMENT_NAME, "xmlns", NAMESPACE);
			for (String postXml : livePosts) {
				builder.openElement("livePost");
				builder.append(postXml);
				builder.closeElement();
			}
			builder.closeElement();
			return builder.toString();
		}
	}

	private static class PrefsChangedExtension implements PacketExtension {
		private static final String ELEMENT_NAME = "prefs";

		private static final String NAMESPACE = "http://dumbhippo.com/protocol/prefs";
		
		Map<String,String> prefs;
		
		public PrefsChangedExtension(Map<String,String> prefs) {
			this.prefs = new HashMap<String,String>(prefs);
		}

		public PrefsChangedExtension(String key, String value) {
			this(Collections.singletonMap(key, value)); 
		}
		
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		public String toXML() {
			XmlBuilder builder = new XmlBuilder();
			builder.openElement(ELEMENT_NAME, "xmlns", NAMESPACE);
			for (String key : prefs.keySet()) {
				builder.appendTextNode("prop", prefs.get(key), "key", key);
			}
			builder.closeElement();
			return builder.toString();
		}
	}
	
	private class XMPPSender {

		private XMPPConnection connection;
				
		private synchronized XMPPConnection getConnection() {
			if (connection != null && !connection.isConnected()) {
				logger.info("Disconnected from XMPP server");
			}
			if (connection == null || !connection.isConnected()) {			
				try {
					String addr = config.getPropertyNoDefault(HippoProperty.XMPP_ADDRESS);
					String port = config.getPropertyNoDefault(HippoProperty.XMPP_PORT);
					String user = config.getPropertyNoDefault(HippoProperty.XMPP_ADMINUSER);
					String password = config.getPropertyNoDefault(HippoProperty.XMPP_PASSWORD);
					connection = new XMPPConnection(addr, Integer.parseInt(port.trim()));
					// We need to use a separate resource ID for each connection
					// TODO create an overoptimized XMPP connection pool 
					RandomToken token = RandomToken.createNew();
					connection.login(user, password, StringUtils.hexEncode(token.getBytes()));
					logger.info("Successfully reconnected to XMPP server");
				} catch (XMPPException e) {
					logger.error("Failed to log in to XMPP server", e);
					connection = null;
				} catch (PropertyNotFoundException e) {
					logger.error("configuration is f'd up, can't connect to XMPP", e);
					connection = null;
				}
			}

			return connection;
		}
		
		private Message createMessageFor(User user, Message.Type type) {
			// FIXME should dumbhippo.com domain be hardcoded here?			
			return new Message(user.getGuid().toJabberId("dumbhippo.com"), type);
		}
		
		private Message createMessageFor(User user) {
			return createMessageFor(user, Message.Type.NORMAL);
		}

		public synchronized void sendPostNotification(User recipient, Post post, List<User> viewers, boolean isTutorialPost) {
			XMPPConnection connection = getConnection();

			if (connection == null || !connection.isConnected()) {
				logger.warn("Connection to XMPP is not active, not sending notification for post {}", post.getId());
				return;
			}
			
			Message message = createMessageFor(recipient); 

			String title = post.getTitle();
			
			String url = post.getUrl() != null ? post.getUrl().toExternalForm() : null;
			
			if (url == null) {
				// this particular jabber message protocol has no point without an url
				logger.debug("no url found on post, not sending xmpp");
				return;
			}
			
			Viewpoint viewpoint = new Viewpoint(recipient);

			PersonView recipientView = identitySpider.getPersonView(viewpoint, post.getPoster());
			String senderName = recipientView.getName();
			Set<PersonView> recipientViews = new HashSet<PersonView>();
			for (Resource r : post.getPersonRecipients()) {
				PersonView viewedP = identitySpider.getPersonView(viewpoint, r, PersonViewExtra.PRIMARY_RESOURCE);
				recipientViews.add(viewedP);
			}
			
			Set<String> groupRecipientNames = new HashSet<String>();
			for (Group g : post.getGroupRecipients()) {
				groupRecipientNames.add(g.getName());
			}
			
			LinkExtension extension = new LinkExtension();
			extension.setPostId(post.getGuid());
			extension.setSenderName(senderName);
			extension.setSenderGuid(post.getPoster().getGuid());
			extension.setSenderPhotoUrl(recipientView.getSmallPhotoUrl());
			extension.setRecipients(recipientViews);
			extension.setGroupRecipients(groupRecipientNames);
			extension.setUrl(url);
			extension.setTitle(title);
			extension.setDescription(post.getText());
			extension.setPostInfo(post.getInfo());
			if (isTutorialPost)
				extension.setTimeout("-1");
			
			if (viewers != null) {
				Set<PersonView> viewerNames = new HashSet<PersonView>();
				for (User u : viewers) {
					PersonView viewedP = identitySpider.getPersonView(viewpoint, u);
					viewerNames.add(viewedP);
				}
				extension.setViewers(viewerNames);
			}
			
			message.addExtension(extension);

			message.setBody(String.format("%s\n%s", title, url));

			logger.debug("Sending jabber message to {}", message.getTo());
			connection.sendPacket(message);
		}
		
		public synchronized void sendPostClickedNotification(Post post, User clicker) {
			XMPPConnection connection = getConnection();

			for (Resource recipientResource : post.getExpandedRecipients()) {
				User recipient = identitySpider.getUser(recipientResource);
				if (recipient == null) {
					logger.debug("No user for resource {}", recipientResource.getId());
					// FIXME it looks like a bug that recipient can be null here and we
					// go on to use it below - investigate
				}
				
				Message message = createMessageFor(recipient);

				Viewpoint viewpoint = new Viewpoint(recipient);
				PersonView senderView = identitySpider.getPersonView(viewpoint, clicker);
				String clickerName = senderView.getName();
				String title = post.getTitle();
				if (title == null || title.equals("")) {
					LinkResource link = null;
					// FIXME don't assume link resources
					Set<Resource> resources = post.getResources();					
					for (Resource r : resources) {
						if (r instanceof LinkResource) {
							link = (LinkResource) r;
							break;
						}
					}
					if (link != null)
						title = link.getHumanReadableString();
					else
						title = "(unknown)";
				}
				message.addExtension(new LinkClickedExtension(recipient.getGuid(), clickerName, post.getGuid(), title));
				message.setBody("");
				logger.debug("Sending jabber message to {}", message.getTo());
				connection.sendPacket(message);
			}
		}

		public void sendMySpaceNameChangedNotification(User user) {
			XMPPConnection connection = getConnection();
			Message message = createMessageFor(user, Message.Type.HEADLINE);
			String newMySpaceName = user.getAccount().getMySpaceName();
			message.addExtension(new MySpaceNameChangedExtension(newMySpaceName));
			logger.debug("Sending mySpaceNameChanged message to {}", message.getTo());			
			connection.sendPacket(message);
		}

		public void sendMySpaceContactCommentNotification(User user) {
			XMPPConnection connection = getConnection();
			Message message = createMessageFor(user, Message.Type.HEADLINE);
			message.addExtension(new MySpaceContactCommentExtension());
			logger.debug("Sending mySpaceContactComment message to {}", message.getTo());			
			connection.sendPacket(message);
		}

		public void sendHotnessChanged(LiveUser user) {
			XMPPConnection connection = getConnection();
			User dbUser = identitySpider.lookupUser(user);			
			Message message = createMessageFor(dbUser, Message.Type.HEADLINE);
			message.addExtension(new HotnessChangedExtension(user.getHotness()));
			logger.debug("Sending hotnessChanged message to {}", message.getTo());			
			connection.sendPacket(message);
		}

		public void sendActivePostsChanged(LiveUser user) {
			XMPPConnection connection = getConnection();
			User dbUser = identitySpider.lookupUser(user);
			Message message = createMessageFor(dbUser, Message.Type.HEADLINE);
			List<String> livePosts = new ArrayList<String>();
			LiveState state = LiveState.getInstance();
			for (Guid guid : user.getActivePosts()) {
				LivePost post = state.peekLivePost(guid);
				if (post == null)
					continue;
				String xml = livePostBoard.getLivePostXML(new Viewpoint(dbUser), post);
				livePosts.add(xml);
			}
			message.addExtension(new ActivePostsChangedExtension(livePosts));
			logger.debug("Sending activePostsChanged message to {}", message.getTo());			
			connection.sendPacket(message);						
		}
		
		public void sendPrefChanged(User user, String key, String value) {
			XMPPConnection connection = getConnection();
			Message message = createMessageFor(user, Message.Type.HEADLINE);
			message.addExtension(new PrefsChangedExtension(key, value));
			logger.debug("Sending prefs changed message to {}", message.getTo());			
			connection.sendPacket(message);
		}
	}

	private class EmailSender {

		public void sendPostNotification(EmailResource recipient, Post post) {
			if (!noMail.getMailEnabled(recipient)) {
				logger.debug("Mail is disabled to {} not sending post notification", recipient);
				return;
			}
			
			String baseurl = config.getProperty(HippoProperty.BASEURL);
			
			// may be null!
			InvitationToken invitation = invitationSystem.updateValidInvitation(post.getPoster(), recipient); 
			String recipientInviteUrl;
			if (invitation != null) 
				recipientInviteUrl = invitation.getAuthURL(config.getProperty(HippoProperty.BASEURL)); 
			else
				recipientInviteUrl = null;
				
			String recipientStopUrl;
			
			if (recipientInviteUrl == null) {
				recipientStopUrl = noMail.getNoMailUrl(recipient, NoMailSystem.Action.NO_MAIL_PLEASE);
			} else {
				recipientStopUrl = recipientInviteUrl + "&disable=true";
			}
			
			// Since the recipient doesn't have an account, we can't get the recipient's view
			// of the poster. Send out information from the poster's view of themself.
			PersonView posterViewedBySelf = identitySpider.getPersonView(new Viewpoint(post.getPoster()), 
					                                                     post.getPoster(),
					                                                     PersonViewExtra.PRIMARY_EMAIL);
			
			StringBuilder messageText = new StringBuilder();
			XmlBuilder messageHtml = new XmlBuilder();
			
			messageHtml.appendHtmlHead("");
			messageHtml.append("<body>\n");
			
			messageHtml.append("<div style=\"width:500px\">\n");
			messageHtml.append("  <div style=\"border:1px solid black;min-height:100px;\"><!-- bubble div -->\n");
			
			PostView postView = new PostView(ejbContext, post, recipient);
			
			String url = postView.getUrl();

			// TEXT: put in the link
			
			messageText.append(url);
			messageText.append("\n");
			
			// HTML: put in the link, with redirect url stuff
			
			StringBuilder redirectUrl = new StringBuilder();
			redirectUrl.append(baseurl);
			redirectUrl.append("/redirect?url=");
			redirectUrl.append(StringUtils.urlEncode(url));
			redirectUrl.append("&postId=");
			redirectUrl.append(post.getId()); // no need to encode, we know it is OK

			if (invitation != null) {
				redirectUrl.append("&inviteKey=");
				redirectUrl.append(invitation.getAuthKey());
			}
			
			String f = "<div style=\"margin:0.3em;\">\n" 
				+ "<a style=\"font-weight:bold;font-size:150%%;\" title=\"%s\" href=\"%s\">%s</a>\n"
				+ "</div>\n";
			 
			messageHtml.append(String.format(f, XmlBuilder.escape(url),
					XmlBuilder.escape(redirectUrl.toString()),
					postView.getTitleAsHtml()));

			// TEXT: append post text
			messageText.append("\n");
			messageText.append(postView.getText());
			messageText.append("\n");
			
			// HTML: append post text
			messageHtml.append("<div style=\"font-size:120%;margin:0.5em;\">\n");
			messageHtml.append(postView.getTextAsHtml());
			messageHtml.append("</div>\n");

			messageHtml.append("  </div><!-- close bubble div -->\n");
			
			// TEXT: "link shared by"
			
			messageText.append("  (Link shared by " + posterViewedBySelf.getName() + ")");
			
			String viewPersonPageId = posterViewedBySelf.getViewPersonPageId();
			String posterPublicPageUrl = null;
			if (viewPersonPageId != null)
				posterPublicPageUrl = baseurl + "/person?who=" + viewPersonPageId;
						
			// HTML: "link shared by"
			String recipientLink;
			if (recipientInviteUrl != null) {
				recipientLink = String.format("<a href=\"%s\">%s</a>",
						XmlBuilder.escape(recipientInviteUrl),
						XmlBuilder.escape(recipient.getEmail())); 
			} else {
				recipientLink = XmlBuilder.escape(recipient.getEmail());
			}
			messageHtml.append("<div style=\"margin:0.2em;font-style:italic;text-align:right;font-size:small;vertical-align:bottom;\">");
			String format = "(Link shared from "
				+ "<a title=\"%s\" href=\"%s\">%s</a> "
				+ "to %s)\n"
				+ "</div>\n";
			messageHtml.append(String.format(format, XmlBuilder.escape(posterViewedBySelf.getEmail().getEmail()),
					// FIXME posterPublicPageUrl in theory could be null (not actually right now afaik)
						XmlBuilder.escape(posterPublicPageUrl),
						XmlBuilder.escape(posterViewedBySelf.getName()),
						recipientLink)); 
			
			// TEXT: append footer
			messageText.append("\n\n");
			if (recipientInviteUrl != null) {
				messageText.append("      " + posterViewedBySelf.getName()
						+ " created an invitation for you: " + recipientInviteUrl + "\n");
			}
			if (recipientStopUrl != null) {
				messageText.append("      To stop getting these mails, go to " + recipientStopUrl + "\n");
			}
			
			// HTML: append footer
			
			if (recipientInviteUrl != null) {
				format = "<div style=\"text-align:center;margin-top:1em;font-size:9pt;\">\n"
					+ "<a href=\"%s\">%s</a> created an open "
					+ "<a href=\"%s\">invitation for you</a> to use <a href=\"%s\">Dumb Hippo</a>\n"
					+ "</div>\n";
				messageHtml.append(String.format(format, 
						// FIXME handle null public page url
						XmlBuilder.escape(posterPublicPageUrl),
						XmlBuilder.escape(posterViewedBySelf.getName()),
						XmlBuilder.escape(recipientInviteUrl),
						XmlBuilder.escape(baseurl)));
			}
			
			String stopLink;
			if (recipientStopUrl != null)
				stopLink = String.format("| <a style=\"font-size:8pt;\" href=\"%s\">Stop Getting These Mails</a>",
						XmlBuilder.escape(recipientStopUrl));
			else
				stopLink = "";
			
			format = "<div style=\"text-align:center;margin-top:1em;font-size:8pt;\">\n" 
				+ "<a style=\"font-size:8pt;\" href=\"%s\">What's DumbHippo?</a> %s\n"
				+ "</div>\n";
			messageHtml.append(String.format(format,
					recipientInviteUrl != null ? XmlBuilder.escape(recipientInviteUrl) : XmlBuilder.escape(baseurl),
					stopLink));
 			
			messageHtml.append("</div>\n");
			messageHtml.append("</body>\n</html>\n");
					
			MimeMessage msg = mailer.createMessage(post.getPoster(), recipient.getEmail());
			
			mailer.setMessageContent(msg, postView.getTitle(), messageText.toString(), messageHtml.toString());
			
			logger.debug("Sending mail to {}", recipient);
			mailer.sendMessage(msg);
		}
	}
	
	public MessageSenderBean() {
		this.emailSender = new EmailSender();
		this.xmppSender = new XMPPSender();
	}
	
	public void sendPostNotification(Resource recipient, Post post, boolean isTutorialPost) {
		User user = identitySpider.getUser(recipient);
		if (user != null) {
			Account account = user.getAccount();
			xmppSender.sendPostNotification(account.getOwner(), post, null, isTutorialPost);
		} else if (recipient instanceof EmailResource) {
			emailSender.sendPostNotification((EmailResource)recipient, post);
		} else {
			throw new IllegalStateException("Don't know how to send a notification to resource: " + recipient);
		}
	}

	public void sendPostClickedNotification(Post post, List<User> viewers, User clicker) {
		for (Resource recipientResource : post.getExpandedRecipients()) {
			User recipient = identitySpider.getUser(recipientResource);
			if (recipient != null) {
				if (!recipient.equals(clicker))
					xmppSender.sendPostNotification(recipient, post, viewers, false);
			}
		}
	}

	public void sendMySpaceNameChangedNotification(User user) {
		xmppSender.sendMySpaceNameChangedNotification(user);
	}

	public void sendMySpaceContactCommentNotification(User user) {
		xmppSender.sendMySpaceContactCommentNotification(user);
	}

	public void sendHotnessChanged(LiveUser user) {
		xmppSender.sendHotnessChanged(user);
	}

	public void sendActivePostsChanged(LiveUser user) {
		xmppSender.sendActivePostsChanged(user);
	}
	
	public void sendPrefChanged(User user, String key, String value) {
		xmppSender.sendPrefChanged(user, key, value);
	}
}
