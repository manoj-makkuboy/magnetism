<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="main"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
	<script src="/javascript/${buildStamp}/dh/stacker.js" type="text/javascript"></script>	
</head>

<dh:bean id="main" class="com.dumbhippo.web.pages.MainPage" scope="request"/>

<dht3:page>
		<dht3:shinyBox color="purple">
		    <div id="dhMugshotIntro">
		        <div class="dh-intro-message">Put you mugshot on the web.</div>
		        <div class="dh-intro-explanation">Everything you and your friends do on the web, in real time.</div>
		        <div class="dh-intro-options">
                    <span class="dh-button"><a href="/signup"><img src="/images3/${buildStamp}/signup.gif"/></a></span>
                    &nbsp; or &nbsp;
                    <span class="dh-button"><a href="/who-are-you"><img src="/images3/${buildStamp}/login.gif"/></a></span>
               </div> 
           </div>        
		</dht3:shinyBox>
        <div class="dh-header">Featured Mugshots</div>
        <c:forEach items="${main.recentActivity}" var="personMugshot" varStatus="status">
           <c:choose>
                <%-- count starts with 1 so we want the odd boxes to float left --%>
			    <c:when test="${status.count % 2 == 0}">
			        <c:set var="floatSide" value="right" scope="page"/>
			   </c:when>
			   <c:otherwise>
			        <c:set var="floatSide" value="left" scope="page"/>
			   </c:otherwise>
		    </c:choose>
            <dht3:personStack contact="${personMugshot.personView}" stackOrder="${status.count}" blocks="${personMugshot.blocks}" showFrom="true" embedVersion="true" width="49%" floatSide="${floatSide}"/>
        </c:forEach>
</dht3:page>
