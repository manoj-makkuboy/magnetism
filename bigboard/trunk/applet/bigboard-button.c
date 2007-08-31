/* -*- mode: C; c-file-style: "linux"; c-basic-offset: 8; indent-tabs-mode: nil; -*- */
/* "Show BigBoard" panel applet based on "show desktop button" */

/*
 * Copyright (C) 2002, 2007 Red Hat, Inc.
 * Developed by Havoc Pennington
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

#include <config.h>

#include <gtk/gtk.h>
#include <panel-applet.h>
#include "hippo-dbus-helper.h"

#include <string.h>

#define TIMEOUT_ACTIVATE 1000

/* FIXME */
#define ICON_NAME "gnome-fs-desktop"

static void
wncklet_set_tooltip (GtkWidget  *widget,
		     const char *tip)
{
        GtkTooltips *tooltips;

        tooltips = g_object_get_data (G_OBJECT (widget), "tooltips");
        if (!tooltips) {
                tooltips = gtk_tooltips_new ();
                g_object_ref (tooltips);
                gtk_object_sink (GTK_OBJECT (tooltips));
                g_object_set_data_full (G_OBJECT (widget), "tooltips", tooltips,
                                        (GDestroyNotify) g_object_unref);
        }

        gtk_tooltips_set_tip (tooltips, widget, tip, NULL);
}

static void
wncklet_display_about (GtkWidget   *applet,
		       GtkWidget  **dialog,
		       const char  *name,
		       const char  *copyright,
		       const char  *comments,
		       const char **authors,
		       const char **documenters,
		       const char  *translator_credits,
		       const char  *icon_name,
		       const char  *wmclass_name,
		       const char  *wmclass_class)
{
	if (*dialog) {
		gtk_window_set_screen (GTK_WINDOW (*dialog),
				       gtk_widget_get_screen (applet));
		gtk_window_present (GTK_WINDOW (*dialog));
		return;
	}

	*dialog = gtk_about_dialog_new ();
	g_object_set (*dialog,
		      "name",  name,
		      "version", VERSION,
		      "copyright", copyright,
		      "comments", comments,
		      "authors", authors,
		      "documenters", documenters,
		      "translator-credits", translator_credits,
		      "logo-icon-name", icon_name,
		      NULL);

	gtk_window_set_wmclass (GTK_WINDOW (*dialog),
				wmclass_name, wmclass_class);
	gtk_window_set_screen (GTK_WINDOW (*dialog),
			       gtk_widget_get_screen (applet));

	gtk_window_set_icon_name (GTK_WINDOW (*dialog), icon_name);
	g_signal_connect (*dialog, "destroy",
			  (GCallback) gtk_widget_destroyed, dialog);

	g_signal_connect (*dialog, "response",
			  G_CALLBACK (gtk_widget_destroy),
			  NULL);

	gtk_widget_show (*dialog);
}

static void
wncklet_display_help (GtkWidget  *widget,
		      const char *doc_id,
		      const char *filename,
		      const char *link_id)
{
#if 0
	GError *error = NULL;

	gnome_help_display_desktop_on_screen (NULL, doc_id, filename, link_id,
					      gtk_widget_get_screen (widget),
					      &error);

	if (error) {
		GtkWidget *dialog;

		dialog = gtk_message_dialog_new (NULL,
						 GTK_DIALOG_DESTROY_WITH_PARENT,
						 GTK_MESSAGE_ERROR,
						 GTK_BUTTONS_OK,
						 _("There was an error displaying help: %s"),
						 error->message);

		g_signal_connect (dialog, "response",
				  G_CALLBACK (gtk_widget_destroy),
				  NULL);

		gtk_window_set_resizable (GTK_WINDOW (dialog), FALSE);
		gtk_window_set_screen (GTK_WINDOW (dialog),
				       gtk_widget_get_screen (widget));
		gtk_widget_show (dialog);
		g_error_free (error);
	}
#endif
}


