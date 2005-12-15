dojo.provide("dh.sharegroup");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.html");
dojo.require("dh.share");
dojo.require("dh.server");
dojo.require("dh.util");

dh.sharegroup.inviteCountMessage = null;

dh.sharegroup.invitesRemaining = function() {
	var count = 0;
	for (var i = 0; i < dh.share.selectedRecipients.length; ++i) {
		var r = dh.share.selectedRecipients[i];
		if (r.isPerson() && !r.hasAccount) {
			count = count + 1;
		}
	}
	return dhShareGroupInvitationCount - count;
}

dh.sharegroup.updateInvitations = function() {

	var remaining = dh.sharegroup.invitesRemaining();
	var message;
	if (remaining > 1)
		message = "You can invite " + remaining + " more people via email";
	else if (remaining == 1)
		message = "You can invite one more person via email";
	else
		message = "No more email invitations! You can only share this group with existing DumbHippo users";

	dojo.dom.textContent(dh.sharegroup.inviteCountMessage, message)

	if (dhShareGroupIsForum)
		dh.sharegroup.inviteCountMessage.style.display = 'none';
	else
		dh.sharegroup.inviteCountMessage.style.display = 'inline';
}

dh.sharegroup.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	var descriptionHtml = dh.util.getTextFromRichText(dh.share.descriptionRichText);
	
	var commaRecipients = dh.util.join(dh.share.selectedRecipients, ",", "id");
	
	var secret = false;
	
	dojo.debug("groupId = " + dhShareGroupId);
	dojo.debug("desc = " + descriptionHtml);
	dojo.debug("rcpts = " + commaRecipients);
	
	// double-check that we're logged in
	dh.server.doPOST("sharegroup",
						{ 
							"groupId" : dhShareGroupId,
						  	"description" : descriptionHtml,
						  	"recipients" : commaRecipients
						},
						function(type, data, http) {
							dojo.debug("sharegroup got back data " + dhAllPropsAsString(data));
							
							if (window.opener) {
								// reload the viewgroup page so it shows we've invited people
								window.opener.location.reload();
							}
							
							dh.util.goToNextPage("viewgroup?groupId=" + dhShareGroupId, 
							                     "The group has been shared!");
						},
						function(type, error, http) {
							dojo.debug("sharegroup got back error " + dhAllPropsAsString(error));
						});
}

dh.sharegroup.loadContacts = function() {
	if (dh.share.haveLoadedContacts)
		return;
	
	dh.server.getXmlGET("addablecontacts",
			{ 
				"groupId" : dhShareGroupId
			},
			function(type, data, http) {
				dojo.debug("got back contacts " + data);
				dojo.debug("text is : " + http.responseText);
							
				dh.share.mergeObjectsDocument(data);
				
				dh.share.haveLoadedContacts = true;
			},
			function(type, error, http) {
				dojo.debug("getting contacts, got back error " + dhAllPropsAsString(error));
				alert("could not get your contacts: " + error);
				
				// note that we don't cache an empty result set, we will retry instead...
			});
}

dh.sharegroup.init = function() {
	dojo.debug("dh.sharegroup.init");

	dh.sharegroup.inviteCountMessage = document.getElementById('dhInvitationsRemainingMessage');
			
	// most of the dojo is set up now, so show the widgets
	dh.util.showId("dhShareForm");
	
	dh.share.init();

	dh.sharegroup.updateInvitations();
	
	dh.share.recipientsChangedCallback = function() {
		dh.sharegroup.updateInvitations();
	}

	dh.share.canAddRecipientCallback = function(recipient) {
		if (recipient.hasAccount)
			return true;
		else {
			if (dh.sharegroup.invitesRemaining() > 0)
				return true;
			else {
				dh.util.flash(dh.sharegroup.inviteCountMessage);	
			}
		}
	}
	
	// set default focus
	dh.share.recipientComboBox.focus();
	
	// load up your contacts
	dh.sharegroup.loadContacts();
}

dhShareGroupInit = dh.sharegroup.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareGroupInit");
