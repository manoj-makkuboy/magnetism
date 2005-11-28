dojo.provide("dh.sharelink");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dojo.widget.HtmlInlineEditBox");
dojo.require("dh.share");
dojo.require("dh.server");
dojo.require("dh.util");

dh.sharelink.urlToShareEditBox = null;
dh.sharelink.urlTitleToShareEditBox = null;
dh.sharelink.secretCheckbox = null;
dh.sharelink.createGroupPopup = null;
dh.sharelink.createGroupNameEntry = null;
dh.sharelink.createGroupLink = null;
dh.sharelink.createGroupPrivateRadio = null;
dh.sharelink.createGroupPublicRadio = null;
dh.sharelink.addMemberLink = null;
dh.sharelink.addMemberDescription = null;
dh.sharelink.addMemberGroup = null;
dh.sharelink.highlightingGroup = false;

dh.sharelink.updateAccessTip = function() {
	if (dh.sharelink.createGroupPrivateRadio.checked) {
		dh.util.showId('dhPrivateGroupAccessTip');
		dh.util.hideId('dhPublicGroupAccessTip');
	} else {
		dh.util.hideId('dhPrivateGroupAccessTip');
		dh.util.showId('dhPublicGroupAccessTip');
	}
}

dh.sharelink.highlightPossibleGroup = function() {
	dh.share.forEachPossibleGroupMember(function(node) {
		dojo.html.addClass(node, "dhCouldBeInGroup");
	});
}

dh.sharelink.unhighlightPossibleGroup = function() {
	// keep highlighted if the popup is showing
	var popup = dh.sharelink.createGroupPopup;
	if (dh.util.isShowing(popup))
		return;
	
	dh.share.forEachPossibleGroupMember(function(node) {
		dojo.html.removeClass(node, "dhCouldBeInGroup");
	});
}

dh.sharelink.toggleCreateGroup = function() {
	dh.sharelink.highlightPossibleGroup();
	var popup = dh.sharelink.createGroupPopup;
	dh.util.toggleShowing(popup);
	
	if (dh.util.isShowing(popup)) {
		dojo.dom.textContent(dh.sharelink.createGroupLink, "Cancel");
	} else {
		dojo.dom.textContent(dh.sharelink.createGroupLink, "Create Group from These");
		dh.sharelink.unhighlightPossibleGroup();
	}
}

dh.sharelink.inAction = false;

dh.sharelink.doCreateGroup = function() {
	var name = dh.sharelink.createGroupNameEntry.value;

	// don't get two actions in flight at once
	if (dh.sharelink.inAction)
		return;
	dh.sharelink.inAction = true;
		
	var statusNode = document.getElementById("dhCreateGroupStatus");
	// this makes things jump around and look ugly, disable for now
	//dojo.dom.textContent(statusNode, "Please wait...");
	//dh.util.show(statusNode);
	
	var groupMembers = [];
	for (var i = 0; i < dh.share.selectedRecipients.length; ++i) {
		var r = dh.share.selectedRecipients[i];
		if (r.isPerson()) {
			groupMembers.push(r);
		}
	}
	var commaMembers = dh.util.join(groupMembers, ",", "id");
		
	var secret = dh.sharelink.createGroupPrivateRadio.checked;
		
	dh.server.getXmlPOST("creategroup",
					{ 
						"name" : name, 
						"members" : commaMembers,
						"secret" : secret
					},
					function(type, data, http) {
						dojo.debug("got back a new group " + data);
						dojo.debug("text is : " + http.responseText);
					
						var newGroups = dh.share.mergeObjectsDocument(data);

						for (var i = 0; i < newGroups.length; ++i) {							    	
							// add the group as a recipient
							dojo.debug("adding newly-created group as recipient");
					    	dh.share.doAddRecipient(newGroups[i].id);
						}
						
						// remove the individual members as recipients
						for (var i = 0; i < groupMembers.length; ++i) {
							dh.share.removeRecipient(groupMembers[i].id);
						}
						
						dh.util.hide(dh.sharelink.createGroupPopup);
						dh.sharelink.createGroupNameEntry.value = "";
						dh.util.hide(statusNode);
						dh.sharelink.unhighlightPossibleGroup();
						dh.sharelink.inAction = false;
					},
					function(type, error, http) {
						//dojo.debug("creategroup got back error " + dhAllPropsAsString(error));
						// FIXME display the error, don't hide status
						dh.util.hide(statusNode);
						dh.sharelink.inAction = false;		
					});
}

