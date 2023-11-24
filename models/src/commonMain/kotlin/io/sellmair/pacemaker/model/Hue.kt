package io.sellmair.pacemaker.model

import okio.Buffer
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

@JvmInline
value class Hue private constructor(val value: Float) {
    companion object {
        fun safe(value: Float): Hue {
            return Hue(value.absoluteValue % 360f)
        }
    }
}


fun Hue(data: ByteArray): Hue? {
    return runCatching {
        val bits = Buffer().write(data).readInt()
        return Hue.safe(Float.fromBits(bits))
    }.getOrNull()
}

fun Hue.encodeToByteArray(): ByteArray {
    return Buffer().writeInt(value.toBits()).readByteArray()
}