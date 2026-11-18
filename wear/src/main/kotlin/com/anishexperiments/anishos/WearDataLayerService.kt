package com.anishexperiments.anishos

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearDataLayerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            LINK_ACCEPTED_PATH -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_LINKED_NODE, event.sourceNodeId)
                    .putBoolean(KEY_LINKED, true)
                    .apply()
            }
            LINK_REVOKED_PATH -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_LINKED_NODE)
                    .putBoolean(KEY_LINKED, false)
                    .apply()
            }
            else -> super.onMessageReceived(event)
        }
    }

    companion object {
        const val REQUEST_PATH = "/anishos/pair/request"
        const val LINK_ACCEPTED_PATH = "/anishos/pair/accepted"
        const val LINK_REVOKED_PATH = "/anishos/pair/revoked"
        const val PREFS = "anishos_watch_link"
        const val KEY_LINKED_NODE = "linked_node"
        const val KEY_LINKED = "linked"
    }
}
