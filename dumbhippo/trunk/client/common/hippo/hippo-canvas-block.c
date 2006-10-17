/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-post.h"
#include "hippo-canvas-block-group-chat.h"
#include "hippo-canvas-block-group-member.h"
#include "hippo-canvas-block-music-person.h"
#include "hippo-canvas-entity-photo.h"
#include "hippo-canvas-entity-name.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-gradient.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-image-button.h>
#include "hippo-actions.h"

static void      hippo_canvas_block_init                (HippoCanvasBlock       *block);
static void      hippo_canvas_block_class_init          (HippoCanvasBlockClass  *klass);
static void      hippo_canvas_block_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_dispose             (GObject                *object);
static void      hippo_canvas_block_finalize            (GObject                *object);

static void hippo_canvas_block_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_block_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);
static GObject* hippo_canvas_block_constructor (GType                  type,
                                                guint                  n_construct_properties,
                                                GObjectConstructParam *construct_properties);


/* Box methods */
static void hippo_canvas_block_hovering_changed (HippoCanvasBox *box,
                                                 gboolean        hovering);

/* our own methods */

static void hippo_canvas_block_set_block_impl (HippoCanvasBlock *canvas_block,
                                               HippoBlock       *block);

static void hippo_canvas_block_expand_impl    (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_unexpand_impl  (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_hush_impl      (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_unhush_impl    (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_set_expanded   (HippoCanvasBlock *canvas_block,
                                               gboolean          value);
static void hippo_canvas_block_set_hushed     (HippoCanvasBlock *canvas_block,
                                               gboolean          value);

/* Callbacks */
static void on_close_activated                (HippoCanvasItem  *button_or_link,
                                               HippoCanvasBlock *canvas_block);
static void on_hush_activated                 (HippoCanvasItem  *button_or_link,
                                               HippoCanvasBlock *canvas_block);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_BLOCK,
    PROP_ACTIONS
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlock, hippo_canvas_block, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_iface_init));

static void
on_title_activated(HippoCanvasLink *title_item,
                   void            *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockClass *klass = HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block);

    if (klass->title_activated != NULL) {
        (* klass->title_activated) (canvas_block);
    }
}

static void
hippo_canvas_block_init(HippoCanvasBlock *block)
{
    block->expandable = TRUE;
    
    HIPPO_CANVAS_BOX(block)->border_left = 1;
    HIPPO_CANVAS_BOX(block)->border_right = 1;
    HIPPO_CANVAS_BOX(block)->border_top = 1;
    HIPPO_CANVAS_BOX(block)->border_bottom = 1;
    HIPPO_CANVAS_BOX(block)->border_color_rgba = 0xffffffff;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_class_init(HippoCanvasBlockClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    object_class->set_property = hippo_canvas_block_set_property;
    object_class->get_property = hippo_canvas_block_get_property;
    object_class->constructor = hippo_canvas_block_constructor;

    object_class->dispose = hippo_canvas_block_dispose;
    object_class->finalize = hippo_canvas_block_finalize;

    box_class->hovering_changed = hippo_canvas_block_hovering_changed;
    
    klass->set_block = hippo_canvas_block_set_block_impl;
    klass->expand = hippo_canvas_block_expand_impl;
    klass->unexpand = hippo_canvas_block_unexpand_impl;
    klass->hush = hippo_canvas_block_hush_impl;
    klass->unhush = hippo_canvas_block_unhush_impl;
    
    g_object_class_install_property(object_class,
                                    PROP_BLOCK,
                                    g_param_spec_object("block",
                                                        _("Block"),
                                                        _("Block to display"),
                                                        HIPPO_TYPE_BLOCK,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY)); 
}

static void
update_time(HippoCanvasBlock *canvas_block)
{
    gint64 server_time_now;
    char *when;

    if (canvas_block->block == NULL)
        return;

    /* Since we do this every minute it's important to keep it relatively cheap,
     * in particular we rely on setting the text on the canvas item to short-circuit
     * and not create X server traffic if there's no change
     */
    
    server_time_now = hippo_current_time_ms() + hippo_actions_get_server_time_offset(canvas_block->actions);
    
    when = hippo_format_time_ago(server_time_now / 1000, hippo_block_get_timestamp(canvas_block->block) / 1000);
    
    g_object_set(G_OBJECT(canvas_block->age_item),
                 "text", when,
                 NULL);
    
    g_free(when);
}

static void
on_minute_ticked(HippoActions     *actions,
                 HippoCanvasBlock *canvas_block)
{
    update_time(canvas_block);
}

static void
set_actions(HippoCanvasBlock *block,
            HippoActions     *actions)
{
    if (actions == block->actions)
        return;
    
    if (block->actions) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(block->actions),
                                             G_CALLBACK(on_minute_ticked),
                                             block);
        
        g_object_unref(block->actions);
        block->actions = NULL;
    }

    if (actions) {
        block->actions = actions;
        g_object_ref(block->actions);
        g_signal_connect(G_OBJECT(block->actions),
                         "minute-ticked",
                         G_CALLBACK(on_minute_ticked),
                         block);
    }

    g_object_notify(G_OBJECT(block), "actions");
}

