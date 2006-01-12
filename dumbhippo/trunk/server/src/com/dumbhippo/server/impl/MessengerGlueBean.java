package com.dumbhippo.server.impl;

import java.util.Date;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostingBoard;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	static private final Log logger = GlobalSetup.getLog(MessengerGlueBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
		
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private PostingBoard postingBoard;
		
	private Account accountFromUsername(String username) throws JabberUserNotFoundException {
		Guid guid;
		try {
			guid = Guid.parseJabberId(username);
		} catch (ParseException e) {
			throw new JabberUserNotFoundException("corrupt username");
		}
		Account account = accountSystem.lookupAccountByPersonId(guid.toString());
		if (account == null)
			throw new JabberUserNotFoundException("username does not exist");
		
		assert account.getOwner().getId().equals(username);
		
		return account;
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		Account account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			return false;
		}
		
		assert account != null;
		
		return account.checkClientCookie(token, digest);
	}
	

	public long getJabberUserCount() {
		return accountSystem.getNumberOfActiveAccounts();
	}


	public void setName(String username, String name)
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


	public void setEmail(String username, String email) 
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public JabberUser loadUser(String username) throws JabberUserNotFoundException {
		
		Account account = accountFromUsername(username);
		
		PersonView view = identitySpider.getSystemView(account.getOwner(), PersonViewExtra.PRIMARY_EMAIL);
		
		JabberUser user = new JabberUser(username, account.getOwner().getNickname(), view.getEmail().getEmail());
	
		return user;
	}

	public void serverStartup(long timestamp) {
		logger.debug("Jabber server startup at " + new Date(timestamp));
	}
	
	public void onUserAvailable(String username) {
		logger.debug("Jabber user " + username + " now available");
		try {
			// account could be missing due to debug users or our own send-notifications
			// user, i.e. any user on the jabber server that we don't know about
			Account account;
			try {
				account = accountFromUsername(username);
			} catch (JabberUserNotFoundException e) {
				logger.debug("username signed on that we don't know: " + username);
				return;
			}
			if (!account.getWasSentShareLinkTutorial()) {
				logger.debug("We have a new user!!!!! WOOOOOOOOOOOOHOOOOOOOOOOOOOOO send them tutorial!");
	
				postingBoard.doShareLinkTutorialPost(account.getOwner());
	
				account.setWasSentShareLinkTutorial(true);
			}
		} catch (RuntimeException e) {
			logger.error("Failed to do share link tutorial", e);
			logger.trace(e);
			logger.trace(ExceptionUtils.getRootCause(e));
			throw e;
		}
	}

	public void onUserUnavailable(String username) {
		logger.debug("Jabber user " + username + " now unavailable");
	}
}
