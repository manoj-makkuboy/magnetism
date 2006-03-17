package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;

/**
 * 
 * This is a "bug buddy"/"crash" servlet that handles errors we weren't expecting to get.
 * 
 * @author hp
 *
 */
public class AbnormalErrorServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = GlobalSetup.getLogger(AbnormalErrorServlet.class);

    private static final String[] errorVars = {
    	"javax.servlet.error.request_uri",
        "javax.servlet.error.status_code",
        "javax.servlet.error.exception_type",
        "javax.servlet.error.message",
        "javax.servlet.error.exception"        
    };

    private Configuration config;
    
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}
    
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws HttpException,
		HumanVisibleException, IOException, ServletException {
    	try {
    		logger.error("Abnormal error occurred");
    		for (String var : errorVars) {
    			logger.error("{} = {}", var, request.getAttribute(var));
    		}
    		Throwable t = (Throwable) request.getAttribute("javax.servlet.error.exception");
    		if (t != null) {
    			logger.error("Backtrace:", t);
    			Throwable root = ExceptionUtils.getRootCause(t);
    			if (root != t) {
    				logger.error("Root cause is {} message: {}", 
    						root.getClass().getName(), root.getMessage());
    			}
    		}
    	} catch (Throwable t) {
    		// not sure what happens if the error servlet throws an error, but it can't be good, so 
    		// we unconditionally eat it here
    		logger.error("Error servlet broke! ", t);
    	}
    	// now redirect to error page
    	throw new HumanVisibleException("There was an unexpected problem with the site. Please try again; " 
    			+ "or if you can describe the situation causing the error and when it occurred, mail us at "
    			+ config.getProperty(HippoProperty.FEEDBACK_EMAIL) + " and we'll investigate as soon as we can.");
    }
    
    @Override
    protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
    	HumanVisibleException, IOException, ServletException {
    	handleRequest(request, response);
    }
    
    @Override
    protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
    	HumanVisibleException, IOException, ServletException {
    	handleRequest(request, response);				 
    }

	@Override
	protected boolean requiresTransaction() {
		return false;
	}
}
