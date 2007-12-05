
from bigboard.libbig.singletonmixin import Singleton

class DefaultTheme(Singleton):
    def __init__(self):
        super(DefaultTheme, self).__init__()
        self.background = 0xFFFFFFFF
        self.foreground = 0x000000FF
        
    def draw_header(self, cr, area):
        cr.set_source_rgb(1.0, 1.0, 1.0)
        cr.rectangle(area.x, area.y, area.width, area.height)
        cr.fill()
        
def getInstance():
    return DefaultTheme.getInstance()