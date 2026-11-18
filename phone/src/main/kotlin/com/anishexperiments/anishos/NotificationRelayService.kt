package com.anishexperiments.anishos

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

/**
 * User-enabled, local-only notification bridge.
 * The relay is OFF by default and forwards text only after notification access
 * has been granted in Android Settings and the in-app switch is enabled.
 */
class NotificationRelayService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_RELAY_ENABLED, false)) return
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.take(160).orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.take(500).orEmpty()
        if (title.isBlank() && body.isBlank()) return

        val linkPrefs = getSharedPreferences(PhoneDataLayerService.PREFS, MODE_PRIVATE)
        if (!linkPrefs.getBoolean(PhoneDataLayerService.KEY_LINKED, false)) return
        val watchNode = linkPrefs.getString(PhoneDataLayerService.KEY_LINKED_NODE, null)
            ?.takeIf { it.isNotBlank() } ?: return

        val payload = JSONObject()
            .put("package", sbn.packageName.take(120))
            .put("title", title)
            .put("text", body)
            .put("postedAt", sbn.postTime)
            .toString()
            .toByteArray(Charsets.UTF_8)

        Wearable.getMessageClient(this)
            .sendMessage(watchNode, NOTIFICATION_PATH, payload)
    }

    companion object {
        const val PREFS = "anishos_notification_privacy"
        const val KEY_RELAY_ENABLED = "relay_enabled"
        const val NOTIFICATION_PATH = "/anishos/notifications/post"
    }
}