dhDoCreateGroupKeyUp = function(event) {
	if (event.keyCode == 13) {
		dh.sharelink.doCreateGroup();
	}
}

dhCreateGroupAccessChanged = function() {
	dh.sharelink.updateAccessTip();
}

dh.sharelink.doAddMembers = function() {
	// don't get two actions in flight at once
	if (dh.sharelink.inAction)
		return;
	dh.sharelink.inAction = true;
		
	var statusNode = document.getElementById("dhCreateGroupStatus");
	dojo.dom.textContent(statusNode, "Please wait...");
	dh.util.show(statusNode);
	
	var groupId;
	
	var groupMembers = [];
	for (var i = 0; i < dh.share.selectedRecipients.length; ++i) {
		var r = dh.share.selectedRecipients[i];
		if (r.isPerson()) {
			groupMembers.push(r);
		} else {
			groupId = r.id;
		}
	}
	if (!groupId) // No group???
		return;
	
	var commaMembers = dh.util.join(groupMembers, ",", "id");
		
	dh.server.getXmlPOST("addmembers",
					{ 
						"groupId" : groupId, 
						"members" : commaMembers
					},
					function(type, data, http) {
						dojo.debug("got back a replacement group " + data);
						dojo.debug("text is : " + http.responseText);
					
						// remove the group and individual members as recipients
						dh.share.removeRecipient(groupId);

						for (var i = 0; i < groupMembers.length; ++i) {
							dh.share.removeRecipient(groupMembers[i].id);
						}
						
						var newGroups = dh.share.mergeObjectsDocument(data);

						for (var i = 0; i < newGroups.length; ++i) {							    	
							// add the group as a recipient
							dojo.debug("adding newly-created group as recipient");
					    	dh.share.doAddRecipient(newGroups[i].id);
						}
						
						dh.util.hide(statusNode);
						dh.sharelink.unhighlightPossibleGroup();
						dh.sharelink.inAction = false;
					},
					function(type, error, http) {
						//dojo.debug("addmembers got back error " + dhAllPropsAsString(error));
						// FIXME display the error, don't hide status
						dh.util.hide(statusNode);
						dh.sharelink.inAction = false;		
					});
}

dh.sharelink.initRecipient = function(recipient, newNode) {
	if (dh.util.isShowing(dh.sharelink.createGroupPopup) && recipient.isPerson())
		dojo.html.addClass(newNode, "dhCouldBeInGroup");
}

dh.sharelink.updateActionLinks = function() {
	var personCount = 0;
	var groupCount = 0;
	var group;
	var person;

	for (var i = 0; i < dh.share.selectedRecipients.length; ++i) {
		if (dh.share.selectedRecipients[i].isPerson()) {
			personCount += 1;
			person = dh.share.selectedRecipients[i];
		} else {
			groupCount += 1;
			group = dh.share.selectedRecipients[i];
		}
	}
	
	// we could also remove the create group dialog if it's up, but 
	// not clear it's right anyway (if you start editing recipients with 
	// that up, maybe you are going to put some group members back in 
	// the list in a minute. If you try to add a group to a group,
	// you should have the ability to remove it again without losing
	// your group name.)
	
	if (personCount > 1 && groupCount == 0) {
		dh.util.show(dh.sharelink.createGroupLink);	
	} else {
		dh.util.hide(dh.sharelink.createGroupLink);
	}
	
	if (personCount > 0 && groupCount == 1) {
		var descriptionText;
		if (personCount == 1)
			descriptionText = person.displayName;
		else 
			descriptionText = "all";
			
		dojo.dom.textContent(dh.sharelink.addMemberDescription, descriptionText);
		dojo.dom.textContent(dh.sharelink.addMemberGroup, group.displayName);
		dh.util.show(dh.sharelink.addMemberLink);
	} else {
		dh.util.hide(dh.sharelink.addMemberLink);
	}
}

