<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.PersonPage"/>

<dh:bean id="account" class="com.dumbhippo.web.pages.AccountPage" scope="page"/>

<%-- This is a Facebook authetication token and an error message generated by us if we couldn't --%>
<%-- process the token. We use FacebookServlet to process the token, which then redirects here. --%>
<jsp:setProperty name="account" property="facebookAuthToken" param="auth_token"/>
<jsp:setProperty name="account" property="facebookErrorMessage" param="error_message"/>

<c:set var="termsOfUseNote" value='false'/>
<c:if test='${!empty param["termsOfUseNote"]}'>
    <c:set var="termsOfUseNote" value='${param["termsOfUseNote"]}'/> 
</c:if>

<c:set var="pageName" value="Account" scope="page"/>

<dh:script modules="dh.account"/>
<script type="text/javascript">
    dh.account.dhNames = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.onlineAccountType.name}"/> ${status.last ? '];' : ','}</c:forEach>
    dh.account.dhUserInfoTypes = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.userInfoType}"/> ${status.last ? '];' : ','}</c:forEach>
    dh.account.dhValues = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.username}"/> ${status.last ? '];' : ','}</c:forEach>
    dh.account.dhHateQuips = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.hateQuip}"/> ${status.last ? '];' : ','}</c:forEach>
    dh.account.dhIds = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.id}"/> ${status.last ? '];' : ','}</c:forEach>
    dh.account.dhDomIds = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.domNodeIdName}"/> ${status.last ? '];' : ','}</c:forEach>
    dh.account.dhMugshotEnabledFlags = [ <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount" varStatus="status"><dh:jsString value="${supportedAccount.mugshotEnabled}"/> ${status.last ? '];' : ','}</c:forEach>
</script>
    
<head>
    <title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>			
	<dht3:stylesheet name="account" iefixes="true"/>	
	<dht:faviconIncludes/>
    <dht3:accountJavascriptSetup account="${account}"/>
</head>
<dht3:page currentPageLink="account">
	<dht3:accountStatus enableControl="true"/>
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName}">
		<dht3:randomTip isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs/> 
	</dht3:pageSubHeader>
	<div id="dhAccountContents" class="${signin.active ? 'dh-account-contents' : 'dh-account-contents-disabled'}">
		<dht3:shinyBox color="grey">
		    <c:if test="${param.fromDownload != 'true' && !accountStatusShowing}">
			    <div class="dh-page-shinybox-subtitle"><span class="dh-download-product">Get maximum Mugshot!</span> <a class="dh-underlined-link" href="/download">Download the Mugshot software</a> to use all of our features.  It's easy and free!</div>
			</c:if>
			<dht3:accountEditTable account="${account}" termsOfUseNote="${termsOfUseNote}"/>
		</dht3:shinyBox>
    </div>
	</dht3:page>		
	<dht:photoChooser/>
</html>
