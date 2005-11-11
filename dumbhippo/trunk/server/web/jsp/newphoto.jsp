<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>New Photo</title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
</head>
<body>
	<dht:header>
		New Photo
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<p>Your new photo looks like this:</p>
		<img src="/files/${photoLocation}/${photoFilename}"/>
		<p>(If this is your old photo, your browser probably cached it. <a href="/home">Go to your page</a> and then press reload.)
		</p>

		<p>If you hate this photo, you can try again:</p>
		<dht:uploadPhoto location="${photoLocation}"/>
		
		<p><a href="/home">Go to your page</a></p>
	</div>
</body>
</html>
