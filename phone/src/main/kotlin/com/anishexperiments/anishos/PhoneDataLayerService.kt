package com.anishexperiments.anishos

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneDataLayerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            REQUEST_PATH -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_PENDING_NODE, event.sourceNodeId)
                    .putLong(KEY_REQUESTED_AT, System.currentTimeMillis())
                    .putBoolean(KEY_PENDING, true)
                    .apply()
            }
            LINK_REVOKED_PATH -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_LINKED_NODE)
                    .putBoolean(KEY_LINKED, false)
                    .putBoolean(KEY_PENDING, false)
                    .apply()
            }
            "/anishos/health/metrics" -> {
                val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                if (!prefs.getBoolean(KEY_LINKED, false) ||
                    prefs.getString(KEY_LINKED_NODE, null) != event.sourceNodeId
                ) return
                val payload = try { String(event.data, Charsets.UTF_8) } catch (_: Exception) { return }
                prefs.edit().putString(KEY_LATEST_HEALTH, payload)
                    .putLong(KEY_HEALTH_RECEIVED_AT, System.currentTimeMillis()).apply()
                HealthCloudSync.enqueue(this, payload)
            }
            else -> super.onMessageReceived(event)
        }
    }

    companion object {
        const val REQUEST_PATH = "/anishos/pair/request"
        const val LINK_ACCEPTED_PATH = "/anishos/pair/accepted"
        const val LINK_REVOKED_PATH = "/anishos/pair/revoked"
        const val PREFS = "anishos_companion_link"
        const val KEY_PENDING_NODE = "pending_node"
        const val KEY_REQUESTED_AT = "requested_at"
        const val KEY_PENDING = "pending"
        const val KEY_LINKED_NODE = "linked_node"
        const val KEY_LINKED = "linked"
        const val KEY_LATEST_HEALTH = "latest_health"
        const val KEY_HEALTH_RECEIVED_AT = "health_received_at"
    }
}
