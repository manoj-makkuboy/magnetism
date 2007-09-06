/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "http.h"
#include <unistd.h> /* getpid() */

#define HIPPO_DBUS_HTTP_BUS_NAME  "org.freedesktop.od.Http"
#define HIPPO_DBUS_HTTP_INTERFACE "org.freedesktop.od.Http"
#define HIPPO_DBUS_HTTP_PATH      "/org/freedesktop/od/http"
#define HIPPO_DBUS_HTTP_DATA_SINK_INTERFACE  "org.freedesktop.od.HttpDataSink"

typedef struct {
    int refcount;
    DBusConnection *connection;
    char *sink_path;
    char *url;
    char *content_type;
    GString *content;    
    HttpFunc func;
    void *data;
} Request;

static void
request_ref(Request *r)
{
    r->refcount += 1;
}

static void
request_unref(Request *r)
{
    r->refcount -= 1;
    if (r->refcount == 0) {
        if (r->connection)
            dbus_connection_unref(r->connection);
        g_free(r->sink_path);
        g_free(r->url);
        g_free(r->content_type);
        if (r->content)
            g_string_free(r->content, TRUE);
        g_free(r);
    }
}

static void
request_unregister(Request *r)
{
    hippo_dbus_helper_unregister_object(r->connection,
                                        r->sink_path);
}

static void
request_failed(Request    *r,
               const char *message)
{
    GString *str;

    /* remember the failure could come after a partial success */

    request_unregister(r);

    str = g_string_new(message);
    (* r->func) (NULL, str, r->data);
    g_string_free(str, TRUE);

    /* drop ref held by the object registration */
    request_unref(r);
}

static DBusMessage*
handle_error (void            *object,
              DBusMessage     *message,
              DBusError       *error)
{
    const char *what;
    Request *r;

    g_debug("Got Error() in http sink");
    
    r = object;
    
    what = NULL;
    dbus_message_get_args(message, NULL,
                          DBUS_TYPE_STRING, &what,
                          DBUS_TYPE_INVALID);

    /* call error callback and unregister object */
    request_failed(r, what);

    return NULL;
}

static DBusMessage*
handle_begin (void            *object,
              DBusMessage     *message,
              DBusError       *error)
{
    Request *r;
    const char *content_type;
    gint64 estimated_size;

    g_debug("Got Begin() in http sink");
    
    r = object;

    if (r->content_type != NULL) {
        dbus_set_error(error, DBUS_ERROR_FAILED,
                       "Begin() invoked twice");
        request_failed(r, "Received two Begin() from http provider");
        return NULL;
    }
    
    content_type = NULL;
    estimated_size = 0;
    dbus_message_get_args(message, NULL,
                          DBUS_TYPE_STRING, &content_type,
                          DBUS_TYPE_INT64, &estimated_size,
                          DBUS_TYPE_INVALID);

    r->content_type = g_strdup(content_type);
    r->content = g_string_sized_new(MIN(estimated_size, 1024*64) + 16);

    g_debug("  content-type '%s' estimated size %d", content_type, (int) estimated_size);
    
    return NULL;
}

static DBusMessage*
handle_end (void            *object,
            DBusMessage     *message,
            DBusError       *error)
{
    Request *r;

    g_debug("Got End() in http sink");
    
    r = object;

    if (r->content_type == NULL) {
        dbus_set_error(error, DBUS_ERROR_FAILED,
                       "End() invoked but no Begin()");
        request_failed(r, "Received End() with no Begin() from http provider");
        return NULL;
    }

    request_unregister(r);

    (* r->func) (r->content_type, r->content, r->data);

    /* drop ref held by registration */
    request_unref(r);

    return NULL;
}

