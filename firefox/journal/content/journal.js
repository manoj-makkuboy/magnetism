/* ***** BEGIN LICENSE BLOCK ****
 *   Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Firefox Journal.
 *
 * The Initial Developer of the Original Code is
 * Colin Walters.
 * Portions created by the Initial Developer are Copyright (C) 2007
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 * 
 * ***** END LICENSE BLOCK ***** */
 
const JOURNAL_CHROME = "chrome://firefoxjournal/content/journal.html"; 
 
const RDF = Components.classes["@mozilla.org/rdf/rdf-service;1"].getService(Components.interfaces.nsIRDFService); 
const BOOKMARK_NAME = RDF.GetResource("http://home.netscape.com/NC-rdf#Name");
const BOOKMARK_DATE = RDF.GetResource("http://home.netscape.com/NC-rdf#Date");
const BOOKMARK_VISITCOUNT = RDF.GetResource("http://home.netscape.com/NC-rdf#VisitCount");

const DAY_MS = 24 * 60 * 60 * 1000;

function LOG(msg) {
  var dl = document.getElementById("debuglog");
  dl.appendChild(document.createTextNode(msg));
  dl.appendChild(document.createElement("br"));
}

function readRDFThingy(ds,res,prop,qi,def) {
  var val = ds.GetTarget(res, prop, true);
  if (val)
    return val.QueryInterface(qi).Value;
  else
    return def;
}

function readRDFString(ds,res,prop) {
  return readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFLiteral,"")
}

function readRDFDate(ds,res,prop) {
  return new Date(readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFDate,null)/1000);
}

function readRDFInt(ds,res,prop) {
  return readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFInt,-1);
}

var getHistory = function() {
    var gh = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIRDFDataSource);
    var iter = gh.GetAllResources();
    var result = [];
    while (iter.hasMoreElements()) {
      var item = iter.getNext();
      var resource = item.QueryInterface(Components.interfaces.nsIRDFResource);
      var itemname = readRDFString(gh, resource, BOOKMARK_NAME);
      var itemdate = readRDFDate(gh, resource, BOOKMARK_DATE);
      var itemcount = readRDFInt(gh, resource, BOOKMARK_VISITCOUNT);
      var displayUrl = resource.Value;
      var action = "visited";

      if (resource.Value.startsWith("http://www.google.com/") && resource.Value.toQueryParams()["q"] ) { 
        /* detect google web searches */
        itemname = decodeURIComponent(resource.Value.toQueryParams()["q"].replace(/\+/g," "));
        action = "searched for";
      }

      if (resource.Value.startsWith("http://www.amazon.com/") && resource.Value.toQueryParams()["field-keywords"] ) { 
        /* detect amazon product searches */
        itemname = decodeURIComponent(resource.Value.toQueryParams()["field-keywords"].replace(/\+/g," "));
        action = "searched for";
        displayURl = "http://www.amazon.com/";
      }

      result.push({'name': itemname, 'date': itemdate, 'url': resource.Value, 'displayurl' : displayUrl, 'visitcount': itemcount, 'action' : action})
    }
    return result
}

var getLocalDayOffset = function(date, tzoffset) {
  var tzoff = tzoffset || (new Date().getTimezoneOffset() * 60 * 1000);
  return Math.floor((date.getTime() - tzoff) / DAY_MS)  
}

var sliceByDay = function(histitems) {
  var tzoffset = (new Date().getTimezoneOffset() * 60 * 1000);
  var days = {}
  for (var i = 0; i < histitems.length; i++) {
    var hi = histitems[i]
    var timeday = getLocalDayOffset(hi.date, tzoffset)
    if (!days[timeday])
      days[timeday] = []
     
    days[timeday].push(hi)
  }
  var sorted_days = []
  for (timeday in days) {
    var dayset = days[timeday]
    sorted_days.push([timeday, dayset])
  }
  sorted_days.sort(function (a,b) { if (a[0] == b[0]) { return 0; } else if (a[0] > b[0]) { return 1; } else { return -1; } })
  days = []
  for (var i = 0; i < sorted_days.length; i++) {
    var dayset = sorted_days[i][1]    
    days.unshift(dayset)
  }
  return days;
}

var limitSliceCount = function(slices, limit) {
  var count = 0;
  var newslices = []
  for (var i = 0; i < slices.length; i++) {
    var slice = slices[i]
    var space = limit - count;
    if (space == 0)
      break;
    if (slice.length > space) {
      slice = slice.splice(0, space)
    }
    count = count + slice.length;
    newslices.push(slice);
  }
  return newslices;
}

var findHighestVisited = function(slices) {
  var highest;
  for (var i = 0; i < slices.length; i++) {
    var slice = slices[i];
    for (var j = 0; j < slice.length; j++) {
      var histitem = slice[j];
      if (!highest) {
        highest = histitem;
      } else if (highest.visitcount < histitem.visitcount) {
        highest = histitem;
      }
    }
  }
  return highest;
}

