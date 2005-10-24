dojo.provide("dh.sharelink");

dojo.require("dojo.event.*");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.RichText");
dojo.require("dojo.widget.html.Button");
dojo.require("dojo.widget.HtmlComboBox");
dojo.require("dojo.widget.HtmlInlineEditBox");
dojo.require("dh.server");
dojo.require("dh.login");
dojo.require("dh.util");
dojo.require("dh.model");

// hash of all persons/groups we have ever loaded up, keyed by guid
dh.sharelink.allKnownIds = {};
// currently selected recipients, may be group or person objects
dh.sharelink.selectedRecipients = [];

dh.sharelink.urlToShareEditBox = null;
dh.sharelink.recipientComboBox = null;
dh.sharelink.descriptionRichText = null;
dh.sharelink.createGroupPopup = null;
dh.sharelink.createGroupNameEntry = null;

dh.sharelink.findGuid = function(set, id) {
	// set can be an array or a hash
	for (var prop in set) {
		if (dojo.lang.has(set[prop], "id")) {
			if (id == set[prop]["id"]) {
				return prop;
			}
		}
	}
	return null;
}

dh.sharelink.findIdNode = function(id) {
	var list = document.getElementById("dhRecipientList");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
			continue;
		var childId = child.getAttribute("dhId");
		if (childId && id == childId) {
			return child;
		}
	}
	return null;
}

dh.sharelink.forEachPossibleGroupMember = function(func) {
	var list = document.getElementById("dhRecipientList");
	for (var i = 0; i < list.childNodes.length; ++i) {
		var child = list.childNodes.item(i);
		if (child.nodeType != dojo.dom.ELEMENT_NODE)
			continue;

		func(child);
	}
	return null;
}

dh.sharelink.highlightPossibleGroup = function() {
	dh.sharelink.forEachPossibleGroupMember(function(node) {
		dojo.html.addClass(node, "dhCouldBeInGroup");
	});
}

dh.sharelink.unhighlightPossibleGroup = function() {
	// keep highlighted if the popup is showing
	var popup = dh.sharelink.createGroupPopup;
	if (dh.util.isShowing(popup))
		return;
	
	dh.sharelink.forEachPossibleGroupMember(function(node) {
		dojo.html.removeClass(node, "dhCouldBeInGroup");
	});
}

dh.sharelink.toggleCreateGroup = function() {
	dh.sharelink.highlightPossibleGroup();
	var popup = dh.sharelink.createGroupPopup;
	dh.util.toggleShowing(popup);
	
	if (!dh.util.isShowing(popup))
		dh.sharelink.unhighlightPossibleGroup();
}

dh.sharelink.doCreateGroup = function() {
	var name = dh.sharelink.createGroupNameEntry.value;
	alert("name = " + name);
	dh.sharelink.createGroupNameEntry.value = "";
}

dhDoCreateGroupKeyUp = function(event) {
	if (event.keyCode == 13) {
		dh.sharelink.doCreateGroup();
	}
}

dhRemoveRecipientClicked = function(event) {
	dojo.debug("remove recipient");
	
	// scan up for the dhId node which is the outermost
	// node of the html representing this person/group, and also 
	// has the person/group ID in question
	
	var idToRemove = null;
	var node = event.target;
	while (node != null) {
		idToRemove = node.getAttribute("dhId");
		if (idToRemove)
			break;
		node = node.parentNode;
	}
	
	var objIndex = dh.sharelink.findGuid(dh.sharelink.selectedRecipients, idToRemove);
	dh.sharelink.selectedRecipients.splice(objIndex, 1);
	
	// remove the HTML representing this recipient
	var anim = dojo.fx.html.fadeOut(node, 800, function(node, anim) {
		node.parentNode.removeChild(node);
	});
}

