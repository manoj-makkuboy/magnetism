package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.jms.ObjectMessage;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.botcom.BotTask;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.SigninSystem;

@Stateless
public class SigninSystemBean implements SigninSystem {

	static private final Logger logger = GlobalSetup.getLogger(SigninSystemBean.class);
	
	@EJB
	private IdentitySpider identitySpider;

	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private LoginVerifier loginVerifier;
	
	@EJB
	private Configuration configuration;
	
	@EJB
	private Mailer mailer;
	
	private String getLoginLink(Resource resource) throws HumanVisibleException {
		LoginToken token = loginVerifier.getLoginToken(resource);
		return token.getAuthURL(configuration.getPropertyFatalIfUnset(HippoProperty.BASEURL));
	}
	
	public void sendSigninLink(String address) throws HumanVisibleException {
		if (address.contains("@")) {
			EmailResource resource = identitySpider.getEmail(address);
			String link = getLoginLink(resource);
			MimeMessage message = mailer.createMessage(Mailer.SpecialSender.LOGIN, resource.getEmail());
			
			StringBuilder bodyText = new StringBuilder();
			XmlBuilder bodyHtml = new XmlBuilder();
			
			bodyText.append("\n");
			bodyText.append("Go to: " + link + "\n");
			bodyText.append("\n");
			
			bodyHtml.appendHtmlHead("");
			bodyHtml.append("<body>\n");
			bodyHtml.appendTextNode("a", "Click here to sign in", "href", link);
			bodyHtml.append("</body>\n</html>\n");
			
			mailer.setMessageContent(message, "Sign in to DumbHippo", bodyText.toString(), bodyHtml.toString());
			mailer.sendMessage(message);
		} else {
			AimResource resource;
			try {
				resource = identitySpider.getAim(address);
			} catch (ValidationException e) {
				throw new HumanVisibleException(e.getMessage());
			}
			String link = getLoginLink(resource);
			XmlBuilder bodyHtml = new XmlBuilder();
			bodyHtml.appendTextNode("a", "Click to sign in", "href", link);
			
			BotTaskMessage message = new BotTaskMessage(null, resource.getScreenName(), bodyHtml.toString());
			JmsProducer producer = new JmsProducer(BotTask.QUEUE, true);
			ObjectMessage jmsMessage = producer.createObjectMessage(message);
			producer.send(jmsMessage);
		}
	}

	public Pair<Client, User> authenticatePassword(String address, String password, String clientIdentifier) throws HumanVisibleException {
		Resource resource;
		
		boolean noAuthentication = configuration.getProperty(HippoProperty.DISABLE_AUTHENTICATION).equals("true"); 
		if (noAuthentication) {
			logger.warn("Not requiring authentication for address {}", address);
		}
		
		if (address.contains("@")) {
			resource = identitySpider.getEmail(address);
		} else {
			try {
				resource = identitySpider.getAim(address);
			} catch (ValidationException e) {
				throw new HumanVisibleException(e.getMessage());
			}
		}
		
		Account account;
		
		User user = identitySpider.lookupUserByResource(resource);
		if (user == null && noAuthentication) {
			logger.warn("Creating new account for resource: {}", resource);
			account = accountSystem.createAccountFromResource(resource);
			user = account.getOwner();
		} else if (user == null) {
			throw new HumanVisibleException("Hmm, there doesn't seem to be an account with the "
					+ ((resource instanceof EmailResource) ? " email address '" : " screen name '") + address + "'");
		} else {
			account = accountSystem.lookupAccountByUser(user);
			assert account != null;
		}
		
		if (account.checkPassword(password) || noAuthentication) {
			Client client = accountSystem.authorizeNewClient(account, clientIdentifier);
			return new Pair<Client,User>(client, user);
		} else {
			throw new HumanVisibleException("You seem to have mistyped your password; or maybe you haven't "
					+ "set a password yet? Try sending yourself a sign-in link if you can't remember your password.");
		}
	}

	public void setPassword(User user, String password) throws HumanVisibleException {
		// password may be null
		
		if (password != null && password.length() < 4) {
			// last-ditch check for reasonable password, a full check would go 
			// elsewhere
			throw new HumanVisibleException("That password is too short, anyone could guess it");
		}
		
		// be sure we get a new account so it's attached
		Account account = accountSystem.lookupAccountByUser(user);
		account.setPassword(password);
	}
}
