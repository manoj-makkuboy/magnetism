import hippo
import gtk

from bigboard.big_widgets import ThemedWidgetMixin

class Slideout(hippo.CanvasWindow):
    def __init__(self, widget=None):
        super(Slideout, self).__init__(gtk.WINDOW_TOPLEVEL)

        self.__widget = widget

        self.set_type_hint(gtk.gdk.WINDOW_TYPE_HINT_DOCK)
        self.set_resizable(False)
        self.set_keep_above(1)
        self.set_focus_on_map(0)

        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))

        self._root = hippo.CanvasBox()

        self.set_root(self._root)

    def get_root(self):
        return self._root
    
    def slideout(self):
        assert(self.__widget)
        coords = item.get_screen_coords()
        self.slideout_from(coords[0] + item.get_allocation()[0] + 4, coords[1])
    
    def slideout_from(self, x, y):
        screen_w = gtk.gdk.screen_width()
        screen_h = gtk.gdk.screen_height()
        (ignorex, ignorey, w, h) = self.get_allocation()
        offscreen_right = x + w - screen_w
        offscreen_bottom = y + h - screen_h
        if offscreen_right > 0:
            x = x - offscreen_right
        if offscreen_bottom > 0:
            y = y - offscreen_bottom
        self.move(x, y)
        self.present_with_time(gtk.get_current_event_time())
    
class ThemedSlideout(Slideout, ThemedWidgetMixin):
    def __init__(self, theme_hints=[], **kwargs):
        Slideout.__init__(self, **kwargs)
        ThemedWidgetMixin.__init__(self, theme_hints=theme_hints)
        
    def _on_theme_changed(self, theme):
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.color_parse("#%6X" % (theme.background >> 8,)))
        self.queue_draw_area(0,0,-1,-1)
                