dh.sharelink.doAddRecipientFromCombo = function(fromKeyPress) {
	var cb = dh.sharelink.recipientComboBox;
	var previousValue = cb.textInputNode.value;
			
	cb.dataProvider.getResults(previousValue,	
							function(completions) {
								if (previousValue != cb.textInputNode.value) {
									// the user typed or deleted or something since we 
									// sent the request; so abort and do nothing
									return;
								}
							
								if (completions.length == 0) {
									alert("Don't know who " + previousValue + " is...");
									return;
								} 
								
								var filtered = dh.sharelink.copyCompletions(completions, true);
								
								if (filtered.length > 1) {
									// ambiguous, pop up combo completer ... better thing to do?
									if (!fromKeyPress) {
										cb.dataProvider.emitProvideSearchResults(filtered, previousValue);
									} else {
										// Dojo calls hideResultList() in its key handler, we need 
										// a better plan here...
										var all = "";
										for (var i = 0; i < filtered.length; ++i) {
											if (i != 0) {
												all = all + ", ";
											}
											all = all + filtered[i][0];
										}
										alert("Not sure which of these you mean: " + all);
									}
								} else if (filtered.length == 1) {
									var recipientId = filtered[0][1];
									dojo.debug("got single completion back, " + recipientId);
									dh.sharelink.doAddRecipient(recipientId);
								} else {
									// filtered.length == 0 and completions.length > 0
									//alert("already added them");
									var node = dh.sharelink.findIdNode(completions[0][1]);
									dh.util.flash(node);
								}
							});
}

dh.sharelink.doAddRecipient = function(selectedId) {	
	
	dojo.debug("adding " + selectedId + " as recipient if they aren't already");
	
	var objKey = dh.sharelink.findGuid(dh.sharelink.allKnownIds, selectedId);
	if (!objKey) {
		// FIXME display something, this is the validation step
		alert("dunno who that is...");
		return;
	}
	
	var obj = dh.sharelink.allKnownIds[objKey];
	
	if (!dh.sharelink.findGuid(dh.sharelink.selectedRecipients, obj.id)) {
		
		dh.sharelink.selectedRecipients.push(obj);
		
		var idNode = document.createElement("table");
		idNode.setAttribute("dhId", obj.id);
		dojo.html.addClass(idNode, "dhRecipient");
		dojo.html.addClass(idNode, "dhItemBox");
		if (dh.util.isShowing(dh.sharelink.createGroupPopup))
			dojo.html.addClass(idNode, "dhCouldBeInGroup");
		
		// don't think tbody is used anymore?
		//var tbody = document.createElement("tbody");
		//idNode.appendChild(tbody);
		var tbody = idNode;
		var tr1 = document.createElement("tr");
		tbody.appendChild(tr1);
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhHeadShot");
		tr1.appendChild(td);
		var img = document.createElement("img");
		img.setAttribute("src", "http://planet.gnome.org/heads/nobody.png");
		td.appendChild(img);
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhRemoveRecipient");
		tr1.appendChild(td);
		var removeLink = document.createElement("a");
		removeLink.appendChild(document.createTextNode("[X]"));
		removeLink.setAttribute("href", "javascript:void(0);");
		dojo.html.addClass(removeLink, "dhRemoveRecipient");
		removeLink.setAttribute("rowSpan", "2");
		dojo.event.connect(removeLink, "onclick", dj_global, "dhRemoveRecipientClicked");
		td.appendChild(removeLink);
		var tr2  = document.createElement("tr");
		tbody.appendChild(tr2);
		var td = document.createElement("td");
		dojo.html.addClass(td, "dhRecipientName");
		td.setAttribute("colSpan","2");
		tr2.appendChild(td);
		td.appendChild(document.createTextNode(obj.displayName));

		dojo.html.setOpacity(idNode, 0);
		
		var recipientsListNode = document.getElementById("dhRecipientList");
		recipientsListNode.appendChild(idNode);
		
		var anim = dojo.fx.html.fadeIn(idNode, 800);
	} else {
		dh.util.flash();
	}
	
	// clear the combo again
	dh.sharelink.recipientComboBox.textInputNode.value = "";
	dh.sharelink.recipientComboBox.dataProvider.lastSearchProvided = null;
}

dhDoAddRecipientKeyUp = function(event) {
	if (event.keyCode == 13) {
		dh.sharelink.doAddRecipientFromCombo(true);
	}
}

