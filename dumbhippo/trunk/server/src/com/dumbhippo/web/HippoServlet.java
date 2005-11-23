package com.dumbhippo.web;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.xmlrpc.XmlRpcServer;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.HttpContentTypes;
import com.dumbhippo.server.HttpMethods;
import com.dumbhippo.server.HttpOptions;
import com.dumbhippo.server.HttpParams;
import com.dumbhippo.server.HttpResponseData;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.XmlRpcMethods;

public class HippoServlet extends AbstractServlet {

	private static final Log logger = GlobalSetup.getLog(HippoServlet.class);
	
	private static final long serialVersionUID = 0L;
	
	private String[] parseUri(String uri) throws HttpException {
		
		if (uri.length() < 4) // "/a/b".length() == 4
			throw new HttpException(HttpResponseCode.NOT_FOUND,
					"URI is too short to be valid, should be of the form /typeprefix/methodname, e.g. /xml/frobate");
		
		// ignore trailing /
		if (uri.endsWith("/"))
			uri = uri.substring(0, uri.length()-1);
		
		// split into the two components, the result is { "", "xml", "frobate" }
		String[] ret = uri.split("/");
		if (ret.length != 3)
			throw new HttpException(HttpResponseCode.NOT_FOUND,
					"All URIs are of the form /typeprefix/methodname, e.g. /xml/frobate, split into: " + Arrays.toString(ret));
		
		return new String[] { ret[1], ret[2] };
	}
	
	private Object[] marshalHttpRequestParams(HttpServletRequest request, OutputStream out, HttpResponseData replyContentType, HttpParams paramsAnnotation, Method m) throws HttpException {
		Class<?> args[] = m.getParameterTypes();

		ArrayList<Object> toPassIn = new ArrayList<Object>();
		
		int i = 0;
				
		boolean methodCanReturnContent = args.length > i && OutputStream.class.isAssignableFrom(args[i]);
		
		if (methodCanReturnContent) {
			toPassIn.add(out);
			i += 1;
			if (!(args.length > i && HttpResponseData.class.isAssignableFrom(args[i]))) {
				throw new RuntimeException("HTTP method " + m.getName() + " must have HttpResponseData contentType as arg " + i);
			}
			toPassIn.add(replyContentType);
			i += 1;
		} else if (replyContentType != HttpResponseData.NONE) {
			throw new RuntimeException("HTTP method " + m.getName() + " must have OutputStream as arg " + i + " to return type " + replyContentType);
		}
		
		if (args.length > i && Person.class.isAssignableFrom(args[i])) {
			Person loggedIn = getLoggedInUser(request);
			toPassIn.add(loggedIn);
			i += 1;
		}
		
		if (args.length != i + paramsAnnotation.value().length) {
			throw new RuntimeException("HTTP method " + m.getName() + " should have params " + paramsAnnotation.value());
		}
		
		for (String pname : paramsAnnotation.value()) {
			String s = request.getParameter(pname);
			
			if (s == null) {
				throw new HttpException(HttpResponseCode.BAD_REQUEST,
						"Parameter " + pname + " not provided to method " + m.getName());
			}
			
			if(String.class.isAssignableFrom(args[i])) {
				toPassIn.add(s);
			} else if (boolean.class.isAssignableFrom(args[i])) {
				if (s.equals("true")) {
					toPassIn.add(true);
				} else if (s.equals("false")) {
					toPassIn.add(false);
				} else {
					throw new HttpException(HttpResponseCode.BAD_REQUEST, "Parameter " + pname + " to method " + m.getName() + " must be 'true' or 'false'");
				}
			} else {
				throw new RuntimeException("Arg " + i + " of type " + args[i].getCanonicalName() + " is not supported");
			}
			
			++i;
		}
		
		return toPassIn.toArray();
	}
	
