#include "hippo-chat-room.h"
#include <string.h>

static void      hippo_chat_room_init        (HippoChatRoom       *room);
static void      hippo_chat_room_class_init  (HippoChatRoomClass  *klass);
static void      hippo_chat_room_finalize    (GObject             *object);

struct _HippoChatRoom {
    GObject parent;
    char *id;
    char *jabber_id;
    HippoChatKind kind;
    char *title;
    GHashTable *viewers;
    GHashTable *chatters;
    GSList *messages; /* sorted latest -> oldest */

    /* FIXME These two counts are preserved across connect/disconnect
     * which is wrong if we change login ID. However, it can't be fixed
     * here due to the "refcount" behavior; the UI code or whatever that 
     * does the joins/leaves affecting the count will have to know
     * to leave the chatroom on login ID change.
     */

    /* number of times we want to be a participant */
    int participant_count;
    /* number of times we want to be a viewer */
    int visitor_count;

    guint filling : 1;
};

struct _HippoChatRoomClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoChatRoom, hippo_chat_room, G_TYPE_OBJECT);

enum {
    TITLE_CHANGED,
    USER_STATE_CHANGED,
    /* JOINED is emitted for a subset of STATE_CHANGED where the state went
     * from nonmember to either viewer or participant, and the chat room
     * isn't in the "filling" state. This means we may want to specially
     * indicate the new member of the chat to users.
     */
    USER_JOINED,
    MESSAGE_ADDED,
    CLEARED,
    LAST_SIGNAL
};
  
static int signals[LAST_SIGNAL];  

static void
make_hash_tables(HippoChatRoom *room)
{
    room->viewers = g_hash_table_new_full(g_str_hash, g_str_equal,
                                          g_free, (GFreeFunc) g_object_unref);
    room->chatters = g_hash_table_new_full(g_str_hash, g_str_equal,
                                           g_free, (GFreeFunc) g_object_unref);
}
                       
static void
hippo_chat_room_init(HippoChatRoom *room)
{
    make_hash_tables(room);
}

static void
hippo_chat_room_class_init(HippoChatRoomClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    signals[TITLE_CHANGED] =
        g_signal_new ("title-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);

    signals[USER_STATE_CHANGED] =
        g_signal_new ("user-state-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[USER_JOINED] =
        g_signal_new ("user-joined",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[MESSAGE_ADDED] =
        g_signal_new ("message-added",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__POINTER,
            		  G_TYPE_NONE, 1, G_TYPE_POINTER);

    signals[CLEARED] =
        g_signal_new ("cleared",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);

    object_class->finalize = hippo_chat_room_finalize;
}

static void
free_messages(HippoChatRoom *room)
{
    g_slist_foreach(room->messages, (GFunc) hippo_chat_message_free, NULL);
    g_slist_free(room->messages);
    room->messages = NULL;
}

static void
hippo_chat_room_finalize(GObject *object)
{
    HippoChatRoom *room = HIPPO_CHAT_ROOM(object);

    free_messages(room);
    g_hash_table_destroy(room->viewers);
    g_hash_table_destroy(room->chatters);
    g_free(room->title);
    g_free(room->jabber_id);
    g_free(room->id);
    
    G_OBJECT_CLASS(hippo_chat_room_parent_class)->finalize(object);
}

HippoChatRoom*
hippo_chat_room_new(const char   *chat_id,
                    HippoChatKind kind)
{
    HippoChatRoom *room = g_object_new(HIPPO_TYPE_CHAT_ROOM, NULL);

    room->id = g_strdup(chat_id);
    room->kind = kind;

    return room;
}

const char*
hippo_chat_room_get_id(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    return room->id;
}

const char*
hippo_chat_room_get_jabber_id(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    if (room->jabber_id == NULL) {
        char *chat_jid;
        
        chat_jid = hippo_id_to_jabber_id(hippo_chat_room_get_id(room));
        room->jabber_id = g_strconcat(chat_jid, "@rooms.dumbhippo.com", NULL);
        g_free(chat_jid);
    }
    return room->jabber_id;
}

HippoChatKind
hippo_chat_room_get_kind(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), HIPPO_CHAT_POST);
    return room->kind;
}

void
hippo_chat_room_set_title(HippoChatRoom  *room,
                          const char     *title)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    if (title == room->title ||
        (title && room->title && strcmp(title, room->title) == 0))
        return;
        
    g_free(room->title);
    room->title = g_strdup(title);
    
    g_signal_emit(room, signals[TITLE_CHANGED], 0, room->title);
}

const char*
hippo_chat_room_get_title(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    return room->title;
}

