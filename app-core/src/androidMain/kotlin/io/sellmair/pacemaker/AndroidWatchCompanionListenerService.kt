package io.sellmair.pacemaker

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import androidx.core.content.ContextCompat

/** System-woken bridge for phone messages; backend returns application-level command ACKs. */
class AndroidWatchCompanionListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WATCH_COMPANION_MESSAGE_PATH) return
        val intent = Intent(this, AndroidApplicationBackend::class.java)
            .setAction(AndroidApplicationBackend.ACTION_DATA_LAYER_MESSAGE)
            .putExtra(AndroidApplicationBackend.EXTRA_NODE_ID, event.sourceNodeId)
            .putExtra(AndroidApplicationBackend.EXTRA_PAYLOAD, event.data)
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (_: RuntimeException) {
            // Android may disallow a background FGS launch. Listener wakes again on
            // the next message; no HR samples are persisted for later replay.
        }
    }
}
