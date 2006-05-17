<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
        <title>About Music Radar</title>
        <link rel="stylesheet" type="text/css" href="/css2/music-learnmore.css"/>
        <dht:scriptIncludes/>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxMusic>	
		
		<div>
		<table cellspacing="0" cellpadding="0">
		<tr><td><img src="/images2/beacon60x60.gif"/></td>
		<td><div class="dh-music-learnmore-header">About Music Radar</div>
			<div>
				Show off your music playlist on your blog or MySpace page.  Easily
				change or edit the visual theme, or create one that fits your personality.  
				Share your music history and see what others are listening to.
			</div>
		</td>
		</tr>
		</table>
		</div>
		
		<br/>
		<div>
			<div>
			<dht:beaconSamples/>
			</div>
			<a href="/radar-themes">Browse themes to see more examples</a>
		</div>
		<br/>
		<div>
			<b>Music Radar</b> shows what's currently on your iTunes or Yahoo! Music player.
			When someone clicks on it, they'll be taken to your Mugshot page to see more
			about you and your musical tastes.  With a free Mugshot account, you can 
			keep track of what your friends are listening to and explore new music.
		</div>
		<div><a style="font-size: larger;" href="/download">Click here to get Music Radar, free!</a></div>
		
		<dht:zoneBoxSeparator/>
		
		<div class="dh-music-learnmore-section"><a href="/links-learnmore">Learn more about Link Swarm</a></div>		
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

