<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- If there are no groups for someone !self, just omit this box --%>
<c:if test="${person.groups.size > 0 || person.self}">
	
	<c:choose>
		<c:when test="${person.self}">
			<c:set var="title" value="MY GROUPS" scope="page"/>
		</c:when>
		<c:otherwise>
			<c:set var="title" value="GROUPS" scope="page"/>
		</c:otherwise>
	</c:choose>
	
	<dht:sidebarBox boxClass="dh-groups-box" title="${title}" more="/groups">
		<c:choose>
			<c:when test="${person.groups.size > 0}">
				<c:forEach items="${person.groups.list}" var="group">
					<dht:sidebarBoxGroupItem group="${group}"/>
				</c:forEach>
			</c:when>
			<c:otherwise>
				No groups <%-- FIXME link to a place to add groups --%>
			</c:otherwise>
		</c:choose>
	</dht:sidebarBox>
</c:if>