static void
wncklet_connect_while_alive (gpointer    object,
			     const char *signal,
			     GCallback   func,
			     gpointer    func_data,
			     gpointer    alive_object)
{
	GClosure *closure;

	closure = g_cclosure_new (func, func_data, NULL);
	g_object_watch_closure (G_OBJECT (alive_object), closure);
	g_signal_connect_closure_by_id (object,
                                        g_signal_lookup (signal, G_OBJECT_TYPE (object)), 0,
                                        closure,
                                        FALSE);
}

typedef struct {
        /* widgets */
        GtkWidget *applet;
        GtkWidget *button;
        GtkWidget *image;
        GtkWidget *about_dialog;

        PanelAppletOrient orient;
        int size;

        guint showing_bigboard : 1;
        guint button_activate;

        GtkIconTheme *icon_theme;

        DBusConnection *connection;
        HippoDBusProxy *bb_proxy;
} ButtonData;

static void display_help_dialog     (BonoboUIComponent *uic,
                                     ButtonData        *button_data,
                                     const gchar       *verbname);
static void display_about_dialog    (BonoboUIComponent *uic,
                                     ButtonData        *button_data,
                                     const gchar       *verbname);
static void update_icon             (ButtonData        *button_data);
static void update_button_state     (ButtonData        *button_data);
static void update_button_display   (ButtonData        *button_data);
static void update_showing_bigboard (ButtonData        *button_data,
                                     gboolean           showing_bigboard);
static void theme_changed_callback  (GtkIconTheme      *icon_theme,
                                     ButtonData        *button_data);
static void button_toggled_callback (GtkWidget         *button,
                                     ButtonData        *button_data);


static void
handle_expanded_changed(DBusConnection *connection,
                        DBusMessage    *message,
                        void           *data)
{
        dbus_bool_t is_expanded;
        ButtonData *button_data;

        button_data = data;

        if (!dbus_message_get_args(message, NULL, DBUS_TYPE_BOOLEAN, &is_expanded,
                                   DBUS_TYPE_INVALID)) {
                g_warning ("Expanded signal from bigboard has wrong signature");
                return;
        }

        update_showing_bigboard (button_data, is_expanded);
}

static void
handle_bigboard_available(DBusConnection *connection,
                          const char     *well_known_name,
                          const char     *unique_name,
                          void           *data)
{
        ButtonData *button_data;

        button_data = data;

        /* request the expanded state */
        hippo_dbus_proxy_VOID__VOID (button_data->bb_proxy,
                                     "EmitExpandedChanged");
}

static void
handle_bigboard_unavailable(DBusConnection *connection,
                            const char     *well_known_name,
                            const char     *unique_name,
                            void           *data)
{
        ButtonData *button_data;

        button_data = data;
        
        update_showing_bigboard (button_data, FALSE);
}

static const HippoDBusSignalTracker signal_handlers[] = {
        { "org.gnome.BigBoard", "ExpandedChanged", handle_expanded_changed },
        { NULL, NULL, NULL }
};

static const HippoDBusServiceTracker bigboard_tracker = {
        handle_bigboard_available,
        handle_bigboard_unavailable
};

static void
update_orientation (ButtonData       *button_data,
                    PanelAppletOrient orient)
{
        GtkOrientation new_orient;

        switch (orient)
        {
        case PANEL_APPLET_ORIENT_LEFT:
        case PANEL_APPLET_ORIENT_RIGHT:
                new_orient = GTK_ORIENTATION_VERTICAL;
                break;
        case PANEL_APPLET_ORIENT_UP:
        case PANEL_APPLET_ORIENT_DOWN:
        default:
                new_orient = GTK_ORIENTATION_HORIZONTAL;
                break;
        }

        if (new_orient == button_data->orient)
                return;

        button_data->orient = new_orient;

        update_icon (button_data);
}

static void
applet_change_orient (PanelApplet       *applet,
                      PanelAppletOrient  orient,
                      ButtonData        *button_data)
{
        update_orientation (button_data, orient);
}

