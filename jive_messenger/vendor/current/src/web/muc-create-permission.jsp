<%--
  -	$RCSfile$
  -	$Revision: 2703 $
  -	$Date: 2005-08-19 20:13:25 -0400 (Fri, 19 Aug 2005) $
  -
  - Copyright (C) 2004 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.util.*,
                 java.util.*,
                 org.jivesoftware.messenger.*,
                 org.jivesoftware.admin.*,
                 org.jivesoftware.messenger.muc.MultiUserChatServer,
                 java.util.Iterator"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager" />
<% admin.init(request, response, session, application, out ); %>

<%  // Get parameters
    String userJID = ParamUtils.getParameter(request,"userJID");
    boolean add = request.getParameter("add") != null;
    boolean save = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    boolean addsuccess = request.getParameter("addsuccess") != null;
    boolean deletesuccess = request.getParameter("deletesuccess") != null;
    boolean delete = ParamUtils.getBooleanParameter(request,"delete");
    boolean openPerms = ParamUtils.getBooleanParameter(request,"openPerms");

	// Get muc server
    MultiUserChatServer mucServer = admin.getMultiUserChatServer();

    // Handle a save
    Map errors = new HashMap();
    if (save) {
        if (openPerms) {
            // Remove all users who have the ability to create rooms
            List<String> removeables = new ArrayList<String>();
            for (Object obj : mucServer.getUsersAllowedToCreate()) {
                String user = (String)obj;
                removeables.add(user);
            }
            for (String user : removeables) {
                mucServer.removeUserAllowedToCreate(user);
            }
            mucServer.setRoomCreationRestricted(false);
            response.sendRedirect("muc-create-permission.jsp?success=true");
            return;
        }
        else {
            mucServer.setRoomCreationRestricted(true);
            response.sendRedirect("muc-create-permission.jsp?success=true");
            return;
        }
    }

    // Handle an add
    if (add) {
        // do validation
        if (userJID == null || userJID.indexOf('@') == -1) {
            errors.put("userJID","userJID");
        }
        if (errors.size() == 0) {
            mucServer.addUserAllowedToCreate(userJID);
            response.sendRedirect("muc-create-permission.jsp?addsuccess=true");
            return;
        }
    }

    if (delete) {
        // Remove the user from the allowed list
        mucServer.removeUserAllowedToCreate(userJID);
        // done, return
        response.sendRedirect("muc-create-permission.jsp?deletesuccess=true");
        return;
    }
%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%  // Title of this page and breadcrumbs
    String title = LocaleUtils.getLocalizedString("muc.create.permission.title");
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(LocaleUtils.getLocalizedString("global.main"), "index.jsp"));
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title, "muc-create-permission.jsp"));
    pageinfo.setPageID("muc-perms");
%>
<jsp:include page="top.jsp" flush="true">
    <jsp:param name="helpPage" value="set_group_chat_room_creation_permissions.html" />
</jsp:include>
<jsp:include page="title.jsp" flush="true" />

<p>
<fmt:message key="muc.create.permission.info" />
</p>

<%  if (errors.size() > 0) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="muc.create.permission.error" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (success || addsuccess || deletesuccess) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <%  if (success) { %>

            <fmt:message key="muc.create.permission.update" />

        <%  } else if (addsuccess) { %>

            <fmt:message key="muc.create.permission.add_user" />

        <%  } else if (deletesuccess) { %>

            <fmt:message key="muc.create.permission.user_removed" />

        <%  } %>
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="muc-create-permission.jsp?save" method="post">

<fieldset>
    <legend><fmt:message key="muc.create.permission.policy" /></legend>
    <div>
        <table cellpadding="3" cellspacing="0" border="0" width="100%">
        <tbody>
            <tr>
                <td width="1%">
                    <input type="radio" name="openPerms" value="true" id="rb01"
                     <%= ((!mucServer.isRoomCreationRestricted()) ? "checked" : "") %>>
                </td>
                <td width="99%">
                    <label for="rb01"><fmt:message key="muc.create.permission.anyone_created" /></label>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <input type="radio" name="openPerms" value="false" id="rb02"
                     onfocus="this.form.userJID.focus();"
                     <%= ((mucServer.isRoomCreationRestricted()) ? "checked" : "") %>>
                </td>
                <td width="99%">
                    <label for="rb02"><fmt:message key="muc.create.permission.specific_created" /></label>
                </td>
            </tr>
        </tbody>
        </table>
        <br>
        <input type="submit" value="<fmt:message key="global.save_settings" />">
    </div>
</fieldset>

</form>

<br>

<%  if (mucServer.isRoomCreationRestricted()) { %>

    <form action="muc-create-permission.jsp?add" method="post">

    <fieldset>
        <legend><fmt:message key="muc.create.permission.allowed_users" /></legend>
        <div>
        <p>
        <label for="userJIDtf"><fmt:message key="muc.create.permission.add_jid" /></label>
        <input type="text" name="userJID" size="30" maxlength="100" value="<%= (userJID != null ? userJID : "") %>"
         onclick="this.form.openPerms[1].checked=true;" id="userJIDtf">
        <input type="submit" value="Add">
        </p>

        <div class="jive-table" style="width:400px;">
        <table cellpadding="0" cellspacing="0" border="0" width="100%">
        <thead>
            <tr>
                <th width="99%">User</th>
                <th width="1%">Remove</th>
            </tr>
        </thead>
        <tbody>
            <%  if (mucServer.getUsersAllowedToCreate().size() == 0) { %>

                <tr>
                    <td colspan="2">
                        <fmt:message key="muc.create.permission.no_allowed_users" />
                    </td>
                </tr>

            <%  } %>

            <%  for (Object obj : mucServer.getUsersAllowedToCreate()) {
                    String user = (String)obj;
            %>
                <tr>
                    <td width="99%">
                        <%= user %>
                    </td>
                    <td width="1%" align="center">
                        <a href="muc-create-permission.jsp?userJID=<%= user %>&delete=true"
                         title="<fmt:message key="muc.create.permission.click_title" />"
                         onclick="return confirm('<fmt:message key="muc.create.permission.confirm_remove" />');"
                         ><img src="images/delete-16x16.gif" width="16" height="16" border="0"></a>
                    </td>
                </tr>

            <%  } %>
        </tbody>
        </table>
        </div>
        </div>
    </fieldset>

    </form>

<%  } %>

<jsp:include page="bottom.jsp" flush="true" />
