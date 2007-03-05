import logging

import hippo

import bigboard, mugshot
from big_widgets import CanvasMugshotURLImage
import slideout
import profile

class EntityItem(hippo.CanvasBox):
    def __init__(self, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_HORIZONTAL
        hippo.CanvasBox.__init__(self, **kwargs)
        
        self._entity = None

        self._photo = CanvasMugshotURLImage(scale_width=30,
                                            scale_height=30,
                                            border=1,
                                            border_color=0x000000ff)
        self.append(self._photo)

        self._name = hippo.CanvasText(xalign=hippo.ALIGNMENT_FILL, yalign=hippo.ALIGNMENT_START,
                                      size_mode=hippo.CANVAS_SIZE_ELLIPSIZE_END)
        self.append(self._name)

        self.set_size(bigboard.Stock.SIZE_BULL)

        self.connect('button-press-event', self._handle_button_press)
        self.connect('button-release-event', self._handle_button_release)
        self.connect('motion-notify-event', self._handle_motion)
        self._pressed = False
        self._hovered = False

    def _update_color(self):
        if self._pressed:
            self.set_property('background-color', 0x00000088)
        elif self._hovered:
            self.set_property('background-color', 0x00000033)
        else:
            self.set_property('background-color', 0x00000000)        

    def _handle_button_press(self, self2, event):
        if event.button != 1:
            return False
        
        self._pressed = True

        self._update_color()

    def _handle_button_release(self, self2, event):
        if event.button != 1:
            return False

        self._pressed = False

        self._update_color()

    def _handle_motion(self, self2, event):
        if event.detail == hippo.MOTION_DETAIL_ENTER:
            self._hovered = True
        elif event.detail == hippo.MOTION_DETAIL_LEAVE:
            self._hovered = False

        self._update_color()
            
    def set_entity(self, entity):
        if self._entity == entity:
            return
        self._entity = entity
        self._update()

    def get_guid(self):
        return self._entity.get_guid()

    def get_entity(self):
        return self._entity

    def set_size(self, size):
        if size == bigboard.Stock.SIZE_BULL:
            self.set_child_visible(self._name, True)
            self._photo.set_property('xalign', hippo.ALIGNMENT_START)
            self._photo.set_property('yalign', hippo.ALIGNMENT_START)
        else:
            self.set_child_visible(self._name, False)
            self._photo.set_property('xalign', hippo.ALIGNMENT_CENTER)
            self._photo.set_property('yalign', hippo.ALIGNMENT_CENTER)

    def _update(self):
        if not self._entity:
            return
        self._name.set_property("text", self._entity.get_name())
        if self._entity.get_photo_url():
            self._photo.set_url(self._entity.get_photo_url())

    def get_screen_coords(self):
        return self.get_context().translate_to_screen(self)

class ProfileItem(hippo.CanvasBox):
    def __init__(self, profiles, **kwargs):
        kwargs['orientation'] = hippo.ORIENTATION_VERTICAL
        hippo.CanvasBox.__init__(self, **kwargs)

        self._profiles = profiles
        self._entity = None

        self._photo = CanvasMugshotURLImage(scale_width=30,
                                            scale_height=30,
                                            border=1,
                                            border_color=0x000000ff)
        self.append(self._photo)

        self._online = hippo.CanvasText(text='Offline')
        self.append(self._online)

    def set_entity(self, entity):
        if self._entity == entity:
            return
        self._entity = entity    

        if self._entity:

            if self._entity.get_photo_url():
                self._photo.set_url(self._entity.get_photo_url())

            self._profiles.fetch_profile(self._entity.get_guid(), self._on_profile_fetched)

    def _on_profile_fetched(self, profile):
        if not profile:
            print "failed to fetch profile"
            return
        
        print str(profile)

        if profile.get_online():
            self._online.set_property('text', 'Online')
        else:
            self._online.set_property('text', 'Offline')

class PeopleStock(bigboard.AbstractMugshotStock):
    def __init__(self):
        super(PeopleStock, self).__init__("People")
        
        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=3)

        self._items = {}

        self._slideout = None
        self._slideout_item = None

        self._profiles = profile.ProfileFactory()
        
    def _on_mugshot_initialized(self):
        super(PeopleStock, self)._on_mugshot_initialized()
        self._mugshot.connect("entity-added", self._handle_entity_added)
        self._mugshot.connect("self-changed", self._handle_self_changed)        
        self._mugshot.get_self()
        self._mugshot.get_network()        
        
    def get_content(self, size):
        return self._box

    def _set_item_size(self, item, size):
        if size == bigboard.Stock.SIZE_BULL:
            item.set_property('xalign', hippo.ALIGNMENT_FILL)
        else:
            item.set_property('xalign', hippo.ALIGNMENT_CENTER)
        
        item.set_size(size)

    def set_size(self, size):
        super(PeopleStock, self).set_size(size)
        for i in self._items.values():
            self._set_item_size(i, size)
            
    def _handle_self_changed(self, mugshot, myself):
        logging.debug("self (%s) changed" % (myself.get_guid(),))
    
    def _handle_entity_added(self, mugshot, entity):
        logging.debug("entity added to people stock %s" % (entity.get_name()))
        item = EntityItem()
        item.set_entity(entity)
        self._box.append(item)
        self._items[entity.get_guid()] = item
        self._set_item_size(item, self.get_size())
        item.connect('button-press-event', self._handle_item_pressed)

    def _handle_item_pressed(self, item, event):
        if self._slideout:
            self._slideout.destroy()
            self._slideout = None
            if self._slideout_item == item:
                self._slideout_item = None
                return

        self._slideout = slideout.Slideout()
        self._slideout_item = item
        coords = item.get_screen_coords()
        self._slideout.slideout_from(coords[0] + item.get_allocation()[0], coords[1])

        p = ProfileItem(self._profiles)
        p.set_entity(item.get_entity())
        self._slideout.get_root().append(p)
