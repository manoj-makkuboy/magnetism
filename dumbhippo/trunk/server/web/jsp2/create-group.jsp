<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Create Group</title>
	<link rel="stylesheet" type="text/css" href="/css2/group-account.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript" src="/javascript/dh/groupaccount.js"></script>
	<script type="text/javascript">
		dojo.require("dh.groupaccount")
		dojo.event.connect(dojo, "loaded", dj_global, "dhCreateGroupInit");
	</script>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${signin.user.id}"/>
	<dht:contentColumn>
		<dht:zoneBoxGroups back='true'>
			<div></div> <!-- IE bug workaround, display:none as first child causes problems -->
			<dht:zoneBoxTitle>ABOUT GROUPS</dht:zoneBoxTitle>
			You can create groups for specific interests and friends. Sharing links 
			with Link Swarm is easier when using a group name rather than selecting 
			the names of friends one by one. Groups can be open to the public to see, 
			or private for only group members. Any member of a group can invite new 
			members and alter account settings.
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>CREATE A GROUP</dht:zoneBoxTitle>
			<div class="dh-message" id="dhMessageDiv" style='display: none;'>
				<c:out value='${param["message"]}'/>
			</div>
			<dht:formTable>
				<dht:formTableRow label="Public or private?">
					<input type="radio" name="dhGroupVisibility" id="dhGroupVisibilityPublic" value="public" checked="yes"><b>Public</b> (Visible to anyone browsing the Mugshot website)</input> 
					<br/>
					<input type="radio" name="dhGroupVisibility" id="dhGroupVisibilityPrivate" value="private"><b>Private</b> (Visible only to group members)</input>
				</dht:formTableRow>
				<dht:formTableRow label="Group Name">
					<dht:textInput id="dhGroupNameEntry"/>
				</dht:formTableRow>
				<dht:formTableRow label="About Group">
					<dht:textInput id="dhAboutGroupEntry" multiline="true"/>
				</dht:formTableRow>
			</dht:formTable>
			<input id="dhCreateGroupSave" type="button" value="Save and start inviting people" onclick="dh.groupaccount.createGroup()"/>
			<input id="dhCreateGroupCancel" type="button" value="Cancel" onclick="document.location.href='/home'"/>
		</dht:zoneBoxGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
