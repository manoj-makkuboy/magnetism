/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-netflix-movie.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_netflix_movie_init                (HippoBlockNetflixMovie       *block);
static void      hippo_block_netflix_movie_class_init          (HippoBlockNetflixMovieClass  *klass);

static void      hippo_block_netflix_movie_dispose             (GObject              *object);
static void      hippo_block_netflix_movie_finalize            (GObject              *object);

static gboolean  hippo_block_netflix_movie_update_from_xml     (HippoBlock           *block,
                                                                HippoDataCache       *cache,
                                                                LmMessageNode        *node);

static void hippo_block_netflix_movie_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_block_netflix_movie_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);

struct _HippoBlockNetflixMovie {
    HippoBlockAbstractPerson      parent;
    char *image_url;
};

struct _HippoBlockNetflixMovieClass {
    HippoBlockAbstractPersonClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
};

G_DEFINE_TYPE(HippoBlockNetflixMovie, hippo_block_netflix_movie, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_netflix_movie_init(HippoBlockNetflixMovie *block_netflix)
{
}

static void
hippo_block_netflix_movie_class_init(HippoBlockNetflixMovieClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_netflix_movie_set_property;
    object_class->get_property = hippo_block_netflix_movie_get_property;

    object_class->dispose = hippo_block_netflix_movie_dispose;
    object_class->finalize = hippo_block_netflix_movie_finalize;

    block_class->update_from_xml = hippo_block_netflix_movie_update_from_xml;    
}

static void
hippo_block_netflix_movie_dispose(GObject *object)
{
    /* HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object); */
    
    G_OBJECT_CLASS(hippo_block_netflix_movie_parent_class)->dispose(object);
}

static void
hippo_block_netflix_movie_finalize(GObject *object)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object);

    g_free(block_netflix->image_url);

    G_OBJECT_CLASS(hippo_block_netflix_movie_parent_class)->finalize(object);
}

static void
hippo_block_netflix_movie_set_property(GObject         *object,
                                         guint            prop_id,
                                         const GValue    *value,
                                         GParamSpec      *pspec)
{
    /* HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object); */

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_netflix_movie_get_property(GObject         *object,
                                         guint            prop_id,
                                         GValue          *value,
                                         GParamSpec      *pspec)
{
    /* HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(object); */

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_block_netflix_movie_update_from_xml (HippoBlock           *block,
                                           HippoDataCache       *cache,
                                           LmMessageNode        *node)
{
    HippoBlockNetflixMovie *block_netflix = HIPPO_BLOCK_NETFLIX_MOVIE(block);
    LmMessageNode *netflix_node, *queue_node;
    const char *image_url;

    if (!HIPPO_BLOCK_CLASS(hippo_block_netflix_movie_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "netflixMovie", HIPPO_SPLIT_NODE, &netflix_node,
                         NULL))
        return FALSE;


    if (!hippo_xml_split(cache, netflix_node, NULL,
                         "queue", HIPPO_SPLIT_NODE, &queue_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, queue_node, NULL,
                                                 "imageUrl", HIPPO_SPLIT_STRING, &image_url,
                         NULL))
        return FALSE;
        
        block_netflix->image_url = g_strdup(image_url);
    
    return TRUE;
}

const char *
hippo_block_netflix_movie_get_image_url (HippoBlockNetflixMovie *netflix)
{
        return netflix->image_url;
}
