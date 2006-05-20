#include "hippo-bubble-manager.h"

/* 7 seconds */
#define BUBBLE_LENGTH_MILLISECONDS 7000
#define BUBBLE_LENGTH_SECONDS (BUBBLE_LENGTH_MILLISECONDS / 1000)

typedef struct {
    int refcount;
    HippoDataCache *cache;
    GHashTable     *chats;
    GtkWidget      *window;
    GtkWidget      *notebook;
    guint           popdown_timeout;
    guint           window_contains_pointer : 1;
} BubbleManager;

static void remove_popdown_timeout  (BubbleManager *manager);
static void update_bubble_paging    (BubbleManager *manager);

static void
manager_remove_bubble_by_page(BubbleManager *manager,
                              int            page)
{
    g_return_if_fail(page >= 0);

    gtk_notebook_remove_page(GTK_NOTEBOOK(manager->notebook), page);
    
    if (gtk_notebook_get_n_pages(GTK_NOTEBOOK(manager->notebook)) == 0) {
        gtk_widget_hide(manager->window);
        remove_popdown_timeout(manager);
    } else {
        update_bubble_paging(manager); /* changes window size, before positioning */
        /* if we closed a bubble we need to redo the positioning */
        hippo_app_put_window_by_icon(hippo_get_app(), GTK_WINDOW(manager->window));
    }
}

static void
manager_remove_bubble(BubbleManager *manager,
                      HippoBubble   *bubble)
{
    int page;
    
    page = gtk_notebook_page_num(GTK_NOTEBOOK(manager->notebook),
                                 GTK_WIDGET(bubble));
    if (page >= 0)
        manager_remove_bubble_by_page(manager, page);
}

static GTime
bubble_get_timestamp(HippoBubble *bubble)
{
    return GPOINTER_TO_INT(g_object_get_data(G_OBJECT(bubble), "bubble-timestamp"));
}

static void
bubble_set_timestamp(HippoBubble *bubble,
                     GTime        timestamp)
{
    g_object_set_data(G_OBJECT(bubble), "bubble-timestamp", GINT_TO_POINTER(timestamp));
}

static gboolean
popdown_timeout(void *data)
{
    BubbleManager *manager = data;
    GList *children;
    GList *link;
    GTime now;
    GTimeVal tv;
    gboolean all_timed_out;
    
    g_get_current_time(&tv);
    now = tv.tv_sec;
    
    all_timed_out = TRUE;
    children = gtk_container_get_children(GTK_CONTAINER(manager->notebook));
    for (link = children; link != NULL; link = link->next) {
        HippoBubble *bubble = HIPPO_BUBBLE(link->data);
        GTime bubble_time = bubble_get_timestamp(bubble);

        /* consider updating bubble timestamp */
        if (bubble_time > now) {
            /* clock went backward ... fixup bubble time */
            bubble_set_timestamp(bubble, now);
        } else if (manager->window_contains_pointer) {
            /* all bubbles are reset if user is doing stuff with them */
            bubble_set_timestamp(bubble, now);
        }
        
        /* see if we've timed out */
        if ((now - bubble_time) > BUBBLE_LENGTH_SECONDS) {
            /* this bubble timed out */
            ;
        } else {
            all_timed_out = FALSE;
        }
    }
    
    g_list_free(children);
    
    if (all_timed_out) {
        gtk_widget_hide(manager->window);
        remove_popdown_timeout(manager); /* remove ourselves */    
    }
    
    /* stay installed, unless we just removed ourselves */
    return TRUE;
}

static void
ensure_popdown_timeout(BubbleManager *manager)
{
    if (manager->popdown_timeout == 0) {
        /* maximum "error" on bubble length is the timeout 
         * length, so we go for 1 second on the timeout
         * which checks on bubbles
         */
        manager->popdown_timeout =
            g_timeout_add(1000,
                          popdown_timeout, manager);
    }
}

static void
remove_popdown_timeout(BubbleManager *manager)
{
    if (manager->popdown_timeout != 0) {
        g_source_remove(manager->popdown_timeout);
        manager->popdown_timeout = 0;
    }
}

