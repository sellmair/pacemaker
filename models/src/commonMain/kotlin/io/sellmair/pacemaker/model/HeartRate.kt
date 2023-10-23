package io.sellmair.pacemaker.model

import okio.Buffer
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

@JvmInline
value class HeartRate(val value: Float) : Comparable<HeartRate> {
    constructor(value: Int) : this(value.toFloat())

    override fun compareTo(other: HeartRate): Int {
        return this.value.compareTo(other.value)
    }

    override fun toString(): String {
        return value.roundToInt().toString()
    }
}

fun HeartRate.encodeToByteArray(): ByteArray {
    return Buffer().writeInt(value.toBits()).readByteArray()
}

fun HeartRate(data: ByteArray): HeartRate? {
    val bits = runCatching {
        Buffer().write(data).readInt()
    }.getOrNull() ?: return null

    return HeartRate(Float.fromBits(bits))
}

infix fun ClosedRange<HeartRate>.step(n: Int): IntProgression {
    return (start.value.roundToInt()..endInclusive.value.roundToInt()) step n
}