HippoChatMessage*
hippo_chat_room_get_last_message(HippoChatRoom  *room)
{
    HippoChatMessage *message;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);

    if (room->messages == NULL)
        return NULL;
        
    message = room->messages->data;
    
    /* -1 message is the post description, which isn't technically
     * a message, it's displayed only in certain contexts.
     */
    if (hippo_chat_message_get_serial(message) == -1)
        return NULL;
    else
        return message;
}

static void
listify_foreach(void *key, void *value, void *data)
{
    GSList **list_p = data;
    *list_p = g_slist_prepend(*list_p, value);
    g_object_ref(value);
}

GSList*
hippo_chat_room_get_users(HippoChatRoom  *room)
{
    GSList *list;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    
    list = NULL;
    g_hash_table_foreach(room->chatters, listify_foreach, &list);
    g_hash_table_foreach(room->viewers, listify_foreach, &list);
    return list;
}

int
hippo_chat_room_get_viewing_user_count(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);
    
    return g_hash_table_size(room->viewers);
}

int
hippo_chat_room_get_chatting_user_count(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);
    
    return g_hash_table_size(room->chatters);
}

GSList*
hippo_chat_room_get_messages(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);

    return room->messages;
}

gboolean
hippo_chat_room_get_filling(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), FALSE);
    
    return room->filling;
}

void
hippo_chat_room_set_filling(HippoChatRoom *room,
                            gboolean       filling)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    room->filling = filling;
}

HippoChatState
hippo_chat_room_get_user_state(HippoChatRoom *room,
                               HippoPerson   *person)
{
    HippoChatState state;
    const char *guid;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), HIPPO_CHAT_NONMEMBER);
    
    guid = hippo_entity_get_guid(HIPPO_ENTITY(person));
    
    if (g_hash_table_lookup(room->viewers, guid) != NULL)
        state = HIPPO_CHAT_VISITOR;
    else if (g_hash_table_lookup(room->chatters, guid) != NULL)
        state = HIPPO_CHAT_PARTICIPANT;
    else
        state = HIPPO_CHAT_NONMEMBER;
        
    return state;
}

void
hippo_chat_room_set_user_state(HippoChatRoom *room,
                               HippoPerson   *person,
                               HippoChatState state)
{
    HippoChatState old_state;
    const char *guid;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    old_state = hippo_chat_room_get_user_state(room, person);
    
    if (old_state == state)
        return;
    
    guid = hippo_entity_get_guid(HIPPO_ENTITY(person));

    /* so we can remove from our hash tables and then 
     * still safely emit the signal 
     */
    g_object_ref(person);
    
    switch (old_state) {
    case HIPPO_CHAT_VISITOR:
        g_hash_table_remove(room->viewers, guid);
        break; 
    case HIPPO_CHAT_PARTICIPANT:
        g_hash_table_remove(room->viewers, guid);
        break;
    case HIPPO_CHAT_NONMEMBER:
        /* no old entry to remove */        
        break;
    }
    
    switch(state) {
    case HIPPO_CHAT_VISITOR:
        g_object_ref(person); /* extra ref for the hash */
        g_hash_table_replace(room->viewers, g_strdup(guid), person);
        break; 
    case HIPPO_CHAT_PARTICIPANT:
        g_object_ref(person); /* extra ref for the hash */    
        g_hash_table_replace(room->chatters, g_strdup(guid), person);    
        break;
    case HIPPO_CHAT_NONMEMBER:
        /* no new entry to insert */
        break;
    }

    g_signal_emit(room, signals[USER_STATE_CHANGED], 0, person);

    if (!room->filling && old_state == HIPPO_CHAT_NONMEMBER) {
        g_signal_emit(room, signals[USER_JOINED], 0, person);
    }

    g_object_unref(person);
}

void
hippo_chat_room_add_message(HippoChatRoom    *room,
                            HippoChatMessage *message)
{
    GSList *link;
    GSList *prev;
    int serial;    
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    if (room->messages == NULL) {
        room->messages = g_slist_prepend(room->messages, message);
        return;
    }

    serial = hippo_chat_message_get_serial(message);

    /* highest serial is earliest in the list */
    prev = NULL;
    for (link = room->messages; link != NULL; link = link->next) {
        HippoChatMessage *old = link->data;
        int old_serial = hippo_chat_message_get_serial(old);
        
        if (old_serial == serial) {
            /* We already have this message */
            hippo_chat_message_free(message);
            return;
        } else if (old_serial < serial) {
            if (prev) {
                g_assert(prev->next == link);
                prev->next = g_slist_prepend(prev->next, message);
            } else {
                g_assert(link == room->messages);
                room->messages = g_slist_prepend(room->messages, message);
            }
            
            g_signal_emit(room, signals[MESSAGE_ADDED], 0, message);
            
            return;
        } else {
            prev = link;
        }
    }
}

