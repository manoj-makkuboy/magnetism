var AppsSidebar = Class.create();
AppsSidebar.prototype = {
  sidebarId: "AppsSidebar",
  sidebarTitle: "Applications",
  initialize: function() {
  },
  setupDom: function(div) {
    var apps = document.createElement("div");
    apps.setAttribute("id", "apps");
    div.appendChild(apps);
    this.sendRequest();  
  },
  sendRequest: function() {
    var me = this;
    new Ajax.Request('http://mugshot.org/xml/popularapplications',
    {
      method:'get',
      onSuccess: function(transport){
        var response = transport.responseXML;
        var apps = $("apps");
        while (apps.firstChild) { apps.removeChild(apps.firstChild); };
        var appsData = response.documentElement.childNodes[0];
        console.log(appsData);
        for (var i = 0; i < appsData.childNodes.length && i < 6; i++) {
          var node = appsData.childNodes[i];
          var a = document.createElement("a");
          Event.observe(a, "click", me.onAppClick.bind(me, node), false);
          a.href = "javascript:void(0);";
          var appId = node.getAttribute("id");
          if (appId == "mozilla-firefox") // No need to be recursive.
            continue;
          var appName = node.getAttribute("name");
          console.log("app: %s", appName);
          a.appendChild(document.createTextNode(appName));
          apps.appendChild(a);
          apps.appendChild(document.createElement("br"));
        };
      },
      onFailure: function(){ alert('Something went wrong...') }
    });
  },
  onAppClick: function(e, appData) {
    alert("app click");
    Event.stop(e);
  },
  redisplay: function(search) {
  }
}

getJournalPageInstance().appendSidebar(new AppsSidebar());
