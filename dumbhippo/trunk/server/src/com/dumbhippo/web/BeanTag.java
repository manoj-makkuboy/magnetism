package com.dumbhippo.web;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

public class BeanTag extends SimpleTagSupport {
	private static final Log logger = GlobalSetup.getLog(BeanTag.class);
	
	String id;
	Scope scope;
	Class clazz;

	private Object findInScope(Scope s, String key) {
		PageContext context = (PageContext)getJspContext();
		
		switch (s) {
		case PAGE:
			return context.getAttribute(key);
		case REQUEST:
			return context.getRequest().getAttribute(key);
		case SESSION:
			return context.getSession().getAttribute(key);
		case APPLICATION:
			return context.getServletContext().getAttribute(key);
		}
		
		throw new IllegalArgumentException("bad scope value");
	}
	
	private Object findObject() {
		return findInScope(scope, id);
	}
	
	public void storeObject(Object o) {
		PageContext context = (PageContext)getJspContext();
		
		switch (scope) {
		case PAGE:
			context.setAttribute(id, o);
			break;
		case REQUEST:
			context.getRequest().setAttribute(id, o);
			break;
		case SESSION:
			context.getSession().setAttribute(id, o);
			break;
		case APPLICATION:
			context.getServletContext().setAttribute(id, o);
			break;
		}		
	}
	
	private SigninBean getSigninBean() {
		PageContext context = (PageContext)getJspContext();
		
		return SigninBean.getForRequest((HttpServletRequest)context.getRequest());
	}
	
	private BrowserBean getBrowserBean() {
		PageContext context = (PageContext)getJspContext();
		
		return BrowserBean.getForRequest((HttpServletRequest)context.getRequest());
	}
	
	private void setField(Object o, Field f, Object value) {
		logger.debug("injecting value of type " + (value != null ? value.getClass().getName() : "null")
				+ " into field " + f.getName() + " of object " + o.getClass().getName());
		try {
			// Like EJB3, we support private-field injection
			f.setAccessible(true);
			f.set(o, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error injecting object", e);
		}
	}
	
	private Object instantiateObject() {
		logger.debug("Instantiating " + clazz.getName());
		// We special-case the SigninBean
		if (clazz == SigninBean.class) 
			return getSigninBean();
		
		Object o;
		try {
			o = clazz.newInstance();
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		}
		
		for (Field f : clazz.getDeclaredFields()) {
			if (f.isAnnotationPresent(Signin.class) &&
				f.getType().isAssignableFrom(SigninBean.class)) {
				setField(o, f, getSigninBean());
			} else if (f.isAnnotationPresent(Browser.class) &&
				f.getType().isAssignableFrom(BrowserBean.class)) {
				setField(o, f, getBrowserBean());
			} else if (f.isAnnotationPresent(FromJspContext.class)) {
				FromJspContext a = f.getAnnotation(FromJspContext.class);
				String key = a.value();
				Scope s = a.scope();
				if (s == null)
					s = scope; // default to scope of the page
				
				Object toInject = findInScope(s, key);
				if (toInject != null)
					setField(o, f, toInject);
				else
					logger.debug("no value " + key + " found in scope " + s + " not injecting into " + o.getClass().getName());
			}
		}
		
		return o;
	}
	
	public void doTag() throws IOException, JspException {
		Object o = findObject();
		if (o == null) {
			o = instantiateObject();			
			storeObject(o);
		}
		
		JspFragment frag = getJspBody();
		if (frag != null)
			frag.invoke(getJspContext().getOut());
	}
	
	public void setId(String i) {
		id = i;
	}
	
	public void setScope(String s) {
		if (s.equals("page"))
			scope = Scope.PAGE;
		else if (s.equals("request"))
			scope = Scope.REQUEST;
		else if (s.equals("session"))
			scope = Scope.SESSION;
		else if (s.equals("application"))
			scope = Scope.APPLICATION;
		else
			throw new IllegalArgumentException("Bad scope value " + s);
	}
	
	public void setClass(Class c) {
		clazz = c;
	}
}
