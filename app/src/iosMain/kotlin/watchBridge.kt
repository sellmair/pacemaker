package io.sellmair.pacemaker

import io.sellmair.evas.emit
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** Entry point used by the iOS WatchConnectivity delegate for live wrist readings. */
object IosWatchBridge {
    private val sensorId = HeartRateSensorId(WATCH_COMPANION_SOURCE_ID)
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

}

private const val JSON_HEX = "0123456789abcdef"
