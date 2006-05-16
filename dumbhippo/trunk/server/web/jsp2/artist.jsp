<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="musicsearch" class="com.dumbhippo.web.MusicSearchPage" scope="page"/>
<jsp:setProperty name="musicsearch" property="song" param="track"/>
<jsp:setProperty name="musicsearch" property="album" param="album"/>
<jsp:setProperty name="musicsearch" property="artist" param="artist"/>

<head>
	<title><c:out value="${musicsearch.expandedArtistView.name}"/></title>
	<link rel="stylesheet" type="text/css" href="/css2/artist.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>	
</head>

<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxArtists>
            <c:choose>
		        <c:when test="${not empty musicsearch.expandedArtistView}">
                    <dht:artistProfile artist="${musicsearch.expandedArtistView}"/>
                    <dht:zoneBoxSeparator/>
                    <a name="dhAlbumsByArtist"></a>
                    <div class="dh-artist-zone-title">DISCOGRAPHY</div>
                    <c:if test="${musicsearch.albumsByArtist.resultCount <= 0}">
                        There were no matching albums.
                    </c:if>            
                       
                    <c:set var="count" value="1"/>   
                    <c:forEach items="${musicsearch.albumsByArtist.results}" var="album">
					    <dht:album album="${album}" order="${count}"/>
					    <c:set var="count" value="${count+1}"/>
					</c:forEach>
	
	                <div class="dh-artist-more">
					    <dht:expandablePager pageable="${musicsearch.albumsByArtist}" anchor="dhAlbumsByArtist"/>
					</div>
                </c:when>
                <c:otherwise>
                    There were no matching results.
                </c:otherwise>
            </c:choose>    
            <dht:zoneBoxSeparator/>
            <dht:webServicesAttributions/>            
        </dht:zoneBoxArtists>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>           
            