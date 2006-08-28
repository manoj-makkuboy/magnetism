/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-text.h"
#include "hippo-canvas-box.h"
#include <pango/pangocairo.h>

static void      hippo_canvas_text_init                (HippoCanvasText       *text);
static void      hippo_canvas_text_class_init          (HippoCanvasTextClass  *klass);
static void      hippo_canvas_text_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_text_finalize            (GObject                *object);

static void hippo_canvas_text_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_text_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_text_paint              (HippoCanvasItem *item,
                                                      cairo_t         *cr);
static int      hippo_canvas_text_get_width_request  (HippoCanvasItem *item);
static int      hippo_canvas_text_get_height_request (HippoCanvasItem *item,
                                                      int              for_width);
static gboolean hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                                      HippoEvent      *event);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_TEXT,
    PROP_COLOR,
    PROP_ATTRIBUTES
};

#define DEFAULT_FOREGROUND 0x000000ff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasText, hippo_canvas_text, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_text_iface_init));

static void
hippo_canvas_text_init(HippoCanvasText *text)
{
    text->color_rgba = DEFAULT_FOREGROUND;
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_text_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_text_paint;
    item_class->get_width_request = hippo_canvas_text_get_width_request;
    item_class->get_height_request = hippo_canvas_text_get_height_request;
    item_class->button_press_event = hippo_canvas_text_button_press_event;
}

