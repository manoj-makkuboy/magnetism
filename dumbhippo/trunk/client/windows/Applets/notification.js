// Notification implementation

dh.notification = {}

dh.display = null;

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl, selfId) {
    dh.util.debug("invoking dhInit")
    
    // Set some global parameters
    dh.selfId = selfId       // Current user ID
    dh.serverUrl = serverUrl // Base URL to server-side web pages
    dh.appletUrl = appletUrl // Base URL to local content files
    
    dh.display = new dh.notification.Display(serverUrl, appletUrl, selfId); 
}

// Constants for the "why" of showing shares
dh.notification.NEW = 1
dh.notification.VIEWER = 2
dh.notification.MESSAGE = 4

dh.notification.Display = function (serverUrl, appletUrl, selfId) {
    // Whether the user is currently using the computer
    this._idle = false
    
    // Whether the bubble is showing
    this._visible = false
    
    this._pageTimeoutId = null
    
    // postid -> notification
    this.savedNotifications = {}

    this._initNotifications = function() {
        this.notifications = []
        this.position = -1
    }
    
    this._initNotifications() // And do it initially
    
    this._bubble = new dh.bubble.Bubble(true)
    
    var display = this;
    this._bubble.onClose = function() {
        dh.util.debug("got close event");
        display._markCurrentAsSeen();
        dh.util.debug("done marking current as seen, closing")
        display.close();
    }
    this._bubble.onPrevious = function() {
        display.goPrevious();
    }
    this._bubble.onNext = function() {
        display._markCurrentAsSeen()
        display.goNext();
    }
    this._bubble.onLinkClick = function(postId, url) {
        window.external.application.DisplaySharedLink(postId, url)
        display._markAsSeenGoNext()
    }
    
    document.body.appendChild(this._bubble.create())

    this._updateIdle = null;
    this._idleUpdateDisplay = function() {
        if (this._updateIdle)
            return;
    
        window.setTimeout(function () {
            this._updateIdle = null;
            window.external.application.UpdateDisplay();
        }, 0)
    }
    
    this._bubble.onSizeChange = function() {
        window.external.application.Resize(display._bubble.getWidth(), display._bubble.getHeight())
        display._idleUpdateDisplay()
    }
    this._bubble.onSizeChange()
    
    this._bubble.onUpdateDisplay = function() {
        display._idleUpdateDisplay()
    }
    
    // returns true if the bubble result is that the bubble is displayed
    this._pushNotification = function (nType, data, timeout, why) {
        this.notifications.push({notificationType: nType,
                                 state: "pending",
                                 data: data,
                                 timeout: timeout,
                                 why: why})
        dh.util.debug("position " + this.position + " notifications: " + this.notifications)                                 
        var result = false
        if (this.position < 0) {
            this.setPosition(0)
            result = true
        }
        this._updateNavigation()
    }
    
    this._removeNotification = function(position) {
        this.notifications.splice(position, 1)
        if (this.notifications.length == 0) {
            this.close()
            return
        }
        if (this.position == position) {
            if (this.position < this.notifications.length)
                this.setPosition(this.position)
            else
                this.setPosition(this.position - 1)
        }
        this._updateNavigation()
        this._idleUpdateDisplay()
    }
    
    this._shouldDisplayShare = function (share) {
        if ((share.post.CurrentViewers.length > 0 && share.post.CurrentViewers.item(0).Id == dh.selfId)
            || (share.post.Sender.id == dh.selfId && share.post.CurrentViewers.length == 0)) {
            dh.util.debug("Not displaying notification of self-view or initial post")
            return false;
        }
        return true
    }
    
    this._findLinkShare = function (postId) {
        for (var i = 0; i < this.notifications.length; i++) {
            var notification = this.notifications[i] 
            if (notification.notificationType == 'linkShare' &&
                notification.data.post.Id == postId) {
                return {notification: notification, position: i}
            }
        }
        if (this.savedNotifications[postId]) {
            return {notification: this.savedNotifications[postId], position: -1}
        }
        return null
    }
    
    // Show the page of the swarm bubble relevant to a particular reason
    this._showRelevantPage = function(why) {
        if ((why & dh.notification.MESSAGE) != 0)
            this._bubble.setPage("someoneSaid")
        else if ((why & dh.notification.VIEWER) != 0)
            this._bubble.setPage("whosThere")
    }

    // Returns true iff we should show the window if it's hidden. 
    this.addLinkShare = function (share, isRedisplay, why) {
        var prevShareData = this._findLinkShare(share.post.Id)
        var shouldDisplayShare = isRedisplay || this._shouldDisplayShare(share)
        if (prevShareData) {
            // Update the viewer data
            prevShareData.notification.data = share
            prevShareData.notification.why |= why
        }
        if (!shouldDisplayShare)
            return false
        if (!prevShareData || prevShareData.position < 0) {   
            // We don't have it at all, or it was saved and needs to be redisplayed
            var displayed = this._pushNotification('linkShare', share, share.post.Timeout, why)
            if (displayed)
                this._showRelevantPage(why)
            this._idleUpdateDisplay() // Handle changes to the navigation arrows
            return true
        } else if (prevShareData && prevShareData.position == this.position) {
            // We're currently displaying this share, set it again in the bubble to force rerendering
            dh.util.debug("resetting current bubble data")
            this._bubble.setData(share)
            this._showRelevantPage(why)
            return shouldDisplayShare
        } else {
            dh.util.debug("not rerendering bubble");
        }
        return false
    }
    
    // Refresh the display of the share, if showing, otherwise do nothing
    this.updatePost = function(id) {
        var prevShareData = this._findLinkShare(id)
        if (!prevShareData)
            return
            
        // Check to see if the last viewer has left the post; if so and
        // we were only showing this page because there were viewers
        // remove the page and possibly close the bubble
        if (prevShareData.notification.data.post.currentViewers.length == 0) {
            prevShareData.notification.why &= ~dh.notification.VIEWER
            if (prevShareData.notification.why == 0) {
                this._removeNotification(prevShareData.position)
                return
            }
        }
        
        if (prevShareData.position == this.position) {
            // set data again in the bubble to force rerendering
            this._bubble.setData(prevShareData.notification.data)
        }
    }
    
    this.addMySpaceComment = function (comment) {
        // If we already have a notification for the same comment, ignore this
        for (var i = 0; i < this.notifications.length; i++) {
            if (this.notifications[i].notificationType == 'mySpaceComment' &&
                this.notifications[i].data.commentId == comment.commentId) {
                return;
            }
        }
        
        // Otherwise, add a new notification page
        this._pushNotification('mySpaceComment', comment, 0)    
    }
    
    this.displayMissed = function () {    
        for (postid in this.savedNotifications) {
            var notification = this.savedNotifications[postid]
            if (notification.state == "missed") {
                this._pushNotification(notification.notificationType, notification.data)
            }
        }    
    }
    
    this._clearPageTimeout = function() {
        if (this._pageTimeoutId != null) {
            window.clearTimeout(this._pageTimeoutId)
            this._pageTimeoutId = null
        }
    }
    
    this._resetPageTimeout = function() {
        if (!this._idle) {
            this._clearPageTimeout();
            // Handle infinite timeout   
            if (this.position >= 0) {
                var timeout = this.notifications[this.position].timeout
                dh.util.debug("current page timeout is " + timeout)        
                if (timeout < 0) {
                    return;
                } else if (timeout == 0) {
                    timeout = 7 // default timeout
                }
                var display = this;                
                this._pageTimeoutId = window.setTimeout(function() {
                    display.displayTimeout();
                    }, timeout * 1000); // 7 seconds
            }
        }
    }

    this.setPosition = function(pos) {
        if (pos < 0) {
            dh.util.debug("negative position specified")
            return
        } else if (pos >= this.notifications.length) {
            dh.util.debug("position " + pos + " is too big")
            return
        }
            
        dh.util.debug("switching to position " + pos + " from " + this.position)
        var notification = this.notifications[pos]  
        this.position = pos
        this._bubble.setData(notification.data)
        this._showRelevantPage(notification.why)
        this._updateNavigation()
        this._resetPageTimeout()
        this._idleUpdateDisplay()
    }

    this._updateNavigation = function() {
        this._bubble.updateNavigation(this.position, this.notifications.length)
    }

    this.goPrevious = function () {
        this.setPosition(this.position - 1)
    }
    
    this.goNext = function () {
        this.setPosition(this.position + 1)
    }
    
    this.displayTimeout = function () {
        dh.util.debug("got display timeout")
        this._markCurrentAsMissed()
        this.goNextOrClose()
    }
    
    this.goNextOrClose = function () {
        dh.util.debug("doing goNextOrClose")
        if (this.position >= (this.notifications.length-1)) {
            this.close();
        } else {
            this.goNext();
        }
    }
    
    this.notifyMissedChanged = function () {
        var haveMissed = false;
        for (postid in this.savedNotifications) {
            var notification = this.savedNotifications[postid]
            if (notification.state == "missed") {
                haveMissed = true;
            }
        }
        window.external.application.SetHaveMissedBubbles(haveMissed)
    }
    
    this.close = function () {
        dh.util.debug("bubble close invoked")
        this.setVisible(false)
        this._clearPageTimeout()
        window.external.application.Close()     
        var curDate = new Date()
        for (var i = 0; i < this.notifications.length; i++) {
            var notification = this.notifications[i]
            notification.saveDate = curDate
            dh.util.debug("saving notification " + notification)
            if (notification.state == "pending") // shouldn't happen
                notification.state = "missed"
            // be sure we've saved it, this is a noop for already saved
            this.savedNotifications[notification.data.postId] = notification
            dh.util.debug("done saving notification " + notification)            
        }
        this.notifyMissedChanged()      
        this._initNotifications()
    }
    
    this.setIdle = function(idle) {
        dh.util.debug("Idle status is now " + idle + " at position " + this.position)
        this._idle = idle
        if (this._idle)
            this._clearPageTimeout()
        else if (this._visible)
            this._resetPageTimeout()
    }
    
    this.setVisible = function(visible) {
        this._visible = visible
    }
    
    this._markCurrentAsMissed = function () {
        var notification = this.notifications[this.position]
        if (notification && notification.state == "pending")
            notification.state = "missed"               
    }
    
    this._markCurrentAsSeen = function () {
        var notification = this.notifications[this.position]
        if (notification && notification.state == "pending")
            notification.state = "seen"            
    }

    this._markAsSeenGoNext = function () {
        this._markCurrentAsSeen()
        this.goNextOrClose();    
    }
}

