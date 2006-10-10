/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music-person.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>

static void      hippo_canvas_block_music_person_init                (HippoCanvasBlockMusicPerson       *block);
static void      hippo_canvas_block_music_person_class_init          (HippoCanvasBlockMusicPersonClass  *klass);
static void      hippo_canvas_block_music_person_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_music_person_dispose             (GObject                *object);
static void      hippo_canvas_block_music_person_finalize            (GObject                *object);

static void hippo_canvas_block_music_person_set_property (GObject      *object,
                                                          guint         prop_id,
                                                          const GValue *value,
                                                          GParamSpec   *pspec);
static void hippo_canvas_block_music_person_get_property (GObject      *object,
                                                          guint         prop_id,
                                                          GValue       *value,
                                                          GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_music_person_set_block       (HippoCanvasBlock *canvas_block,
                                                             HippoBlock       *block);

static void hippo_canvas_block_music_person_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_music_person_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_music_person_unexpand (HippoCanvasBlock *canvas_block);

/* internals */
static void set_person (HippoCanvasBlockMusicPerson *block_music_person,
                        HippoPerson                 *person);


struct _HippoCanvasBlockMusicPerson {
    HippoCanvasBlock canvas_block;
    HippoPerson *person;
};

struct _HippoCanvasBlockMusicPersonClass {
    HippoCanvasBlockClass parent_class;

};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockMusicPerson, hippo_canvas_block_music_person, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_music_person_iface_init));

static void
hippo_canvas_block_music_person_init(HippoCanvasBlockMusicPerson *block_music_person)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_music_person);
    HippoCanvasBox *box;

    block->required_type = HIPPO_BLOCK_TYPE_MUSIC_PERSON;
    block->expandable = FALSE; /* currently we have nothing to show on expand */
    
    hippo_canvas_block_set_heading(block, _("Music Radar: "));

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       NULL);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_music_person_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_music_person_class_init(HippoCanvasBlockMusicPersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_music_person_set_property;
    object_class->get_property = hippo_canvas_block_music_person_get_property;

    object_class->dispose = hippo_canvas_block_music_person_dispose;
    object_class->finalize = hippo_canvas_block_music_person_finalize;

    canvas_block_class->set_block = hippo_canvas_block_music_person_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_music_person_title_activated;
    canvas_block_class->expand = hippo_canvas_block_music_person_expand;
    canvas_block_class->unexpand = hippo_canvas_block_music_person_unexpand;
}

static void
hippo_canvas_block_music_person_dispose(GObject *object)
{
    HippoCanvasBlockMusicPerson *block_music_person;

    block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object);

    set_person(block_music_person, NULL);
    
    G_OBJECT_CLASS(hippo_canvas_block_music_person_parent_class)->dispose(object);
}

static void
hippo_canvas_block_music_person_finalize(GObject *object)
{
    /* HippoCanvasBlockMusicPerson *block = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_music_person_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_block_music_person_new(void)
{
    HippoCanvasBlockMusicPerson *block_music_person;

    block_music_person = g_object_new(HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON, NULL);

    return HIPPO_CANVAS_ITEM(block_music_person);
}

static void
hippo_canvas_block_music_person_set_property(GObject         *object,
                                             guint            prop_id,
                                             const GValue    *value,
                                             GParamSpec      *pspec)
{
    HippoCanvasBlockMusicPerson *block_music_person;

    block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_music_person_get_property(GObject         *object,
                                             guint            prop_id,
                                             GValue          *value,
                                             GParamSpec      *pspec)
{
    HippoCanvasBlockMusicPerson *block_music_person;

    block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
on_current_track_changed(HippoPerson *person,
                         GParamSpec *arg, /* null when first calling this */
                         HippoCanvasBlockMusicPerson *block_music_person)
{
    HippoTrack *track;
    
    track = NULL;
    g_object_get(G_OBJECT(person), "current-track", &track, NULL);

    if (track) {
        const char *artist = NULL;
        const char *name = NULL;
        char *title;
        
        g_object_get(G_OBJECT(track), "artist", &artist, "name", &name,
                     NULL);

        title = g_strdup_printf("%s (%s)", name, artist);
        hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(block_music_person),
                                     title);
        g_free(title);
    } else {
        hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(block_music_person),
                                     "");
    }
}

static void
set_person(HippoCanvasBlockMusicPerson *block_music_person,
           HippoPerson                 *person)
{
    if (person == block_music_person->person)
        return;
    
    if (block_music_person->person) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(block_music_person->person),
                                             G_CALLBACK(on_current_track_changed),
                                             block_music_person);
        g_object_unref(block_music_person->person);
        block_music_person->person = NULL;
    }

    if (person) {
        block_music_person->person = person;
        g_object_ref(G_OBJECT(person));
        g_signal_connect(G_OBJECT(person), "notify::current-track",
                         G_CALLBACK(on_current_track_changed),
                         block_music_person);

        on_current_track_changed(person, NULL, block_music_person);
    }

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_music_person),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockMusicPerson *block_music_person)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    set_person(block_music_person, person);
}

static void
hippo_canvas_block_music_person_set_block(HippoCanvasBlock *canvas_block,
                                          HippoBlock       *block)
{
    /* g_debug("canvas-block-music-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
        set_person(HIPPO_CANVAS_BLOCK_MUSIC_PERSON(canvas_block), NULL);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_person_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_MUSIC_PERSON(canvas_block));
    }
}

static void
hippo_canvas_block_music_person_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoPerson *person;

    if (canvas_block->block == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    person = NULL;
    g_object_get(G_OBJECT(canvas_block->block),
                 "user", &person,
                 NULL);

    if (person == NULL)
        return;
    
    hippo_actions_visit_entity(actions, HIPPO_ENTITY(person));
}

static void
hippo_canvas_block_music_person_expand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockMusicPerson *block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_person_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_music_person_unexpand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockMusicPerson *block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_person_parent_class)->unexpand(canvas_block);
}
