package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.evas.State
import io.sellmair.evas.Event
import kotlin.time.Duration.Companion.seconds

/** The currently selected input for this participant. Only one source is active at a time. */
data class HeartRateSource(
    val id: String,
    val kind: Kind,
    val availability: Availability,
    val heartRate: HeartRate? = null,
    /** Unix epoch milliseconds at which this value was measured by the source. */
    val measuredAtEpochMillis: Long? = null,
    val connectionName: String? = null,
    /** Platform-local identity of an explicitly selected companion device. */
    val companionDeviceId: String? = null
) {
    enum class Kind { BLE_SENSOR, COMPANION_WATCH, ONBOARD_WATCH }
    enum class Availability { AVAILABLE, CONNECTING, DISCONNECTED, PERMISSION_REQUIRED, UNSUPPORTED }

    /** Do not display, save, alert on, or rebroadcast measurements older than 15 seconds. */
    fun currentHeartRate(nowEpochMillis: Long): HeartRate? {
        val value = heartRate ?: return null
        val measuredAt = measuredAtEpochMillis ?: return null
        val age = nowEpochMillis - measuredAt
        return value.takeIf { age >= 0 && age <= MAX_AGE.inWholeMilliseconds && availability == Availability.AVAILABLE }
    }

    companion object {
        val MAX_AGE = 15.seconds
    }
}

/** Explicit input choice for this device. A disconnected selected watch does not silently fall back to a strap. */
data class SelectedHeartRateSourceState(val source: HeartRateSource?) : State {
    companion object Key : State.Key<SelectedHeartRateSourceState> {
        override val default = SelectedHeartRateSourceState(null)
    }
}

data class HeartRateSourceSelectionIntent(val source: HeartRateSource?) : Event

/** Stable watch-side identifier for a phone SQL session mirrored to the companion. */
fun phonePublishedWatchRunId(phoneSessionId: String): String = "phone-session-$phoneSessionId"

/** The watch may stop either a run it started or the currently mirrored phone run. */
fun isWatchStopForRun(command: WatchSessionCommand, requestedRunId: String?, publishedRunId: String?): Boolean =
    command.action == WatchSessionCommand.Action.STOP &&
        (command.runId == requestedRunId || command.runId == publishedRunId)

fun isWatchStopForSession(
    command: WatchSessionCommand,
    activeSessionId: String?,
    requestedRunId: String?,
    requestedSessionId: String?,
    publishedRunId: String?,
    publishedSessionId: String?
): Boolean = command.action == WatchSessionCommand.Action.STOP && (
    command.runId == requestedRunId && (activeSessionId == null || requestedSessionId == activeSessionId) ||
        command.runId == publishedRunId && (activeSessionId == null || publishedSessionId == activeSessionId)
    )

data class WatchCompanionCandidate(
    val id: String,
    val name: String,
    val paired: Boolean = true,
    val connected: Boolean = false
)

/** Guards ephemeral measurements and phone-requested disconnect stops against delayed messages. */
class WatchMeasurementRunGate(
    activeRunId: String? = null,
    lastStopGeneration: Long = 0L
) {
    var activeRunId: String? = activeRunId
        private set
    var lastStopGeneration: Long = lastStopGeneration
        private set

    fun start(runId: String) {
        require(runId.isNotBlank())
        activeRunId = runId
    }

    fun acceptsHeartRate(runId: String?): Boolean = runId != null && runId == activeRunId

    /** A stale stop is harmless; a current stop only applies to its own run. */
    fun shouldStop(targetRunId: String?, generation: Long): Boolean =
        generation > lastStopGeneration && (targetRunId == null || targetRunId == activeRunId)

    fun stopped(generation: Long) {
        if (generation > lastStopGeneration) lastStopGeneration = generation
        activeRunId = null
    }
}

/** Keep an authenticated phone pin stable across discovery changes. */
fun resolvePinnedWatchPhoneNode(pinnedNodeId: String?, candidates: List<WatchCompanionCandidate>): String? {
    val reachable = candidates.filter { it.connected }
    return if (pinnedNodeId != null) pinnedNodeId.takeIf { id -> reachable.any { it.id == id } }
    else reachable.singleOrNull()?.id
}

