<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.BlockView" %>
<%@ attribute name="cssClass" required="true" type="java.lang.String" %>

<c:set var="blockId" value="${block.postView.post.id}" scope="page"/>

<div class="dh-stacker-block ${cssClass}" id="dhStackerBlock-${blockId}" onmouseover="dh.stacker.blockHoverStart('${blockId}');" onmouseout="dh.stacker.blockHoverStop('${blockId}');">
	<table class="dh-stacker-block-header" cellspacing="0" cellpadding="0" width="100%">
	<tr><td align="left" width="20px"><img class="dh-stacker-block-icon" src="/images3/${buildStamp}/${block.iconName}"/></td>
	<td align="left"><span class="dh-stacker-block-title">
		<span class="dh-stacker-block-title-type"><c:out value="${block.webTitleType}"/>:</span>
		<span class="dh-stacker-block-title-title">
			<c:choose>
				<c:when test="${!empty block.webTitleLink}">
					<jsp:element name="a">
						<jsp:attribute name="href"><c:out value="${block.webTitleLink}"/></jsp:attribute>
						<jsp:body><c:out value="${block.webTitle}"/></jsp:body>
					</jsp:element>
				</c:when>
				<c:otherwise>
					 <c:out value="${block.webTitle}"/>
				</c:otherwise>
			</c:choose>
		</span>
	</span>
	</td>
	<td>&nbsp;</td>
	<td align="right">
	<div class="dh-stacker-block-close" id="dhStackerBlockClose-${blockId}">
		<a href="javascript:dh.stacker.blockClose('${blockId}')">CLOSE</a> <a href="javascript:dh.stacker.blockClose('${blockId}')"><img src="/images3/${buildStamp}/close.png"/></a>
	</div>
	<span class="dh-stacker-block-right">
		${block.timeAgo}
	</span>
	</td>
	</tr>
	</table>	
	<div class="dh-stacker-block-description">
		${block.descriptionHtml}
	</div>
	<div class="dh-stacker-block-content" id="dhStackerBlockContent-${blockId}">	
		<div class="dh-stacker-block-content-padding">&nbsp;</div>	
		<c:choose>
			<c:when test="${dh:enumIs(block.blockType, 'POST')}">
				<dht3:postBlock block="${block}"/>
			</c:when>
			<c:otherwise>
			</c:otherwise>
		</c:choose>
		<div class="dh-stacker-block-content-padding">&nbsp;</div>		
	</div>
</div>
