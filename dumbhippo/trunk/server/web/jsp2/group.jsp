<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="group" class="com.dumbhippo.web.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="who"/>
<jsp:setProperty name="group" property="fromInvite" param="fromInvite"/>

<c:if test="${empty group.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${group.name}"/>'s Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
	<dht:scriptIncludes/>
	<%-- dht:embedObject --%>
    <script type="text/javascript">
        dojo.require("dh.util");
    </script>
</head>
<dht:twoColumnPage alwaysShowSidebar="true">
	<dht:sidebarGroup/>
	<dht:contentColumn>
		<dht:zoneBoxGroup>
			<c:if test="${group.latestTracks.size > 0}">
				<dht:zoneBoxTitle>RECENT SONGS</dht:zoneBoxTitle>
				<c:forEach var="track" items="${group.latestTracks.list}">
					<div><c:out value="${track.name}"/> by <c:out value="${track.artist}"/> played by FIXME</div>
				</c:forEach>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle>LINKS RECENTLY SHARED WITH <c:out value="${group.name}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${group.posts.size > 0}">
					<dht:postList posts="${group.posts.list}" format="full-with-photos" favesMode="add-only"/>
					<dht:moreExpander open="false"/>
				</c:when>
				<c:otherwise>
					Nothing ever shared with this group!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxGroup>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