	private <T> void invokeHttpRequest(T object, HttpServletRequest request, HttpServletResponse response)
			throws IOException, HttpException, HumanVisibleException {
		
		String requestUri = request.getRequestURI();
		Class<?>[] interfaces = object.getClass().getInterfaces();
		boolean isPost = request.getMethod().toUpperCase().equals("POST"); 
		String[] uriComponents = parseUri(requestUri);
		String typeDir = uriComponents[0];
		String requestedMethod = uriComponents[1];
		String getRequestedMethod = "get" + requestedMethod;
		String doRequestedMethod = null;
		if (isPost) {
			doRequestedMethod = "do" + requestedMethod;
			logger.debug("is a post, will also look for " + doRequestedMethod);
		}

		logger.debug("trying to invoke http request at " + requestUri);
		
		HttpResponseData requestedContentType;
		if (typeDir.equals("xml"))
			requestedContentType = HttpResponseData.XML;
		else if (typeDir.equals("text"))
			requestedContentType = HttpResponseData.TEXT;
		else if (isPost && typeDir.equals("action"))
			requestedContentType = HttpResponseData.NONE;
		else {
			throw new HttpException(HttpResponseCode.NOT_FOUND,
					"Don't know about URI path /" + typeDir + " , only /xml, /text for GET plus /action for POST only)");
		}

		boolean foundMethod = false;
		
		for (Class<?> iface : interfaces) {
			logger.debug("Looking for method " + requestedMethod + " on " + iface.getCanonicalName());
			for (Method m : iface.getMethods()) {
				HttpContentTypes contentAnnotation = m.getAnnotation(HttpContentTypes.class);
				HttpParams paramsAnnotation = m.getAnnotation(HttpParams.class);
				HttpOptions optionsAnnotation = m.getAnnotation(HttpOptions.class);

				if (contentAnnotation == null) {
					logger.debug("Method " + m.getName() + " has no content type annotation, skipping");
					continue;
				}

				if (paramsAnnotation == null) {
					throw new HttpException(HttpResponseCode.INTERNAL_SERVER_ERROR, "missing params annotation on " + m.getName());
				}
				
				String lowercase = m.getName().toLowerCase();
				if (!(lowercase.equals(getRequestedMethod) || lowercase.equals(doRequestedMethod))) {
					logger.debug("Method " + m.getName() + " does not match " + getRequestedMethod + " or "
							+ doRequestedMethod + ", skipping");
					continue;
				}

				logger.debug("found method " + m.getName());

				if (m.getReturnType() != void.class)
					throw new RuntimeException("HTTP method " + m.getName() + " must return void not "
							+ m.getReturnType().getCanonicalName());

				boolean requestedContentTypeSupported = false;
				for (HttpResponseData t : contentAnnotation.value()) {
					if (t == requestedContentType) {
						requestedContentTypeSupported = true;
						break;
					}
				}

				if (!requestedContentTypeSupported) {
					throw new HttpException(HttpResponseCode.NOT_FOUND, "Wrong content type requested "
							+ requestedContentType + " valid types for method are " + contentAnnotation.value());
				}

				OutputStream out = response.getOutputStream();
				Object[] methodArgs = marshalHttpRequestParams(request, out, requestedContentType, paramsAnnotation, m);
				

				if (requestedContentType != HttpResponseData.NONE)
					response.setContentType(requestedContentType.getMimeType());

				try {
					logger.debug("Invoking method " + m.getName() + " with args " + Arrays.toString(methodArgs));
					m.invoke(object, methodArgs);
				} catch (IllegalArgumentException e) {
					logger.error(e);
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					logger.error(e);
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					logger.debug("Exception thrown by invoked method: " + cause);
					logger.error(cause);
					
					if (cause instanceof HumanVisibleException) {
						throw (HumanVisibleException) cause;
					} else {					
						throw new RuntimeException(e);
					}
				}
				
				if (optionsAnnotation != null && optionsAnnotation.invalidatesSession()) {
					HttpSession sess = request.getSession(false);
					if (sess != null)
						sess.invalidate();	
				}

				out.flush();

				logger.debug("Reply for " + m.getName() + " sent");
		
				foundMethod = true;
				break; // don't call two different methods!
			}
		}
		
		if (!foundMethod) {
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No such method " + requestedMethod);
		}
	}
	
	private User getLoggedInUser(HttpServletRequest request) throws HttpException {
		SigninBean signin = SigninBean.getForRequest(request);
		User user = signin.getUser();
		if (user == null) {
			// we have no UI so the user is pretty much jacked at this stage; but it 
			// should not happen in any non-broken situation
			throw new HttpException(HttpResponseCode.FORBIDDEN, "You need to log in");
		}
		return user;
	}
		
	private void writeHtmlHeaderBoilerplate(OutputStream out) throws IOException {
		out.write(("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">"
				+ "<html>"
				+ "<head>"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
				+ "</head><body>").getBytes());		
	}
	
