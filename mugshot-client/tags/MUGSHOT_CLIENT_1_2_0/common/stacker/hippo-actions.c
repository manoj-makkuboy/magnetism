/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include "hippo-actions.h"
#include "hippo-image-cache.h"
#include "hippo-stack-manager.h"
#include "hippo-quip-window.h"
#include <string.h>


static void      hippo_actions_init                (HippoActions       *actions);
static void      hippo_actions_class_init          (HippoActionsClass  *klass);

static void      hippo_actions_dispose             (GObject            *object);
static void      hippo_actions_finalize            (GObject            *object);

struct _HippoActions {
    GObject parent;
    DDMDataModel *model;
    HippoStackerPlatform *platform;
    HippoStackManager *stack_manager;

    /* We have an image cache for each kind of
     * image, because otherwise we can't really predict
     * cache behavior.
     */
    HippoImageCache *entity_photo_cache;
    HippoImageCache *favicon_cache;
    HippoImageCache *thumbnail_cache; /* photos/videos */
    HippoImageCache *music_thumbnail_cache; /* album art */
    
    guint minute_timeout_id;
};

struct _HippoActionsClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoActions, hippo_actions, G_TYPE_OBJECT);

enum {
    MINUTE_TICKED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static gboolean
minute_tick_timeout(void *data)
{
    HippoActions *actions;

    actions = HIPPO_ACTIONS(data);

    g_signal_emit(G_OBJECT(actions), signals[MINUTE_TICKED], 0);

    return TRUE;
}

static void
hippo_actions_init(HippoActions  *actions)
{
    /* This is so every canvas item displaying a time doesn't have to
     * install its own timeout
     */
    actions->minute_timeout_id = g_timeout_add(60 * 1000, minute_tick_timeout, actions);
}

static void
hippo_actions_class_init(HippoActionsClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = hippo_actions_dispose;
    object_class->finalize = hippo_actions_finalize;

    signals[MINUTE_TICKED] =
        g_signal_new ("minute-ticked",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

static void
hippo_actions_dispose(GObject *object)
{
    HippoActions *actions = HIPPO_ACTIONS(object);

    if (actions->minute_timeout_id) {
        g_source_remove(actions->minute_timeout_id);
        actions->minute_timeout_id = 0;
    }
    
    if (actions->model) {
        g_object_unref(actions->model);
        actions->model = NULL;
    }

    if (actions->platform) {
        g_object_unref(actions->platform);
        actions->platform = NULL;
    }

    actions->stack_manager = NULL;
    
    if (actions->entity_photo_cache) {
        g_object_run_dispose(G_OBJECT(actions->entity_photo_cache));
        g_object_unref(actions->entity_photo_cache);
        actions->entity_photo_cache = NULL;
    }

    if (actions->favicon_cache) {
        g_object_run_dispose(G_OBJECT(actions->favicon_cache));
        g_object_unref(actions->favicon_cache);
        actions->favicon_cache = NULL;
    }
    
    if (actions->thumbnail_cache) {
        g_object_run_dispose(G_OBJECT(actions->thumbnail_cache));
        g_object_unref(actions->thumbnail_cache);
        actions->thumbnail_cache = NULL;
    }
    
    if (actions->music_thumbnail_cache) {
        g_object_run_dispose(G_OBJECT(actions->music_thumbnail_cache));
        g_object_unref(actions->music_thumbnail_cache);
        actions->music_thumbnail_cache = NULL;
    }

    G_OBJECT_CLASS(hippo_actions_parent_class)->dispose(object);
}

static void
hippo_actions_finalize(GObject *object)
{
    /* HippoActions *actions = HIPPO_ACTIONS(object); */

    G_OBJECT_CLASS(hippo_actions_parent_class)->finalize(object);
}

HippoActions*
hippo_actions_new(DDMDataModel             *model,
                  HippoStackerPlatform     *platform,
                  HippoStackManager        *stack_manager)
{
    HippoActions *actions;

    actions = g_object_new(HIPPO_TYPE_ACTIONS,
                           NULL);

    actions->model = g_object_ref(model);
    actions->platform = g_object_ref(platform);
    actions->stack_manager = stack_manager;
    
    return actions;
}

void
hippo_actions_visit_post(HippoActions   *actions,
                         HippoPost      *post)
{
    const char *visit_url;
    
    ddm_data_resource_get(hippo_post_get_resource(post),
                          "visitUrl", DDM_DATA_URL, &visit_url,
                          NULL);

    if (visit_url != NULL)
        hippo_actions_open_url(actions, visit_url);
}

void
hippo_actions_visit_entity(HippoActions    *actions,
                           HippoEntity     *entity)
{
    const char *home_url = hippo_entity_get_home_url(entity);
    hippo_actions_open_url(actions, home_url);
}

static void
image_set_on_canvas_item_func(HippoSurface *surface,
                              void         *data)
{
    HippoCanvasItem *item = HIPPO_CANVAS_ITEM(data);
    
    if (surface != NULL) {
        /* g_debug("Setting image on canvas item, surface=%p %d x %d", surface,
                cairo_image_surface_get_width(hippo_surface_get_surface(surface)),
                cairo_image_surface_get_height(hippo_surface_get_surface(surface))); */
        
        g_object_set(G_OBJECT(item),
                     "image",
                     hippo_surface_get_surface(surface),
                     NULL);
    }

    /* this function held a ref */
    g_object_unref(item);
}

static void
load_image_url_async(HippoActions    *actions,
                     HippoImageCache *cache,
                     const char      *url,
                     HippoCanvasItem *image_item)
{
    g_object_ref(image_item); /* held by the loader func */
    hippo_image_cache_load(cache, url,
                           image_set_on_canvas_item_func,
                           image_item);
}

void
hippo_actions_load_favicon_async(HippoActions    *actions,
                                 const char      *image_url,
                                 HippoCanvasItem *image_item)
{
    if (actions->favicon_cache == NULL) {
        actions->favicon_cache = hippo_image_cache_new(actions->platform);
    }

    /* hippo_object_cache_debug_dump(HIPPO_OBJECT_CACHE(actions->favicon_cache)); */
    
    load_image_url_async(actions, actions->favicon_cache, image_url, image_item);
}

void
hippo_actions_load_thumbnail_async(HippoActions    *actions,
                                   const char      *image_url,
                                   HippoCanvasItem *image_item)
{
    if (actions->thumbnail_cache == NULL) {
        actions->thumbnail_cache = hippo_image_cache_new(actions->platform);
    }

    /* hippo_object_cache_debug_dump(HIPPO_OBJECT_CACHE(actions->thumbnail_cache)); */
    
    load_image_url_async(actions, actions->thumbnail_cache, image_url, image_item);
}

void
hippo_actions_load_music_thumbnail_async(HippoActions    *actions,
                                         const char      *image_url,
                                         HippoCanvasItem *image_item)
{
    if (actions->music_thumbnail_cache == NULL) {
        actions->music_thumbnail_cache = hippo_image_cache_new(actions->platform);
    }

    /* hippo_object_cache_debug_dump(HIPPO_OBJECT_CACHE(actions->music_thumbnail_cache)); */
    
    load_image_url_async(actions, actions->music_thumbnail_cache, image_url, image_item);
}

void
hippo_actions_load_entity_photo_async(HippoActions    *actions,
                                      HippoEntity     *entity,
                                      int              size,
                                      HippoCanvasItem *image_item)
{
    const char *url;
    char *sized;
    
    url = hippo_entity_get_photo_url(entity);
    
    g_debug("Loading photo for entity '%s' url '%s' to a canvas item",
            hippo_entity_get_guid(entity),
            url ? url : "null");
    
    if (url == NULL) {
        /* not gonna succeed in loading this... */
        return;
    }

    sized = hippo_size_photo_url(url, size);

    if (actions->entity_photo_cache == NULL) {
        actions->entity_photo_cache = hippo_image_cache_new(actions->platform);
    }

    /* hippo_object_cache_debug_dump(HIPPO_OBJECT_CACHE(actions->entity_photo_cache)); */
    
    load_image_url_async(actions, actions->entity_photo_cache, sized, image_item);
    
    g_free(sized);
}

gint64
hippo_actions_get_server_time_offset (HippoActions *actions)
{
    return ddm_data_model_get_server_time_offset(actions->model);
}

void
hippo_actions_close_browser(HippoActions    *actions)
{
    hippo_stack_manager_close_browser(actions->stack_manager);
}

void
hippo_actions_close_notification(HippoActions    *actions)
{
    hippo_stack_manager_close_notification(actions->stack_manager);
}

void
hippo_actions_hush_notification(HippoActions    *actions)
{
    hippo_stack_manager_hush(actions->stack_manager);
}

void
hippo_actions_expand_notification(HippoActions *actions)
{
    hippo_stack_manager_close_notification(actions->stack_manager);
    hippo_stack_manager_show_browser(actions->stack_manager, FALSE);
}

void
hippo_actions_toggle_filter(HippoActions *actions)
{
    hippo_stack_manager_toggle_filter(actions->stack_manager);
}

void
hippo_actions_toggle_nofeed(HippoActions *actions)
{
    hippo_stack_manager_toggle_nofeed(actions->stack_manager);
}

void
hippo_actions_toggle_noselfsource(HippoActions *actions)
{
    hippo_stack_manager_toggle_noselfsource(actions->stack_manager);
}

void
hippo_actions_open_home_page(HippoActions    *actions)
{
    DDMDataResource *self = ddm_data_model_get_self_resource(actions->model);
    const char *home_url;

    ddm_data_resource_get(self,
                          "homeUrl", DDM_DATA_URL, &home_url,
                          NULL);

    if (home_url != NULL)
        hippo_actions_open_url(actions, home_url);
}

void
hippo_actions_open_url(HippoActions *actions,
                       const char   *url)
{
    ddm_data_model_update(actions->model,
                          "online-desktop:/p/system#openUrl", NULL,
                          "url", url,
                          NULL);
}

static void
set_block_hushed(HippoActions *actions,
                 HippoBlock   *block,
                 gboolean      hushed)
{
    ddm_data_model_update(actions->model,
                          "http://mugshot.org/p/blocks#setBlockHushed", NULL,
                          "blockId", hippo_block_get_guid(block),
                          "hushed", hushed ? "true" : "false",
                          NULL);
}

void
hippo_actions_hush_block(HippoActions    *actions,
                         HippoBlock      *block)
{
    set_block_hushed(actions, block, TRUE);
}

void
hippo_actions_unhush_block(HippoActions    *actions,
                           HippoBlock      *block)
{
    set_block_hushed(actions, block, FALSE);
}

void
hippo_actions_add_to_faves(HippoActions    *actions,
                           HippoBlock      *block)
{
    /* FIXME */
}

void
hippo_actions_join_chat_id(HippoActions    *actions,
                           const char      *chat_id)
{

    hippo_stacker_platform_show_chat_window(actions->platform, chat_id);
}

void
hippo_actions_invite_to_group(HippoActions    *actions,
                              HippoGroup      *group,
                              HippoPerson     *person)
{
    ddm_data_model_update(actions->model,
                          "http://mugshot.org/p/groups#inviteUser", NULL,
                          "groupId", hippo_entity_get_guid(HIPPO_ENTITY(group)),
                          "userId", hippo_entity_get_guid(HIPPO_ENTITY(person)),
                          NULL);
}

gboolean
hippo_actions_can_play_song_download(HippoActions      *actions,
                                     HippoSongDownload *song_download)
{
    return hippo_stacker_platform_can_play_song_download(actions->platform, song_download);
}

void
hippo_actions_quip(HippoActions   *actions,
                   HippoChatKind   kind,
                   const char     *id,
                   HippoSentiment  sentiment,
                   const char     *title)
{
    HippoQuipWindow *quip_window = hippo_quip_window_new(actions->model, actions->platform);

    hippo_quip_window_set_chat(quip_window, kind, id);
    hippo_quip_window_set_sentiment(quip_window, sentiment);
    hippo_quip_window_set_title(quip_window, title);
    
    hippo_quip_window_show(quip_window);
    g_object_unref(quip_window);
}

void 
hippo_actions_send_account_question_response(HippoActions *actions,
                                             const char   *block_id,
                                             const char   *response)
{
    ddm_data_model_update(actions->model,
                          "http://mugshot.org/p/blocks#accountQuestionResponse", NULL,
                          "blockId", block_id,
                          "response", response,
                          NULL);
}