static DBusMessage*
handle_data (void            *object,
             DBusMessage     *message,
             DBusError       *error)
{
    Request *r;
    const char *bytes;
    int bytes_len;
    gint64 start_offset;
    gint64 estimated_remaining;
    DBusMessageIter toplevel_iter, array_iter;

    g_debug("Got Data() in http sink");
    
    r = object;

    if (r->content_type == NULL) {
        dbus_set_error(error, DBUS_ERROR_FAILED,
                       "Data() invoked but no Begin()");
        request_failed(r, "Received Data() with no Begin() from http provider");
        return NULL;
    }

    dbus_message_iter_init(message, &toplevel_iter);
    dbus_message_iter_get_basic(&toplevel_iter, &start_offset);
    dbus_message_iter_next(&toplevel_iter);

    dbus_message_iter_get_basic(&toplevel_iter, &estimated_remaining);
    dbus_message_iter_next(&toplevel_iter);

    dbus_message_iter_recurse(&toplevel_iter, &array_iter);

    bytes = NULL;
    bytes_len = 0;
    dbus_message_iter_get_fixed_array(&array_iter, &bytes, &bytes_len);

    if (start_offset != (gint64) r->content->len) {
        dbus_set_error(error, DBUS_ERROR_FAILED,
                       "Data() received for byte %ld but expecting next byte to be %ld",
                       (long) start_offset, (long) r->content->len);
        request_failed(r, "Received wrong byte offset in Data() from http provider");
        return NULL;
    }

    g_string_append_len(r->content, bytes, bytes_len);

    return NULL;
}

static const HippoDBusMember sink_members[] = {
    /* arg is error message */
    { HIPPO_DBUS_MEMBER_METHOD, "Error", "s",  "",       handle_error },
    /* args are content type and estimated data length */
    { HIPPO_DBUS_MEMBER_METHOD, "Begin", "sx", "",       handle_begin },
    { HIPPO_DBUS_MEMBER_METHOD, "End",   "",   "",       handle_end },
    /* args are total data sent in previous calls, estimated remaining data, and a block of data */
    { HIPPO_DBUS_MEMBER_METHOD, "Data",  "xxay",   "",   handle_data },
    { 0, NULL }
};

static const HippoDBusProperty sink_properties[] = {
    { NULL }
};


static void
on_request_reply(DBusMessage *reply,
                 void        *data)
{
    Request *r = data;

    g_debug("Got reply to http request message");
    
    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_ERROR) {
        const char *message = NULL;
        if (dbus_message_get_args(reply, NULL,
                                  DBUS_TYPE_STRING, &message,
                                  DBUS_TYPE_INVALID)) {
            request_failed(r, message);
        } else {
            request_failed(r, "Unknown error");
        }
    }

    /* If it wasn't an error there's nothing to do yet, we just wait for
     * the data sink to hear something back.
     *
     * To be fully robust we should bind to the specific HTTP provider's
     * unique bus name and monitor when it vanishes, or alternatively
     * have some kind of timeout. FIXME
     */

    request_unref(r);
}

void
http_get(DBusConnection *connection,
         const char     *url,
         HttpFunc        func,
         void           *data)
{
    Request *r;
    HippoDBusProxy *proxy;
    static int sink_number = 0;
    
    hippo_dbus_helper_register_interface(connection,
                                         HIPPO_DBUS_HTTP_DATA_SINK_INTERFACE,
                                         sink_members, sink_properties);

    r = g_new0(Request, 1);
    r->refcount = 1;
    r->connection = connection;
    dbus_connection_ref(r->connection);
    r->url = g_strdup(url);
    r->func = func;
    r->data = data;

    /* the pid is to prevent recycling issues when we restart, though
     * in theory the other end should also deal with this by using
     * our unique bus name
     */
    r->sink_path = g_strdup_printf("/org/freedesktop/od/http_sinks/%d_%d",
                                   sink_number, (int) getpid());

    hippo_dbus_helper_register_object(connection,
                                      r->sink_path,
                                      r,
                                      HIPPO_DBUS_HTTP_DATA_SINK_INTERFACE,
                                      NULL);
    
    proxy = hippo_dbus_proxy_new(connection,
                                 HIPPO_DBUS_HTTP_BUS_NAME,
                                 HIPPO_DBUS_HTTP_PATH,
                                 HIPPO_DBUS_HTTP_INTERFACE);
    request_ref(r); /* extra ref held by the reply handler */
    hippo_dbus_proxy_call_method_async(proxy,
                                       "Request",
                                       on_request_reply,
                                       r,
                                       NULL,
                                       DBUS_TYPE_STRING, &r->url,
                                       DBUS_TYPE_OBJECT_PATH, &r->sink_path,
                                       DBUS_TYPE_INVALID);
    hippo_dbus_proxy_unref(proxy);

    g_debug("requesting http url '%s' using sink path '%s'",
            r->url, r->sink_path);
}
