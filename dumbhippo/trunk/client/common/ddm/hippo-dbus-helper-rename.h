/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_HELPER_RENAME_H__
#define __HIPPO_DBUS_HELPER_RENAME_H__

G_BEGIN_DECLS

/*
 * To generate this,
 *  nm linux/mugshot-hippo-dbus-helper.o | grep ' T ' | cut -d ' ' -f 3 | sed -e 's/\(hippo_\)\(.*\)/#define hippo_\2 foo_\2/g'
 *
 *
 * Lame, but don't want to make the hippo-dbus-helper.[hc] files
 * hideous by using macros for all the symbol names, which will
 * confuse indentation engines.
 */

#define hippo_dbus_helper_emit_signal _ddm_dbus_helper_emit_signal
#define hippo_dbus_helper_emit_signal_appender _ddm_dbus_helper_emit_signal_appender
#define hippo_dbus_helper_emit_signal_valist _ddm_dbus_helper_emit_signal_valist
#define hippo_dbus_helper_object_is_registered _ddm_dbus_helper_object_is_registered
#define hippo_dbus_helper_register_g_object _ddm_dbus_helper_register_g_object
#define hippo_dbus_helper_register_interface _ddm_dbus_helper_register_interface
#define hippo_dbus_helper_register_object _ddm_dbus_helper_register_object
#define hippo_dbus_helper_register_service_tracker _ddm_dbus_helper_register_service_tracker
#define hippo_dbus_helper_unregister_object _ddm_dbus_helper_unregister_object
#define hippo_dbus_helper_unregister_service_tracker _ddm_dbus_helper_unregister_service_tracker
#define hippo_dbus_proxy_ARRAYINT32__INT32 _ddm_dbus_proxy_ARRAYINT32__INT32
#define hippo_dbus_proxy_ARRAYINT32__INT32_STRING _ddm_dbus_proxy_ARRAYINT32__INT32_STRING
#define hippo_dbus_proxy_ARRAYINT32__VOID _ddm_dbus_proxy_ARRAYINT32__VOID
#define hippo_dbus_proxy_INT32__INT32 _ddm_dbus_proxy_INT32__INT32
#define hippo_dbus_proxy_INT32__VOID _ddm_dbus_proxy_INT32__VOID
#define hippo_dbus_proxy_STRING__INT32 _ddm_dbus_proxy_STRING__INT32
#define hippo_dbus_proxy_VOID__VOID _ddm_dbus_proxy_VOID__VOID
#define hippo_dbus_proxy_call_method_async _ddm_dbus_proxy_call_method_async
#define hippo_dbus_proxy_call_method_async_appender _ddm_dbus_proxy_call_method_async_appender
#define hippo_dbus_proxy_call_method_async_valist _ddm_dbus_proxy_call_method_async_valist
#define hippo_dbus_proxy_call_method_sync _ddm_dbus_proxy_call_method_sync
#define hippo_dbus_proxy_call_method_sync_appender _ddm_dbus_proxy_call_method_sync_appender
#define hippo_dbus_proxy_call_method_sync_valist _ddm_dbus_proxy_call_method_sync_valist
#define hippo_dbus_proxy_finish_method_call_freeing_reply _ddm_dbus_proxy_finish_method_call_freeing_reply
#define hippo_dbus_proxy_finish_method_call_keeping_reply _ddm_dbus_proxy_finish_method_call_keeping_reply
#define hippo_dbus_proxy_new _ddm_dbus_proxy_new
#define hippo_dbus_proxy_set_method_prefix _ddm_dbus_proxy_set_method_prefix
#define hippo_dbus_proxy_unref _ddm_dbus_proxy_unref


G_END_DECLS

#endif /* __HIPPO_DBUS_HELPER_RENAME_H__ */