static void
reset_popdown_timeout(BubbleManager *manager)
{
    remove_popdown_timeout(manager);
    ensure_popdown_timeout(manager);
}

static gboolean
find_bubble_for_post(BubbleManager *manager,
                     HippoPost     *post,
                     HippoBubble  **bubble_p)
{
    GList *children;
    GList *link;
    
    children = gtk_container_get_children(GTK_CONTAINER(manager->notebook));
    for (link = children; link != NULL; link = link->next) {
        HippoBubble *bubble = HIPPO_BUBBLE(link->data);
     
        if (hippo_bubble_get_post(bubble) == post) {
            g_list_free(children);
            *bubble_p = bubble;
            return TRUE;
        }
    }
    
    g_list_free(children);
    *bubble_p = NULL;
    return FALSE;
}                     

/* this has to be called manually; GTK 2.10 has page-added/page-removed 
 * on gtknotebook but earlier versions don't
 */
static void
update_bubble_paging(BubbleManager *manager)
{
    GList *children;
    GList *link;
    int i;
    int total;
    
    total = gtk_notebook_get_n_pages(GTK_NOTEBOOK(manager->notebook));
    
    i = 0;
    children = gtk_container_get_children(GTK_CONTAINER(manager->notebook));
    for (link = children; link != NULL; link = link->next) {
        HippoBubble *bubble = HIPPO_BUBBLE(link->data);
        hippo_bubble_set_page_n_of_total(bubble, i, total);
        ++i;     
    }
    
    g_list_free(children);
}

/* note, our job in this file is just to be sure the bubble exists
 * and has the right "reason";
 * The bubble itself is responsible for watching changes to the post
 * and chat room in hippo-bubble-util.c, so e.g. a "recent links"
 * window could share that code.
 * 
 * We also simply skip updating the bubble if the chat is open.
 * 
 * So basically don't call most of the bubble setters in this file 
 * or it will be broken.
 */
static void
manager_bubble_post(BubbleManager    *manager,
                    HippoPost        *post,
                    HippoBubbleReason reason)
{
    HippoBubble *bubble;
    int page;
    GTimeVal tv;
    
    /* if chat is open, we don't want to bubble */
    if (hippo_app_post_is_active(hippo_get_app(), hippo_post_get_guid(post)))
        return;

    if (!find_bubble_for_post(manager, post, &bubble)) {
        bubble = HIPPO_BUBBLE(hippo_bubble_new());
        hippo_bubble_set_post(bubble, post, manager->cache);
        gtk_notebook_append_page(GTK_NOTEBOOK(manager->notebook), GTK_WIDGET(bubble), NULL);
        gtk_widget_show(GTK_WIDGET(bubble));
    }

    g_debug("Showing bubble window");

    hippo_bubble_notify_reason(bubble, reason);

    page = gtk_notebook_page_num(GTK_NOTEBOOK(manager->notebook), GTK_WIDGET(bubble));
    gtk_notebook_set_current_page(GTK_NOTEBOOK(manager->notebook), page);
    
    update_bubble_paging(manager);
    
    hippo_app_put_window_by_icon(hippo_get_app(), GTK_WINDOW(manager->window));
    
    g_get_current_time(&tv);
    bubble_set_timestamp(bubble, tv.tv_sec);
    ensure_popdown_timeout(manager);
 
    /* don't gtk_window_present since we don't want focus
     */   
    gtk_widget_show(manager->window);
}

static HippoPost*
manager_post_for_room(BubbleManager *manager,
                      HippoChatRoom *room)
{
    HippoPost *post;
    
    post = hippo_data_cache_lookup_post(manager->cache, 
                hippo_chat_room_get_id(room));

    return post;
}                      

/* happens for both participants and chatters */
static void
on_user_joined(HippoChatRoom *room,
               HippoPerson   *user,
               BubbleManager *manager)
{
    HippoPost *post;
    post = manager_post_for_room(manager, room);
    if (post != NULL) {                
        manager_bubble_post(manager, post, HIPPO_BUBBLE_REASON_VIEWER);
    }
}

