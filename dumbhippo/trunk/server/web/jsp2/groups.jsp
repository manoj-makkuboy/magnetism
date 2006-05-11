<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test='${empty param["who"] && !signin.valid}'>
	<dht:errorPage>I don't know whose groups you are looking at!</dht:errorPage>
</c:if>

<c:choose>
	<c:when test='${empty param["who"]}'>
		<c:set var="fromHome" value='true' scope="page"/>
		<c:set var="who" value='${signin.user.id}' scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="fromHome" value='false' scope="page"/>
		<c:set var="who" value='${param["who"]}' scope="page"/>
	</c:otherwise>
</c:choose>

<dh:bean id="person" class="com.dumbhippo.web.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>

<c:if test="${!person.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Groups</title>
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxGroups back='true'>
			<dht:zoneBoxTitle>ALL <c:out value='${fromHome ? "MY " : "" }'/>GROUPS</dht:zoneBoxTitle>
			<dht:twoColumnList>
				<c:forEach items="${person.groups.list}" var="group">
					<dht:groupItem group="${group}"/>
				</c:forEach>
			</dht:twoColumnList>
		</dht:zoneBoxGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
