<%--
  -	$RCSfile$
  -	$Revision: 1771 $
  -	$Date: 2005-08-11 13:40:32 -0400 (Thu, 11 Aug 2005) $
  -
  - Copyright (C) 2004 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.messenger.user.*,
                 org.jivesoftware.messenger.*,
                 java.text.DateFormat,
                 org.jivesoftware.admin.*,
                 java.util.HashMap,
                 java.util.Map,
                 java.net.URLEncoder,
                 org.jivesoftware.util.LocaleUtils"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%  // Get parameters
    boolean save = ParamUtils.getBooleanParameter(request,"save");
    boolean success = ParamUtils.getBooleanParameter(request,"success");
    String username = ParamUtils.getParameter(request,"username");
    String name = ParamUtils.getParameter(request,"name");
    String email = ParamUtils.getParameter(request,"email");

    // Handle a cancel
    if (request.getParameter("cancel") != null) {
        response.sendRedirect("user-properties.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Load the user object
    User user = webManager.getUserManager().getUser(username);

    // Handle a save
    if (save) {
        user.setEmail(email);
        user.setName(name);

        // Changes good, so redirect
        response.sendRedirect("user-properties.jsp?editsuccess=true&username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }
%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%  // Title of this page and breadcrumbs
    String title = LocaleUtils.getLocalizedString("user.edit.form.title");
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(LocaleUtils.getLocalizedString("global.main"), "index.jsp"));
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title,
            "user-edit-form.jsp?username="+URLEncoder.encode(username, "UTF-8")));
    pageinfo.setSubPageID("user-properties");
    pageinfo.setExtraParams("username="+URLEncoder.encode(username, "UTF-8"));
%>
<jsp:include page="top.jsp" flush="true" />
<jsp:include page="title.jsp" flush="true" />

<%  if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="user.edit.form.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="user.edit.form.info" />
</p>

<form action="user-edit-form.jsp">

<input type="hidden" name="username" value="<%= username %>">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="user.edit.form.property" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.username" />:
            </td>
            <td>
                <%= user.getUsername() %>
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.name" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="name"
                 value="<%= user.getName() %>">
            </td>
        </tr>
        <tr>
            <td class="c1">
                <fmt:message key="user.create.email" />:
            </td>
            <td>
                <input type="text" size="30" maxlength="150" name="email"
                 value="<%= ((user.getEmail()!=null) ? user.getEmail() : "") %>">
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br><br>

<input type="submit" value="<fmt:message key="global.save_properties" />">
<input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">

</form>

<jsp:include page="bottom.jsp" flush="true" />