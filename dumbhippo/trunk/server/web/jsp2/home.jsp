<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css2/home.css"/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage disableHomeLink="true">
	<dht:sidebarPerson who="${signin.user.id}"/>
	<dht:contentColumn>
		<dht:zoneBoxWeb more="true">
			<dht:requireLinksPersonBean who="${signin.user.id}"/>
			<c:if test="${links.favoritePosts.size > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
					<dht:postList posts="${links.favoritePosts.list}" format="simple"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.receivedPosts.totalCount > 0}">
					<dht:postList posts="${links.receivedPosts.results}" format="simple"/>
				</c:when>
				<c:otherwise>
					Nothing shared with you yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic more="true">
			<dht:requireMusicPersonBean who="${signin.user.id}"/>
			<dht:zoneBoxTitle>CURRENTLY LISTENING TO</dht:zoneBoxTitle>
			<dh:nowPlaying userId="${signin.user.id}" hasLabel="false"/>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>MY RECENT SONGS</dht:zoneBoxTitle>
					
			<c:forEach items="${musicPerson.recentTracks.results}" var="track">
				<dht:track track="${track}" oneLine="true" playItLink="false"/>
			</c:forEach>
		</dht:zoneBoxMusic>
		<dht:zoneBoxTv more="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
