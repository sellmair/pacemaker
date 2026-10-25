package io.sellmair.pacemaker

import android.content.SharedPreferences
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock
import kotlin.time.Instant

/** Validates inbound watch messages before they can change the selected source or emit a reading. */
internal class WatchCompanionMessageActor(
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val transport: WatchCompanionTransport,
    private val stopDelivery: WatchMeasurementStopDelivery,
    private val userService: UserService
) {
    private val events = scope.coroutineContext.eventsOrThrow
    private val states = scope.coroutineContext.statesOrThrow

    fun start() {
        scope.collectEventsAsync<WatchCompanionInboundMessage> { inbound ->
            val accepted = handle(inbound.nodeId, inbound.message)
            if (inbound.message is WatchCompanionMessage.Hello) {
                transport.sendTo(inbound.nodeId, WatchCompanionMessage.Ack(inbound.message.messageId, accepted))
            }
        }
    }

    private suspend fun handle(peerId: String, message: WatchCompanionMessage): Boolean = when (message) {
        is WatchCompanionMessage.Ack -> {
            stopDelivery.reconcileAck(peerId, message)
            transport.onMessage(peerId, message)
            true
        }
        is WatchCompanionMessage.Hello -> {
            events.emit(WatchCompanionSnapshotRequested(peerId))
            true
        }
        is WatchCompanionMessage.HeartRate -> handleHeartRate(peerId, message)
        is WatchCompanionMessage.MeasurementStop,
        is WatchCompanionMessage.Configure, is WatchCompanionMessage.GroupSnapshot -> false
    }

    private suspend fun handleHeartRate(peerId: String, message: WatchCompanionMessage.HeartRate): Boolean {
        val me = userService.me()
        if (message.userId != me.id.value.toString()) return false
        val sensorId = HeartRateSensorId(WATCH_COMPANION_SOURCE_ID)
        val selected = states.getState(SelectedHeartRateSourceState).value.source
        if (selected?.id != sensorId.value || !acceptsWatchMeasurementFrom(selected.companionDeviceId, peerId)) return false
        val stoppedRunIds = prefs.getStringSet("watch-measurement-stopped-run-ids", emptySet()).orEmpty()

        if (message.bpm == null) {
            val activeRunId = prefs.getString("watch-measurement-run-id", null)
            val activeRunNodeId = prefs.getString("watch-measurement-run-node-id", null)
            val incomingRunId = message.measurementRunId
            val activeRunBelongsToPeer = activeRunNodeId == null || activeRunNodeId == peerId
            if (incomingRunId != null) {
                val editor = prefs.edit().putStringSet(
                    "watch-measurement-stopped-run-ids",
                    (stoppedRunIds + incomingRunId).toList().takeLast(64).toSet()
                )
                if (activeRunBelongsToPeer && activeRunId == incomingRunId) {
                    editor.remove("watch-measurement-run-id").remove("watch-measurement-run-node-id")
                }
                editor.apply()
                if (activeRunBelongsToPeer && activeRunId != null && activeRunId != incomingRunId) return true
            }
            states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(selected.copy(
                availability = HeartRateSource.Availability.DISCONNECTED,
                heartRate = null,
                measuredAtEpochMillis = null
            )))
            return true
        }

        val bpm = message.bpm
        if (bpm !in 25..240 ||
            Clock.System.now().toEpochMilliseconds() - message.measuredAtEpochMillis !in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds
        ) return false
        if (message.measurementRunId != null && message.measurementRunId in stoppedRunIds) return false
        message.measurementRunId?.let { runId ->
            val previousRun = prefs.getString("watch-measurement-run-id", null)
            if (previousRun != null && previousRun != runId) {
                prefs.edit().putStringSet(
                    "watch-measurement-stopped-run-ids",
                    (stoppedRunIds + previousRun).toList().takeLast(64).toSet()
                ).apply()
            }
            prefs.edit().putString("watch-measurement-run-id", runId)
                .putString("watch-measurement-run-node-id", peerId).apply()
        }
        userService.linkSensor(me, sensorId)
        // Linking may suspend while the user switches or disconnects the selected source.
        val current = states.getState(SelectedHeartRateSourceState).value.source
        if (current?.id != sensorId.value || !acceptsWatchMeasurementFrom(current.companionDeviceId, peerId)) return false
        states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(current.copy(
            availability = HeartRateSource.Availability.AVAILABLE,
            heartRate = HeartRate(bpm), measuredAtEpochMillis = message.measuredAtEpochMillis
        )))
        events.emit(HeartRateMeasurementEvent(
            HeartRate(bpm), sensorId, Instant.fromEpochMilliseconds(message.measuredAtEpochMillis)
        ))
        return true
    }
}
