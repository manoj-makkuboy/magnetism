<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- web, music, tv ... --%>
<%@ attribute name="zone" required="true" type="java.lang.String" %>
<%@ attribute name="topImage" required="true" type="java.lang.String" %>
<%@ attribute name="bottomImage" required="true" type="java.lang.String" %>
<%@ attribute name="more" required="false" type="java.lang.String" %>
<%@ attribute name="back" required="false" type="java.lang.String" %>
<%@ attribute name="disableJumpTo" required="false" type="java.lang.Boolean" %>

<%-- provide the zone name to child nodes; this "set a global variable" approach
	sort of sucks, but I can't figure out how to "set a variable for our child tags only" --%>
<c:set var="zoneName" value="${zone}" scope="request"/>

<div class="dh-zone-box dh-color-${zone}">
	<%-- Having whitespace in here seems to confuse IE, so it's a huge line; probably there's some better way --%>
	<div class="dh-zone-box-header">
		<img src="${topImage}" class="dh-header-image"/>
		<div class="dh-zone-box-header-links">
			<c:choose>
				<c:when test="${!empty more}">
					<dht:zoneBoxLinkHeader value="MORE" link="${more}"/>
				</c:when>
				<c:when test="${!empty back}">
					<dht:zoneBoxLinkHeader value="Go back" link="${back}"/>
				</c:when>
				<c:when test="${disableJumpTo}">
					<%-- nothing --%>
				</c:when>
				<c:otherwise>
					<dht:zoneBoxJumpToHeader skip="${zone}"/>
				</c:otherwise>
			</c:choose>
		</div>
	</div>
	<div class="dh-zone-box-border">
		<div class="dh-zone-box-content dh-color-normal">			
			<jsp:doBody/>
		</div>
	</div>
	<div><img src="${bottomImage}" class="dh-bottom-image"/></div>					
</div>
