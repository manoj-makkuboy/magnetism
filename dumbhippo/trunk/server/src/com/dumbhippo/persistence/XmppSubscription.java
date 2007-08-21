package com.dumbhippo.persistence;

/**
 * XmppSubscription is used to track subscriptions between our XMPP admin address 
 * and users. We need to subscribe to user's presence in order to have permissions
 * to send them a message in normal usage. 
 * 
 * In most cases, we could get away without this table - our presence status never
 * changes, so we don't need to send anything - but there are some corner cases
 * we can't get right without it. For example, if a user adds a resource to their
 * account, removes it, and then adds it back again, we shouldn't try to subscribe
 * to their presence since we are already subscribed.
 * 
 * We store the local JID, since we might send messages from different servers
 * (admin@mugshot.org vs. admin@online.gnome.org) , and subscriptions for each
 * have to be handled separately. 
 * 
 * @author otaylor
 */
public class XmppSubscription extends GuidPersistable {
	String localJid;
	XmppResource remoteResource;
	SubscriptionStatus status;

	public XmppSubscription(String localJid, XmppResource remoteResource) {
		this.localJid = localJid;
		this.remoteResource = remoteResource;
		this.status = SubscriptionStatus.NONE;
	}
	
	public String getLocalJid() {
		return localJid;
	}
	
	protected void setLocalJid(String localJid) {
		this.localJid = localJid;
	}
	
	public XmppResource getRemoteResource() {
		return remoteResource;
	}
	
	protected void setRemoteResource(XmppResource remoteResource) {
		this.remoteResource = remoteResource;
	}
	
	public SubscriptionStatus getStatus() {
		return status;
	}
	
	public void setStatus(SubscriptionStatus status) {
		this.status = status;
	}
}