dh.sharelink.submitButtonClicked = function() {
	dojo.debug("clicked share link button");
	
	var urlHtml = dh.sharelink.urlToShareEditBox.textValue;
	var descriptionHtml = dh.sharelink.descriptionRichText.getEditorContent();
	
	var commaRecipients = "";
	for (var i = 0; i < dh.sharelink.selectedRecipients.length; ++i) {
		if (i != 0) {
			commaRecipients = commaRecipients + ",";
		}
		commaRecipients = commaRecipients + dh.sharelink.selectedRecipients[i].id;
	}
	
	dojo.debug("url = " + urlHtml);
	dojo.debug("desc = " + descriptionHtml);
	dojo.debug("rcpts = " + commaRecipients);
	
	// FIXME we don't really want to send HTML to the server... at least not 
	// without "simplification" to tags we understand which would be easy on 
	// client side...
	
	// double-check that we're logged in
	dh.login.requireLogin(function() {					
		dh.server.doPOST("sharelink",
						{ 
							"url" : urlHtml, 
						  	"description" : descriptionHtml,
						  	"recipients" : commaRecipients
						},
						function(type, data, http) {
							dojo.debug("sharelink got back data " + dhAllPropsAsString(data));
							dh.util.closeWindow();
						},
						function(type, error, http) {
							dojo.debug("sharelink got back error " + dhAllPropsAsString(error));

						});
	});
}

dh.sharelink.copyCompletions = function(completions, filterSelected) {
	// second arg is optional
	if (arguments.length < 2) {
		arguments.push(false);
	}

	var copy = [];
	for (var i = 0; i < completions.length; ++i) {
		if (filterSelected && dh.sharelink.findGuid(dh.sharelink.selectedRecipients,
							                         completions[i][1])) {
			continue;
		}
		
		copy.push([ completions[i][0], completions[i][1] ]);
	}
	return copy;
}

dh.sharelink.FriendListProvider = function() {

	// completions we are working on, hash from search string to array of resultsFunc
	this.pendingCompletions = {};

	// all completions we have done, hash from the search string to the provideSearchResults arg
	this.allKnownCompletions = {};

	this.lastSearchProvided = null;

	this.notifyPending = function(searchStr, completions) {
		dojo.debug("notifying of completion to " + searchStr);
		var pending = this.pendingCompletions[searchStr];
		delete this.pendingCompletions[searchStr];
		for (var i = 0; i < pending.length; ++i) {
			pending[i](completions);
		}
	}

	// resultsFunc should NOT mutate the returned results, unlike the Dojo signal 
	// handler on provideSearchResults()
	this.getResults = function(searchStr, resultsFunc) {

		if (dojo.lang.has(this.allKnownCompletions, searchStr)) {
			dojo.debug("using cached result for " + searchStr);
			resultsFunc(this.allKnownCompletions[searchStr]);
			return;
		}

		if (dojo.lang.has(this.pendingCompletions, searchStr)) {
			dojo.debug("adding another resultsFunc for " + searchStr);
			this.pendingCompletions[searchStr].push(resultsFunc);
			return;
		}

		dojo.debug("creating first results query for " + searchStr);
		this.pendingCompletions[searchStr] = [resultsFunc];
		
		var _this = this;
				
		dh.server.getXmlGET("friendcompletions",
							{ "entryContents" : searchStr },
							function(type, data, http) {
								dojo.debug("friendcompletions got back data " + data);
								dojo.debug("text is : " + http.responseText);
								//dojo.debug(data.doctype);
								
								var completions = [];
								
								var objectsElement = data.getElementsByTagName("objects").item(0);
								var nodeList = objectsElement.childNodes;
								for (var i = 0; i < nodeList.length; ++i) {
									var element = nodeList.item(i);
									if (element.nodeType != dojo.dom.ELEMENT_NODE) {
										continue;
									} else if (element.nodeName == "completion") {
										completions.push([element.getAttribute("text"), element.getAttribute("id")]);
									} else {
										var obj = dh.model.objectFromXmlNode(element);
									    // merge in a new person/group we know about, overwriting any older data
									    dh.sharelink.allKnownIds[obj.id] = obj;
									}
								}
								
								// save cached completions
								_this.allKnownCompletions[searchStr] = completions;

								_this.notifyPending(searchStr, completions);
							},
							function(type, error, http) {
								dojo.debug("friendcompletions got back error " + dhAllPropsAsString(error));
								_this.notifyPending(searchStr, []);
								
								// note that we don't cache an empty result set, we will retry instead...
							});
	}

	// type is a string "STARTSTRING", "SUBSTRING", "STARTWORD"
	this.startSearch = function(searchStr, type, ignoreLimit) {
		//dojo.debug("friend startSearch");
		
		var _this = this;

		this.getResults(searchStr, function(completions) {
			// dojo "eats" the completions so we have to copy them (we need to filter anyhow)
			dh.sharelink.lastSearchProvidedToComboBox = searchStr;
			_this.emitProvideSearchResults(dh.sharelink.copyCompletions(completions, true), searchStr);
		});
	}

	// a "signal", pass it an array of 2-item arrays, where the pairs
	// are usercompletion+ourselectionid ; BEWARE dojo destroys this array so pass it a copy
	// if you are also keeping a reference
	this.provideSearchResults = function(resultsDataPairs) {
		dojo.debug("friend provideSearchResults results = " + resultsDataPairs);
	}
	
	this.emitProvideSearchResults = function(resultsDataPairs, forSearchStr) {
	
		// HtmlComboBox should probably do this itself... working around it
	
		dojo.debug("lastSearchProvided -" + this.lastSearchProvided + "- forSearchStr -" + forSearchStr + "-");
	
		if (this.lastSearchProvided == forSearchStr) {
			// just show the list.
			dh.sharelink.recipientComboBox.showResultList(); // is a no-op if already showing
		} else {
			dojo.debug("providing search results to the combo for '" + forSearchStr + "' results are " + resultsDataPairs);
			this.lastSearchProvided = forSearchStr;
			this.provideSearchResults(resultsDataPairs);
		}
	}
}
dojo.inherits(dh.sharelink.FriendListProvider, Object);