fun acceptsWatchMeasurementFrom(selectedNodeId: String?, inboundNodeId: String?): Boolean =
    selectedNodeId != null && selectedNodeId == inboundNodeId

fun isLegacyBluetoothCompanionId(id: String?): Boolean = id == null ||
    Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$").matches(id)

/** Persist these run IDs at each endpoint so a delayed Start cannot undo an acknowledged Stop after process death. */
class WatchSessionStopTombstones(initialStoppedRunIds: Set<String> = emptySet(), private val capacity: Int = 256) {
    private val stoppedRunIds = LinkedHashSet(initialStoppedRunIds)

    init { require(capacity > 0) }

    fun markStopped(runId: String) {
        if (runId.isBlank()) return
        stoppedRunIds += runId
        while (stoppedRunIds.size > capacity) stoppedRunIds.remove(stoppedRunIds.first())
    }

    fun rejectsStart(runId: String): Boolean = runId in stoppedRunIds
    fun snapshot(): Set<String> = stoppedRunIds.toSet()
}

/** A paired companion can be remembered while it is offline; connected means its local BLE/WC link is live. */
data class WatchCompanionDeviceState(
    val paired: Boolean = false,
    val connected: Boolean = false,
    val name: String = "Wrist watch",
    val candidates: List<WatchCompanionCandidate> = emptyList(),
    val platformAvailable: Boolean = true
) : State {
    companion object Key : State.Key<WatchCompanionDeviceState> {
        override val default = WatchCompanionDeviceState()
    }
}

fun MeState.hasFreshHeartRate(nowEpochMillis: Long): Boolean {
    val measuredAt = measuredAtEpochMillis ?: return false
    val age = nowEpochMillis - measuredAt
    return heartRate != null && age in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds
}

fun MeState.heartRateForBroadcast(nowEpochMillis: Long): HeartRate? = heartRate.takeIf { hasFreshHeartRate(nowEpochMillis) }

fun acceptsHeartRateMeasurement(
    selectedSource: HeartRateSource?, ownUserId: String, measurementUserId: String, sensorId: String
): Boolean = measurementUserId != ownUserId || selectedSource == null || selectedSource.id == sensorId

/** Idempotency key carried by watch/phone session commands so a lost acknowledgement can be retried. */
data class WatchSessionCommand(
    val commandId: String,
    val action: Action,
    val issuedAtEpochMillis: Long,
    val runId: String,
    val revision: Long
) {
    enum class Action { START, STOP }
}

/** Rejects duplicate and out-of-order session state updates, including a delayed START after STOP. */
class WatchSessionCommandGate {
    private val commandResults = LinkedHashMap<String, Boolean>()
    private val revisionsByRun = mutableMapOf<String, Long>()
    private val previousOwnersByCommand = mutableMapOf<String, String?>()
    private var activeRunId: String? = null

    /** The phone publisher owns the current run even when the latest Start came from the phone. */
    fun synchronizeAuthoritativeRun(runId: String?) {
        activeRunId = runId
    }

    data class Decision(val accepted: Boolean, val shouldApply: Boolean)

    fun accept(command: WatchSessionCommand): Boolean = decide(command).accepted

    /** Replace the optimistic admission with the real result once the local action has finished. */
    fun complete(commandId: String, accepted: Boolean) {
        if (commandId in commandResults) {
            commandResults[commandId] = accepted
            if (!accepted) activeRunId = previousOwnersByCommand[commandId]
        }
        previousOwnersByCommand.remove(commandId)
    }