	private boolean tryRedirectRequests(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException {
		if (!request.getRequestURI().equals("/redirect")) {
			return false;
		}
		
		String url = request.getParameter("url");
		String postId = request.getParameter("postId");
		String inviteKey = request.getParameter("inviteKey");
		
		if (postId == null || url == null) {
			// print a little message about this page
			response.setContentType("text/plain");
			OutputStream out = response.getOutputStream();
			out.write("This page forwards you to a shared link, and tells the friend who shared the link that you're going to look at it.\n".getBytes());
			out.write("It isn't doing that right now though, because it didn't receive the right parameters.\n".getBytes());
			out.flush();
		} else {
			HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);
			User user;
			try {
				user = getLoggedInUser(request);
			} catch (HttpException e) {
				user = null; // not fatal as it usually is
			}
			try { 
				glue.handleRedirect(user, url, postId, inviteKey);
				
				// If "url" were on our own site, we would need to call encodeRedirectURL to be sure it had the jsessionid in it
				response.sendRedirect(url);
				response.setContentType("text/html");
				OutputStream out = response.getOutputStream();
				writeHtmlHeaderBoilerplate(out);
				String escapedUrl = XmlBuilder.escape(url);
				out.write(("<p>Go to <a href=\"" + escapedUrl + "\">" + escapedUrl + "</a></p>\n").getBytes());
				out.write("</body>\n".getBytes());
				out.flush();
			} catch (HumanVisibleException e) {
				response.setContentType("text/html");
				OutputStream out = response.getOutputStream();
				writeHtmlHeaderBoilerplate(out);
				
				out.write("<p>Oops! Could not send you to your link automatically.</p>".getBytes());
				out.write(("<p><i>" + e.getHtmlMessage() + "</i></p>").getBytes());
				
				String escapedUrl = XmlBuilder.escape(url);
				out.write(("<p style=\"font-size: larger;\">Try <a href=\"" + escapedUrl + "\">clicking here</a></p>\n").getBytes());
				
				out.write("</body>\n".getBytes());
				out.flush();
			}
		}
		
		return true;
	}
	
	private boolean tryLoginRequests(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException {
		// special method that magically causes us to look at your cookie and log you 
		// in if it's set, then return person you're logged in as or "false"
		
		if (!(request.getRequestURI().equals("/text/dologin") &&
				request.getMethod().toUpperCase().equals("POST"))
				&& !request.getRequestURI().equals("/text/checklogin")) {
			return false;
		}
			
		User user = doLogin(request, response, false);
		
		logger.debug("sending checklogin reply");
			
		response.setContentType("text/plain");
		OutputStream out = response.getOutputStream();
		if (user != null)
			out.write(user.getId().getBytes());
		else
			out.write("false".getBytes());
		out.flush();
		
		return true;
	}

	private boolean trySignoutRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, HttpException { 
		if (!request.getRequestURI().equals("/action/signout") ||
		    !request.getMethod().toUpperCase().equals("POST"))
			return false;
		
		HttpSession session = request.getSession();
		if (session != null)
			session.invalidate();		
		
		// FIXME we need to drop the Client object when we do this,
		// both to save our own disk space, and in case someone stole the 
		// cookie.
		
		response.addCookie(LoginCookie.newDeleteCookie());
		
		return true;
	}
	
	@Override
	protected void wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
			IOException, HumanVisibleException {
		if (tryRedirectRequests(request, response) ||
		    tryLoginRequests(request, response) ||
		    trySignoutRequest(request, response)) {
			/* nothing */
		} else if (request.getRequestURI().startsWith("/xmlrpc/")) {
			XmlRpcServer xmlrpc = new XmlRpcServer();
			// Java thread locks are recursive so this is OK...
			XmlRpcMethods glue = WebEJBUtil.defaultLookup(XmlRpcMethods.class);

			// glue is a proxy that only exports the one interface,
			// so safe to export it all
			xmlrpc.addHandler("dumbhippo", glue);
			byte[] result = xmlrpc.execute(request.getInputStream());
			response.setContentType("text/xml");
			response.setContentLength(result.length);
			OutputStream out = response.getOutputStream();
			out.write(result);
			out.flush();
		} else {
			HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);

			invokeHttpRequest(glue, request, response);
		}
	}
	
	@Override
	protected void wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException, IOException, HumanVisibleException {
		if (tryRedirectRequests(request, response)) {
			return;
		} else {
			HttpMethods glue = WebEJBUtil.defaultLookup(HttpMethods.class);
			invokeHttpRequest(glue, request, response);
		}
	}
}
