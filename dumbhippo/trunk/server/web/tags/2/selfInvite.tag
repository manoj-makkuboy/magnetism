<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%-- this tag is intended to be included only if invites are available --%>
<%@ attribute name="promotion" required="true" type="java.lang.String"%>
<%@ attribute name="invitesAvailable" required="true" type="java.lang.Integer"%>
<%@ attribute name="summitSelfInvite" required="false" type="java.lang.Boolean" %>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhSelfInviteCount}">
	<c:set var="dhSelfInviteCount" value="0" scope="request"/>
</c:if>
<c:set var="dhSelfInviteCount" value="${dhSelfInviteCount + 1}" scope="request"/>
<c:set var="N" value="${dhSelfInviteCount}" scope="page"/>

<div>
	<script type="text/javascript">
		dojo.require('dh.util');
		dojo.require('dh.server');
		
		var dhSelfInviteComplete${N} = function(message) {
			var messageNode = document.getElementById('dhSelfInviteMessage${N}');
			dh.util.clearNode(messageNode);
			messageNode.appendChild(document.createTextNode(message));
		}
		var dhSelfInvite${N} = function() {
			var addressNode = document.getElementById('dhSelfInviteAddress${N}');
			dh.util.hideId('dhSelfInviteForm${N}');
			dh.util.showId('dhSelfInviteMessage${N}');
		   	dh.server.getXmlPOST("inviteself",
			     {
			     	"address" : addressNode.value,
			     	"promotion" : "${promotion}"
			     },
	  	    	 function(type, data, http) {
	  	    	 	var messageElements = data.getElementsByTagName("message");
	  	    	 	if (!messageElements)
	  	    	 		text = "Something went wrong... (1)";
	  	    	 	else {
						var messageElement = messageElements.item(0);
						if (!messageElement)
							text = "Something went wrong... (2)";
						else
							text = messageElement ? dojo.dom.textContent(messageElement) : "Something went wrong... (3)";
					}
					dhSelfInviteComplete${N}(text);
	  	    	 },
	  	    	 function(type, error, http) {
	  	    	    dhSelfInviteComplete${N}("Something went wrong! Reload the page and try again.");
	  	    	 });
		}
	</script>
	<div id="dhSelfInviteForm${N}">
	    <c:choose>
	        <c:when test="${summitSelfInvite}">
		        <div class="dh-special-subtitle">
                    You heard, you browsed, you signed up!
                </div>
		    </c:when>
		    <c:otherwise>
		        <div class="dh-special-subtitle">
		            Enter your email address to get our fun and free stuff.
		            <br/>
			        Then check your email for a link to our download page.
		        </div>		   
		    </c:otherwise>
		</c:choose>    	
		<input type="text" class="dh-text-entry" id="dhSelfInviteAddress${N}"/>
		<input type="button" value="Send" onclick="dhSelfInvite${N}()"/>
	</div>
	<br/>
	<div id="dhSelfInviteMessage${N}" class="dh-landing-result dhInvisible">
		Thinking...
	</div>
</div>