static void
hippo_canvas_block_dispose(GObject *object)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object);

    hippo_canvas_block_set_block(block, NULL);

    set_actions(block, NULL);
    
    block->age_item = NULL;

    G_OBJECT_CLASS(hippo_canvas_block_parent_class)->dispose(object);
}

static void
hippo_canvas_block_finalize(GObject *object)
{
    /* HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object); */

    G_OBJECT_CLASS(hippo_canvas_block_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_block_new(HippoBlockType type,
                       HippoActions  *actions)
{
    HippoCanvasBlock *block;
    GType object_type;

    object_type = HIPPO_TYPE_CANVAS_BLOCK;
    switch (type) {
    case HIPPO_BLOCK_TYPE_UNKNOWN:
        object_type = HIPPO_TYPE_CANVAS_BLOCK;
        break;
    case HIPPO_BLOCK_TYPE_POST:
        object_type = HIPPO_TYPE_CANVAS_BLOCK_POST;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_CHAT:
        object_type = HIPPO_TYPE_CANVAS_BLOCK_GROUP_CHAT;
        break;
    case HIPPO_BLOCK_TYPE_MUSIC_PERSON:
        object_type = HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON;
        break;
    case HIPPO_BLOCK_TYPE_GROUP_MEMBER:
        object_type = HIPPO_TYPE_CANVAS_BLOCK_GROUP_MEMBER;
        break;
    }

    block = g_object_new(object_type,
                         "actions", actions,
                         NULL);

    return HIPPO_CANVAS_ITEM(block);
}

static void
hippo_canvas_block_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasBlock *block;

    block = HIPPO_CANVAS_BLOCK(object);

    switch (prop_id) {
    case PROP_BLOCK:
        {
            HippoBlock *new_block = (HippoBlock*) g_value_get_object(value);
            hippo_canvas_block_set_block(block, new_block);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            set_actions(block, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasBlock *block;

    block = HIPPO_CANVAS_BLOCK (object);

    switch (prop_id) {
    case PROP_BLOCK:
        g_value_set_object(value, (GObject*) block->block);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) block->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_constructor (GType                  type,
                                guint                  n_construct_properties,
                                GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_block_parent_class)->constructor(type,
                                                                                   n_construct_properties,
                                                                                   construct_properties);

    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object);
    
    HippoCanvasItem *item;
    HippoCanvasBox *box;
    HippoCanvasBox *box2;
    HippoCanvasBox *box3;
    HippoCanvasBox *left_column;
    HippoCanvasBox *right_column;

    box = g_object_new(HIPPO_TYPE_CANVAS_GRADIENT,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "start-color", 0xf3f3f3ff,
                       "end-color", 0xdededeff,
                       "padding", 4,
                       "font", "11px",
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block),
                            HIPPO_CANVAS_ITEM(box), HIPPO_PACK_EXPAND);

    /* Create left column for title/description */
    
    left_column = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                               "orientation", HIPPO_ORIENTATION_VERTICAL,
                               "xalign", HIPPO_ALIGNMENT_FILL,
                               "yalign", HIPPO_ALIGNMENT_START,
                               NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(left_column), HIPPO_PACK_EXPAND);

    /* create right column for from/stats */
    
    right_column = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                "orientation", HIPPO_ORIENTATION_VERTICAL,
                                "xalign", HIPPO_ALIGNMENT_FILL,                                
                                "yalign", HIPPO_ALIGNMENT_START,
                                "padding-left", 8,
                                NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(right_column), HIPPO_PACK_END);

    /* Fill in left column */

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "font", "Bold 12px",
                       NULL);
    hippo_canvas_box_append(left_column, HIPPO_CANVAS_ITEM(box), 0);
    
    block->heading_text_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                            "text", NULL,
                                            "xalign", HIPPO_ALIGNMENT_START,
                                            "yalign", HIPPO_ALIGNMENT_START,
                                            NULL);
    hippo_canvas_box_append(box, block->heading_text_item, 0);
    
    block->title_link_item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                                          "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                          "xalign", HIPPO_ALIGNMENT_START,
                                          "yalign", HIPPO_ALIGNMENT_START,
                                          "text", NULL,
                                          NULL);
    hippo_canvas_box_append(box, block->title_link_item, 0);

    g_signal_connect(G_OBJECT(block->title_link_item), "activated",
                     G_CALLBACK(on_title_activated), block);

    
    block->content_container_item = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                                 NULL);
    hippo_canvas_box_append(left_column, block->content_container_item, HIPPO_PACK_EXPAND);

    
    /* Fill in right column */
    block->close_controls_parent = right_column;
    block->close_controls = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                         "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                         "xalign", HIPPO_ALIGNMENT_END,
                                         "yalign", HIPPO_ALIGNMENT_START,
                                         "border-bottom", 4,
                                         NULL);
    hippo_canvas_box_append(block->close_controls_parent, block->close_controls, 0);
    /* we start out !expanded */
    hippo_canvas_box_set_child_visible(block->close_controls_parent, block->close_controls,
                                       FALSE);

    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text", "CLOSE",
                        "tooltip", "Shrink this item",
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block->close_controls), item, 0);

    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_close_activated), block);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "normal-image-name", "blue_x",
                        "border-left", 4,
                        "tooltip", "Shrink this item",
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block->close_controls), item, 0);

    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_close_activated), block);
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "xalign", HIPPO_ALIGNMENT_FILL,
                       "yalign", HIPPO_ALIGNMENT_START,
                       NULL);
    hippo_canvas_box_append(right_column, HIPPO_CANVAS_ITEM(box), HIPPO_PACK_EXPAND);
    
    /* Create photo */
    block->headshot_item = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_PHOTO,
                                        "actions", block->actions,
                                        "photo-size", 30,
                                        "xalign", HIPPO_ALIGNMENT_END,
                                        "yalign", HIPPO_ALIGNMENT_START,
                                        "border-left", 6,
                                        NULL);
    hippo_canvas_box_append(box, block->headshot_item, HIPPO_PACK_END);

    box2 = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "xalign", HIPPO_ALIGNMENT_FILL,
                        "yalign", HIPPO_ALIGNMENT_START,
                        "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                       NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(box2), HIPPO_PACK_EXPAND);
    
    box3 = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "font", "Italic 12px",
                        "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                        "xalign", HIPPO_ALIGNMENT_FILL,
                        "yalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box2, HIPPO_CANVAS_ITEM(box3), HIPPO_PACK_EXPAND);
    
    block->name_item = g_object_new(HIPPO_TYPE_CANVAS_ENTITY_NAME,
                                    "actions", block->actions,
                                    "xalign", HIPPO_ALIGNMENT_FILL,
                                    "yalign", HIPPO_ALIGNMENT_START,
                                    "border-right", 8,
                                    NULL);
    hippo_canvas_box_append(box3, block->name_item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "from ",
                        "yalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box3, item, HIPPO_PACK_END);
    
    box3 = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "xalign", HIPPO_ALIGNMENT_END,
                       "yalign", HIPPO_ALIGNMENT_START,                        
                       NULL);
    hippo_canvas_box_append(box2, HIPPO_CANVAS_ITEM(box3), 0);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text", "Hush",
                        "color-cascade", HIPPO_CASCADE_MODE_NONE,
                        "tooltip", "Stop showing me this item",
                        NULL);
    block->toggle_hush_link = item;
    hippo_canvas_box_append(box3, item, HIPPO_PACK_END);

    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hush_activated), block);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", " | ",
                        NULL);
    hippo_canvas_box_append(box3, item, HIPPO_PACK_END);
    
    block->age_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                   NULL);
    hippo_canvas_box_append(box3, block->age_item, HIPPO_PACK_END);

    return object;
}

