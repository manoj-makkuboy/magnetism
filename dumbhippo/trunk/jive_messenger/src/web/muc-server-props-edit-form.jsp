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

<%@ page import="org.jivesoftware.util.ParamUtils,
                 java.text.DateFormat,
                 org.jivesoftware.messenger.XMPPServerInfo,
                 org.jivesoftware.messenger.muc.MultiUserChatServer,
                 org.jivesoftware.admin.*,
                 org.jivesoftware.util.JiveGlobals,
                 java.util.*,
                 org.jivesoftware.util.LocaleUtils"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
   // Handle a cancel
    if (request.getParameter("cancel") != null) {
      response.sendRedirect("muc-server-props-edit-form.jsp");
      return;
    }
%>

<%-- Define Administration Bean --%>
<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager"  />
<c:set var="admin" value="${admin.manager}" />
<% admin.init(pageContext); %>

<%  // Get parameters
    boolean save = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    String name = ParamUtils.getParameter(request,"servername");
    String muc = ParamUtils.getParameter(request,"mucname");

    // Handle a save
    Map errors = new HashMap();
    if (save) {
        // Make sure that the MUC Service is lower cased.
        muc = muc.toLowerCase();

        // do validation
        if (muc == null  || muc.indexOf('.') >= 0) {
            errors.put("mucname","mucname");
        }
        if (errors.size() == 0) {
            admin.getMultiUserChatServer().setServiceName(muc);
            response.sendRedirect("muc-server-props-edit-form.jsp?success=true&mucname="+muc);
            return;
        }
    }
    else if(muc == null) {
        name = admin.getServerInfo().getName() == null ? "" : admin.getServerInfo().getName();
        muc = admin.getMultiUserChatServer().getServiceName() == null  ? "" : admin.getMultiUserChatServer().getServiceName();
    }

    name = admin.getServerInfo().getName();
    if (errors.size() == 0 && muc == null) {
        muc = admin.getMultiUserChatServer().getServiceName();
    }
%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%  // Title of this page and breadcrumbs
    String title = LocaleUtils.getLocalizedString("groupchat.service.properties.title");
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(LocaleUtils.getLocalizedString("global.main"), "index.jsp"));
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title, "muc-server-props-edit-form.jsp"));
    pageinfo.setPageID("muc-server-props");
%>
<jsp:include page="top.jsp" flush="true">
    <jsp:param name="helpPage" value="edit_group_chat_service_properties.html" />
</jsp:include>
<jsp:include page="title.jsp" flush="true" />

<p>
<fmt:message key="groupchat.service.properties.introduction" />
</p>

<%  if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
            <fmt:message key="groupchat.service.properties.saved_successfully" /> <b><fmt:message key="global.restart" /></b> <fmt:message key="groupchat.service.properties.saved_successfully2" /> <a href="index.jsp"><fmt:message key="global.server_status" /></a>).
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (errors.size() > 0) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="groupchat.service.properties.error_service_name" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="muc-server-props-edit-form.jsp" method="post">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="groupchat.service.properties.legend" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0">

    <tr>
        <td class="c1">
           <fmt:message key="groupchat.service.properties.label_service_name" />
        </td>
        <td>
        <input type="text" size="30" maxlength="150" name="mucname"  value="<%= (muc != null ? muc : "") %>">

        <%  if (errors.get("mucname") != null) { %>

            <span class="jive-error-text">
            <br><fmt:message key="groupchat.service.properties.error_service_name" />
            </span>

        <%  } %>
        </td>
    </tr>
    </table>
    </div>
</fieldset>

<br><br>

<input type="submit" value="<fmt:message key="groupchat.service.properties.save" />">

</form>

<jsp:include page="bottom.jsp" flush="true" />