static void
update_size (ButtonData *button_data,
             int         size)
{
        if (button_data->size == size)
                return;

        button_data->size = size;

        update_icon (button_data);
}

/* this is when the panel size changes */
static void
button_size_allocated (GtkWidget       *button,
		       GtkAllocation   *allocation,
                       ButtonData      *button_data)
{
        switch (button_data->orient) {
        case GTK_ORIENTATION_HORIZONTAL:
                update_size (button_data, allocation->height);
                break;
        case GTK_ORIENTATION_VERTICAL:
                update_size (button_data, allocation->width);
                break;
        }
}

static void
update_icon (ButtonData *button_data)
{
        int width, height;
        GdkPixbuf *icon;
        GdkPixbuf *scaled;
        int        icon_size;
        GError    *error;
        int	   focus_width = 0;
        int	   focus_pad = 0;
        int	   thickness = 0;

        if (!button_data->icon_theme)
                return;

        gtk_widget_style_get (button_data->button,
                              "focus-line-width", &focus_width,
                              "focus-padding", &focus_pad,
                              NULL);

        switch (button_data->orient) {
        case GTK_ORIENTATION_HORIZONTAL:
                thickness = button_data->button->style->ythickness;
                break;
        case GTK_ORIENTATION_VERTICAL:
                thickness = button_data->button->style->xthickness;
                break;
        }

        icon_size = button_data->size - 2 * (focus_width + focus_pad + thickness);

        if (icon_size < 22)
                icon_size = 16;
        else if (icon_size < 32)
                icon_size = 22;
        else if (icon_size < 48)
                icon_size = 32;

        error = NULL;
        icon = gtk_icon_theme_load_icon (button_data->icon_theme,
                                         ICON_NAME,
                                         icon_size, 0, &error);

        if (icon == NULL) {
                g_printerr (_("Failed to load %s: %s\n"), ICON_NAME,
                            error ? error->message : _("Icon not found"));
                if (error) {
                        g_error_free (error);
                        error = NULL;
                }

                gtk_image_set_from_stock (GTK_IMAGE (button_data->image),
                                          GTK_STOCK_MISSING_IMAGE,
                                          GTK_ICON_SIZE_SMALL_TOOLBAR);
                return;
        }

        width = gdk_pixbuf_get_width (icon);
        height = gdk_pixbuf_get_height (icon);

        scaled = NULL;

        /* Make it fit on the given panel */
        switch (button_data->orient) {
        case GTK_ORIENTATION_HORIZONTAL:
                width = (icon_size * width) / height;
                height = icon_size;
                break;
        case GTK_ORIENTATION_VERTICAL:
                height = (icon_size * height) / width;
                width = icon_size;
                break;
        }

        scaled = gdk_pixbuf_scale_simple (icon,
                                          width, height,
                                          GDK_INTERP_BILINEAR);

        if (scaled != NULL) {
                gtk_image_set_from_pixbuf (GTK_IMAGE (button_data->image),
                                           scaled);
                g_object_unref (scaled);
        } else
                gtk_image_set_from_pixbuf (GTK_IMAGE (button_data->image),
                                           icon);

        g_object_unref (icon);
}

static const BonoboUIVerb bigboard_button_menu_verbs [] = {
        BONOBO_UI_UNSAFE_VERB ("BigBoardButtonHelp",        display_help_dialog),
        BONOBO_UI_UNSAFE_VERB ("BigBoardButtonAbout",       display_about_dialog),
        BONOBO_UI_VERB_END
};

/* This updates things that should be consistent with the button's appearance,
 * and update_button_state updates the button appearance itself
 */
static void
update_button_display (ButtonData *button_data)
{
        if (gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (button_data->button))) {
                wncklet_set_tooltip (button_data->button,
                                     _("Click here to show the desktop sidebar."));
        } else {
                wncklet_set_tooltip (button_data->button,
                                     _("Click here to hide the desktop sidebar."));
        }
}

