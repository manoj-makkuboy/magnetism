package com.dumbhippo.web;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.web.LoginCookie.BadTastingException;
import com.dumbhippo.web.LoginCookie.NotLoggedInException;

public class SigninBean {

	private static Logger logger = Logger.getLogger(SigninBean.class);

	private HippoAccount account;

	private transient AccountSystem accountSystem;
	
	private void initAccountFromCookie() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest request = (HttpServletRequest) ctx.getRequest();
		try {
			account = LoginCookie.attemptLogin(accountSystem, request);
		} catch (BadTastingException e) {
			logger.info("invalid login: ", e);
		} catch (NotLoggedInException e) {
			account = null;
		}
	}

	public SigninBean() throws NamingException {
		accountSystem = (AccountSystem) (new InitialContext()).lookup(AccountSystem.class.getName());
		initAccountFromCookie();
	}
	
	private static String computeClientIdentifier() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		
		StringBuilder ret = new StringBuilder();
		ret.append(req.getRemoteAddr());
		String agent = req.getHeader("user-agent");
		if (agent != null) {
			ret.append('/');
			ret.append(agent);
		}
		
		return ret.toString();
	}
	
	public void initNewAccountFromEmail(String email) {
		setAccount(accountSystem.createAccountFromEmail(email));
		initNewClient(account);
	}
	
	public static Client initNewClient(HippoAccount account) {
		AccountSystem accounts;
		try {
			accounts = (AccountSystem) (new InitialContext()).lookup(AccountSystem.class.getName());
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
		return initNewClient(accounts, account);
	}

	public static Client initNewClient(AccountSystem accounts, HippoAccount account) {
		Client client = accounts.authorizeNewClient(account, computeClientIdentifier());
		
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
		LoginCookie loginCookie = new LoginCookie(account, client);
		response.addCookie(loginCookie.getCookie());
		return client;
	}

	public boolean isValid() {
		return account != null;
	}
	
	public HippoAccount getAccount() {
		return account;
	}

	public void setAccount(HippoAccount account) {
		this.account = account;
	}
}
