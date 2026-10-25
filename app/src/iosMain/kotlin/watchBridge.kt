package io.sellmair.pacemaker

import io.sellmair.evas.emit
import io.sellmair.evas.flow
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.time.Instant
import kotlin.time.Clock

/** Entry point used by the iOS WatchConnectivity delegate for live wrist readings. */
object IosWatchBridge {
    private val sensorId = HeartRateSensorId(WATCH_COMPANION_SOURCE_ID)
    private val commandGate = WatchSessionCommandGate()
    private val pendingSessionCompletions = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()
    private var watchOwnedRunId: String? = null
    private var watchOwnedSessionId: Long? = null
    private var watchStartPending = false

    fun isWatchHeartRateSourceSelected(): Boolean =
        backend.states.getState(SelectedHeartRateSourceState).value?.source?.id == sensorId.value

    fun receiveHeartRateBpm(bpm: Int, measuredAtEpochMillis: Long) {
        if (bpm !in 25..240) return
        val source = backend.states.getState(SelectedHeartRateSourceState).value?.source ?: return
        if (source.id != sensorId.value) return
        backend.states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(source.copy(
            availability = HeartRateSource.Availability.AVAILABLE,
            heartRate = HeartRate(bpm), measuredAtEpochMillis = measuredAtEpochMillis
        )))
        backend.launch {
            backend.userService.linkSensor(backend.userService.me(), sensorId)
            backend.events.emit(
                HeartRateMeasurementEvent(
                    heartRate = HeartRate(bpm),
                    sensorId = sensorId,
                    time = Instant.fromEpochMilliseconds(measuredAtEpochMillis)
                )
            )
        }
    }

    fun clearWatchHeartRate() {
        val source = backend.states.getState(SelectedHeartRateSourceState).value?.source ?: return
        if (source.id != sensorId.value) return
        backend.states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(source.copy(
            availability = HeartRateSource.Availability.DISCONNECTED,
            heartRate = null, measuredAtEpochMillis = null
        )))
    }

    fun setWatchCompanionState(paired: Boolean, connected: Boolean) {
        backend.states.setState(WatchCompanionDeviceState, WatchCompanionDeviceState(
            paired = paired, connected = connected, name = "Apple Watch",
            candidates = if (paired) listOf(WatchCompanionCandidate("apple-watch", "Apple Watch", true, connected)) else emptyList()
        ))
    }

    fun receiveSessionCommand(commandId: String, runId: String, revision: Long, action: String,
                              completion: (Boolean) -> Unit) {
        val command = WatchSessionCommand(
            commandId = commandId,
            action = if (action == "stop") WatchSessionCommand.Action.STOP else WatchSessionCommand.Action.START,
            issuedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            runId = runId,
            revision = revision
        )
        val decision = commandGate.decide(command)
        if (!decision.accepted) {
            completion(decision.accepted)
            return
        }
        if (!decision.shouldApply) {
            val pending = pendingSessionCompletions[commandId]
            if (pending != null) pending += completion else completion(true)
            return
        }
        pendingSessionCompletions[commandId] = mutableListOf(completion)
        fun complete(accepted: Boolean) {
            commandGate.complete(commandId, accepted)
            pendingSessionCompletions.remove(commandId).orEmpty().forEach { it(accepted) }
        }
        backend.launch {
            val activeSession = backend.states.getState(ActiveSessionState).value?.session
            if (command.action == WatchSessionCommand.Action.START) {
                watchOwnedRunId = runId
                if (activeSession != null) {
                    watchOwnedSessionId = activeSession.id.value
                    watchStartPending = false
                    complete(true)
                } else {
                    watchOwnedSessionId = null
                    watchStartPending = true
                    backend.events.emit(ActiveSessionIntent.Start)
                    val started = ActiveSessionState.flow().first { it?.session != null }?.session
                    if (started != null) watchOwnedSessionId = started.id.value
                    watchStartPending = false
                    complete(started != null)
                }
            }
            if (command.action == WatchSessionCommand.Action.STOP) {
                var stopped = false
                if (activeSession != null && watchOwnedRunId == runId) {
                    backend.events.emit(ActiveSessionIntent.Stop)
                    stopped = kotlinx.coroutines.withTimeoutOrNull(5_000) {
                        ActiveSessionState.flow().first { it?.session == null }
                    } != null
                } else {
                    stopped = activeSession == null && watchOwnedRunId == runId
                }
                if (!stopped) { complete(false); return@launch }
                clearWatchHeartRate()
                watchOwnedRunId = null
                watchOwnedSessionId = null
                watchStartPending = false
                complete(true)
            }
        }
    }

    /** JSON snapshot used as WCSession application context (configuration and current nearby group). */
    fun watchConfigurationJson(): String {
        val me = backend.states.getState(MeState).value ?: return "{}"
        fun quote(value: String): String = buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (char.code < 0x20) {
                        append("\\u00")
                        append(JSON_HEX[(char.code ushr 4) and 15])
                        append(JSON_HEX[char.code and 15])
                    } else append(char)
                }
            }
            append('"')
        }
        val members = backend.states.getState(GroupState.Key).value?.members.orEmpty().filterNot { it.user.id == me.me.id }
        val groupJson = members.joinToString(",") { member ->
            "{\"userId\":${quote(member.user.id.value.toString())}," +
                "\"name\":${quote(member.user.name)}," +
                "\"bpm\":${member.heartRate.value.toInt().takeUnless { member.isConnectionLost } ?: "null"}," +
                "\"limit\":${member.heartRateLimit?.value?.toInt() ?: "null"}," +
                "\"measuredAt\":${member.measuredAtEpochMillis}}"
        }
        val color = backend.states.getState(MeColorState).value?.color?.hue ?: UserColors.defaultHue(me.me.id)
        return "{\"userId\":${quote(me.me.id.value.toString())}," +
            "\"name\":${quote(me.me.name)},\"limit\":${me.heartRateLimit.value.toInt()}," +
            "\"colorHue\":$color,\"group\":[$groupJson]}"
    }

    fun watchGroupSnapshotJson(): String = watchConfigurationJson()

    fun watchActiveSessionJson(): String {
        if (!isWatchHeartRateSourceSelected()) return "{}"
        val session = backend.states.getState(ActiveSessionState).value?.session ?: run {
            if (!watchStartPending) {
                watchOwnedRunId = null
                watchOwnedSessionId = null
            }
            return "{}"
        }
        // Map phone-originated SQL sessions to the watch command identity. A later
        // Stop initiated on the watch must stop this same phone session even though
        // the phone did not receive a watch-originated Start command.
        if (watchOwnedRunId == null) {
            watchOwnedRunId = phonePublishedWatchRunId(session.id.value.toString())
            watchOwnedSessionId = session.id.value
        }
        if (watchOwnedRunId != null && watchOwnedSessionId == null && !watchStartPending) watchOwnedSessionId = session.id.value
        if (watchOwnedSessionId != null && watchOwnedSessionId != session.id.value) {
            watchOwnedRunId = null
            watchOwnedSessionId = session.id.value
        }
        return "{\"runId\":\"${watchOwnedRunId ?: session.id.value.toString()}\"}"
    }

}

private const val JSON_HEX = "0123456789abcdef"
