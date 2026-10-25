package io.sellmair.pacemaker

import android.content.SharedPreferences
import io.sellmair.evas.Event
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class PendingMeasurementStop(
    val nodeId: String,
    val messageId: String,
    val generation: Long,
    val measurementRunId: String?
)

internal data class WatchCompanionSnapshotRequested(val nodeId: String) : Event

internal interface WatchMeasurementStopStore {
    fun load(): List<PendingMeasurementStop>
    fun save(queue: List<PendingMeasurementStop>)
    fun nextGeneration(): Long
    fun activeRunFor(nodeId: String): String?
    fun markStopped(nodeId: String, runId: String?)
}

/** Keeps the currently deployed queue format and run tombstones in phone preferences. */
internal class SharedPreferencesWatchMeasurementStopStore(private val prefs: SharedPreferences) : WatchMeasurementStopStore {
    override fun load(): List<PendingMeasurementStop> {
        val encoded = prefs.getString("measurement-stop-queue-v1", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            (0 until array.length()).mapNotNull { index ->
                val row = array.optJSONObject(index) ?: return@mapNotNull null
                PendingMeasurementStop(
                    row.getString("node"), row.getString("message"), row.getLong("generation"),
                    row.optString("run").takeIf { it.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
    }

    override fun save(queue: List<PendingMeasurementStop>) {
        val editor = prefs.edit()
        if (queue.isEmpty()) editor.remove("measurement-stop-queue-v1")
        else editor.putString("measurement-stop-queue-v1", JSONArray().apply {
            queue.forEach { pending ->
                put(JSONObject().put("node", pending.nodeId).put("message", pending.messageId)
                    .put("generation", pending.generation).put("run", pending.measurementRunId ?: ""))
            }
        }.toString())
        editor.apply()
    }

    override fun nextGeneration(): Long = (prefs.getLong("measurement-stop-generation", 0L) + 1L).also {
        prefs.edit().putLong("measurement-stop-generation", it).apply()
    }

    override fun activeRunFor(nodeId: String): String? = prefs.getString("watch-measurement-run-id", null)
        .takeIf { prefs.getString("watch-measurement-run-node-id", null) == nodeId }

    override fun markStopped(nodeId: String, runId: String?) {
        if (runId == null) return
        val stopped = prefs.getStringSet("watch-measurement-stopped-run-ids", emptySet()).orEmpty()
        val editor = prefs.edit().putStringSet("watch-measurement-stopped-run-ids",
            (stopped + runId).toList().takeLast(64).toSet())
        if (prefs.getString("watch-measurement-run-id", null) == runId &&
            prefs.getString("watch-measurement-run-node-id", null) == nodeId
        ) editor.remove("watch-measurement-run-id").remove("watch-measurement-run-node-id")
        editor.apply()
    }
}

/** Owns the durable, ordered stop queue. A retry always targets the originally selected watch. */
internal class WatchMeasurementStopDelivery(
    private val store: WatchMeasurementStopStore,
    private val scope: CoroutineScope,
    private val transport: WatchCompanionTransport
) {
    private val events = scope.coroutineContext.eventsOrThrow
    private val states = scope.coroutineContext.statesOrThrow
    private val mutex = Mutex()
    private var retryJob: Job? = null

    fun pendingTargets(): Set<String> = store.load().mapTo(mutableSetOf()) { it.nodeId }
    fun hasPendingFor(nodeId: String): Boolean = store.load().any { it.nodeId == nodeId }

    fun enqueue(nodeId: String) {
        val generation = store.nextGeneration()
        val runId = store.activeRunFor(nodeId)
        store.save(store.load() + PendingMeasurementStop(
            nodeId, "measurement-stop-$generation-${UUID.randomUUID()}", generation, runId
        ))
    }

    fun reconcileAck(peerId: String, message: WatchCompanionMessage.Ack) {
        if (!message.accepted || message.measurementRunId == null) return
        val queue = store.load()
        val updated = queue.map { pending ->
            if (pending.messageId == message.messageId && pending.nodeId == peerId)
                pending.copy(measurementRunId = message.measurementRunId) else pending
        }
        if (updated != queue) store.save(updated)
    }

    /** Remove a command only after the transport receives its matching peer and ID ACK. */
    suspend fun drain(): Boolean = mutex.withLock {
        while (true) {
            val pending = store.load().firstOrNull() ?: break
            val accepted = transport.sendReliableToOutcome(
                pending.nodeId,
                WatchCompanionMessage.MeasurementStop(
                    pending.messageId, pending.generation, pending.measurementRunId
                ),
                retries = 3
            )
            if (accepted != true) {
                scheduleRetry()
                return@withLock false
            }
            val queue = store.load()
            val acknowledged = queue.firstOrNull()?.takeIf {
                it.nodeId == pending.nodeId && it.messageId == pending.messageId && it.generation == pending.generation
            } ?: return@withLock false
            store.save(queue.drop(1))
            store.markStopped(pending.nodeId, acknowledged.measurementRunId)
            events.emitAsync(WatchCompanionSnapshotRequested(pending.nodeId))
        }
        states.selectedWatchNodeId()?.let { events.emitAsync(WatchCompanionSnapshotRequested(it)) }
        true
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            var waitMillis = 5_000L
            repeat(5) {
                delay(waitMillis)
                if (drain()) return@launch
                waitMillis = (waitMillis * 2).coerceAtMost(30_000L)
            }
        }
    }

}
