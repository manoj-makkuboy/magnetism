﻿#include "hippo.as"

import flash.geom.Matrix;

var entireWidth:Number = 250;
var entireHeight:Number = 180;
var outerBorderWidth:Number = 1;
var paddingInsideOuterBorder:Number = 5;
var headshotSize:Number = 30;
var presenceIconSize:Number = 12;
var ribbonIconSize:Number = 16;

var rootMovie:MovieClip = createEmptyMovieClip("rootMovie", 0);
var currentView:MovieClip = null;
var currentSummary:Object = null;

var simpleGradientFill = function(mc:MovieClip, x:Number, y:Number, width:Number, height:Number, horizontal:Boolean, colors:Array, ratios:Array) {
	var matrix:Matrix = new Matrix();
	matrix.createGradientBox(width, height, horizontal ? 0 : Math.PI / 2);
	var alphas:Array = [];  // 0-100 alpha
	var autoRatios:Array = []; 	// from 0-0xFF, the point where each color starts fading out (I think)
	for (var i = 0; i < colors.length; ++i) {
		alphas.push(100);
		autoRatios.push(i * (0xFF / colors.length));
	}
	if (!ratios)
		ratios = autoRatios;
	mc.beginGradientFill("linear", colors, alphas, ratios, matrix);
	mc.moveTo(x, y);
	mc.lineTo(x + width, y);
	mc.lineTo(x + width, y + height);
	mc.lineTo(x, y + height);
	mc.lineTo(x, y);
	mc.endFill();
}