dhAdaptLinkRecipients = function (recipients) {
    var result = []
    var tmp = dh.core.adaptExternalArray(recipients)
    for (var i = 0; i < tmp.length; i += 2) {
        result.push({ id: tmp[i], name: tmp[i + 1] })
    }
    return result;
}

// Global namespace since it's painful to do anything else from C++

// Note if you change the parameters to these functions, you must change HippoBubble.cpp

dhAddLinkShare = function (isRedisplay, post) {
    dh.display.setVisible(true)
    
    dh.util.debug("adding link share id=" + post.Id)
    var data = new dh.bubble.PostData(post)
    return dh.display.addLinkShare(data, isRedisplay, dh.notification.NEW)
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    var data = new dh.bubble.MySpaceData(myId, blogId, commentId, posterId, posterName, posterImgUrl, content)
    dh.display.addMySpaceComment(data)
}

dhDisplayMissed = function () {
    dh.display.displayMissed()
}

dhViewerJoined = function(post, shouldNotify) {
    dh.display.setVisible(true)
    
    var data = new dh.bubble.PostData(post)
    return dh.display.addLinkShare(data, true, dh.notification.VIEWER)
}

dhChatRoomMessage = function(post, shouldNotify) {
    dh.display.setVisible(true)
        
    var data = new dh.bubble.PostData(post)
    return dh.display.addLinkShare(data, true, dh.notification.MESSAGE)
}

dhUpdatePost = function(post) {
    dh.display.updatePost(post.Id)
}

dhSetIdle = function(idle) {
    dh.display.setIdle(idle)
}