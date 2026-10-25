package io.sellmair.pacemaker

/** Live Android phone and Wear OS messages sent over Wearable Data Layer. */
sealed interface WatchCompanionMessage {
    val messageId: String

    /** Watch hello confirms the selected Data Layer peer before live readings. */
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

/** Compact escaped wire format for the Android phone and Wear OS Data Layer. */
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
            "P" -> {
                require(fields.size == 2)
                WatchCompanionMessage.Hello(fields[1])
            }
            "C" -> {
                require(fields.size == 7)
                WatchCompanionMessage.Configure(fields[1], fields[2], fields[3], fields[4].toFloat(),
                    fields[5].toInt(), fields[6].toBooleanStrict())
            }
            "H" -> {
                require(fields.size == 6)
                WatchCompanionMessage.HeartRate(fields[1], fields[2], fields[3].takeIf { it.isNotEmpty() }?.toInt(),
                    fields[4].toLong(), fields[5].ifBlank { null })
            }
            "M" -> {
                require(fields.size == 4)
                WatchCompanionMessage.MeasurementStop(fields[1], fields[2].toLong(), fields[3].ifBlank { null })
            }
            "A" -> {
                require(fields.size == 4)
                WatchCompanionMessage.Ack(fields[1], fields[2].toBooleanStrict(), fields[3].ifBlank { null })
            }
            "G" -> {
                require(fields.size >= 3)
                val count = fields[2].toInt()
                require((fields.size - 3) % 5 == 0 && count == (fields.size - 3) / 5)
                WatchCompanionMessage.GroupSnapshot(fields[1], (0 until count).map { index ->
                    val offset = 3 + index * 5
                    WatchCompanionMessage.GroupSnapshot.Member(
                        fields[offset], fields[offset + 1], fields[offset + 2].takeIf { it.isNotEmpty() }?.toInt(),
                        fields[offset + 3].takeIf { it.isNotEmpty() }?.toInt(),
                        fields[offset + 4].takeIf { it.isNotEmpty() }?.toLong()
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
