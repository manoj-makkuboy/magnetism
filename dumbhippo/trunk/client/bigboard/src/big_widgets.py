import os, code, sys, traceback, logging, StringIO, threading

import cairo, gtk, gobject

import hippo

from libgimmie import DockWindow
from libbig import URLImageCache
import libbig, mugshot, bigboard

class CanvasURLImage(hippo.CanvasImage):
    """A wrapper for CanvasImage which has a set_url method to retrieve
       images from a URL."""
    def __init__(self, url=None, **kwargs):
        hippo.CanvasImage.__init__(self, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, **kwargs) 
        if url:
            self.set_url(url)
        
    def set_url(self, url):
        if url:
            #print "fetching %s" % url
            image_cache = URLImageCache.getInstance()
            image_cache.get(url, self.__handle_image_load, self.__handle_image_error)

    def __handle_image_load(self, url, image):
        #print "got %s: %s" % (url, str(image))
        self.set_property("image", image)
        
    def __handle_image_error(self, url, exc):
        # note exception is automatically added to log
        logging.exception("failed to load image for '%s'", url)  #FIXME queue retry
        
class CanvasMugshotURLImage(CanvasURLImage):
    """A canvas image that takes a Mugshot-relative image URL."""
    def __init__(self, url=None, **kwargs):
        CanvasURLImage.__init__(self, **kwargs)
        self.__rel_url = None
        if url:
            self.set_url(url)
        
    def set_url(self, url):
        if url:
            self.__rel_url = url
            self.__sync()
        
    def __sync(self):
        baseurl = mugshot.get_mugshot().get_baseurl()
        if not (baseurl is None or self.__rel_url is None):
            CanvasURLImage.set_url(self, baseurl + self.__rel_url)

class PrelightingCanvasBox(hippo.CanvasBox):
    """A box with a background that changes color on mouse hover."""
    def __init__(self, **kwargs):
        hippo.CanvasBox.__init__(self, **kwargs)
        self.__hovered = False
        self.connect('motion-notify-event', lambda self, event: self.__handle_motion(event))
        
    def __handle_motion(self, event):
        if event.detail == hippo.MOTION_DETAIL_ENTER:
            self.__hovered = True
        elif event.detail == hippo.MOTION_DETAIL_LEAVE:
            self.__hovered = False

        self.sync_prelight_color()
        
    # protected
    def sync_prelight_color(self): 
        if self.__hovered and self.do_prelight():
            self.set_property('background-color', 0x00000033)
        else:
            self.set_property('background-color', 0x00000000)           
            
    # protected
    def do_prelight(self):
        return True
    
class PhotoContentItem(PrelightingCanvasBox):
    """A specialized container that has a photo and some
    corresponding content.  Handles size changes via 
    set_size."""
    def __init__(self, **kwargs):
        PrelightingCanvasBox.__init__(self,
                                      orientation=hippo.ORIENTATION_HORIZONTAL,
                                      spacing=4, **kwargs)
        self.__photo = None
        self.__child = None
        
    def set_photo(self, photo):
        assert(self.__photo is None)
        self.__photo = photo
        self.append(self.__photo)       
        
    def set_child(self, child):
        assert(not self.__photo is None)
        assert(self.__child is None)
        self.__child = child
        self.append(self.__child)         
        
    def set_size(self, size):
        assert(not (self.__photo is None or self.__child is None))
        if size == bigboard.Stock.SIZE_BULL:
            self.set_child_visible(self.__child, True)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_START)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_START)
        else:
            self.set_child_visible(self.__child, False)
            self.__photo.set_property('xalign', hippo.ALIGNMENT_CENTER)
            self.__photo.set_property('yalign', hippo.ALIGNMENT_CENTER)        

class Sidebar(DockWindow):
    __gsignals__ = {
        'size-request' : 'override'
        }

    def __init__(self, is_left):
        gravity = gtk.gdk.GRAVITY_WEST
        if not is_left:
            gravity = gtk.gdk.GRAVITY_EAST
        DockWindow.__init__(self, gravity)
        self.is_left = is_left

    def do_size_request(self, req):
        ret = DockWindow.do_size_request(self, req)
        # Give some whitespace
        geom = self.get_screen().get_monitor_geometry(0)
        
        screen = gtk.gdk.screen_get_default()
        rootw = screen.get_root_window()
        prop = rootw.property_get("_NET_WORKAREA")
        logging.debug("got _NET_WORKAREA: %s" % (prop,))
        (_, _, workarea) = prop
        work_height = workarea[3]
        req.height = work_height 
        # Never take more than available size
        req.width = min(geom.width, req.width)
        return ret

class CommandShell(gtk.Window):
    """Every application needs a development shell."""
    def __init__(self, locals={}):
        gtk.Window.__init__(self, type=gtk.WINDOW_TOPLEVEL)
        
        self._locals = locals
        
        self._history_path = libbig.get_bigboard_config_file('cmdshell_history')
        self._save_text_id = 0        
        
        box = gtk.VBox()
        paned = gtk.VPaned()
        self.output = gtk.TextBuffer()
        self.output_view = gtk.TextView(self.output)
        self.output_view.set_property("editable", False)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)
        scroll.add(self.output_view)
        paned.pack1(scroll, True, True)

        self.input = gtk.TextBuffer()
        self.input_view = gtk.TextView(self.input)
        self.input.connect("changed", self._handle_text_changed)
        scroll = gtk.ScrolledWindow()
        scroll.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_ALWAYS)        
        scroll.add(self.input_view)        
        paned.pack2(scroll, True, True)
        
        box.pack_start(paned, True, True)
        
        eval_button = gtk.Button("Eval")
        eval_button.connect("clicked", self.do_eval)
        box.pack_start(eval_button, False)
        self.add(box)

        try:
            history = file(self._history_path).read()
            self.input.set_property("text", history)
        except IOError, e:
            pass

        self.set_size_request(400, 600)
        self.set_focus(self.input_view)
    
    def _idle_save_text(self):
        history_file = file(self._history_path, 'w+')
        text = self.input.get_property("text")
        history_file.write(text)
        history_file.close()
        self._save_text_id = 0
        return False
    
    def _handle_text_changed(self, text):
        if self._save_text_id == 0:
            self._save_text_id = gobject.timeout_add(3000, self._idle_save_text)
    
    def do_eval(self, entry):
        try:
            output_stream = StringIO.StringIO()
            text = self.input.get_property("text")
            code_obj = compile(text, '<input>', 'exec')
            locals = {}
            for k, v in self._locals.items():
                locals[k] = v
            locals['output'] = output_stream
            exec code_obj in locals
            logging.debug("execution complete with %d output characters" % (len(output_stream.getvalue())),)
            self.output.set_property("text", output_stream.getvalue())
        except:
            logging.debug("caught exception executing")
            self.output.set_property("text", traceback.format_exc())
