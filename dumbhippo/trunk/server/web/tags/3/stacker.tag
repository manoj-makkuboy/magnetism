<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="stack" required="true" type="java.util.List" %>
<%@ attribute name="stackSize" required="false" type="java.lang.Integer" %>

<c:if test="${stackSize == 0}">
	<c:set var="stackSize" value="5"/>
</c:if>

<div class="dh-stacker-container">
	<c:forEach items="${stack}" end="${stackSize}" var="block" varStatus="blockIdx">
		<c:choose>
			<c:when test="${blockIdx.count % 2 == 0}">
				<dht3:block block="${block}" cssClass="dh-stacker-block-grey1"/>
			</c:when>
			<c:otherwise>
				<dht3:block block="${block}" cssClass="dh-stacker-block-grey2"/>
			</c:otherwise>
		</c:choose>
		<div class="dh-stacker-block-bottom-padding">&nbsp;</div>
	</c:forEach>	
</div>