var createView = function(summary:Object) {
	var viewRoot:MovieClip = rootMovie.createEmptyMovieClip("viewRoot", rootMovie.getNextHighestDepth());

	// gray border
	viewRoot.beginFill(0x999999);
	viewRoot.moveTo(0, 0);
	viewRoot.lineTo(entireWidth, 0);
	viewRoot.lineTo(entireWidth, entireHeight);
	viewRoot.lineTo(0, entireHeight);
	viewRoot.lineTo(0, 0);
	viewRoot.endFill();
	
	// gradient background
	simpleGradientFill(viewRoot, outerBorderWidth, outerBorderWidth,
					   entireWidth - outerBorderWidth*2,
					   entireHeight - outerBorderWidth*2,
					   false, [ 0xefeeee, 0xffffff ], [0x0, 0xFF] );
	
	var topStuff:MovieClip = viewRoot.createEmptyMovieClip("topStuff", viewRoot.getNextHighestDepth());
	var ribbonBar:MovieClip = viewRoot.createEmptyMovieClip("ribbonBar", viewRoot.getNextHighestDepth());
	var stack:MovieClip = viewRoot.createEmptyMovieClip("stack", viewRoot.getNextHighestDepth());
	
	var leftSide = outerBorderWidth + paddingInsideOuterBorder;
	var topSide = outerBorderWidth + paddingInsideOuterBorder;
	
	topStuff._x = leftSide;
	topStuff._y = topSide;

	ribbonBar._x = leftSide;
	// ribbonBar y set later
	
	stack._x = leftSide;
	// stack y set later
	
	var photo:MovieClip = topStuff.createEmptyMovieClip("photo", topStuff.getNextHighestDepth());
	photo._x = 0;
	photo._y = 0;
	addImageToClip(viewRoot, photo, summary.photo, null);

	var presenceIcon:MovieClip = topStuff.createEmptyMovieClip("presenceIcon", topStuff.getNextHighestDepth());
	presenceIcon._x = photo._x + headshotSize + 5;
	presenceIcon._y = 0;
	addImageToClip(viewRoot, presenceIcon, summary.onlineIcon, null);
	
	var rightEdgeOfPresenceIcon = presenceIcon._x + presenceIconSize;
	
	var name:TextField = topStuff.createTextField("name", topStuff.getNextHighestDepth(),
											      rightEdgeOfPresenceIcon + 5, 0, 200, 14);
	name.autoSize = 'left';
	name.html = true;
	name.htmlText = "<b><a href='" + escapeXML(summary.homeUrl) + "'>" + escapeXML(summary.name) + "'s Mugshot</a></b>";
	formatText(name, 14, 0x000000);
	
	var homeLink:TextField = topStuff.createTextField("homeLink", topStuff.getNextHighestDepth(),
													  rightEdgeOfPresenceIcon + 5, name._y + 14 + 5, 200, 12);
	homeLink.autoSize = 'left';
	homeLink.html = true;
	homeLink.htmlText = "<u><a href='" + escapeXML(summary.homeUrl) + "'>Visit my Mugshot page</a></u>";
	formatText(homeLink, 12, 0x0033ff);
	
	var topStuffBottomSide = topSide + name._y + 14 + 5 + 12;
	
	ribbonBar._y = topStuffBottomSide + 5;
	stack._y = ribbonBar._y + ribbonIconSize + 5;
	
	var ribbon:MovieClip = ribbonBar.createEmptyMovieClip("ribbon", ribbonBar.getNextHighestDepth());
	
	var nextX:Number = 0;
	for (var i = 0; i < summary.accounts.length; ++i) {
		var account:Object = summary.accounts[i];
		var accountButton:MovieClip = ribbon.createEmptyMovieClip("account" + i, ribbon.getNextHighestDepth());
		accountButton._x = nextX;
		accountButton._y = 0;
		addImageToClip(viewRoot, accountButton, account.icon, null);
		nextX = nextX + ribbonIconSize + 2;
	}
	
	var blockHeight:Number = 33;
	var blockWidth:Number = entireWidth - outerBorderWidth*2 - paddingInsideOuterBorder*2;
	var nextY:Number = 0;
	for (var i = 0; i < summary.stack.length; ++i) {
		var block:Object = summary.stack[i];
		
		var blockClip:MovieClip = stack.createEmptyMovieClip("block" + i, stack.getNextHighestDepth());
		
		// setting background is how we set the movie clip size; just setting _width/_height doesn't seem to work
		simpleGradientFill(blockClip, 0, 0, blockWidth, blockHeight,
					   false, [ 0xededed, 0xf3f3f3 ], [0x0, 0xFF] );
				
		blockClip._x = 0;
		blockClip._y = nextY;
		nextY = nextY + blockHeight + 5;
		//blockClip._height = blockHeight;
		//blockClip._width = 220;

		var maxTextWidth:Number = blockWidth - 10;
		
		var heading:TextField = blockClip.createTextField("heading", blockClip.getNextHighestDepth(), 5, 0, maxTextWidth, 12);
		heading.autoSize = 'left';
		heading.html = true;
		heading.htmlText = "<b>" + escapeXML(block.heading) + "</b>";
		formatText(heading, 11, 0x000000);
		
		var timeAgoSpace = maxTextWidth - heading._width - 5;
		
		if (timeAgoSpace >= 0) {
			var timeAgo:TextField = blockClip.createTextField("timeAgo", blockClip.getNextHighestDepth(), heading._width + 5, 0,
															  timeAgoSpace, 12);
			timeAgo.autoSize = 'left';
			timeAgo.text = "(" + block.timeAgo + ")";
			formatText(timeAgo, 11, 0xaaaaaa);
			
			// hide it if it doesn't fit
			if (timeAgo._width > timeAgoSpace) {
				timeAgo._visible = false;
			}
		} else {
			// the heading may be too long so we need to truncate it
			heading.autoSize = 'none';
			heading._width = maxTextWidth;
			heading._height = 12;
		}
		
		var link:TextField = blockClip.createTextField("link", blockClip.getNextHighestDepth(), 5, 15, maxTextWidth, 17);
		//link.autoSize = 'left'; // we want it to be clipped
		link.html = true;
		link.htmlText = "<u><b><a href='" + escapeXML(block.link) + "'>" + escapeXML(block.linkText) + "</a></b></u>";
		formatText(link, 11, 0x0033ff);	
		
		trace("height of link is " + link._height + " and bottom of heading is " + (heading._y + heading._height))
	}
	
	return clip;
}

var setSummaryData = function(summary:Object) {
	if (genericEquals(currentSummary, summary))
		return;
	if (currentView) {
		currentView.removeMovieClip();
		currentView = null;
	}
	currentSummary = null;
	if (summary) {
		currentView = createView(summary);
		currentSummary = summary;
	}
}