static gboolean
expand_if_still_hovering_timeout(void *data)
{
    HippoCanvasBlock *canvas_block;
    HippoCanvasBox *box;

    canvas_block = HIPPO_CANVAS_BLOCK(data);
    box = HIPPO_CANVAS_BOX(data);

    /* don't expand hushed blocks on hover */
    if (box->hovering &&
        !canvas_block->maybe_expand_timeout_canceled &&
        !canvas_block->hushed)
        hippo_canvas_block_set_expanded(canvas_block, TRUE);

    canvas_block->maybe_expand_timeout_active = FALSE;
    g_object_unref(G_OBJECT(canvas_block));
    
    /* Remove self */
    return FALSE;
}

static void
hippo_canvas_block_hovering_changed(HippoCanvasBox *box,
                                    gboolean        hovering)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(box);

    if (hovering && !canvas_block->maybe_expand_timeout_active) {
        /* We have the timeout hold a ref, this lets us store
         * the active/canceled 1-bit flags instead of a timeout id
         * and avoid worrying about the timeout on finalize
         */
        g_object_ref(G_OBJECT(canvas_block));
        canvas_block->maybe_expand_timeout_active = TRUE;
        canvas_block->maybe_expand_timeout_canceled = FALSE;
        g_timeout_add(750, expand_if_still_hovering_timeout, canvas_block);
    }
}

