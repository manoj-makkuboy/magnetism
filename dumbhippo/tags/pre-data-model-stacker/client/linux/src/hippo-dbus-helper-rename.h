/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_HELPER_RENAME_H__
#define __HIPPO_DBUS_HELPER_RENAME_H__

G_BEGIN_DECLS

/*
 * To generate this,
 *  nm linux/mugshot-hippo-dbus-helper.o | grep ' T ' | cut -d ' ' -f 3 | sed -e 's/\(hippo_\)\(.*\)/#define hippo_\2 foo_\2/g'
 *
 * (you have to run this after building *without* any renames)
 *
 *
 * Lame, but don't want to make the hippo-dbus-helper.[hc] files
 * hideous by using macros for all the symbol names, which will
 * confuse indentation engines.
 */

#define hippo_dbus_helper_register_name_owner mugshot_linux_dbus_helper_register_name_owner
#define hippo_dbus_helper_unregister_name_owner mugshot_linux_dbus_helper_unregister_name_owner
#define hippo_dbus_helper_emit_signal mugshot_linux_dbus_helper_emit_signal
#define hippo_dbus_helper_emit_signal_appender mugshot_linux_dbus_helper_emit_signal_appender
#define hippo_dbus_helper_emit_signal_valist mugshot_linux_dbus_helper_emit_signal_valist
#define hippo_dbus_helper_object_is_registered mugshot_linux_dbus_helper_object_is_registered
#define hippo_dbus_helper_register_connection_tracker mugshot_linux_dbus_helper_register_connection_tracker
#define hippo_dbus_helper_register_g_object mugshot_linux_dbus_helper_register_g_object
#define hippo_dbus_helper_register_interface mugshot_linux_dbus_helper_register_interface
#define hippo_dbus_helper_register_object mugshot_linux_dbus_helper_register_object
#define hippo_dbus_helper_register_service_tracker mugshot_linux_dbus_helper_register_service_tracker
#define hippo_dbus_helper_unregister_connection_tracker mugshot_linux_dbus_helper_unregister_connection_tracker
#define hippo_dbus_helper_unregister_object mugshot_linux_dbus_helper_unregister_object
#define hippo_dbus_helper_unregister_service_tracker mugshot_linux_dbus_helper_unregister_service_tracker
#define hippo_dbus_proxy_ARRAYINT32__INT32 mugshot_linux_dbus_proxy_ARRAYINT32__INT32
#define hippo_dbus_proxy_ARRAYINT32__INT32_STRING mugshot_linux_dbus_proxy_ARRAYINT32__INT32_STRING
#define hippo_dbus_proxy_ARRAYINT32__VOID mugshot_linux_dbus_proxy_ARRAYINT32__VOID
#define hippo_dbus_proxy_INT32__INT32 mugshot_linux_dbus_proxy_INT32__INT32
#define hippo_dbus_proxy_INT32__VOID mugshot_linux_dbus_proxy_INT32__VOID
#define hippo_dbus_proxy_STRING__INT32 mugshot_linux_dbus_proxy_STRING__INT32
#define hippo_dbus_proxy_VOID__VOID mugshot_linux_dbus_proxy_VOID__VOID
#define hippo_dbus_proxy_VOID__UINT32 mugshot_linux_dbus_proxy_VOID__UINT32
#define hippo_dbus_proxy_call_method_async mugshot_linux_dbus_proxy_call_method_async
#define hippo_dbus_proxy_call_method_async_appender mugshot_linux_dbus_proxy_call_method_async_appender
#define hippo_dbus_proxy_call_method_async_valist mugshot_linux_dbus_proxy_call_method_async_valist
#define hippo_dbus_proxy_call_method_sync mugshot_linux_dbus_proxy_call_method_sync
#define hippo_dbus_proxy_call_method_sync_appender mugshot_linux_dbus_proxy_call_method_sync_appender
#define hippo_dbus_proxy_call_method_sync_valist mugshot_linux_dbus_proxy_call_method_sync_valist
#define hippo_dbus_proxy_finish_method_call_freeing_reply mugshot_linux_dbus_proxy_finish_method_call_freeing_reply
#define hippo_dbus_proxy_finish_method_call_keeping_reply mugshot_linux_dbus_proxy_finish_method_call_keeping_reply
#define hippo_dbus_proxy_new mugshot_linux_dbus_proxy_new
#define hippo_dbus_proxy_set_method_prefix mugshot_linux_dbus_proxy_set_method_prefix
#define hippo_dbus_proxy_unref mugshot_linux_dbus_proxy_unref


G_END_DECLS

#endif /* __HIPPO_DBUS_HELPER_RENAME_H__ */