static void
update_button_state (ButtonData *button_data)
{
        if (button_data->showing_bigboard) {
                g_signal_handlers_block_by_func (G_OBJECT (button_data->button),
                                                 G_CALLBACK (button_toggled_callback),
                                                 button_data);
                gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (button_data->button),
                                              TRUE);
                g_signal_handlers_unblock_by_func (G_OBJECT (button_data->button),
                                                   G_CALLBACK (button_toggled_callback),
                                                   button_data);
        } else {
                g_signal_handlers_block_by_func (G_OBJECT (button_data->button),
                                                 G_CALLBACK (button_toggled_callback),
                                                 button_data);
                gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (button_data->button),
                                              FALSE);
                g_signal_handlers_unblock_by_func (G_OBJECT (button_data->button),
                                                   G_CALLBACK (button_toggled_callback),
                                                   button_data);
        }

        update_button_display (button_data);
}

static void
update_showing_bigboard (ButtonData        *button_data,
                         gboolean           showing_bigboard)
{
        if ((showing_bigboard != FALSE) == button_data->showing_bigboard)
                return;


        button_data->showing_bigboard = showing_bigboard != FALSE;

        update_button_state (button_data);
}

static void
applet_destroyed (GtkWidget       *applet,
                  ButtonData      *button_data)
{
        if (button_data->connection) {
                hippo_dbus_helper_unregister_service_tracker(button_data->connection,
                                                             "org.gnome.BigBoard",
                                                             &bigboard_tracker,
                                                             button_data);

                hippo_dbus_proxy_unref (button_data->bb_proxy);

                button_data->bb_proxy = NULL;

                dbus_connection_unref (button_data->connection);

                button_data->connection = NULL;
        }

        if (button_data->about_dialog) {
                gtk_widget_destroy (button_data->about_dialog);
                button_data->about_dialog =  NULL;
        }

        if (button_data->button_activate != 0) {
                g_source_remove (button_data->button_activate);
                button_data->button_activate = 0;
        }

        if (button_data->icon_theme != NULL) {
                g_signal_handlers_disconnect_by_func (button_data->icon_theme,
                                                      theme_changed_callback,
                                                      button_data);
                button_data->icon_theme = NULL;
        }

        g_free (button_data);
}

static gboolean
do_not_eat_button_press (GtkWidget      *widget,
                         GdkEventButton *event)
{
        if (event->button != 1) {
                g_signal_stop_emission_by_name (widget, "button_press_event");
        }

        return FALSE;
}

static gboolean
button_motion_timeout (gpointer data)
{
        ButtonData *button_data = (ButtonData*) data;

        button_data->button_activate = 0;

        g_signal_emit_by_name (G_OBJECT (button_data->button), "clicked", button_data);

        return FALSE;
}

static void
button_drag_leave (GtkWidget          *widget,
		   GdkDragContext     *context,
		   guint               time,
		   ButtonData    *button_data)
{
        if (button_data->button_activate != 0) {
                g_source_remove (button_data->button_activate);
                button_data->button_activate = 0;
        }
}

static gboolean
button_drag_motion (GtkWidget          *widget,
		    GdkDragContext     *context,
		    gint                x,
		    gint                y,
		    guint               time,
		    ButtonData    *button_data)
{

        if (button_data->button_activate == 0)
                button_data->button_activate = g_timeout_add (TIMEOUT_ACTIVATE,
                                                              button_motion_timeout,
                                                              button_data);
        gdk_drag_status (context, 0, time);

        return TRUE;
}

static void
bigboard_button_applet_realized (PanelApplet *applet,
                                 gpointer     data)
{
        ButtonData *button_data;
        GdkScreen       *screen;

        button_data = (ButtonData *) data;

        if (button_data->icon_theme != NULL)
                g_signal_handlers_disconnect_by_func (button_data->icon_theme,
                                                      theme_changed_callback,
                                                      button_data);

        screen = gtk_widget_get_screen (button_data->applet);

        /* FIXME set initial button state according to whether board is showing */

        button_data->icon_theme = gtk_icon_theme_get_for_screen (screen);
        wncklet_connect_while_alive (button_data->icon_theme, "changed",
                                     G_CALLBACK (theme_changed_callback),
                                     button_data,
                                     button_data->applet);

        update_icon (button_data);
}