/* Called on reconnect, since users and messages will be resent */
void
hippo_chat_room_clear(HippoChatRoom *room)
{
    GHashTable *old_viewers;
    GHashTable *old_chatters;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    /* Save old tables to hold refs while
     * we emit the "cleared" signal so 
     * api users can drop their usage of the users 
     */

    old_viewers = room->viewers;
    room->viewers = NULL;
    old_chatters = room->chatters;
    room->chatters = NULL;
    
    make_hash_tables(room);
    
    free_messages(room);
    
    g_signal_emit(room, signals[CLEARED], 0);
    
    g_hash_table_destroy(old_viewers);
    g_hash_table_destroy(old_chatters);        
}

void
hippo_chat_room_increment_state_count(HippoChatRoom *room,
                                      HippoChatState state)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    g_return_if_fail(state != HIPPO_CHAT_NONMEMBER);

    if (state == HIPPO_CHAT_PARTICIPANT) {
        room->participant_count += 1;
        g_debug("participant_count for %s incremented to %d visitor_count %d",
                room->id, room->participant_count, room->visitor_count);
    } else if (state == HIPPO_CHAT_VISITOR) {
        room->visitor_count += 1;
        g_debug("visitor_count for %s incremented to %d participant_count %d",
                room->id, room->visitor_count, room->participant_count);
    }
}

void
hippo_chat_room_decrement_state_count(HippoChatRoom *room,
                                      HippoChatState state)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    g_return_if_fail(state != HIPPO_CHAT_NONMEMBER);
    
    if (state == HIPPO_CHAT_PARTICIPANT) {
        g_return_if_fail(room->participant_count >= 0);
        
        room->participant_count -= 1;
        g_debug("participant_count for %s decremented to %d visitor_count %d",
                room->id, room->participant_count, room->visitor_count);
    } else if (state == HIPPO_CHAT_VISITOR) {
        g_return_if_fail(room->visitor_count >= 0);

        room->visitor_count -= 1;
        g_debug("visitor_count for %s decremented to %d participant_count %d",
                room->id, room->visitor_count, room->participant_count);
    }
}

HippoChatState
hippo_chat_room_get_desired_state(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);

    if (room->participant_count > 0)
        return HIPPO_CHAT_PARTICIPANT;
    else if (room->visitor_count > 0)
        return HIPPO_CHAT_VISITOR;
    else
        return HIPPO_CHAT_NONMEMBER;
}

/* === HippoChatMessage === */

/* This is a "value object", has to be copied instead of ref'd */

struct _HippoChatMessage {
    guint32 magic;
    HippoPerson *person;
    char *text;
    GTime timestamp;
    int serial;
};

/* a little paranoia to help with our C++ skillz */
#define HIPPO_CHAT_MESSAGE_MAGIC 0x1234cafe
#define HIPPO_IS_CHAT_MESSAGE(message)(((HippoChatMessage*)message)->magic == HIPPO_CHAT_MESSAGE_MAGIC)

HippoChatMessage*
hippo_chat_message_new(HippoPerson *person,
                       const char  *text,
                       GTime        timestamp,
                       int          serial)
{
    HippoChatMessage *message;
    
    message = g_new0(HippoChatMessage, 1);
    message->magic = HIPPO_CHAT_MESSAGE_MAGIC;
    message->person = person;
    g_object_ref(message->person);
    message->text = g_strdup(text);
    message->timestamp = timestamp;
    message->serial = serial;

    return message;
}

void
hippo_chat_message_free(HippoChatMessage *message)
{
    g_return_if_fail(HIPPO_IS_CHAT_MESSAGE(message));
    
    message->magic = 0xdeadbeef;
    g_object_unref(message->person);
    g_free(message->text);
    g_free(message);
}

HippoChatMessage*
hippo_chat_message_copy(HippoChatMessage *message)
{
    HippoChatMessage *copy;
 
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), NULL);
    
    copy = g_new(HippoChatMessage, 1);
    *copy = *message;
    g_object_ref(copy->person);
    copy->text = g_strdup(message->text);
    return copy;
}

HippoPerson*
hippo_chat_message_get_person(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), NULL);

    return message->person;
}

const char*
hippo_chat_message_get_text(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), NULL);

    return message->text;
}

GTime
hippo_chat_message_get_timestamp(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), 0);

    return message->timestamp;
}

int
hippo_chat_message_get_serial(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), 0);

    return message->serial;
}