static void
on_message_added(HippoChatRoom    *room,
                 HippoChatMessage *message,
                 BubbleManager    *manager)
{
    HippoPost *post;
    
    if (hippo_chat_room_get_loading(room))
        return;
    
    post = manager_post_for_room(manager, room);    
    if (post != NULL) {                
        manager_bubble_post(manager, post, HIPPO_BUBBLE_REASON_CHAT);
    }
}

static void
chat_room_disconnect(BubbleManager *manager,
                     HippoChatRoom *room)
{
    g_signal_handlers_disconnect_by_func(G_OBJECT(room), G_CALLBACK(on_user_joined), manager);
    g_signal_handlers_disconnect_by_func(G_OBJECT(room), G_CALLBACK(on_message_added), manager);
}

/* we can get this multiple times on a single chat room, remember */
static void 
on_chat_room_loaded(HippoPost     *post,
                    HippoChatRoom *room,
                    BubbleManager *manager)
{
    g_debug("bubble manager, room loaded %s", hippo_chat_room_get_id(room));
    
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_POST &&
        g_hash_table_lookup(manager->chats, room) == NULL) {
        g_signal_connect(G_OBJECT(room), "user-joined", G_CALLBACK(on_user_joined), manager);
        g_signal_connect(G_OBJECT(room), "message-added", G_CALLBACK(on_message_added), manager);
        
        g_hash_table_replace(manager->chats, room, room);
    }
}

static void 
on_post_added(HippoDataCache *cache,
              HippoPost      *post,
              BubbleManager  *manager)
{
    g_debug("bubble manager, post added %s new = %d",
            hippo_post_get_guid(post), hippo_post_get_new(post));
    if (hippo_post_get_new(post)) {
        hippo_post_set_new(post, FALSE);

        manager_bubble_post(manager, post, HIPPO_BUBBLE_REASON_NEW);
    }
}

static void
foreach_disconnect(void *key, void *value, void *data)
{
    BubbleManager *manager = data;
    HippoChatRoom *room = value;
    
    chat_room_disconnect(manager, room);
}

static void
manager_disconnect(BubbleManager *manager)
{
    if (manager->cache) {
        /* remove all notebook pages and hide window */
        while (gtk_notebook_get_n_pages(GTK_NOTEBOOK(manager->notebook)) > 0) {
            gtk_notebook_remove_page(GTK_NOTEBOOK(manager->notebook), 0);
        }
        gtk_widget_hide(manager->window);
        
        /* nuke everything */
        g_hash_table_foreach(manager->chats, foreach_disconnect, manager);
        g_hash_table_destroy(manager->chats);
        manager->chats  = NULL;
    
        g_signal_handlers_disconnect_by_func(manager->cache, G_CALLBACK(on_chat_room_loaded), manager);
        g_signal_handlers_disconnect_by_func(manager->cache, G_CALLBACK(on_post_added), manager);    
    
        g_object_unref(manager->cache);
        manager->cache = NULL;        
    }
}

static void
manager_ref(BubbleManager *manager)
{
    manager->refcount += 1;
}

static void
manager_unref(BubbleManager *manager)
{
    g_return_if_fail(manager->refcount > 0);
    manager->refcount -= 1;
    if (manager->refcount == 0) {
        g_debug("Finalizing bubble manager");
        manager_disconnect(manager);
        gtk_object_destroy(GTK_OBJECT(manager->window));
        g_free(manager);
    }
}

static gboolean
window_delete_event(GtkWindow     *window,
                    GdkEvent      *event,
                    BubbleManager *manager)
{
    /* change this to remove current bubble, and hide window 
     * if no more bubbles
     */

    int page;
    
    page = gtk_notebook_get_current_page(GTK_NOTEBOOK(manager->notebook));
    if (page >= 0) {
        manager_remove_bubble_by_page(manager, page);
    }

    /* don't destroy us */
    return TRUE;
}