dh.sharelink.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	var title = dh.sharelink.urlTitleToShareEditBox.textValue;
	
	var url = dh.sharelink.urlToShareEditBox.value;
	
	var descriptionHtml = dh.util.getTextFromRichText(dh.share.descriptionRichText);
	
	var commaRecipients = dh.util.join(dh.share.selectedRecipients, ",", "id");
	
	var secret = dh.sharelink.secretCheckbox.checked ? "true" : "false";
	
	dojo.debug("url = " + url);
	dojo.debug("title = " + title);
	dojo.debug("desc = " + descriptionHtml);
	dojo.debug("rcpts = " + commaRecipients);
	dojo.debug("secret = " + secret);
	
	// double-check that we're logged in
	dh.server.doPOST("sharelink",
						{ 
							"url" : url,
							"title" : title, 
						  	"description" : descriptionHtml,
						  	"recipients" : commaRecipients,
						  	"secret" : secret
						},
						function(type, data, http) {
							dojo.debug("sharelink got back data " + dhAllPropsAsString(data));
							dh.util.goToNextPage("home", "You've been shared!");
						},
						function(type, error, http) {
							dojo.debug("sharelink got back error " + dhAllPropsAsString(error));

						});
}

dh.sharelink.init = function() {
	dojo.debug("dh.sharelink.init");
			
	var params = dh.util.getParamsFromLocation();
	
	dh.sharelink.urlToShareEditBox = document.getElementById("dhUrlToShare");
	
	var urlParam = params["url"]
	if (urlParam) {
		dh.sharelink.urlToShareEditBox.value = urlParam;
	} else {
		dh.sharelink.urlToShareEditBox.value = "(enter link to share)";
		var urlDiv = document.getElementById("dhUrlToShareDiv");
		dh.util.show(urlDiv);
	}
	
	dh.sharelink.urlTitleToShareEditBox = dojo.widget.manager.getWidgetById("dhUrlTitleToShare");
	var params = dh.util.getParamsFromLocation();
	if (dojo.lang.has(params, "title")) {
		dh.sharelink.urlTitleToShareEditBox.setText(params["title"]);
	}

	dh.sharelink.secretCheckbox = document.getElementById("dhSecretCheckbox");

	// most of the dojo is set up now, so show the widgets
	dh.util.showId("dhShareLinkForm");
	
	dh.share.recipientCreatedCallback = dh.sharelink.initRecipient;
	dh.share.recipientsChangedCallback = dh.sharelink.updateActionLinks;
	
	dh.share.init();
	
	dh.sharelink.createGroupPopup = document.getElementById("dhCreateGroupPopup");					 
	dh.sharelink.createGroupNameEntry = document.getElementById("dhCreateGroupName");
	dojo.event.connect(dh.sharelink.createGroupNameEntry, "onkeyup",
						dj_global, "dhDoCreateGroupKeyUp");
	dh.sharelink.createGroupPrivateRadio = document.getElementById("dhCreateGroupPrivateRadio");
	dh.sharelink.createGroupPublicRadio = document.getElementById("dhCreateGroupPublicRadio");
	// HTML is stupid; you only get the signal on the one that changed, not the whole group
	dojo.event.connect(dh.sharelink.createGroupPrivateRadio, "onchange",
						dj_global, "dhCreateGroupAccessChanged");
	dojo.event.connect(dh.sharelink.createGroupPublicRadio, "onchange",
						dj_global, "dhCreateGroupAccessChanged");
	dh.sharelink.updateAccessTip();
	dh.sharelink.createGroupLink = document.getElementById("dhCreateGroupLink");
	dh.sharelink.addMemberLink = document.getElementById("dhAddMemberLink");
	dh.sharelink.addMemberDescription = document.getElementById("dhAddMemberDescription");
	dh.sharelink.addMemberGroup = document.getElementById("dhAddMemberGroup");
				
	// set default focus
	dh.share.recipientComboBox.focus();
	
	// load up your contacts
	dh.share.loadContacts();
}

dhShareLinkInit = dh.sharelink.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareLinkInit");
