<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<script type="text/javascript">dojo.require("dh.artist");</script>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView"%>

<div class="dh-framer-share">
	<div class="dh-framer-from-container">
		<div class="dh-framer-from-area">
			<div class="dh-framer-headshot">
				<dht:headshot person="${post.poster}" size="60"/>
			</div>
			<div>
				<a class="dh-framer-from" href="/person?who=${post.poster.viewPersonPageId}"><c:out value="${post.poster.name}"/></a>
			</div>
		</div>
	</div>
	<div class="dh-framer-content-container">
		<div class="dh-framer-content">
			<div class="dh-framer-title">
				<jsp:element name="a">
					<jsp:attribute name="href"><c:out value="${post.url}"/></jsp:attribute>
					<jsp:attribute name="onClick">return dh.util.openFrameSet(window,event,this,'${post.post.id}');</jsp:attribute>
					<jsp:body><c:out value="${post.titleAsHtml}" escapeXml="false"/></jsp:body>
				</jsp:element>
			</div>
			<div class="dh-framer-description">
				<c:out value="${post.textAsHtml}" escapeXml="false"/>
			</div>
			<div class="dh-framer-sent-to">
				Sent to <dh:entityList value="${post.recipients}" separator=", "/>
			</div>				
		</div>	
	</div>
</div>