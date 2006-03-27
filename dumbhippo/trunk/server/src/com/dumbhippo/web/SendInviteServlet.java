package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.UserViewpoint;

public class SendInviteServlet extends AbstractServlet {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SendInviteServlet.class);
	
	static final long serialVersionUID = 1;

	private void doSendInvite(HttpServletRequest request, HttpServletResponse response)
	throws HttpException, HumanVisibleException, IOException, ServletException {
		User user = doLogin(request);
		
		String message = request.getParameter("message");
		if (message != null)
			message = message.trim() + "\n";

		String subject = request.getParameter("subject");
		if (subject != null)
			subject = subject.trim();
		
		String email = request.getParameter("email");
		if (email != null)
			email = email.trim();
		
		// the error page redirect kind of sucks, but if we do inline javascript 
		// validation it would only happen in weird manual-url-typing cases
		if (email == null || email.equals("") || !email.contains("@")) 
			throw new HumanVisibleException("Missing or invalid email address");
		
		InvitationSystem invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		
		// we no longer need to check if the user has an invitation voucher to spend, 
		// because invitationSystem will take care of it
		String note = invitationSystem.sendEmailInvitation(new UserViewpoint(user), null, email, subject, message);
		
		if (note == null)
		request.setAttribute("email", email);
		request.setAttribute("remaining", invitationSystem.getInvitations(user));
		request.setAttribute("note", note);
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException, HttpException, HumanVisibleException {
		doSendInvite(request, response);
		return "/invitesent";
	}

	@Override
	protected boolean requiresTransaction() {
		return true;
	}
}
