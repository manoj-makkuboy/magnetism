/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-window.h"
#include "hippo-common-marshal.h"

static void     hippo_window_base_init (void                  *klass);

enum {
    LAST_SIGNAL
};
/* static int signals[LAST_SIGNAL]; */

GType
hippo_window_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoWindowClass),
                hippo_window_base_init,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoWindow",
                                      &info, 0);
    }

    return type;
}

static void
hippo_window_base_init(void *klass)
{
    static gboolean initialized = FALSE;

    if (!initialized) {
        /* create signals in here */

        g_object_interface_install_property(klass,
                                            g_param_spec_boolean("app-window",
                                                                 _("App Window"),
                                                                 _("Whether the window should appear in the tasklist"),
                                                                 TRUE,
                                                                 G_PARAM_READABLE | G_PARAM_WRITABLE));

        initialized = TRUE;
    }
}

void
hippo_window_set_contents(HippoWindow     *window,
                          HippoCanvasItem *item)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));
    g_return_if_fail(item == NULL || HIPPO_IS_CANVAS_ITEM(item));

    HIPPO_WINDOW_GET_CLASS(window)->set_contents(window, item);
}

void
hippo_window_set_visible(HippoWindow     *window,
                         gboolean         visible)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_visible(window, visible);
}

void
hippo_window_set_position(HippoWindow     *window,
                          int              x,
                          int              y)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_position(window, x, y);
}

void
hippo_window_set_size(HippoWindow     *window,
                      int              width,
                      int              height)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_size(window, width, height);
}

void
hippo_window_get_position(HippoWindow     *window,
                          int             *x_p,
                          int             *y_p)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->get_position(window, x_p, y_p);
}

void
hippo_window_get_size(HippoWindow     *window,
                      int             *width_p,
                      int             *height_p)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->get_size(window, width_p, height_p);
}

void
hippo_window_set_resizable(HippoWindow      *window,
                           HippoOrientation  orientation,
                           gboolean          value)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_resizable(window, orientation, value);
}

void
hippo_window_begin_move_drag (HippoWindow      *window,
                              HippoEvent       *event)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->begin_move_drag(window, event);
}

void
hippo_window_begin_resize_drag (HippoWindow      *window,
                                HippoSide         side,
                                HippoEvent       *event)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->begin_resize_drag(window, side, event);
}
