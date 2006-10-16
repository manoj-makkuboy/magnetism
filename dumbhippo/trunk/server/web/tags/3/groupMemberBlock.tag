<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.GroupMemberBlockView" %>
<%@ attribute name="cssClass" required="true" type="java.lang.String" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<dht3:blockContainer cssClass="${cssClass}" blockId="${blockId}">
	<dht3:blockHeader icon="/images3/${buildStamp}/mugshot_icon.png" blockId="${blockId}">
		<dht3:blockHeaderLeft>
			<span class="dh-stacker-block-title-group-member-name"><dht3:personLink who="${block.memberView}"/></span> is a new 
			<c:choose>
				<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}">follower.</c:when>
				<c:otherwise>member.</c:otherwise>
			</c:choose>		
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}">
		</dht3:blockHeaderRight>
	</dht3:blockHeader>
	<dht3:blockDescription>
		<c:choose>
			<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}">
				<img src="/images2/${buildStamp}/add.png"/>	
				<dht:asyncActionLink 
					tagName="span"
					exec="dh.actions.addMember('${group.viewedGroupId}', '${who.user.id}', function () { dh.asyncActionLink.complete('addMember${group.viewedGroupId}${who.user.id}') })"
					ctrlId="addMember${group.viewedGroupId}${who.user.id}"
					text="Invite to group"
					completedText="Invited to group"/>	
			</c:when>
			<c:otherwise>
				Invited by <dht3:personLink who="${block.memberView}"/>.
			</c:otherwise>
		</c:choose>	
	</dht3:blockDescription>		
	<dht3:blockContent blockId="${blockId}">
	</dht3:blockContent>
</dht3:blockContainer>

