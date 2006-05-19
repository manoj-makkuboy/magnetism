<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="bookmark" class="com.dumbhippo.web.BookmarkPage" scope="request"/>

<c:choose>
<c:when test='${(browser.ie && browser.windows && empty param["browser"]) || param["browser"] == "ie" }'>
	<c:set var="isIeWin" value="true" scope="page"/>
	<c:set var="unsupported" value="false" scope="page"/>	
</c:when>
<c:when test='${(browser.gecko && empty param["browser"]) || param["browser"] =="gecko" }'>
	<c:set var="isIeWin" value="false" scope="page"/>
	<c:set var="dragDestImg" value="bookmarkfirefox.gif" scope="page"/>
	<c:set var="browserTitle" value="FIREFOX" scope="page"/>
	<c:set var="unsupported" value="false" scope="page"/>	
</c:when>
<c:when test='${(browser.khtml && os.mac && empty param["browser"]) || param["browser"] == "safari" }'>
	<c:set var="isIeWin" value="false" scope="page"/>
	<c:set var="dragDestImg" value="bookmarksafari.gif" scope="page"/>
	<c:set var="browserTitle" value="SAFARI" scope="page"/>
	<c:set var="unsupported" value="false" scope="page"/>	
</c:when>
<c:otherwise>
	<c:set var="unsupported" value="true" scope="page"/>
</c:otherwise>
</c:choose>

<head>
	<title>Mugshot Bookmark</title>
	<link rel="stylesheet" type="text/css" href="/css2/bookmark.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage topImage="/images2/header_bookmark500.gif" fullHeader="true">
<c:choose>
	<c:when test="${isIeWin}">
		<dht:zoneBoxTitle>USING MUGSHOT LINK SWARM</dht:zoneBoxTitle>	
		Since you're using Internet Explorer, you can <a href="/welcome">download</a> the
		Link Swarm application and share links right from your Explorer toolbar.
	</c:when>
	<c:when test="${unsupported}">
		<dht:zoneBoxTitle>BROWSER NOT SUPPORTED</dht:zoneBoxTitle>		
		Your browser isn't recognized as being supported.  We're working on supporting
		more, check back here later.  You can see instructions for different browsers
		below; one of them might apply for you.
	</c:when>
	<c:otherwise>
	<dht:zoneBoxTitle>USING MUGSHOT LINK SWARM IN <c:out value="${browserTitle}"/></dht:zoneBoxTitle>
	<p>Drag the link below onto the Bookmarks toolbar.  Then to share and chat about
	a site with friends, click 'Mugshot Link Swarm' on your Bookmarks toolbar.</p>
	<div id="dhBookmarkHowto">
	<table cellspacing="0" cellpadding="0">
	<tr>
	<td width="10px;"><div></div></td>
	<td align="left" valign="bottom"><img src="/images2/dragthis.gif"/></td>
	<td align="center" valign="bottom">
	<div id="dhBookmarkLink">
	<a href="javascript:window.open('${bookmark.baseUrl}/sharelink?v=1&url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title)+'&next=close','_NEW','menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=400,width=550,top='+((screen.availHeight-400)/2)+',left='+((screen.availWidth-550)/2));void(0);">Link Swarm</a>
	</div>
	</td>
	<td align="right" valign="bottom"><img src="/images2/${dragDestImg}"/></td>
	<td width="10px;"><div></div></td>	
	</tr>
	</table>
	</div>
	<dht:zoneBoxTitle>IF THE BOOKMARK DOESN'T SHOW UP IN THE TOOLBAR</dht:zoneBoxTitle>
	<p>Go to the <span class="dh-bookmark-uiref">View</span> menu, and under <span class="dh-bookmark-uiref">Toolbars</span> make sure <span class="dh-bookmark-uiref">Bookmark Toolbar</span> is
	checked.</p>
	<p>If you see the Bookmark Toolbar but the Mugshot Link Swarm does not appear on it,
	choose <span class="dh-bookmark-uiref">Manage Bookmarks</span> from the <span class="dh-bookmark-uiref">Bookmarks</span> menu,
	and move the bookmark to you <span class="dh-bookmark-uiref">Personal Toolbar</span> folder.
	</p>
	</c:otherwise>
</c:choose>	
	<dht:zoneBoxSeparator/>
	<div class="dh-option-list">
	Instructions for: <a class="dh-option-list-option" href="?browser=ie">Internet Explorer</a> | 
	<a class="dh-option-list-option" href="?browser=gecko">Firefox</a> | 
	<a class="dh-option-list-option" href="?browser=safari">Safari</a>
	</div>
</dht:systemPage>
</html>
