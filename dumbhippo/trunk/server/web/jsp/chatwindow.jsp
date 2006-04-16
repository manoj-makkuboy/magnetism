<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="chatwindow" class="com.dumbhippo.web.ChatWindowPage" scope="request"/>
<%-- only one of these params is expected at a time... chatId just means 
	"figure out whether it's post or group" which is less efficient but 
	some calling contexts might not know the type of chat --%>
<jsp:setProperty name="chatwindow" property="postId" param="postId"/>
<jsp:setProperty name="chatwindow" property="groupId" param="groupId"/>
<jsp:setProperty name="chatwindow" property="chatId" param="chatId"/>

<c:if test="${! chatwindow.aboutSomething}">
	<%-- no post or group, or invalid/not-allowed post or group --%>
	<dht:errorPage>Can't find this chat</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${chatwindow.title}"/></title>
	<dht:stylesheets href="chatwindow.css" iehref="chatwindow-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
    	dojo.require("dh.chatwindow");
    	dh.chatwindow.setSelfId("${chatwindow.signin.userId}")
	</script>
	<dht:chatControl userId="${chatwindow.signin.userId}" chatId="${chatwindow.chatId}"/>
	<script for="dhChatControl" type="text/javascript" event="OnUserJoin(userId, version, name, participant)">
		dh.chatwindow.onUserJoin(userId, version, name, participant)
	</script>
	<script for="dhChatControl" language="javascript" event="OnUserLeave(userId)">
		dh.chatwindow.onUserLeave(userId)
	</script>
	<script for="dhChatControl" language="javascript" event="OnMessage(userId, version, name, text, timestamp, serial)">
		dh.chatwindow.onMessage(userId, version, name, text, timestamp, serial)
	</script>
	<script for="dhChatControl" language="javascript" event="OnReconnect()">
		dh.chatwindow.onReconnect()
	</script>
	<script for="dhChatControl" type="text/javascript" event="OnUserMusicChange(userId, arrangementName, artist, musicPlaying)">
		dh.chatwindow.onUserMusicChange(userId, arrangementName, artist, musicPlaying)
	</script>
	<script type="text/javascript">
		var chatControl = document.getElementById("dhChatControl")
        if (chatControl && chatControl.readyState && chatControl.readyState == 4) {
			chatControl.Join(true)
		}
	</script>
	<script defer type="text/javascript">
		dh.chatwindow.init()
	</script>
</head>
<body scroll="no" onload="dh.chatwindow.rescan()">
    <div id="dhChatPostInfoDiv">
    	${chatwindow.titleAsHtml}
    	<c:if test="${chatwindow.aboutPost}">
    	(from <dh:entity value="${chatwindow.post.poster}" photo="false"/>)
    	</c:if>
	</div>
	<div id="dhChatPeopleContainer">
        <div id="dhChatPeopleDiv"></div>
        <div id="dhChatPeopleNE"></div>
	    <div id="dhChatPeopleNW"></div>
	</div> <!-- dhChatPeopleContainer -->
    <div id="dhChatAdsDiv">
        <div id="dhChatAdsSE"></div>
        <div id="dhChatAdsInnerDiv">
        	<dht:ad src="${psa1}"/>
        </div>
    </div>
    <div id="dhChatMessagesDiv"></div>
    <textarea id="dhChatMessageInput" cols="40" rows="3"></textarea>
    <input id="dhChatSendButton" type="button" value="Send" onclick="dh.chatwindow.sendClicked()"></input>
</body>
</html>