static void
on_block_clicked_count_changed(HippoBlock *block,
                               GParamSpec *arg, /* null when we invoke callback manually */
                               void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockClass *klass;

    klass = HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block);
    if (klass->clicked_count_changed)
        (* klass->clicked_count_changed) (canvas_block);
}

static void
on_block_timestamp_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when we invoke callback manually */
                           void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);

    if (block == NULL) /* should be impossible */
        return;

    update_time(canvas_block);
    
    /* FIXME update all the other info */
}

static void
on_block_ignored_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when we invoke callback manually */
                         void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    gboolean hushed;
    
    if (block == NULL) /* should be impossible */
        return;

    hushed = FALSE;
    g_object_get(G_OBJECT(block), "ignored", &hushed, NULL);

    if (hushed != canvas_block->hushed) {
        hippo_canvas_block_set_hushed(canvas_block, hushed);
        
        /* automatically collapse when you hush and expand on unhush */
        hippo_canvas_block_set_expanded(canvas_block, !hushed);
    }
}

static void
hippo_canvas_block_set_block_impl(HippoCanvasBlock *canvas_block,
                                  HippoBlock       *new_block)
{
#if 0
    g_debug("setting block on canvas block to %s",
            new_block ? hippo_block_get_guid(new_block) : "null");
#endif
    
    if (new_block != canvas_block->block) {
        if (new_block) {
            if (canvas_block->required_type != HIPPO_BLOCK_TYPE_UNKNOWN &&
                hippo_block_get_block_type(new_block) != canvas_block->required_type) {
                g_warning("Trying to set block type %d on canvas block type %s",
                          hippo_block_get_block_type(new_block),
                          g_type_name_from_instance((GTypeInstance*)canvas_block));
                return;
            }
            
            g_object_ref(new_block);
            g_signal_connect(G_OBJECT(new_block), "notify::timestamp",
                             G_CALLBACK(on_block_timestamp_changed),
                             canvas_block);
            g_signal_connect(G_OBJECT(new_block), "notify::clicked-count",
                             G_CALLBACK(on_block_clicked_count_changed),
                             canvas_block);
            g_signal_connect(G_OBJECT(new_block), "notify::ignored",
                             G_CALLBACK(on_block_ignored_changed),
                             canvas_block);
        }
        if (canvas_block->block) {
            g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                                 G_CALLBACK(on_block_timestamp_changed),
                                                 canvas_block);
            g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                                 G_CALLBACK(on_block_clicked_count_changed),
                                                 canvas_block);
            g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                                 G_CALLBACK(on_block_ignored_changed),
                                                 canvas_block);
            g_object_unref(canvas_block->block);
        }
        canvas_block->block = new_block;

        if (new_block) {
            on_block_timestamp_changed(new_block, NULL, canvas_block);
            on_block_clicked_count_changed(new_block, NULL, canvas_block);
            on_block_ignored_changed(new_block, NULL, canvas_block);
        }
        
        g_object_notify(G_OBJECT(canvas_block), "block");
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(canvas_block));
    }
}

static void
hippo_canvas_block_expand_impl(HippoCanvasBlock *canvas_block)
{
    hippo_canvas_box_set_child_visible(canvas_block->close_controls_parent, canvas_block->close_controls,
                                       TRUE);
}

static void
hippo_canvas_block_unexpand_impl(HippoCanvasBlock *canvas_block)
{
    hippo_canvas_box_set_child_visible(canvas_block->close_controls_parent, canvas_block->close_controls,
                                       FALSE);
}