static void
hippo_canvas_text_class_init(HippoCanvasTextClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_text_set_property;
    object_class->get_property = hippo_canvas_text_get_property;

    object_class->finalize = hippo_canvas_text_finalize;

    g_object_class_install_property(object_class,
                                    PROP_TEXT,
                                    g_param_spec_string("text",
                                                        _("Text"),
                                                        _("Text to display"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_COLOR,
                                    g_param_spec_uint("color",
                                                      _("Foreground Color"),
                                                      _("32-bit RGBA foreground color"),
                                                      0,
                                                      G_MAXUINT,
                                                      DEFAULT_FOREGROUND,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_ATTRIBUTES,
                                    g_param_spec_boxed ("attributes",
                                                        _("Attributes"),
                                                        _("A list of style attributes to apply to the text"),
                                                        PANGO_TYPE_ATTR_LIST,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_text_finalize(GObject *object)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(object);

    g_free(text->text);
    text->text = NULL;

    if (text->attributes) {
        pango_attr_list_unref(text->attributes);
        text->attributes = NULL;
    }
    
    G_OBJECT_CLASS(hippo_canvas_text_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_text_new(void)
{
    HippoCanvasText *text = g_object_new(HIPPO_TYPE_CANVAS_TEXT, NULL);

    
    
    return HIPPO_CANVAS_ITEM(text);
}

static void
hippo_canvas_text_set_property(GObject         *object,
                               guint            prop_id,
                               const GValue    *value,
                               GParamSpec      *pspec)
{
    HippoCanvasText *text;

    text = HIPPO_CANVAS_TEXT(object);

    switch (prop_id) {
    case PROP_TEXT:
        g_free(text->text);
        text->text = g_value_dup_string(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        break;
    case PROP_COLOR:
        text->color_rgba = g_value_get_uint(value);
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(text), 0, 0, -1, -1);
        break;
    case PROP_ATTRIBUTES:
        {
            PangoAttrList *attrs = g_value_get_boxed(value);
            if (attrs)
                pango_attr_list_ref(attrs);
            if (text->attributes)
                pango_attr_list_unref(text->attributes);            
            text->attributes = attrs;
            hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_text_get_property(GObject         *object,
                               guint            prop_id,
                               GValue          *value,
                               GParamSpec      *pspec)
{
    HippoCanvasText *text;

    text = HIPPO_CANVAS_TEXT (object);

    switch (prop_id) {
    case PROP_TEXT:
        g_value_set_string(value, text->text);
        break;
    case PROP_COLOR:
        g_value_set_uint(value, text->color_rgba);
        break;
    case PROP_ATTRIBUTES:
        g_value_set_boxed(value, text->attributes);
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static PangoLayout*
create_layout(HippoCanvasText *text)
{
    HippoCanvasContext *context;
    PangoLayout *layout;
    PangoFontDescription *font;
    
    context = hippo_canvas_box_get_context(HIPPO_CANVAS_BOX(text));
    
    layout = hippo_canvas_context_create_layout(context);
    pango_layout_set_text(layout, text->text, -1);
    font = pango_font_description_from_string("Sans 12");
    pango_layout_set_font_description(layout, font);
    pango_font_description_free(font);

    if (text->text != NULL)
        pango_layout_set_text(layout, text->text, -1);

    if (text->attributes)
        pango_layout_set_attributes(layout, text->attributes);
    
    return layout;
}

static void
hippo_canvas_text_paint(HippoCanvasItem *item,
                        cairo_t         *cr)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);

    /* Draw the background and any child items */
    item_parent_class->paint(item, cr);

    /* draw text on top */
    if ((text->color_rgba & 0xff) != 0 && text->text != NULL) {
        PangoLayout *layout;
        int layout_width, layout_height;
        int allocation_width, allocation_height;
        int x, y, w, h;

        hippo_canvas_item_get_allocation(item, &allocation_width, &allocation_height);
        
        layout = create_layout(text);
        pango_layout_get_size(layout, &layout_width, &layout_height);
        layout_width /= PANGO_SCALE;
        layout_height /= PANGO_SCALE;

        /* Force layout smaller if required, but we don't want to make
         * the layout _wider_ because it breaks alignment, so only do
         * this if required.
         */
        if (layout_width > allocation_width) {
            pango_layout_set_width(layout, allocation_width * PANGO_SCALE);
            pango_layout_get_size(layout, &layout_width, &layout_height);
            layout_width /= PANGO_SCALE;
            layout_height /= PANGO_SCALE;
        }

        x = 0;
        y = 0;
        w = layout_width;
        h = layout_height;
        
        hippo_canvas_box_align(HIPPO_CANVAS_BOX(item), &x, &y, &w, &h);

        /* we can't really "fill" so we fall back to center if we seem to be
         * in fill mode
         */
        if (w > layout_width) {
            x += (w - layout_width) / 2;
        }
        if (h > layout_height) {
            y += (h - layout_height) / 2;
        }
        
        /* Clipping is needed since we have no idea how high the layout is.
         * FIXME It would be better to ellipsize or something instead, though.
         */
        cairo_save(cr);
        cairo_rectangle(cr, 0, 0, allocation_width, allocation_height);
        cairo_clip(cr);
        cairo_move_to (cr, x, y);
        hippo_cairo_set_source_rgba32(cr, text->color_rgba);
        pango_cairo_show_layout(cr, layout);
        cairo_restore(cr);
        
        g_object_unref(layout);
    }
}

static int
hippo_canvas_text_get_width_request(HippoCanvasItem *item)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_width;
    int layout_width;
    
    children_width = item_parent_class->get_width_request(item);

    if (hippo_canvas_box_get_fixed_width(HIPPO_CANVAS_BOX(item)) < 0) {
        PangoLayout *layout = create_layout(text);
        pango_layout_get_size(layout, &layout_width, NULL);
        layout_width /= PANGO_SCALE;
    } else {
        /* keep the fixed width the box will have returned */
        layout_width = children_width;
    }

    return MAX(children_width, layout_width + box->padding_left + box->padding_right);
}

static int
hippo_canvas_text_get_height_request(HippoCanvasItem *item,
                                     int              for_width)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_height;
    PangoLayout *layout;
    int layout_height;
    
    children_height = item_parent_class->get_height_request(item, for_width);

    layout = create_layout(text);
    pango_layout_set_width(layout, for_width * PANGO_SCALE);
    pango_layout_get_size(layout, NULL, &layout_height);
    layout_height /= PANGO_SCALE;
    
    return MAX(layout_height + box->padding_top + box->padding_bottom, children_height);
}

static gboolean
hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(item); */

    return item_parent_class->button_press_event(item, event);
}
