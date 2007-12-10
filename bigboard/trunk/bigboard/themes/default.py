import sys

import hippo

from bigboard.libbig.singletonmixin import Singleton

class DefaultTheme(Singleton):
    def __init__(self):
        super(DefaultTheme, self).__init__()
        self.background = 0xFFFFFFFF
        self.prelight = 0xE2E2E2FF
        self.foreground = 0x000000FF
        self.subforeground = 0x666666FF
        self.header_start = 0xF4F4F4FF
        self.header_end = 0xC7C7C7FF
        
    def draw_header(self, cr, area):
        cr.set_source_rgb(1.0, 1.0, 1.0)
        cr.rectangle(area.x, area.y, area.width, area.height)
        cr.fill()
        
    def set_properties(self, widget):
        if isinstance(widget, hippo.CanvasText) or \
            isinstance(widget, hippo.CanvasLink):
            hints = widget.get_theme_hints()
            if 'subforeground' in hints:
                widget.set_properties(color=self.subforeground)
            else:
                widget.set_properties(color=self.foreground)
        
def getInstance():
    return DefaultTheme.getInstance()