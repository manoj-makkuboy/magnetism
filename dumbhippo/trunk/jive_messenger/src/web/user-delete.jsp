<%--
  -	$RCSfile$
  -	$Revision: 2701 $
  -	$Date: 2005-08-19 19:48:22 -0400 (Fri, 19 Aug 2005) $
  -
  - Copyright (C) 2004 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.util.*,
                 org.jivesoftware.messenger.user.*,
                 org.jivesoftware.admin.*,
                 org.xmpp.packet.JID,
                 java.net.URLEncoder,
                 org.jivesoftware.messenger.group.GroupManager"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    String username = ParamUtils.getParameter(request,"username");

    // Handle a cancel
    if (cancel) {
        response.sendRedirect("user-properties.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Load the user object
    User user = webManager.getUserManager().getUser(username);

    // Handle a user delete:
    if (delete) {
        // Delete the user
        webManager.getUserManager().deleteUser(user);
        // Delete the user's roster
        JID userAddress = new JID(username, webManager.getServerInfo().getName(), null);
        // Delete the roster of the user
        webManager.getRosterManager().deleteRoster(userAddress);
        // Delete the user from all the Groups
        GroupManager.getInstance().deleteUser(user);
        // Deleted your own user account, force login
        if (username.equals(webManager.getAuthToken().getUsername())){
            session.removeAttribute("jive.admin.authToken");
            response.sendRedirect("login.jsp");
        }
        else {
            // Done, so redirect
            response.sendRedirect("user-summary.jsp?deletesuccess=true");
        }
        return;
    }
%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%  // Title of this page and breadcrumbs
    String title = LocaleUtils.getLocalizedString("user.delete.title");
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(LocaleUtils.getLocalizedString("global.main"), "index.jsp"));
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title,
            "user-delete.jsp?username="+URLEncoder.encode(username, "UTF-8")));
    pageinfo.setSubPageID("user-delete");
    pageinfo.setExtraParams("username="+URLEncoder.encode(username, "UTF-8"));
%>
<jsp:include page="top.jsp" flush="true">
    <jsp:param name="helpPage" value="remove_a_user_from_the_system.html" />
</jsp:include>
<jsp:include page="title.jsp" flush="true" />

<p>
<fmt:message key="user.delete.info" />
<b><a href="user-properties.jsp?username=<%= URLEncoder.encode(user.getUsername(), "UTF-8") %>"><%= user.getUsername() %></a></b>
<fmt:message key="user.delete.info1" />
</p>

<c:if test="${admin.user.username == param.username}">
    <p class="jive-warning-text">
    <fmt:message key="user.delete.warning" /> <b><fmt:message key="user.delete.warning2" /></b> <fmt:message key="user.delete.warning3" />
    </p>
</c:if>

<form action="user-delete.jsp">
<input type="hidden" name="username" value="<%= username %>">
<input type="submit" name="delete" value="<fmt:message key="user.delete.delete" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>

<jsp:include page="bottom.jsp" flush="true" />