    fun decide(command: WatchSessionCommand): Decision {
        commandResults[command.commandId]?.let { return Decision(it, false) }
        if (command.runId.isBlank() || command.revision < 0) return Decision(remember(command.commandId, false), false)
        val previous = revisionsByRun[command.runId] ?: -1L
        if (command.revision <= previous) return Decision(remember(command.commandId, false), false)
        revisionsByRun[command.runId] = command.revision
        if (command.action == WatchSessionCommand.Action.START && activeRunId != null && activeRunId != command.runId) return Decision(remember(command.commandId, false), false)
        if (command.action == WatchSessionCommand.Action.STOP && activeRunId != null && activeRunId != command.runId) return Decision(remember(command.commandId, false), false)
        previousOwnersByCommand[command.commandId] = activeRunId
        activeRunId = when {
            command.action == WatchSessionCommand.Action.START -> command.runId
            activeRunId == command.runId -> null
            else -> activeRunId
        }
        return Decision(remember(command.commandId, true), true)
    }

    private fun remember(id: String, accepted: Boolean): Boolean {
        commandResults[id] = accepted
        while (commandResults.size > 128) {
            val oldest = commandResults.keys.first()
            commandResults.remove(oldest)
            previousOwnersByCommand.remove(oldest)
        }
        return accepted
    }
}

/** A small, local-only companion protocol. Transport adapters must send live messages only while reachable. */
sealed interface WatchCompanionMessage {
    val messageId: String

    /** First encrypted message after link setup; used to confirm the selected bonded companion. */
    data class Hello(override val messageId: String) : WatchCompanionMessage

    data class Configure(
        override val messageId: String,
        val userId: String,
        val userName: String,
        val colorHue: Float,
        val heartRateLimit: Int,
        val sourceSelected: Boolean = true,
    ) : WatchCompanionMessage

    data class HeartRate(
        override val messageId: String,
        val userId: String,
        val bpm: Int?,
        val measuredAtEpochMillis: Long,
        val measurementRunId: String? = null
    ) : WatchCompanionMessage

    data class Session(
        override val messageId: String,
        val command: WatchSessionCommand
    ) : WatchCompanionMessage

    /** Phone asks the watch to stop its local Health Services measurement. */
    data class MeasurementStop(
        override val messageId: String,
        val generation: Long,
        val measurementRunId: String? = null
    ) : WatchCompanionMessage

    data class Ack(
        override val messageId: String,
        val accepted: Boolean,
        val measurementRunId: String? = null
    ) : WatchCompanionMessage

    data class GroupSnapshot(
        override val messageId: String,
        val members: List<Member>
    ) : WatchCompanionMessage {
        data class Member(val userId: String, val name: String, val bpm: Int?, val limit: Int?, val measuredAtEpochMillis: Long?)
    }
}

/** Compact escaped wire format shared by the iOS and Wear OS local companion transports. */
object WatchCompanionCodec {
    fun encode(message: WatchCompanionMessage): ByteArray = encodeToString(message).encodeToByteArray()

    fun encodeToString(message: WatchCompanionMessage): String = when (message) {
        is WatchCompanionMessage.Hello -> listOf("P", message.messageId).frame()
        is WatchCompanionMessage.Configure -> listOf(
            "C", message.messageId, message.userId, message.userName, message.colorHue.toString(),
            message.heartRateLimit.toString(), message.sourceSelected.toString()
        ).frame()
        is WatchCompanionMessage.HeartRate -> listOf(
            "H", message.messageId, message.userId, message.bpm?.toString().orEmpty(),
            message.measuredAtEpochMillis.toString(), message.measurementRunId.orEmpty()
        ).frame()
        is WatchCompanionMessage.Session -> listOf(
            "S", message.messageId, message.command.commandId, message.command.action.name,
            message.command.issuedAtEpochMillis.toString(), message.command.runId, message.command.revision.toString()
        ).frame()
        is WatchCompanionMessage.MeasurementStop -> listOf(
            "M", message.messageId, message.generation.toString(), message.measurementRunId.orEmpty()
        ).frame()
        is WatchCompanionMessage.Ack -> listOf(
            "A", message.messageId, message.accepted.toString(), message.measurementRunId.orEmpty()
        ).frame()
        is WatchCompanionMessage.GroupSnapshot -> buildList {
            add("G")
            add(message.messageId)
            add(message.members.size.toString())
            message.members.forEach { member ->
                add(member.userId); add(member.name); add(member.bpm?.toString().orEmpty())
                add(member.limit?.toString().orEmpty()); add(member.measuredAtEpochMillis?.toString().orEmpty())
            }
        }.frame()
    }

