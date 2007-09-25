import logging, subprocess, urllib

import gobject, gtk
import hippo

from bigboard.big_widgets import CanvasMugshotURLImage, CanvasHBox, CanvasVBox, ActionLink, IconLink, PrelightingCanvasBox
from bigboard.overview_table import OverviewTable

import FilesStock

_logger = logging.getLogger("bigboard.FileBrowser")

def create_account_url(account):
    account = urllib.unquote(account)
    domain = account[account.find("@") + 1:]
    if domain == "gmail.com":
        return "http://docs.google.com"
    else:
        return "https://docs.google.com/a/" + domain

class FileBrowser(hippo.CanvasWindow):
    __gsignals__ = {
        "activated" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
    }
    def __init__(self, stock):
        super(FileBrowser, self).__init__(gtk.WINDOW_TOPLEVEL)
        
        self.__stock = stock
        
        self.modify_bg(gtk.STATE_NORMAL, gtk.gdk.Color(65535,65535,65535))        

        self.set_title('Files')
        self.set_default_size(750, 600)
    
        self.__box = CanvasVBox(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, box_width=750, box_height=600)
        self.__box.set_property('background-color', 0xEEEEEEFF)

        browse_text = hippo.CanvasText(text="Browse:", font="Bold 12px", color=0x3F3F3FFF, padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        self.__box.append(browse_text)

        browse_options = CanvasVBox(xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START, box_width=750)
        self.__box.append(browse_options)

        local_files_link = ActionLink(text="Local Files", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
        local_files_link.connect("activated", self.__on_browse_local_files_clicked)
        browse_options.append(local_files_link)
 
        for google_account in self.__stock.googles.itervalues():
            google_docs_link = ActionLink(text=google_account.get_auth()[0] + " Docs", font="14px", padding=4, xalign=hippo.ALIGNMENT_START, yalign=hippo.ALIGNMENT_START)
            google_docs_link.connect("activated", FilesStock.on_link_clicked, create_account_url(google_account.get_auth()[0]))
            browse_options.append(google_docs_link)

        self.__search_box = CanvasHBox(padding_top=6, padding_bottom=6, box_width=750)        
        self.__search_text = hippo.CanvasText(text="Search Recent Files:", font="Bold 12px",
                                              color=0x3F3F3FFF, xalign=hippo.ALIGNMENT_START, padding_right=6)
        self.__search_box.append(self.__search_text)
        self.__search_input = hippo.CanvasEntry(box_width=250)
        self.__search_input.connect("notify::text", self.__on_search_changed)
        self.__search_input.connect("key-press-event", self.__on_search_keypress)
        self.__idle_search_id = 0
        self.__search_box.append(self.__search_input)
        self.__box.append(self.__search_box)

        self.__section_head = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL, color=0xAAAAAAFF, border_bottom=1, border_color=0xAAAAAAFF)
        self.__section_head.append(hippo.CanvasText(text="Recent Files", font="Bold 14px", xalign=hippo.ALIGNMENT_START))
        self.__box.append(self.__section_head)

        self.__files_outter_box = CanvasVBox(box_height=445, background_color=0xFFFFFFFF)
        self.__box.append(self.__files_outter_box, hippo.PACK_EXPAND)

        self.__right_scroll = hippo.CanvasScrollbars()
        self.__right_scroll.set_policy(hippo.ORIENTATION_HORIZONTAL,
                                       hippo.SCROLLBAR_NEVER)
        self.__files_box = CanvasVBox(border=0, background_color=0xFFFFFFFF)
        self.__files_outter_box.append(self.__right_scroll, hippo.PACK_EXPAND)
        
        self.__file_list = OverviewTable()
        self.__files_box.append(self.__file_list, hippo.PACK_EXPAND)

        self.__right_scroll.set_root(self.__files_box) 

        self.__file_items = []
        self.refresh_files()

        self.connect("delete-event", lambda *args: self.__hide() or True)
        self.connect("key-press-event", lambda win, event: self.__on_keypress(event))

        self.set_root(self.__box)

    def refresh_files(self):
        search = self.__get_search_text()
        self.__file_list.remove_all()
        self.__file_items = []
        for a_file in self.__stock.get_files():         
            if a_file.is_valid():                          
                link = a_file.create_icon_link()
                self.__file_list.add_column_item(0, link)
                self.__file_items.append(link)
                visible = search == None or link.link.get_property("text").lower().find(search) >= 0        
                link.set_visible(visible)

    def __on_search_changed(self, input, text):
        if self.__idle_search_id > 0:
            return
        self.__idle_search_id = gobject.timeout_add(500, self.__idle_do_search)
        
    def __on_search_keypress(self, entry, event):
        if event.key == hippo.KEY_RETURN:
            # if there is only one file that matches the search, we'll open it
            self.__select_single_visible_item()

    def __get_search_text(self):
        search = self.__search_input.get_property("text")
        if search.strip() == "":
            return None
        else:
            return search.lower()

    def __idle_do_search(self):
        search = self.__get_search_text()

        for item in self.__file_items:
            visible = search == None or item.link.get_property("text").lower().find(search) >= 0        
            item.set_visible(visible)

        self.__idle_search_id = 0
 
    def __select_single_visible_item(self):
        visible_item = None
        
        for item in self.__file_items:
            if item.get_visible():
                if visible_item == None:
                    visible_item = item
                else:
                    return # Two visible
                    
        if visible_item != None:
            visible_item.link.emit("activated")

    def __on_browse_local_files_clicked(self, canvas_item):
        subprocess.Popen(['nautilus', '--browser', self.__stock.desktop_path])

    def __on_link_clicked(self, canvas_item, url):
        subprocess.Popen(['gnome-open', url])

    def __hide(self):
        self.hide()
        
    def __on_keypress(self, event):
        if event.keyval == 65307:
            self.__hide()
