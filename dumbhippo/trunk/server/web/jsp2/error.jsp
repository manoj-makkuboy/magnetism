<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:choose>
	<c:when test='${!empty param["text"]}'>
		<c:set var="text" value='${param["text"]}'/>
	</c:when>
	<c:otherwise>
		<c:set var="text" value="Oops! Our system burped."/>
	</c:otherwise>
</c:choose>

<head>
	<title>Oops!</title>
	<link rel="stylesheet" type="text/css" href="/css2/error.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage disableJumpTo="true" topImage="/images2/header_oops500.gif">
	<p class="dh-error-header">
		<c:choose>
			<c:when test='${!empty errorHtml}'>
				${errorHtml}
			</c:when>
			<c:otherwise>
				<c:out value='${text}'/>
			</c:otherwise>
		</c:choose>
	</p>
	<p><dht:backLink/>
	<c:if test='${!empty param["retry"]}'> or <a href='${param["retry"]}'>try again</a></c:if></p>
</dht:systemPage>	
</html>
