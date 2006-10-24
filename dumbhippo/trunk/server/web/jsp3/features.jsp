<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Features</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>	
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht3:page>
	<dht3:shinyBox color="grey">
		<div class="dh-download-header-area">
			<div class="dh-page-shinybox-title-large">Mugshot Features</div>
			<div class="dh-page-shinybox-subtitle-bold"><span class="dh-download-product">Get maximum Mugshot!</span> The Mugshot download gives you all of our features.  It's easy and free!</div>
	        <table class="dh-download-buttons" cellspacing="0" cellpadding="0">
	        	<tr height="27px">
	        	<td><span class="dh-button"><a href="/signup"><img src="/images3/${buildStamp}/signup.gif"/></a></span></td>
	        	<td valign="middle" align="center"><span class="dh-download-buttons-or">or</span></td>
	        	<td><span class="dh-button"><a href="/who-are-you"><img src="/images3/${buildStamp}/login.gif"/></a></span></td>
	        	</tr>
	        </table>
            </div> 			
		</div>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/mugshot70x64.png"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Set up your own <span class="dh-download-product">Mugshot</span>.</div>
			<div class="dh-download-section-description">
				It's an easy way for you and friends to keep track of other online.  Show a list of sites you belong to, like MySpace, Digg, and LiveJournal.  Display
				streams from your Flickr and YouTube accounts.  Share what you're browsing and listening to, using Web Swarm and Music Radar.  Your Mugshot page also
				includes friends' recent web activity.  Keep you with everything on one convenient page!
				<div><a class="dh-underlined-link" href="/active-people">See some members' Mugshots</a></div>
			</div>
			</td>
			</tr>
		</table>		
		<dht3:webSwarmLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/links-learnmore">Learn more</a>		
		</dht3:webSwarmLearnMore>
		<dht3:radarLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/radar-learnmore">Learn more</a>	
		</dht3:radarLearnMore>
		<dht3:stackerLearnMore>
			<a class="dh-download-learnmore dh-underlined-link" href="/stacker-learnmore">Learn more</a>
		</dht3:stackerLearnMore>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/groups70x59.png"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Join some <span class="dh-download-product">Groups</span>.</div>
			<div class="dh-download-section-description">
				<p>Join groups for what interests you to share posts and RSS feeds.  Or <a href="/create-group">start your own</a>!</p>
				<p><span class="dh-download-section-subheader">Browse Groups by:</span> <a href="/active-groups">Activity</a></p>
			</div>
			</td>
			</tr>
		</table>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/mugshot70x64.png"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header"><span class="dh-download-product">Invite friends</span> to Mugshot.</div>
			<div class="dh-download-section-description">
				<div>It's free, fun and easy to use.  The more the merrier!</div>
				<div><a class="dh-underlined-link" href="/invitation">Invite friends</a></div>
			</div>
			</td>
			</tr>
		</table>
		<table class="dh-box-grey1 dh-download-section" cellspacing="0" cellpadding="0">
			<tr>
			<td valign="top" class="dh-download-section-icon-area"><img src="/images3/${buildStamp}/underthehood54x62.png"/></td>
			<td class="dh-download-section-details-area"><div class="dh-download-section-header">Get under the hood.</div>
			<div class="dh-download-section-description">
				<div>Mugshot is all open source.  Here's text explaining how you can get involved in tweaking and contributing to Mugshot!</div>
				<div><a class="dh-underlined-link" href="http://developer.mugshot.org">Learn more</a></div>
			</div>
			</td>
			</tr>
		</table>				
	</dht3:shinyBox>
</dht3:page>