static gboolean
window_enter_leave_event(GtkWidget     *window,
                         GdkEvent      *event,
                         BubbleManager *manager)
{
    GtkWidget *event_widget;
    
    event_widget = gtk_get_event_widget(event);
    
    if (event_widget == window &&
        ((GdkEventCrossing*) event)->detail != GDK_NOTIFY_INFERIOR) {        
        if (event->type == GDK_ENTER_NOTIFY) {
            manager->window_contains_pointer = TRUE;
        } else if (event->type == GDK_LEAVE_NOTIFY) {
            manager->window_contains_pointer = FALSE;
        }
    }
    
    return FALSE;
}

static BubbleManager*
manager_new(void)
{
    BubbleManager *manager;
    GdkColor border_color;
    
    manager = g_new0(BubbleManager, 1);
    manager->refcount = 1;
    
    manager->window = gtk_window_new(GTK_WINDOW_POPUP);
    gtk_window_set_decorated(GTK_WINDOW(manager->window), FALSE);
    gtk_window_set_resizable(GTK_WINDOW(manager->window), FALSE);
    border_color.red = 0x9999;
    border_color.green = 0x9999;
    border_color.blue = 0x9999;
    gtk_widget_modify_bg(manager->window, GTK_STATE_NORMAL, &border_color);
    gtk_container_set_border_width(GTK_CONTAINER(manager->window), 1);

    gtk_window_set_accept_focus(GTK_WINDOW(manager->window), FALSE);
    gtk_window_set_focus_on_map(GTK_WINDOW(manager->window), FALSE);
    
    g_signal_connect(G_OBJECT(manager->window), "delete-event", 
                     G_CALLBACK(window_delete_event), manager);

    g_signal_connect(G_OBJECT(manager->window), "enter-notify-event", 
                     G_CALLBACK(window_enter_leave_event), manager);
    g_signal_connect(G_OBJECT(manager->window), "leave-notify-event", 
                     G_CALLBACK(window_enter_leave_event), manager);
    
    /* the various bubbles are in notebook pages */
    manager->notebook = gtk_notebook_new();
    gtk_notebook_set_show_tabs(GTK_NOTEBOOK(manager->notebook), FALSE);
    gtk_notebook_set_show_border(GTK_NOTEBOOK(manager->notebook), FALSE);    
    gtk_container_add(GTK_CONTAINER(manager->window), GTK_WIDGET(manager->notebook));
    gtk_widget_show(GTK_WIDGET(manager->notebook));

    return manager;
}

static void
manager_attach(BubbleManager   *manager,
               HippoDataCache  *cache)
{
    g_debug("Bubble manager attaching to data cache");

    manager->cache = cache;
    g_object_ref(manager->cache);

    /* this creates a refcount cycle, but
     * hippo_bubble_manager_unmanage breaks it.
     * Also, too lazy right now to key to the cache/icon 
     * pair, right now it just keys to the cache
     */
    manager_ref(manager);
    g_object_set_data_full(G_OBJECT(cache), "bubble-manager",
                           manager, (GFreeFunc) manager_unref);

    manager->chats = g_hash_table_new(g_direct_hash, g_direct_equal);
                           
    g_signal_connect(cache, "chat-room-loaded", G_CALLBACK(on_chat_room_loaded), manager);
    g_signal_connect(cache, "post-added", G_CALLBACK(on_post_added), manager);
}

static void
manager_detach(HippoDataCache  *cache)
{
    BubbleManager *manager;
    
    manager = g_object_get_data(G_OBJECT(cache), "bubble-manager");
    g_return_if_fail(manager != NULL);
    
    manager_disconnect(manager);

    /* may destroy the manager */
    g_object_set_data(G_OBJECT(cache), "bubble-manager", NULL);
}

void
hippo_bubble_manager_manage(HippoDataCache  *cache)
{
    BubbleManager *manager;
    
    manager = manager_new();

    manager_attach(manager, cache);
    manager_unref(manager);
}                            

void
hippo_bubble_manager_unmanage(HippoDataCache  *cache)
{
    manager_detach(cache);
}
