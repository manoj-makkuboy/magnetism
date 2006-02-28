<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="post"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>
<head>
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
	<script type="text/javascript">
		function dhWriteFramesPage() {
			document.open()
			document.write(
"<html>" +
"<head>" +
"   <title><c:out value='${title}'/></title>" +
"	<script type='text/javascript'>" +
"		if (parent != self) {" +
"			// only look at the base parts of the url" +
"			var pLoc = parent.location.host + parent.location.pathname;" +
"			var sLoc = self.location.host + self.location.pathname;" +
"			if (pLoc == sLoc)" +
"				parent.location.href = self.location.href;" +
"		}" +
"	<" + "/script>" +
"</head>" +
"<frameset rows='*,125'>" +
"   <frame name='top' src='${framer.post.url}'>" +
"    <frame name='bottom' src='framer?postId=${framer.postId}' scrolling='no' noresize bordercolor='#cccccc' marginwidth='0' marginheight='0'>" +
"</frameset>" +
"<noframes>" + 
"    Your browser does not support frames.  <a href='${framer.post.url}'>Click here</a> for page." +
"</noframes>" +
"</html>"
			)
			document.close()
		}
		function dhInit() {
			var embedObject = document.getElementById("dhEmbedObject")
	        if (embedObject && embedObject.readyState && embedObject.readyState == 4) {
				embedObject.OpenBrowserBar()
		        window.open("${framer.post.url}", "_self", "", true)
			} else {
				dhWriteFramesPage()
			}
		}
	</script>
    <title><c:out value="${title}"/></title>
    <dht:stylesheets />
</head>
<body onload="dhInit()">
</body>
</html>
