#ifndef __HIPPO_BUBBLE_H__
#define __HIPPO_BUBBLE_H__

#include <gtk/gtkwidget.h>
#include <gtk/gtkfixed.h>

G_BEGIN_DECLS

typedef struct {
    const char *name;
    const char *entity_guid; /* NULL for "the world" recipient */
} HippoRecipientInfo;

typedef struct {
    const char *name;
    const char *entity_guid;
    gboolean    present;  /* currently viewing or chatting */
    gboolean    chatting; /* currently chatting */
} HippoViewerInfo;

typedef enum {
    HIPPO_BUBBLE_REASON_NEW,
    HIPPO_BUBBLE_REASON_CHAT,
    HIPPO_BUBBLE_REASON_VIEWER
} HippoBubbleReason;

typedef struct _HippoBubble      HippoBubble;
typedef struct _HippoBubbleClass HippoBubbleClass;

#define HIPPO_TYPE_BUBBLE              (hippo_bubble_get_type ())
#define HIPPO_BUBBLE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BUBBLE, HippoBubble))
#define HIPPO_BUBBLE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BUBBLE, HippoBubbleClass))
#define HIPPO_IS_BUBBLE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BUBBLE))
#define HIPPO_IS_BUBBLE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BUBBLE))
#define HIPPO_BUBBLE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BUBBLE, HippoBubbleClass))

GType        	 hippo_bubble_get_type               (void) G_GNUC_CONST;

GtkWidget*       hippo_bubble_new                    (void);

void             hippo_bubble_set_sender_guid        (HippoBubble *bubble,
                                                      const char  *value);
void             hippo_bubble_set_post_guid          (HippoBubble *bubble,
                                                      const char  *value);
void             hippo_bubble_set_sender_name        (HippoBubble *bubble, 
                                                      const char  *value);
void             hippo_bubble_set_sender_photo       (HippoBubble *bubble, 
                                                      GdkPixbuf   *pixbuf);
void             hippo_bubble_set_link_title         (HippoBubble *bubble, 
                                                      const char  *title);
void             hippo_bubble_set_link_description   (HippoBubble *bubble, 
                                                      const char  *value);
void             hippo_bubble_set_recipients         (HippoBubble *bubble, 
                                                      const HippoRecipientInfo *recipients,
                                                      int          n_recipients);
void             hippo_bubble_set_viewers            (HippoBubble *bubble,
                                                      const HippoViewerInfo    *viewers,
                                                      int          n_viewers);
void             hippo_bubble_set_last_chat_message  (HippoBubble *bubble,
                                                      const char  *message,
                                                      const char  *sender_id);
void             hippo_bubble_set_last_chat_photo    (HippoBubble *bubble,
                                                      GdkPixbuf   *pixbuf);
void             hippo_bubble_set_page_n_of_total    (HippoBubble *bubble,
                                                      int          n,
                                                      int          total);
void             hippo_bubble_notify_reason          (HippoBubble *bubble,
                                                      HippoBubbleReason reason);

G_END_DECLS

#endif /* __HIPPO_BUBBLE_H__ */
