<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%--
  -	$RCSfile$
  -	$Revision: 1410 $
  -	$Date: 2005-05-27 02:00:40 -0400 (Fri, 27 May 2005) $
--%>

<%@ page import="org.jivesoftware.util.ParamUtils,
                 java.util.HashMap,
                 javax.naming.Context,
                 javax.naming.NamingEnumeration,
                 javax.naming.InitialContext,
                 javax.naming.Binding,
                 org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.database.JNDIDataSourceProvider,
                 org.jivesoftware.database.DbConnectionManager,
                 org.jivesoftware.database.JNDIDataSourceProvider" %>

<%@ include file="setup-global.jspf" %>

<%  // check for embedded mode:
    if (embeddedMode) {
        // disallow jndi, redirect back to main db page:
        response.sendRedirect("setup-datasource-settings.jsp");
        return;
    }
%>

<%  // Get parameters
    String jndiName = ParamUtils.getParameter(request,"jndiName");
    String jndiNameMode = ParamUtils.getParameter(request,"jndiNameMode");

    // Handle a continue request:
    Map errors = new HashMap();
    if (request.getParameter("continue") != null) {
        String lookupName = null;
        // Validate the fields:
        if ("custom".equals(jndiNameMode) && jndiName == null) {
            errors.put("jndiName","Please enter a valid JNDI name.");
        }
        else if ((jndiNameMode == null || "custom".equals(jndiNameMode)) && jndiName != null) {
            lookupName = jndiName;
        }
        else {
            lookupName = jndiNameMode;
        }
        // if no errors, continue
        if (errors.size() == 0) {
            // Set the JNDI connection class property in the jive props file
            JiveGlobals.setProperty("connectionProvider.className",
                    "org.jivesoftware.database.JNDIDataSourceProvider");
            // Save the name (must do this *first* before initializing
            // the JNDIDataSourceProvider
            JiveGlobals.setProperty("database.JNDIProvider.name",lookupName);
            // Use the Jive default connection provider
            JNDIDataSourceProvider conProvider = new JNDIDataSourceProvider();
            conProvider.setProperty("name", lookupName);
            // Set the provider in the connection manager
            DbConnectionManager.setConnectionProvider(conProvider);
            // Try to establish a connection to the datasource
            if (testConnection(errors)) {
                // update the sidebar status
                session.setAttribute("jive.setup.sidebar.3","done");
                session.setAttribute("jive.setup.sidebar.4","in_progress");
                // All good, so redirect
                response.sendRedirect("setup-admin-settings.jsp");
                return;
            }
        }
    }
%>

<%@ include file="setup-header.jspf" %>

<p class="jive-setup-page-header">
<fmt:message key="setup.datasource.jndi.setting" />
</p>

<p>
<fmt:message key="setup.datasource.jndi.setting_info">
    <fmt:param value="<%= LocaleUtils.getLocalizedString("short.title") %>" />
    <fmt:param value="<tt>java:comp/env/jdbc/[DataSourceName]</tt>" />
</fmt:message>
</p>

<%  if (errors.size() > 0 && errors.get("jndiName") == null) { %>

    <p class="jive-error-text">
    <%= errors.get("general") %>
    </p>

<%  } %>

<form action="setup-datasource-jndi.jsp" name="jndiform" method="post">

<%  boolean isLookupNames = false;
    Context context = null;
    NamingEnumeration ne = null;
    try {
        context = new InitialContext();
        ne = context.listBindings("java:comp/env/jdbc");
        isLookupNames = ne.hasMore();
    }
    catch (Exception e) {}
%>

<%  if (!isLookupNames) { %>

    <fmt:message key="setup.datasource.jndi.name" />
    <input type="text" name="jndiName" size="30" maxlength="100"
     value="<%= ((jndiName!=null) ? jndiName : "") %>">

<%  } else { %>

    <table cellpadding="3" cellspacing="3" border="0">
    <tr>
        <td><input type="radio" name="jndiNameMode" value="custom"></td>
        <td>
            <span onclick="document.jndiform.jndiName.focus();"
            ><fmt:message key="setup.datasource.jndi.custom" /></span>
            &nbsp;
            <input type="text" name="jndiName" size="30" maxlength="100"
             value="<%= ((jndiName!=null) ? jndiName : "") %>"
             onfocus="this.form.jndiNameMode[0].checked=true;">
            <%  if (errors.get("jndiName") != null) { %>

                <span class="jive-error-text"><br>
                <fmt:message key="setup.datasource.jndi.valid_name" />
                </span>

            <%  } %>
        </td>
    </tr>
        <%  int i = 0;
            while (ne != null && ne.hasMore()) {
                i++;
                Binding binding = (Binding)ne.next();
                String name = "java:comp/env/jdbc/" + binding.getName();
                String display = "java:comp/env/jdbc/<b>" + binding.getName() + "</b>";
        %>
            <tr>
                <td><input type="radio" name="jndiNameMode" value="<%= name %>" id="rb<%= i %>"></td>
                <td>
                    <label for="rb<%= i %>" style="font-weight:normal"
                     ><%= display %></label>
                </td>
            </tr>

        <%  } %>
    </table>

<%  } %>

<br><br>

<hr size="0">

<div align="right">
    <input type="submit" name="continue" value=" <fmt:message key="global.continue" /> ">
    <br>
    <fmt:message key="setup.datasource.jndi.note" />
</div>

</form>

<script language="JavaScript" type="text/javascript">
<!--
document.jndiform.jndiName.focus();
//-->
</script>

<%@ include file="setup-footer.jsp" %>