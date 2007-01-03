dojo.provide('dh.infoviewer');
dojo.require('dh.util');

dh.infoviewer.onImageMouseOver = function(e) {
    if (!e) e = window.event;    
	var infoDiv = document.getElementById("dhInfo" + this.dhImageId);
	var imageDiv = document.getElementById("dhImage" + this.dhImageId);
	var pageOuterDiv = document.getElementById("dhPageOuter");
	
	// we don't want the info box to move if it is already visible	
	if (infoDiv.style.display == "block")
	    return;
        
    var width = window.innerWidth ? window.innerWidth : document.body.offsetWidth;
    var xOffset = window.pageXOffset ? window.pageXOffset : document.body.scrollLeft;
	var pageOuterPos = dh.util.getBodyPosition(pageOuterDiv);
	var imageDivPos = dh.util.getBodyPosition(imageDiv);	
	
	if (e.clientX + 350 > width) {
 	    infoDiv.style.left = (xOffset + width - 350 - pageOuterPos.x) + "px"; 
 	} else {
 	    infoDiv.style.left = (imageDivPos.x - pageOuterPos.x + 15) + "px";
 	}
 	    
	infoDiv.style.top = (imageDivPos.y - pageOuterPos.y + 50) + "px";
	
	infoDiv.style.display = "block";				
}

dh.infoviewer.onImageMouseOut = function(e) {
	if (!e) e = window.event;
	var infoDiv = document.getElementById("dhInfo" + this.dhImageId);
    var relTarget = e.relatedTarget || e.toElement;		
    if (!dh.util.isDescendant(infoDiv, relTarget))
	    infoDiv.style.display = "none";
}

dh.infoviewer.onInfoMouseOut = function(e) {
	if (!e) e = window.event;
	var imageDiv = document.getElementById("dhImage" + this.dhInfoId);
	var infoDiv = document.getElementById("dhInfo" + this.dhInfoId);
    var relTarget = e.relatedTarget || e.toElement;		
    if (!dh.util.isDescendant(imageDiv, relTarget) && !dh.util.isDescendant(infoDiv, relTarget))
	    infoDiv.style.display = "none";
}