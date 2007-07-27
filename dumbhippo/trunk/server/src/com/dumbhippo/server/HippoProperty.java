package com.dumbhippo.server;

public enum HippoProperty {
	BASEURL_MUGSHOT("dumbhippo.server.baseurl", "http://mugshot.org"),
	BASEURL_GNOME("dumbhippo.server.baseurlGnome", "http://online.gnome.org"),
	FEATURES("dumbhippo.server.features", ""),
	DISABLE_AUTHENTICATION("dumbhippo.server.disable.authentication", "false"),
	DISABLE_EMAIL("dumbhippo.server.email.disable", "false"),
	FEEDBACK_EMAIL("dumbhippo.server.email.feedback", "feedback@mugshot.org"),
	DOWNLOADS("dumbhippo.server.downloads", null),
	WEB_VERSION("dumbhippo.server.web.version", null),
	FORCE_IE_ALPHA_IMAGE("dumbhippo.server.web.ie.forcealphaimage", "false"),
	XMPP_ADDRESS("dumbhippo.server.xmpp.address", null),
	XMPP_PORT("dumbhippo.server.xmpp.port", null),
	XMPP_ADMINUSER("dumbhippo.server.xmpp.adminuser", null),
	XMPP_PASSWORD("dumbhippo.server.xmpp.password", null),
	HTTP_PORT("dumbhippo.server.http.port", "8080"),
	APPINFO_DIR("dumbhippo.server.appinfo.dir", null),
	CRASHDUMP_DIR("dumbhippo.server.crashdump.dir", null),
	FILES_SAVEURL("dumbhippo.server.files.saveurl", null),
	LUCENE_INDEXDIR("dumbhippo.server.lucene.indexdir", null),
	AIMBOT_NAME("dumbhippo.server.aimbot.name", null),
	AIM_PRESENCE_KEY("dumbhippo.server.aim.presence.key", null),
	AMAZON_ACCESS_KEY_ID("dumbhippo.server.amazon.accesskeyid", null),
	AMAZON_SECRET_KEY("dumbhippo.server.amazon.secretkey", null),
	AMAZON_ASSOCIATE_TAG_ID("dumbhippo.server.amazon.associatetag", null),
	AMAZON_S3_BUCKET_NAME("dumbhippo.server.amazon.s3bucket", null),
	YAHOO_APP_ID("dumbhippo.server.yahoo.appid", null),
	EBAY_DEVID("dumbhippo.server.ebay.devid", null),
	EBAY_APPID("dumbhippo.server.ebay.appid", null),
	EBAY_CERTID("dumbhippo.server.ebay.certid", null), 
	ENABLE_ADMIN_CONSOLE("dumbhippo.server.adminconsole.enable", "false"),
	NEW_FEATURES("dumbhippo.server.newfeatures", "false"),
	STEALTH_MODE("dumbhippo.server.stealthmode", "false"),
	SLAVE_MODE("dumbhippo.server.slavemode", "false"),	
	USER_UPDATE_INTERVAL("dumbhippo.server.userupdateinterval", "300"), // 5 minutes
	SUMMIT_GROUP_GUID("dumbhippo.server.summitgroupguid", null),	
	NEW_USER_INVITATION_COUNT("dumbhippo.server.newuserinvitationcount", "0"),
	SPECIAL_NEW_USER_INVITATION_COUNT("dumbhippo.server.specialnewuserinvitationcount", "5"), 
	FLICKR_API_ID("dumbhippo.server.flickr.apiid", null),
	FACEBOOK_API_KEY("dumbhippo.server.facebook.apikey", null),
	FACEBOOK_SECRET("dumbhippo.server.facebook.secret", null),	
	BIND_HOST("dumbhippo.server.bind.host", null), 
	GOOGLE_ANALYTICS_KEY("dumbhippo.server.googleanalytics.key", "");
	
	private String key;
	private String def;
	
	HippoProperty(String key, String def) {
		this.key = key;
		this.def = def;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getDefault() {
		return def;
	}
}