var isWebLink = function(text) {
  var idx = text.lastIndexOf('.')
  if (idx > 0) {
    return $A(["com", "org", "net", "uk", "us", "cn", "fm"]).include(text.substring(idx+1))
  }
  return false;
}

var parseUserUrl = function(text) {
  if (text.match(/^[A-Za-z]:.*$/)) {
    return text;
  }
  return "http://" + text;
}

var getSearchUrl = function(text) {
  return "http://google.com/search?q=" + escape(text);
}

var createSpanText = function(text, className) {
  var span = document.createElement('span')
  span.className = className
  span.appendChild(document.createTextNode(text))
  return span
}

var createAText = function(text, href, className) {
  var a = document.createElement('a');
  a.setAttribute('class' , className);
  a.setAttribute('href', href);
  a.appendChild(document.createTextNode(text));
  return a;
}

var filterHistoryItems = function(items, q) {
  var result = []
  q = q.toLowerCase();
  for (var i = 0; i < items.length; i++) {
    item = items[i]
    if (item['url'].indexOf(q) >= 0 || item['name'].toLowerCase().indexOf(q) >= 0) {
      result.push(item)
    }
  }
  return result;
}

var pad = function(x) { return x < 9 ? "0" + x : "" + x };
var twelveHour = function(x) { return (x > 12) ? (x % 12) : x };
var meridiem = function(x) { return (x > 12) ? "pm" : "am" };

