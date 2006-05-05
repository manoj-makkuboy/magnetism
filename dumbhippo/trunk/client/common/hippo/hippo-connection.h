#ifndef __HIPPO_CONNECTION_H__
#define __HIPPO_CONNECTION_H__

#include <hippo/hippo-platform.h>

G_BEGIN_DECLS

typedef struct _HippoDataCache      HippoDataCache;
typedef struct _HippoDataCacheClass HippoDataCacheClass;

typedef enum {
    HIPPO_STATE_SIGNED_OUT,     // User hasn't asked to connect
    HIPPO_STATE_SIGN_IN_WAIT,   // Waiting for the user to sign in
    HIPPO_STATE_CONNECTING,     // Waiting for connecting to server
    HIPPO_STATE_RETRYING,       // Connection to server failed, retrying
    HIPPO_STATE_AUTHENTICATING, // Waiting for authentication
    HIPPO_STATE_AUTH_WAIT,      // Authentication failed, waiting for new creds
    HIPPO_STATE_AUTHENTICATED   // Authenticated to server
} HippoState;

typedef struct {
    char **keys;
    char **values;
} HippoSong;

typedef struct _HippoConnection      HippoConnection;
typedef struct _HippoConnectionClass HippoConnectionClass;

#define HIPPO_TYPE_CONNECTION              (hippo_connection_get_type ())
#define HIPPO_CONNECTION(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CONNECTION, HippoConnection))
#define HIPPO_CONNECTION_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CONNECTION, HippoConnectionClass))
#define HIPPO_IS_CONNECTION(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CONNECTION))
#define HIPPO_IS_CONNECTION_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CONNECTION))
#define HIPPO_CONNECTION_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CONNECTION, HippoConnectionClass))

GType        	 hippo_connection_get_type                  (void) G_GNUC_CONST;
HippoConnection *hippo_connection_new                       (HippoPlatform    *platform);

void             hippo_connection_set_cache                 (HippoConnection  *connection,
                                                             HippoDataCache   *cache);

HippoState       hippo_connection_get_state                 (HippoConnection  *connection);
HippoHotness     hippo_connection_get_hotness               (HippoConnection  *connection);
/* signin returns TRUE if we're waiting on the user to set the login cookie, FALSE if we already have it */
gboolean         hippo_connection_signin                    (HippoConnection  *connection);
void             hippo_connection_signout                   (HippoConnection  *connection);
void             hippo_connection_notify_post_clicked       (HippoConnection  *connection,
                                                             const char       *post_id);
void             hippo_connection_notify_music_changed      (HippoConnection  *connection,
                                                             gboolean          currently_playing,
                                                             const HippoSong  *song);
gboolean         hippo_connection_get_music_sharing_enabled (HippoConnection  *connection);
gboolean         hippo_connection_get_need_priming_music    (HippoConnection  *connection);
void             hippo_connection_provide_priming_music     (HippoConnection  *connection,
                                                             const HippoSong **songs,
                                                             int               n_songs);
                                                            

/* return string form of enum values */
const char*      hippo_state_debug_string(HippoState state);

G_END_DECLS

#endif /* __HIPPO_CONNECTION_H__ */
