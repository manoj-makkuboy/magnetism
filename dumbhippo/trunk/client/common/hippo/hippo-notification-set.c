/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "hippo-data-model-internal.h"
#include "hippo-data-resource-internal.h"
#include "hippo-notification-set.h"

typedef struct _ResourceInfo ResourceInfo;

struct _ResourceInfo
{
    HippoDataResource *resource;
    GSList *changed_properties;
};

struct _HippoNotificationSet
{
    HippoDataModel *model;
    GHashTable     *resources;
};

static void
free_resource_info (ResourceInfo *info)
{
    g_slist_free(info->changed_properties);
    g_free(info);
}

HippoNotificationSet *
_hippo_notification_set_new (HippoDataModel *model)
{
    HippoNotificationSet *notifications = g_new0(HippoNotificationSet, 1);

    notifications->model = model;
    notifications->resources = g_hash_table_new_full(g_str_hash, g_str_equal, NULL, (GDestroyNotify)free_resource_info);

    return notifications;
}

void
_hippo_notification_set_add (HippoNotificationSet *notifications,
                             HippoDataResource    *resource,
                             HippoQName           *property_id)
{
    const char *resource_id = hippo_data_resource_get_resource_id(resource);
    
    ResourceInfo *info = g_hash_table_lookup(notifications->resources, resource_id);
    if (info == NULL) {
        info = g_new0(ResourceInfo, 1);
        
        info->resource = resource;
        info->changed_properties = NULL;

        g_hash_table_insert(notifications->resources, (char *)resource_id, info);
    }

    if (g_slist_find(info->changed_properties, property_id) == NULL)
        info->changed_properties = g_slist_prepend(info->changed_properties, property_id);
}

gboolean
_hippo_notification_set_has_property (HippoNotificationSet *notifications,
                                      const char           *resource_id,
                                      HippoQName           *property_id)
{
    ResourceInfo *info = g_hash_table_lookup(notifications->resources, resource_id);
    if (info == NULL)
        return FALSE;

    return g_slist_find(info->changed_properties, property_id) != NULL;
}

static void
send_notification_foreach(gpointer key,
                          gpointer value,
                          gpointer data)
{
    /* HippoNotificationSet *notifications = data; */
    ResourceInfo *info = value;

    _hippo_data_resource_on_resource_change(info->resource, info->changed_properties);
}

void
_hippo_notification_set_send (HippoNotificationSet *notifications)
{
    g_hash_table_foreach(notifications->resources, send_notification_foreach, notifications);
}

typedef struct {
    HippoNotificationSet *notifications;
    gint64 timestamp;
} SaveNotificationsClosure;

static void
save_notification_foreach(gpointer key,
                          gpointer value,
                          gpointer data)
{
    SaveNotificationsClosure *snc = data;
    ResourceInfo *info = value;

    _hippo_data_model_save_properties_to_disk(snc->notifications->model, info->resource, info->changed_properties,
                                              snc->timestamp);
}

void
_hippo_notification_set_save_to_disk (HippoNotificationSet *notifications,
                                      gint64                timestamp)
{
    SaveNotificationsClosure snc;

    snc.notifications = notifications;
    snc.timestamp = timestamp;
    
    g_hash_table_foreach(notifications->resources, save_notification_foreach, &snc);
}

void
_hippo_notification_set_free (HippoNotificationSet *notifications)
{
    g_hash_table_destroy(notifications->resources);
    g_free(notifications);
}
