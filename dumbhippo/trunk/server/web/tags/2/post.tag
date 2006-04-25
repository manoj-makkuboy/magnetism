<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView" %>
<%@ attribute name="favesMode" required="false" type="java.lang.String" %>
<%@ attribute name="showPhoto" required="false" type="java.lang.Boolean" %>

<c:if test="${showPhoto}">
	<c:set var="itemPhotoClass" value="dh-item-with-photo" scope="page"/>
</c:if>

<div class="dh-item ${itemPhotoClass}">
	<c:if test="${showPhoto}">
		<div class="dh-image">
		</div>
	</c:if>
	<div class="dh-next-to-image">
		<div class="dh-title">
			<a href="${post.url}" onClick="return dh.util.openFrameSet(window,event,this,'${post.post.id}');"
				title="${post.url}">
				<c:out value="${post.titleAsHtml}" escapeXml="false"/>
			</a>
		</div>
		<div class="dh-blurb">
			<c:out value="${post.textAsHtml}" escapeXml="false"/>
		</div>
		<div class="dh-extra-info">
			<table cellpadding="0" cellspacing="0">
				<tbody>
					<tr>
						<td align="left">
							<div class="dh-attribution">
								sent by <a href="/person?who=${post.poster.viewPersonPageId}" class="dh-name-link">
									<c:out value="${post.poster.name}"/>
								</a>
							</div>
						</td>
						<td align="right">
							<%-- FIXME --%>
							<div class="dh-counts">NN views | NN quips							
								<c:choose>
									<c:when test="${favesMode == 'none' || !signin.valid}">
										
									</c:when>
									<c:when test="${post.favorite}">
										<c:if test="${favesMode != 'add-only'}">
										 | <a href="javascript:dh.actions.setPostFavorite('${post.post.id}', false);">remove</a>
										</c:if>
									</c:when>
									<c:otherwise>
										| <a href="javascript:dh.actions.setPostFavorite('${post.post.id}', true);">add to faves</a>
									</c:otherwise>
								</c:choose>
							</div>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
	<div class="dh-grow-div-around-floats"><div></div></div>
</div>
