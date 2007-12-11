import copy
import logging

import gobject
import hippo
import layout_utils

_logger = logging.getLogger("bigboard.ScrollRibbon")

class ScrollRibbonLayout(gobject.GObject,hippo.CanvasLayout):
    """A Canvas Layout manager that creates a scrollable area with buttons

    """

    def __init__(self):
        gobject.GObject.__init__(self)
        self.__box = None

        self.__content_height = 0
        self.__offset = 0

        self.viewport = gtk.gdk.Rectangle(0, 0, 0, 0)

    def scroll_by(self, increment):
        self.__offset = self.__offset + increment
        self.__box.emit_request_changed()

    def __on_up_clicked(self, button):
        self.scroll_by(-5)

    def __on_down_clicked(self, button):
        self.scroll_by(5)

    def add(self, child):
        if self.__box == None:
            raise Exception("Layout must be set on a box before adding children")
        
        self.__box.append(child)
        box_child = self.__box.find_box_child(child)
        box_child.is_contents = True
        box_child.x = 0
        box_child.y = 0

    def do_set_box(self, box):
        self.__box = box

        self.__up_button = hippo.CanvasButton(text="Up")
        self.__down_button = hippo.CanvasButton(text="Down")

        self.__box.append(self.__up_button)
        self.__box.append(self.__down_button, flags=hippo.PACK_END)

        box_child = self.__box.find_box_child(self.__up_button)
        box_child.is_contents = False
        box_child.x = 0
        box_child.y = 0

        box_child = self.__box.find_box_child(self.__down_button)
        box_child.is_contents = False
        box_child.x = 0
        box_child.y = 0

        self.__up_button.connect('activated', self.__on_up_clicked)
        self.__down_button.connect('activated', self.__on_down_clicked)

    def do_get_width_request(self):

        content_min = 0
        content_natural = 0

        for box_child in self.__box.get_layout_children():

            #_logger.debug("Width requesting child " + str(box_child))

            (child_min, child_natural) = box_child.get_width_request()
            
            content_min = max(content_min, child_min)
            content_natural = max(content_natural, child_natural)

        return (content_min, content_natural)
        
    def __get_height_request(self, item, for_width):
        box_child = self.__box.find_box_child(item)
        return box_child.get_height_request(for_width)

    def do_get_height_request(self, for_width):

        (up_min, up_natural) = self.__get_height_request(self.__up_button, for_width)
        (down_min, down_natural) = self.__get_height_request(self.__down_button, for_width)

        MIN_CONTENT_HEIGHT = 5

        self.__content_height = 0
        for box_child in self.__box.get_layout_children():

            #_logger.debug("Height requesting child " + str(box_child))

            if not box_child.is_contents:
                continue

            (child_min, child_natural) = box_child.get_height_request(for_width)
            self.__content_height = self.__content_height + child_natural

        return (up_min + down_min + MIN_CONTENT_HEIGHT, up_natural + down_natural + MIN_CONTENT_HEIGHT)

    def do_allocate(self, x, y, width, height, requested_width, requested_height, origin_changed):
        (up_min, up_natural) = self.__get_height_request(self.__up_button, width)
        (down_min, down_natural) = self.__get_height_request(self.__down_button, width)

        box_child = self.__box.find_box_child(self.__up_button)
        box_child.x = 0
        box_child.y = 0
        box_child.allocate(box_child.x, box_child.y, width, up_natural, origin_changed)

        box_child = self.__box.find_box_child(self.__down_button)
        box_child.x = 0
        box_child.y = height - down_natural
        box_child.allocate(box_child.x, box_child.y, width, down_natural, origin_changed)

        self.viewport.x = 0
        self.viewport.y = up_natural
        self.viewport.width = width
        self.viewport.height = height - down_natural - up_natural

        ## this only works with a single child right now, despite the loop.
        ## add a box, put stuff in the box, if you want two children.
        for box_child in self.__box.get_layout_children():

            #_logger.debug("Allocating child " + str(box_child))
            
            if box_child.is_contents:
                (child_min, child_natural) = box_child.get_height_request(width)
                
                # min offset has bottom of child aligned with bottom of viewport, 
                # but if there's too much space for child, child is always top-aligned
                min_offset = self.viewport.height - child_natural
                if min_offset > 0:
                    min_offset = 0

                # max offset has top of child aligned with top of viewport
                max_offset = 0

                offset = max(self.__offset, min_offset)
                offset = min(offset, max_offset)

                ## save this new offset
                self.__offset = offset

                # we always allocate the child its full height; then we 
                # don't draw the parts outside the viewport
                box_child.x = 0
                box_child.y = self.viewport.y + self.__offset
                box_child.allocate(box_child.x, box_child.y, width,
                                   child_natural, origin_changed)


gobject.type_register(ScrollRibbonLayout)


class VerticalScrollArea(hippo.CanvasBox):
    """A box with scroll arrows on top and bottom."""

    def __init__(self, **kwargs):
        hippo.CanvasBox.__init__(self, **kwargs)

        self.__offset = 0

        self.__layout = ScrollRibbonLayout()
        self.set_layout(self.__layout)

    def add(self, child):
        self.__layout.add(child)

    def do_paint_children(self, cr, damaged_box):
        for box_child in self.get_layout_children():
            if not box_child.visible:
                continue
            
            if box_child.is_contents:
                cr.save()
                cr.rectangle(self.__layout.viewport.x,
                             self.__layout.viewport.y,
                             self.__layout.viewport.width, 
                             self.__layout.viewport.height)
                cr.clip()

            box_child.item.process_paint(cr, damaged_box, box_child.x, box_child.y)

            if box_child.is_contents:
                cr.restore()

gobject.type_register(VerticalScrollArea)

if __name__ == "__main__":

    import gtk
    import bigboard.libbig.logutil

    bigboard.libbig.logutil.init("DEBUG", ['bigboard.ScrollRibbon'], '')

    window = hippo.CanvasWindow()
    area = VerticalScrollArea()

    area.add(hippo.CanvasText(text='A\nB\nC\nD\nE\nF\nG'))

    window.set_root(area)
    window.show()
    
    gtk.main()
