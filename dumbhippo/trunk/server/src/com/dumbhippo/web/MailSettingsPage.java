package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ToggleNoMailToken;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.PersonView;

public class MailSettingsPage {
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(MailSettingsPage.class);
	
	@FromJspContext(value="dumbhippo.toggleNoMailToken", scope=Scope.SESSION)
	private ToggleNoMailToken token;
	@Signin
	private SigninBean signin;
	
	private NoMailSystem noMail;
	
	private PersonView person;
	
	public MailSettingsPage() {
		noMail = WebEJBUtil.defaultLookup(NoMailSystem.class);
	}
	
	public SigninBean getSignin() { 
		return signin;
	}

	public PersonView getPerson() {
		if (person == null) {
			IdentitySpider spider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			person = spider.getPersonView(signin.getViewpoint(), signin.getUser()); // don't get any extras
		}
		
		return person;
	}
	
	public String getEmail() {
		if (token == null)
			return null;
		else
			return token.getEmail().getEmail();
	}
	
	public boolean getEnabled() {
		return noMail.getMailEnabled(token.getEmail());
	}
	
	public String getEnableLink() {
		return noMail.getNoMailUrl(token, NoMailSystem.Action.WANTS_MAIL);
	}
	
	public String getDisableLink() {
		return noMail.getNoMailUrl(token, NoMailSystem.Action.NO_MAIL_PLEASE);
	}
}