dh.sharelink.HtmlFriendComboBox = function(){
	// dojo.debug("creating HtmlFriendComboBox");
	dojo.widget.HtmlComboBox.call(this);
	
	this.widgetType = "FriendComboBox";
	
	this.fillInTemplate = function(args, frag){
		// override the default provider
		this.dataProvider = new dh.sharelink.FriendListProvider();
		// DEBUG - put data in the default provider
		//this.dataProvider = new dojo.widget.ComboBoxDataProvider();
		//this.dataProvider.setData(dh.sharelink.stateNames);
    }
}

dojo.inherits(dh.sharelink.HtmlFriendComboBox, dojo.widget.HtmlComboBox);

dojo.widget.manager.registerWidgetPackage("dh.sharelink");
dojo.widget.tags.addParseTreeHandler("dojo:friendcombobox");

dh.sharelink.init = function() {
	dojo.debug("dh.sharelink.init");
		
	dh.login.requireLogin(function() {
		dojo.debug("dh.sharelink logged in!");
		var params = dh.util.getParamsFromLocation();
		dh.sharelink.urlToShareEditBox = dojo.widget.manager.getWidgetById("dhUrlToShare");
		if (dojo.lang.has(params, "url")) {
			// FIXME InlineEditBox takes HTML, even though it's called setText, need to escape
			dh.sharelink.urlToShareEditBox.setText(params["url"]);
		}
	
		dh.sharelink.recipientComboBox = dojo.widget.manager.getWidgetById("dhRecipientComboBox");
		dojo.event.connect(dh.sharelink.recipientComboBox.textInputNode, "onkeyup", dj_global, "dhDoAddRecipientKeyUp");
		
		// most of the dojo is set up now, so show the widgets
		dh.util.showId("dhShareLinkForm");
		
		// rich text areas can't exist when display:none, so we have to create it after showing
		dh.sharelink.descriptionRichText = dojo.widget.fromScript("richtext", 
																 {}, // props,
																 document.getElementById("dhShareLinkDescription"));
																 
		dh.sharelink.createGroupPopup = document.getElementById("dhCreateGroupPopup");					 
		dh.sharelink.createGroupNameEntry = document.getElementById("dhCreateGroupName");
		dojo.event.connect(dh.sharelink.createGroupNameEntry, "onkeyup",
							dj_global, "dhDoCreateGroupKeyUp");
	});
}

dhShareLinkInit = dh.sharelink.init; // connect doesn't like namespaced things
dojo.event.connect(dojo, "loaded", dj_global, "dhShareLinkInit");
