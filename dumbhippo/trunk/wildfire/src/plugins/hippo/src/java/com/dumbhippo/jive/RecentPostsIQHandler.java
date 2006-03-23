package com.dumbhippo.jive;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

public class RecentPostsIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public RecentPostsIQHandler() {
		super("DumbHippo Recent Posts IQ Handler");
		
		Log.debug("creating Hotness handler");
		info = new IQHandlerInfo("recentPosts", "http://dumbhippo.com/protocol/post");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		String recentPostsString = glue.getRecentPostsXML(from.getNode());
		Document recentPostsDocument;
		try {
			recentPostsDocument = DocumentHelper.parseText(recentPostsString);
		} catch (DocumentException e) {
			throw new RuntimeException("Couldn't parse result from getRecentPostsXML()");
		}
		
		Element childElement = recentPostsDocument.getRootElement();
		childElement.detach();
		reply.setChildElement(childElement);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
