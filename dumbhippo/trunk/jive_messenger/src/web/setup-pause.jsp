<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%--
  -	$RCSfile$
  -	$Revision: 985 $
  -	$Date: 2005-02-18 13:35:44 -0500 (Fri, 18 Feb 2005) $
--%>

<%@ page import="org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.messenger.auth.UnauthorizedException" %>

<%-- Define Administration Bean --%>
<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager"  />
<% admin.init(request, response, session, application, out ); %>

<%  boolean showSidebar = false; %>

<%@ include file="setup-header.jspf" %>
<div align=center>

<p>
<fmt:message key="setup.pause.title" />
</p>

<a href="javascript:window.close();"><fmt:message key="setup.pause.close" /></a>
</div>

<%@ include file="setup-footer.jsp" %>


