#ifndef __HIPPO_BUBBLE_UTIL_H__
#define __HIPPO_BUBBLE_UTIL_H__

/* 
 * This file has the glue between the bubble and our data cache
 * types and stuff like that; hippo-bubble.[hc] is purely the 
 * rendering-related bits.
 */

#include <config.h>
#include "hippo-bubble.h"
#include "main.h"

G_BEGIN_DECLS

void             hippo_bubble_set_post               (HippoBubble    *bubble,
                                                      HippoPost      *post,
                                                      HippoDataCache *cache);
HippoPost*       hippo_bubble_get_post               (HippoBubble    *bubble);

G_END_DECLS

#endif /* __HIPPO_BUBBLE_UTIL_H__ */