static void
theme_changed_callback (GtkIconTheme    *icon_theme,
			ButtonData *button_data)
{
        update_icon (button_data);
}

static ButtonData*
bigboard_button_add_to_widget (GtkWidget *applet)
{
        ButtonData *button_data;
        AtkObject  *atk_obj;

        button_data = g_new0 (ButtonData, 1);

        button_data->applet = applet;

        button_data->image = gtk_image_new ();

        button_data->orient = GTK_ORIENTATION_VERTICAL;

        button_data->size = 24;

        g_signal_connect (G_OBJECT (button_data->applet), "realize",
                          G_CALLBACK (bigboard_button_applet_realized), button_data);

        button_data->button = gtk_toggle_button_new ();

        gtk_widget_set_name (button_data->button, "showdesktop-button");
        gtk_rc_parse_string ("\n"
                             "   style \"showdesktop-button-style\"\n"
                             "   {\n"
                             "      GtkWidget::focus-line-width=0\n"
                             "      GtkWidget::focus-padding=0\n"
                             "   }\n"
                             "\n"
                             "    widget \"*.showdesktop-button\" style \"showdesktop-button-style\"\n"
                             "\n");

        atk_obj = gtk_widget_get_accessible (button_data->button);
        atk_object_set_name (atk_obj, _("Show Desktop Button"));
        g_signal_connect (G_OBJECT (button_data->button), "button_press_event",
                          G_CALLBACK (do_not_eat_button_press), NULL);

        g_signal_connect (G_OBJECT (button_data->button), "toggled",
                          G_CALLBACK (button_toggled_callback), button_data);

        gtk_container_set_border_width (GTK_CONTAINER (button_data->button), 0);
        gtk_container_add (GTK_CONTAINER (button_data->button), button_data->image);
        gtk_container_add (GTK_CONTAINER (button_data->applet), button_data->button);

        g_signal_connect (G_OBJECT (button_data->button),
                          "size_allocate",
                          G_CALLBACK (button_size_allocated),
                          button_data);

        g_signal_connect (G_OBJECT (button_data->applet),
                          "destroy",
                          G_CALLBACK (applet_destroyed),
                          button_data);

        gtk_drag_dest_set (GTK_WIDGET(button_data->button), 0, NULL, 0, 0);

        g_signal_connect (G_OBJECT(button_data->button), "drag_motion",
                          G_CALLBACK (button_drag_motion),
                          button_data);
        g_signal_connect (G_OBJECT(button_data->button), "drag_leave",
                          G_CALLBACK (button_drag_leave),
                          button_data);

        button_data->connection = dbus_bus_get (DBUS_BUS_SESSION, NULL);
        if (button_data->connection) {
                hippo_dbus_helper_register_service_tracker(button_data->connection,
                                                           "org.gnome.BigBoard",
                                                           &bigboard_tracker,
                                                           signal_handlers,
                                                           button_data);
                button_data->bb_proxy =
                        hippo_dbus_proxy_new (button_data->connection,
                                              "org.gnome.BigBoard",
                                              "/bigboard/panel",
                                              "org.gnome.BigBoard.Panel");
        }

        gtk_widget_show_all (button_data->button);

        return button_data;
}

