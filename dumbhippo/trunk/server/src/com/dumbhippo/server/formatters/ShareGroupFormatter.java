package com.dumbhippo.server.formatters;

import javax.ejb.EJBContext;

import org.slf4j.Logger;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.ShareGroupPostInfo;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.util.EJBUtil;

public class ShareGroupFormatter extends DefaultFormatter {

	static private final Logger logger = GlobalSetup.getLogger(ShareGroupFormatter.class);
	
	private String inviterName;
	
	@Override
	public void init(PostView postView, EJBContext ejbContext) {
		super.init(postView, ejbContext);

		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return;
		
		ShareGroupPostInfo shareGroupInfo = (ShareGroupPostInfo) postInfo;
		
		String groupId = shareGroupInfo.getGroupId();
		
		IdentitySpider identitySpider = EJBUtil.contextLookup(ejbContext, IdentitySpider.class);
		GroupSystem groupSystem = EJBUtil.contextLookup(ejbContext, GroupSystem.class);
		
		Group group = null;
		GroupMember member = null;
		try {
			group = identitySpider.lookupGuidString(Group.class, groupId);
			
			if (postView.getContext() == PostView.Context.MAIL_NOTIFICATION)
				member = groupSystem.getGroupMember(group, postView.getMailRecipient());
			else if (postView.getViewpoint().getViewer() != null)
				member = groupSystem.getGroupMember(postView.getViewpoint(), group, postView.getViewpoint().getViewer());;
		} catch (ParseException e) {
			logger.warn("Bad group ID {} in post {}", groupId, postView.getPost().getId());
			logger.warn("Parse exception on group id", e);
		} catch (NotFoundException e) {
			logger.warn("Bad group ID {} in post {} or recipient has no GroupMember", groupId, postView.getPost().getId());
			logger.warn("NotFoundException on group id", e);
		}
		
		if (member != null) {
			User adder = member.getAdder();
			// adder is null if you created the group, I believe
			if (adder != null) {
				String nick = adder.getNickname();
				if (nick != null) {
					FullName parsed = FullName.parseHumanString(nick);
					inviterName = parsed.getFirstName();
				}
			}
		}
	}
	
	@Override
	public String getTextAsText() {
		StringBuilder sb = new StringBuilder();
		if (inviterName != null) {
			sb.append("        ");
			sb.append(inviterName);
			sb.append(" invited you to join this group.\n");
		}
		sb.append(super.getTextAsText());
		return sb.toString();
	}
	
	@Override
	public String getTextAsHtml() {		
		XmlBuilder xml = new XmlBuilder();
		
		xml.append("<div>");
		if (inviterName != null) {
			xml.append("<div><span style=\"width: 3em;\"></span><em>");
			xml.appendEscaped(inviterName);
			xml.appendEscaped(" invited you to join this group.");
			xml.append("</em>");
			xml.append("</div>");
		}
		
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		xml.append("</div>");
		
		return xml.toString();
	}
}
