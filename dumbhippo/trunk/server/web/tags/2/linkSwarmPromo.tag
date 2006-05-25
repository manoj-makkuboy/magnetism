<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="separator" required="false" type="java.lang.Boolean" %>

<table><tr>
<td rowSpan="2"><img src="/images2/${buildStamp}/buzzer50x44.gif" /></td>
<td><strong>Link Swarm</strong> lets you share and chat about links with friends.</td>
</tr><tr>
<td>
	<span class="dh-option-list"><%-- Just want to hack the style of this one element --%>
	<a style="margin-left:0;" class="dh-option-list-option" href="/links-learnmore">Learn More</a>
	|
	<a class="dh-option-list-option" href="/links">See what people are sharing</a>
	</span>
</td>
</tr></table>
<c:if test="${!empty separator && separator}">
        <dht:zoneBoxSeparator/>
</c:if>

