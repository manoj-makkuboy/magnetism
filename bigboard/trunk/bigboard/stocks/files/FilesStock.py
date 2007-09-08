import logging, os, subprocess, urlparse, urllib
import xml.dom, xml.dom.minidom

import gobject, gtk, pango
import gconf, gnomevfs
import gnome.ui
import dbus, dbus.glib
import hippo

import bigboard.libbig as libbig
from bigboard.libbig.logutil import log_except
from bigboard.workboard import WorkBoard
from bigboard.stock import Stock
from bigboard.big_widgets import PhotoContentItem, CanvasVBox, CanvasHBox, ActionLink, Separator
from bigboard.libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs

_logger = logging.getLogger('bigboard.stocks.FilesStock')

class IconLink(CanvasHBox):
    def __init__(self, text, **kwargs):
        kwargs['spacing'] = 4
        super(IconLink, self).__init__(**kwargs)
        self.img = hippo.CanvasImage(scale_width=20, scale_height=20)
        self.append(self.img)
        self.link = hippo.CanvasLink(text=text)
        self.append(self.link)

class FilesStock(Stock):
    """Shows recent files."""
    def __init__(self, *args, **kwargs):
        super(FilesStock,self).__init__(*args, **kwargs)

        self.__display_limit = 3
        self.__thumbnails = gnome.ui.ThumbnailFactory(gnome.ui.THUMBNAIL_SIZE_NORMAL)
        self.__itheme = gtk.icon_theme_get_default() 
        self.__recentf_path = os.path.expanduser('~/.recently-used')        

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)
        browsefiles = IconLink('Browse Files', xalign=hippo.ALIGNMENT_CENTER)
        browsefiles.img.set_property('image-name', 'gtk-directory')
        browsefiles.link.connect("activated", lambda l: self.__on_browse_files())
        self._box.append(browsefiles)
        self._recentbox = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4)
        self._box.append(self._recentbox)

        gnomevfs.monitor_add(self.__recentf_path, gnomevfs.MONITOR_FILE, lambda *args: self.__redisplay())
        gobject.idle_add(self.__redisplay)
        
    def get_content(self, size):
        return self._box
    
    def __on_browse_files(self):
        subprocess.Popen(['gnome-open', os.path.expanduser('~/Desktop')])        
        
    @log_except(logger=_logger)
    def __redisplay(self):
        _logger.debug("doing redisplay")
        if not os.path.isfile(self.__recentf_path):
            _logger.debug("no recent files")
            return
        f = open(self.__recentf_path, 'r')
        doc = xml.dom.minidom.parseString(f.read())
        self._recentbox.remove_all()
        i = 0
        for child in xml_query(doc.documentElement, 'RecentItem*'):         
            if i >= self.__display_limit: break
            url = xml_query(child, 'URI#')
            _logger.debug("using recent url %s", url)
            try:            
                vfsstat = gnomevfs.get_file_info(url.encode('utf-8'), gnomevfs.FILE_INFO_GET_MIME_TYPE | gnomevfs.FILE_INFO_FOLLOW_LINKS)
            except gnomevfs.NotFoundError, e:
                _logger.debug("Failed to get file info for target of '%s'", url, exc_info=True)
                continue
            try:
                (result, flags) = gnome.ui.icon_lookup(self.__itheme, self.__thumbnails, url, file_info=vfsstat, mime_type=vfsstat.mime_type)
            except gnomevfs.NotFoundError, e:
                _logger.debug("Failed to get icon info for '%s'", url, exc_info=True)
                continue
                         
            text = urllib.unquote(os.path.basename(url))
            link = IconLink(text)
            link.img.set_property('image-name', result)
            link.link.connect("activated", lambda l: self.__on_recentitem(url))
            self._box.append(link)
            i += 1
        
    def __on_recentitem(self, url):
        subprocess.Popen(['gnome-open', url])
        