    fun decode(bytes: ByteArray): WatchCompanionMessage? = decode(bytes.decodeToString())

    fun decode(frame: String): WatchCompanionMessage? = runCatching {
        val fields = frame.split('|').map(::decodeField)
        when (fields.firstOrNull()) {
            "P" -> WatchCompanionMessage.Hello(fields[1])
            "C" -> WatchCompanionMessage.Configure(fields[1], fields[2], fields[3], fields[4].toFloat(), fields[5].toInt(),
                fields.getOrNull(6)?.toBooleanStrictOrNull() ?: true)
            "H" -> WatchCompanionMessage.HeartRate(fields[1], fields[2], fields[3].toIntOrNull(), fields[4].toLong(), fields.getOrNull(5)?.ifBlank { null })
            "S" -> WatchCompanionMessage.Session(
                fields[1], WatchSessionCommand(fields[2], WatchSessionCommand.Action.valueOf(fields[3]),
                    fields[4].toLong(), fields[5], fields[6].toLong())
            )
            "M" -> WatchCompanionMessage.MeasurementStop(fields[1], fields[2].toLong(), fields.getOrNull(3)?.ifBlank { null })
            "A" -> WatchCompanionMessage.Ack(fields[1], fields[2].toBooleanStrict(), fields.getOrNull(3)?.ifBlank { null })
            "G" -> {
                val count = fields[2].toInt()
                require(count >= 0 && fields.size == 3 + count * 5)
                WatchCompanionMessage.GroupSnapshot(fields[1], (0 until count).map { index ->
                    val offset = 3 + index * 5
                    WatchCompanionMessage.GroupSnapshot.Member(
                        fields[offset], fields[offset + 1], fields[offset + 2].toIntOrNull(),
                        fields[offset + 3].toIntOrNull(), fields[offset + 4].toLongOrNull()
                    )
                })
            }
            else -> null
        }
    }.getOrNull()

    private fun List<String>.frame() = joinToString("|") { encodeField(it) }

    private fun encodeField(value: String): String = buildString {
        value.encodeToByteArray().forEach { byte ->
            val unsigned = byte.toUByte().toInt()
            if (unsigned in 'a'.code..'z'.code || unsigned in 'A'.code..'Z'.code ||
                unsigned in '0'.code..'9'.code || unsigned == '-'.code || unsigned == '_'.code ||
                unsigned == '.'.code || unsigned == '~'.code) append(unsigned.toChar())
            else {
                append('%')
                append(HEX[unsigned ushr 4])
                append(HEX[unsigned and 15])
            }
        }
    }

    private fun decodeField(value: String): String {
        val bytes = ByteArray(value.length)
        var out = 0
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                require(index + 2 < value.length)
                val high = value[index + 1].digitToInt(16)
                val low = value[index + 2].digitToInt(16)
                bytes[out++] = ((high shl 4) or low).toByte()
                index += 3
            } else {
                val char = value[index]
                val encoded = char.toString().encodeToByteArray()
                encoded.copyInto(bytes, out)
                out += encoded.size
                index++
            }
        }
        return bytes.copyOf(out).decodeToString()
    }

    private const val HEX = "0123456789ABCDEF"
}

const val WATCH_COMPANION_SOURCE_ID = "pacemaker-watch-companion"

/** Bounded de-duplication helper for transports that retry after a disconnect. */
class MessageDeduplicator(private val capacity: Int = 128) {
    private val seen = LinkedHashSet<String>()

    init { require(capacity > 0) }

    fun accept(messageId: String): Boolean {
        if (!seen.add(messageId)) return false
        while (seen.size > capacity) seen.remove(seen.first())
        return true
    }
}