var formatMonth = function(i) { return ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"][i]}

var journal = {
  appendDaySet: function(dayset) {
    var me = this;
    var date = dayset[0].date;
    var today = new Date();

    dayset.sort(function (a,b) { if (a.date > b.date) { return -1; } else { return 1; }})

    var content = document.getElementById('history');    
    var headernode = document.createElement('h4');
    headernode.className = 'date';
    if (getLocalDayOffset(today) == getLocalDayOffset(date))
      headernode.appendChild(document.createTextNode("Today, "))
    headernode.appendChild(document.createTextNode(formatMonth(date.getMonth()) + " " + pad(date.getDate()) + " " + date.getFullYear()));
    content.appendChild(headernode);
    var histnode = document.createElement('div');
    histnode.className = 'history';
    content.appendChild(histnode);

    for (var i = 0; i < dayset.length; i++) {
      var viewed_item = dayset[i]
      var is_target = viewed_item == this.targetHistoryItem;
      var histitemnode = document.createElement('div');
      histitemnode.className = 'item';
      var hrs = viewed_item.date.getHours();
      var mins = viewed_item.date.getMinutes();
      histitemnode.appendChild(createSpanText(twelveHour(hrs) + ":" + pad(mins) + " " + meridiem(hrs), 'time'));
      histitemnode.appendChild(createSpanText(viewed_item.action, 'action'));
      var titleLink = createAText(viewed_item.name,viewed_item.url,'title');
      titleLink.setAttribute('tabindex', 1);
      if (is_target) 
        this.targetHistoryItemLink = titleLink;
      titleLink.addEventListener("focus", function(e) { me.onResultFocus(e, true); }, false);
      titleLink.addEventListener("blur", function(e) { me.onResultFocus(e, false); }, false);             
      histitemnode.appendChild(titleLink);

      var histmetanode = document.createElement('div');
      histmetanode.className = 'meta';
      histmetanode.appendChild(createSpanText(' ', 'blue'));
      histmetanode.appendChild(createSpanText(' ', 'tags'));
      var hrefLink = createAText(viewed_item.url.split("?")[0],viewed_item.url,'url');
      hrefLink.setAttribute('tabindex', 0); 
      histmetanode.appendChild(hrefLink);
      histnode.appendChild(histitemnode);
      histnode.appendChild(histmetanode);
      if (is_target) {
        this.setAsTargetItem(histitemnode);
        histitemnode.setAttribute('id', 'default-target-item');
      }      
    }
  },
  setAsTargetItem: function (node) {
    node.addClassName("target-item");
  },
  unsetAsTargetItem: function (node) {
    node.removeClassName("target-item");
  },
  searchInfoBar: function(q) {
      var me = this;
      var node = document.createElement("div");
      node.className = "search-info-bar";
 
      node.appendChild(createSpanText("Searching for ", "search-pre-term"));
      node.appendChild(createSpanText(q,"search-term"));
      var clearSearch = document.createElement("a");
      clearSearch.addEventListener("click", function() { me.clearSearch(); }, false);
      clearSearch.href = "javascript:void(0);";
      clearSearch.className = "clear-search";
      clearSearch.setAttribute("accesskey", "c");
      clearSearch.setAttribute("title", "Clear this search [shift-alt-c]");
      clearSearch.appendChild(document.createTextNode("[clear]"));
      node.appendChild(clearSearch);
      
      var searchService = Components.classes["@mozilla.org/browser/search-service;1"].getService(Components.interfaces.nsIBrowserSearchService);
      var currentEngine = searchService.currentEngine;
      if (currentEngine && currentEngine.name) {
        var searchUri = currentEngine.getSubmission(q, null).uri.spec;
        node.appendChild(document.createTextNode(" | "));  
        var searchNode = document.createElement("span");       
        searchNode.addClassName("search-provider");
        var a = document.createElement("a");
        a.setAttribute("href", searchUri);
        var img = document.createElement("img");
        img.setAttribute("src", currentEngine.iconURI.spec);
        a.appendChild(img);
        searchNode.appendChild(a);
        searchNode.appendChild(document.createTextNode(" "));
        a = document.createElement("a");
        a.setAttribute("href", searchUri);   
        a.appendChild(document.createTextNode(currentEngine.name + ": " + q));
        searchNode.appendChild(a);  
        node.appendChild(searchNode);
      }
      return node;
  },
  onResultFocus: function(e, focused) {
    if (focused) {
      var defTarget = document.getElementById("default-target-item");
      if (defTarget) 
        this.unsetAsTargetItem(defTarget); 
      this.setAsTargetItem(e.target.parentNode);
    } else {
      this.unsetAsTargetItem(e.target.parentNode);
    }
  },
  redisplay: function() {
    var me = this;    
    var content = document.getElementById('history'); 
    var searchbox = document.getElementById('q');     
    while (content.firstChild) { content.removeChild(content.firstChild) };
    
    var histitems = getHistory();
    
    var viewed_items;
    var highest_item;
    var search = searchbox.value;
    if (search)
      search = search.strip()
    if (search) {
      content.appendChild(this.searchInfoBar(search))

      viewed_items = sliceByDay(filterHistoryItems(histitems, search));
      viewed_items = limitSliceCount(viewed_items, 6);
      this.targetHistoryItem = findHighestVisited(viewed_items);
      if (viewed_items.length == 0) {
        content.appendChild(createSpanText("(No results)", "no-results"))
      }
    } else {
      viewed_items = sliceByDay(histitems);
      for (var i = 0; i < viewed_items.length; i++) {
        if (viewed_items[i].length > 0) {
          viewed_items = [viewed_items[i]]
          break;
        }
      }
    }

    for (var i = 0; i < viewed_items.length; i++) {
      this.appendDaySet(viewed_items[i]);
    }
  },
  clearSearch : function() {
    var searchbox = document.getElementById('q');
    searchbox.value='';
    this.redisplay();
    searchbox.select();
    searchbox.focus();
  },
  onload: function() {
    this.searchTimeout = null;
    this.searchValue = null;
    this.targetHistoryItem = null;
    
    var searchbox = document.getElementById('q');
    var searchform = document.forms['qform'];
    
    var me = this;
    searchbox.addEventListener("keyup", function (e) { me.handleSearchChanged(e) }, false);
    searchform.addEventListener("submit", function (e) { me.onsubmit(); Event.stop(e); }, true);
    
    searchbox.focus();
    
    $("history").appendChild(document.createTextNode("Loading journal..."))
    window.setTimeout(function () { me.redisplay(); }, 150);    
  },
  clearSearchTimeouts: function() {
    if (typeof this.searchTimeout == "number") {
      window.clearTimeout(this.searchTimeout);
      this.searchTimeout = null;
    }
    if (typeof this.webSearchTimeout == "number") {
      window.clearTimeout(this.webSearchTimeout);
      this.webSearchTimeout = null;
    }  
  },
  onsubmit: function() {
    this.clearSearchTimeouts();
    if (this.targetHistoryItem) {
      window.location.href = this.targetHistoryItem.url;
    }
  },
  idleDoSearch: function() {
    this.searchTimeout = null;
    this.redisplay(); 
  },
  idleDoWebSearch: function() {
    this.webSearchTimeout = null;
    var q = document.getElementById("q").value;
    if (q) {
      document.getElementById("google-q").src = "http://www.gnome.org/~clarkbw/google/?q=" + escape(q.strip());
    }    
  },
  handleSearchChanged: function(e) {
    var q = e.target;
    var search = q.value.strip()
    if (search == this.searchValue)
      return;
    this.searchValue = search;
    if (!this.searchTimeout) {
      var me = this;
      this.searchTimeout = window.setTimeout(function () { me.idleDoSearch() }, 350);
      this.webSearchTimeout = window.setTimeout(function () { me.idleDoWebSearch() }, 600);      
    }
  }
}