var parseExternalAccounts = function(externalAccountsNode:XMLNode) {
	var accounts:Array = [];
	for (var i = 0; i < externalAccountsNode.childNodes.length; ++i) {
		var node:XMLNode = externalAccountsNode.childNodes[i];
		
		if (node.nodeName == "externalAccount") {
			var account:Object = {};
			account.link = makeAbsoluteUrl(node.attributes["link"]);
			account.tooltip = makeAbsoluteUrl(node.attributes["linkText"]);
			account.icon = makeAbsoluteUrl(node.attributes["icon"]);
			if (account.link && account.tooltip && account.icon) {
				accounts.push(account);
			} else {
				trace("account missing needed attrs");
			}
		} else {
			trace("ignoring unknown node " + node.nodeName);
		}
	}
	
	return accounts;
}

var parseBlocks = function(stackNode:XMLNode) {
	var blocks:Array = [];

	for (var i = 0; i < stackNode.childNodes.length; ++i) {
		var node:XMLNode = stackNode.childNodes[i];
		
		if (node.nodeName == "block") {
			var block:Object = {};
			block.timeAgo = node.attributes["timeAgo"];
			block.heading = node.attributes["heading"];
			block.link = makeAbsoluteUrl(node.attributes["link"]);
			block.linkText = node.attributes["linkText"];
			if (block.timeAgo && block.heading && block.link && block.linkText) {
				blocks.push(block);
			} else {
				trace("block missing needed attrs");
			}
		} else {
			trace("ignoring unknown node " + node.nodeName);
		}
	}
	
	return blocks;
}

var updateCount:Number = 0;

var updateSummaryData = function() {	
	updateCount = songUpdateCount + 1;
	
	if (updateCount > 1000) // if someone just leaves a browser open, stop eventually
		return;
	
	var meuXML:XML = new XML();
	meuXML.ignoreWhite = true;
	meuXML.onLoad = function(success:Boolean) {
		if (!success) {
			trace("xml load failure");
			return;
		}
		var root:XML = this.childNodes[0];
		
		if (root.nodeName != "rsp") {
			trace("root node is not rsp");
			return;
		}
		
		if (root.attributes["stat"] != "ok") {
			trace("request status: " + root.attributes["stat"]);
			return;
		}
		
		if (root.childNodes.length < 1) {
			trace("no child nodes of rsp");
			return;
		}

		var summary:Object = {};
		
		var summaryNode:XMLNode = root.childNodes[0];
		
		if (summaryNode.nodeName != "userSummary") {
			trace("wrong node name " + summaryNode.nodeName);
			return;
		}
		
		if (summaryNode.attributes["who"] != who) {
			trace("wrong user " + summaryNode.attributes["who"]);
			return;
		}
		
		summary.who = who;
		summary.photo = makeAbsoluteUrl(summaryNode.attributes["photo"] + "?size=" + headshotSize);
		summary.online = summaryNode.attributes["online"] == "true";
		summary.onlineIcon = makeAbsoluteUrl(summaryNode.attributes["onlineIcon"]);
		summary.name = summaryNode.attributes["name"];
		summary.homeUrl = makeAbsoluteUrl(summaryNode.attributes["homeUrl"]);	
		summary.accounts = [];
		summary.stack = [];
		
		for (var i = 0; i < summaryNode.childNodes.length; ++i) {
			var node:XMLNode = summaryNode.childNodes[i];
			if (node.nodeName == "accounts") {
				summary.accounts = parseExternalAccounts(node);
			} else if (node.nodeName == "stack") {
				summary.stack = parseBlocks(node);
			} else {
				trace("ignoring unknown node " + node.nodeName);
			}
		}
		
		trace("summary for " + who + " photo " + summary.photo + " " + summary.accounts.length + " accounts " + summary.stack.length + " blocks");
		
		setSummaryData(summary);
	};
	var reqUrl = makeAbsoluteUrl("/xml/usersummary?who=" + who + "&participantOnly=true&includeStack=true");
	meuXML.load(reqUrl);
};

updateSummaryData();