static gboolean
bigboard_button_applet_fill (PanelApplet *applet)
{
        ButtonData *button_data;

        panel_applet_set_flags (applet, PANEL_APPLET_EXPAND_MINOR);

        button_data = bigboard_button_add_to_widget (GTK_WIDGET (applet));

        update_size (button_data,
                     panel_applet_get_size (applet));

        update_orientation (button_data,
                            panel_applet_get_orient (applet));

        /* FIXME: Update this comment. */
        /* we have to bind change_orient before we do applet_widget_add
           since we need to get an initial change_orient signal to set our
           initial oriantation, and we get that during the _add call */
        g_signal_connect (G_OBJECT (button_data->applet),
                          "change_orient",
                          G_CALLBACK (applet_change_orient),
                          button_data);

        panel_applet_set_background_widget (PANEL_APPLET (button_data->applet),
                                            GTK_WIDGET (button_data->applet));

        panel_applet_setup_menu_from_file (PANEL_APPLET (button_data->applet),
                                           NULL,
                                           "GNOME_OnlineDesktop_BigBoardButtonApplet.xml",
                                           NULL,
                                           bigboard_button_menu_verbs,
                                           button_data);

        gtk_widget_show_all (button_data->applet);

        return TRUE;
}

static void
display_about_dialog (BonoboUIComponent *uic,
                      ButtonData        *button_data,
                      const gchar       *verbname)
{
        static const gchar *authors[] = {
                "Havoc Pennington <hp@redhat.com>",
                NULL
        };
        static const char *documenters[] = {
                NULL
        };

        /* Translator credits */
        const char *translator_credits = _("translator-credits");

        wncklet_display_about (button_data->applet, &button_data->about_dialog,
                               _("Show Sidebar Button"),
                               "Copyright \xc2\xa9 2002 Red Hat, Inc.",
                               _("This button lets you toggle the sidebar on and off."),
                               authors,
                               documenters,
                               translator_credits,
                               ICON_NAME,
                               "sidebar-button",
                               "sidebar-button");
}


static void
display_help_dialog (BonoboUIComponent *uic,
                     ButtonData        *button_data,
                     const gchar       *verbname)
{
        /* FIXME */
#if 0
        wncklet_display_help (button_data->applet, "user-guide",
                              "user-guide.xml", "gospanel-564");
#else
        display_about_dialog(uic, button_data, verbname);
#endif
}

static void
button_toggled_callback (GtkWidget       *button,
                         ButtonData      *button_data)
{
        if (button_data->bb_proxy) {
                if (gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (button)))
                        hippo_dbus_proxy_VOID__VOID (button_data->bb_proxy,
                                                     "Expand");
                else
                        hippo_dbus_proxy_VOID__VOID (button_data->bb_proxy,
                                                     "Unexpand");
        }
        
        update_button_display (button_data);
}

static gboolean
online_desktop_factory (PanelApplet *applet,
                        const char  *iid,
                        gpointer     data)
{
        gboolean retval = FALSE;

        if (!strcmp (iid, "OAFIID:GNOME_OnlineDesktop_BigBoardButtonApplet"))
                retval = bigboard_button_applet_fill (applet);

        return retval;
}


#if 1

#if APPLET_INPROCESS
PANEL_APPLET_BONOBO_SHLIB_FACTORY ("OAFIID:GNOME_OnlineDesktop_Factory",
				   PANEL_TYPE_APPLET,
				   "BigBoardApplets",
				   online_desktop_factory,
				   NULL);
#else
PANEL_APPLET_BONOBO_FACTORY ("OAFIID:GNOME_OnlineDesktop_Factory",
                             PANEL_TYPE_APPLET,
                             "OnlineDesktopApplets",
                             "0",
                             online_desktop_factory,
                             NULL);
#endif

#else /* main() test mode */

#if APPLET_INPROCESS
#error "You won't be able to test if you build a shared lib"
#endif

int
main (int argc, char **argv)
{
        GtkWidget *window;

        gtk_init (&argc, &argv);

        window = gtk_window_new (GTK_WINDOW_TOPLEVEL);

        bigboard_button_add_to_widget (window);

        g_signal_connect (G_OBJECT (window),
                          "destroy",
                          G_CALLBACK (gtk_main_quit),
                          NULL);

        gtk_widget_show (window);

        gtk_main ();

        return 0;
}
#endif /* main() test mode */