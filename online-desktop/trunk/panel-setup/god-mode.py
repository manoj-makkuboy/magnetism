#!/usr/bin/python
# ~ god_mode 1

import os,sys

import gobject

import gnomepanel

def god_mode():
    godpanel = 'bottom_panel_GOD'
    gnomepanel.create_toplevel(godpanel)
    gnomepanel.add_applet(godpanel, 'show_desktop_button_GOD', 'GNOME_ShowDesktopApplet', 0)
    gnomepanel.add_applet(godpanel, 'bigboard_GOD', 'BigBoard_Applet', 1)
    gnomepanel.add_applet(godpanel, 'windowlist_GOD', 'GNOME_WindowListApplet', 2)
    gnomepanel.add_applet(godpanel, 'clock_GOD', 'GNOME_ClockApplet', 1, right=True)
    gnomepanel.add_applet(godpanel, 'tray_GOD', 'GNOME_SystemTrayApplet', 0, right=True)
    gnomepanel.set_toplevels([godpanel])
    
def apostate_mode():
    gnomepanel.set_toplevels(['bottom_panel', 'top_panel'])

if __name__ == '__main__':
    if len(sys.argv) == 2 and sys.argv[1] == '1':
        god_mode()
    else:
        apostate_mode()
