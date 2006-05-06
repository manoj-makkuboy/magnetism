#ifndef __HIPPO_DATA_CACHE_INTERNAL_H__
#define __HIPPO_DATA_CACHE_INTERNAL_H__

/*
 * 
 * Methods here are for updating the data cache, used by HippoConnection.
 * This avoids a bunch of extra signals on HippoConnection that would
 * only be used by the data cache to see new info.
 * 
 * The public API to HippoDataCache is generally "read only" since changes
 * are made asynchronously by sending them to the server, then being notified
 * that the change has taken effect.
 */

#include <hippo/hippo-data-cache.h>

G_BEGIN_DECLS

/* A convenience method like ensure_post() doesn't work well because we want to be 
 * able to init the properties of a post before adding it and thus emitting the 
 * post-added signal
 */
void             hippo_data_cache_add_post               (HippoDataCache *cache,
                                                          HippoPost      *post);
void             hippo_data_cache_add_entity             (HippoDataCache *cache,
                                                          HippoEntity    *entity);
/* but sometimes we want an entity with no properties anyhow */
HippoEntity*     hippo_data_cache_ensure_bare_entity     (HippoDataCache *cache,
                                                          HippoEntityType type,
                                                          const char     *guid);
                                                          

HippoChatRoom*   hippo_data_cache_ensure_chat_room       (HippoDataCache  *cache,
                                                          const char      *chat_id,
                                                          HippoChatKind    kind);
                                                          
void             hippo_data_cache_set_hotness               (HippoDataCache  *cache,
                                                             HippoHotness     hotness);
void             hippo_data_cache_set_music_sharing_enabled (HippoDataCache  *cache,
                                                             gboolean         enabled);
void             hippo_data_cache_set_music_sharing_primed  (HippoDataCache  *cache,
                                                             gboolean         primed);

G_END_DECLS

#endif /* __HIPPO_DATA_CACHE_INTERNAL_H__ */
