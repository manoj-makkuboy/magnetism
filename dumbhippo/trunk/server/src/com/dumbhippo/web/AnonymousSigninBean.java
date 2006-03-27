package com.dumbhippo.web;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AnonymousViewpoint;
import com.dumbhippo.server.Viewpoint;

public class AnonymousSigninBean extends SigninBean {
	/**
	 * Creates a new SigninBean object when there is no signed in
	 * DO NOT CALL THIS CONSTRUCTOR. Use Signin.getForRequest()instead. 
	 */
	AnonymousSigninBean() {
	}
	
	@Override
	public Viewpoint getViewpoint() {
		return AnonymousViewpoint.getInstance();
	}
	
	public User getUser() {
		return null;
	}
	
	public String getUserId() {
		return null;
	}
	
	public Guid getUserGuid() {
		return null;
	}
	
	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public void resetSessionObjects() {
	}
}
