<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.BlockView" %>
<%@ attribute name="cssClass" required="true" type="java.lang.String" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<div class="dh-stacker-block ${cssClass}" id="dhStackerBlock-${blockId}" onmouseover="dh.stacker.blockHoverStart('${blockId}');" onmouseout="dh.stacker.blockHoverStop('${blockId}');">
	<table class="dh-stacker-block-header" cellspacing="0" cellpadding="0">
	<tr><td align="left" width="20px"><img class="dh-stacker-block-icon" src="/images3/${buildStamp}/${block.iconName}"/></td>
	<td align="left"><span class="dh-stacker-block-title">	
		<span class="dh-stacker-block-title-type">
		    <c:if test="{!empty block.personSource}">
		        <a href="/person?who=${block.personSource.viewPersonPageId}"><c:out value="${block.personSource.name}"/></a>'s
		    </c:if>    
		    <c:out value="${block.webTitleType}"/>:
		</span>
		<span class="dh-stacker-block-title-title">
			<c:choose>
		    	<c:when test="${dh:enumIs(block.blockType, 'MUSIC_PERSON')}">
					<dht3:musicPersonTitle block="${block}"/>
				</c:when>					
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
	<div class="dh-stacker-block-right">
		<div class="dh-stacker-block-close" id="dhStackerBlockClose-${blockId}">
			<a href="javascript:dh.stacker.blockClose('${blockId}')">CLOSE</a> <a href="javascript:dh.stacker.blockClose('${blockId}')"><img src="/images3/${buildStamp}/close.png"/></a>
		</div>
		<span class="dh-stacker-block-time">
			${block.timeAgo}
		</span>
		<div class="dh-stacker-block-controls" id="dhStackerBlockControls-${blockId}">
	    <c:choose>
	    	<c:when test="${dh:enumIs(block.blockType, 'POST')}">
				<dht3:postBlockControls block="${block}"/>
			</c:when>		
		</c:choose>
		</div>
	</div>
	</td>
	</tr>
	</table>	
	<div class="dh-stacker-block-description">
		${block.descriptionHtml}
	</div>
	<table class="dh-stacker-block-content" id="dhStackerBlockContent-${blockId}">
		<tr>
		<td class="dh-stacker-block-content-left">&nbsp;</td>
		<td width="100%">
	    <c:choose>
	    	<c:when test="${dh:enumIs(block.blockType, 'POST')}">
				<dht3:postBlock block="${block}"/>
			</c:when>
			<c:when test="${dh:enumIs(block.blockType, 'MUSIC_PERSON')}">	
				<dht3:musicPersonBlock block="${block}"/>
			</c:when>
		</c:choose>
		</td>
		</tr>
		<tr><td><div class="dh-stacker-block-content-padding">&nbsp;</div></td></tr>
	</table>
</div>