static void
hippo_canvas_block_hush_impl(HippoCanvasBlock *canvas_block)
{
    g_object_set(G_OBJECT(canvas_block),
                 "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                 NULL);
    g_object_set(G_OBJECT(canvas_block->toggle_hush_link),
                 "text", "Unhush",
                 "tooltip", "Show me changes to this item",
                 NULL);
}

static void
hippo_canvas_block_unhush_impl(HippoCanvasBlock *canvas_block)
{
    g_object_set(G_OBJECT(canvas_block),
                 "color-set", FALSE,
                 NULL);
    g_object_set(G_OBJECT(canvas_block->toggle_hush_link),
                 "text", "Hush",
                 "tooltip", "Stop showing me this item",
                 NULL);
}

static void
hippo_canvas_block_set_hushed(HippoCanvasBlock *canvas_block,
                              gboolean          value)
{
    value = value != FALSE;

    if (value == canvas_block->hushed)
        return;

    if (value) {
        canvas_block->hushed = TRUE;
        HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block)->hush(canvas_block);
    } else {
        HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block)->unhush(canvas_block);
        canvas_block->hushed = FALSE;
    }
}

static void
hippo_canvas_block_set_expanded(HippoCanvasBlock *canvas_block,
                                gboolean          value)
{
    value = value != FALSE;

    if (value == canvas_block->expanded)
        return;

    if (value) {
        canvas_block->expanded = TRUE;
        if (canvas_block->expandable)
            HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block)->expand(canvas_block);
    } else {
        if (canvas_block->expandable)
            HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block)->unexpand(canvas_block);
        canvas_block->expanded = FALSE;
    }
}

static void
on_close_activated(HippoCanvasItem  *button_or_link,
                   HippoCanvasBlock *canvas_block)
{
    canvas_block->maybe_expand_timeout_canceled = TRUE;
    hippo_canvas_block_set_expanded(canvas_block, FALSE);
}

static void
on_hush_activated(HippoCanvasItem  *button_or_link,
                  HippoCanvasBlock *canvas_block)
{
    if (canvas_block->actions && canvas_block->block) {
        if (canvas_block->hushed)
            hippo_actions_unhush_block(canvas_block->actions,
                                       canvas_block->block);
        else
            hippo_actions_hush_block(canvas_block->actions,
                                     canvas_block->block);
    }
}

void
hippo_canvas_block_set_block(HippoCanvasBlock *canvas_block,
                             HippoBlock       *block)
{
    HippoCanvasBlockClass *klass;

    if (canvas_block->block == block)
        return;
    
    klass = HIPPO_CANVAS_BLOCK_GET_CLASS(canvas_block);
    
    (* klass->set_block) (canvas_block, block);
}

void
hippo_canvas_block_set_heading (HippoCanvasBlock *canvas_block,
                                const char       *heading)
{
    g_object_set(G_OBJECT(canvas_block->heading_text_item),
                 "text", heading,
                 NULL);
}

void
hippo_canvas_block_set_title(HippoCanvasBlock *canvas_block,
                             const char       *text)
{
    /* keep in mind that title may be NULL */
    g_object_set(G_OBJECT(canvas_block->title_link_item),
                 "text", text,
                 NULL);
}

void
hippo_canvas_block_set_content(HippoCanvasBlock *canvas_block,
                               HippoCanvasItem  *content_item)
{
    if (content_item)
        g_object_ref(content_item); /* in case we remove it below */
    
    hippo_canvas_box_remove_all(HIPPO_CANVAS_BOX(canvas_block->content_container_item));

    if (content_item) {
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(canvas_block->content_container_item),
                                content_item, HIPPO_PACK_EXPAND);
        g_object_unref(content_item);
    }
}

void
hippo_canvas_block_set_sender(HippoCanvasBlock *canvas_block,
                              const char       *entity_guid)
{    
    if (entity_guid) {
        HippoEntity *entity;

        if (canvas_block->actions == NULL) {
            g_warning("setting block sender before setting actions");
            return;
        }
        
        entity = hippo_actions_lookup_entity(canvas_block->actions,
                                             entity_guid);
        if (entity == NULL) {
            g_warning("needed entity is unknown %s", entity_guid);
            return;
        }
        
        g_object_set(G_OBJECT(canvas_block->headshot_item),
                     "entity", entity,
                     NULL);
        
        g_object_set(G_OBJECT(canvas_block->name_item),
                     "entity", entity,
                     NULL);
    } else {        
        g_object_set(G_OBJECT(canvas_block->headshot_item),
                     "entity", NULL,
                     NULL);
        g_object_set(G_OBJECT(canvas_block->name_item),
                     "entity", NULL,
                     NULL);
    }
}

HippoActions*
hippo_canvas_block_get_actions(HippoCanvasBlock *canvas_block)
{
    return canvas_block->actions;
}
