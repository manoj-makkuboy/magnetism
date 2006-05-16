dojo.provide("dh.formtable")
dojo.require("dojo.lang")

dh.formtable.undoValues = {};
dh.formtable.currentValues = null; // gets filled in as part of the jsp

dh.formtable.linkClosures = {};
dh.formtable.invokeLinkClosure = function(which) {
	var f = dh.formtable.linkClosures[which];
	f.call(this);
}

dh.formtable.linkClosuresCount = 0;

dh.formtable.makeLinkClosure = function(f) {
	dh.formtable.linkClosures[dh.formtable.linkClosuresCount] = f;
	var link = "javascript:dh.formtable.invokeLinkClosure(" + dh.formtable.linkClosuresCount + ");";
	dh.formtable.linkClosuresCount = dh.formtable.linkClosuresCount + 1;
	return link;
}

dh.formtable.getStatusRow = function(controlId) {
	return document.getElementById(controlId + 'StatusRow');
}

dh.formtable.getStatusSpacer = function(controlId) {
	return document.getElementById(controlId + 'StatusSpacer');
}

dh.formtable.getStatusText = function(controlId) {
	return document.getElementById(controlId + 'StatusText');
}

dh.formtable.getStatusLink = function(controlId) {
	return document.getElementById(controlId + 'StatusLink');
}

dh.formtable.hideStatus = function(controlId) {
	var statusRow = dh.formtable.getStatusRow(controlId);
	var statusSpacer = dh.formtable.getStatusSpacer(controlId);
	statusRow.style.display = 'none';
	statusSpacer.style.display = 'none';
}

dh.formtable.showStatus = function(controlId, statusText, linkText, linkHref, linkTitle) {
	var statusRow = dh.formtable.getStatusRow(controlId);
	var statusSpacer = dh.formtable.getStatusSpacer(controlId);
	var statusTextNode = dh.formtable.getStatusText(controlId);
	var statusLinkNode = dh.formtable.getStatusLink(controlId);
	dojo.dom.textContent(statusTextNode, statusText);
	
	if (linkText) {
		dojo.dom.textContent(statusLinkNode, linkText);
		statusLinkNode.href = linkHref;
		statusLinkNode.title = linkTitle;
		statusLinkNode.style.display = 'inline';
	} else {
		statusLinkNode.style.display = 'none';
	}
	
	// Show everything
	try {
		// Firefox
		statusRow.style.display = 'table-row';
		statusSpacer.style.display = 'table-row';
	} catch (e) {
		// IE
		statusRow.style.display = 'block';
		statusSpacer.style.display = 'block';
	}
}

dh.formtable.showStatusMessage = function(controlId, message, hideClose) {
	dh.formtable.showStatus(controlId, message, hideClose ? null : "Close",
		hideClose ? null : dh.formtable.makeLinkClosure(function() {
			dh.formtable.hideStatus(controlId);
		}),
		hideClose ? null : "I've read this already, go away");
}

dh.formtable.onValueChanged = function(entryObject, postMethod, argName, value,
 									   pendingMessage, successMessage, fixedArgs, onUpdate) {
	var controlId = entryObject.elem.id;										
	if (!value || value.length == 0) {
		return;
	}
	var args
	if (fixedArgs != null)
		args = dojo.lang.shallowCopy(fixedArgs)
	else
		args = {}
	args[argName] = value;
	dh.formtable.showStatus(controlId, pendingMessage, null, null);
   	dh.server.doPOST(postMethod,
			     		args,
		  	    		function(type, data, http) {
		  	    			var oldValue = dh.formtable.undoValues[controlId];
		  	    	 		dh.formtable.showStatus(controlId, successMessage,
		  	    	 			oldValue ? "Undo" : null,
		  	    	 			oldValue ?
				  	    	 		dh.formtable.makeLinkClosure(function() {
				  	    	 			dh.formtable.undo(entryObject, oldValue, postMethod, argName, fixedArgs, onUpdate);
				  	    	 		}) : null,
			  	    	 		oldValue ? "Change back to the previous setting" : null);
		  	    	 		// this saves what we'll undo to after the NEXT change
							dh.formtable.undoValues[controlId] = value;
							if (onUpdate != null)
								onUpdate(value)
			  	    	},
			  	    	function(type, error, http) {
							dh.formtable.showStatusMessage(controlId, "Failed to save this setting.");
			  	    	});
}

dh.formtable.undo = function(entryObject, oldValue, postMethod, argName, fixedArgs, onUpdate) {
	var controlId = entryObject.elem.id;
	dh.formtable.showStatus(controlId, "Undoing...", null, null, null);
	var args
	if (fixedArgs != null)
		args = dojo.lang.shallowCopy(fixedArgs)
	else
		args = {}
	args[argName] = oldValue;
   	dh.server.doPOST(postMethod,
			     		args,
		  	    		function(type, data, http) {
		  	    			entryObject.setValue(oldValue, true); // true = doesn't emit the changed signal
		  	    	 		dh.formtable.showStatusMessage(controlId, "Undone!");
	 					  	// this saves what we'll undo to after the NEXT change
							dh.formtable.undoValues[controlId] = oldValue;
							if (onUpdate != null)
								onUpdate(oldValue)
			  	    	},
			  	    	function(type, error, http) {
							dh.formtable.showStatus(controlId, "Failed to undo!", null, null, null);
			  	    	});	
}
