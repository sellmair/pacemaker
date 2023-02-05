package io.sellmair.broadheart

import kotlin.math.roundToInt

@JvmInline
value class HeartRate(val value: Float) : Comparable<HeartRate> {
    constructor(value: Int): this(value.toFloat())
    override fun compareTo(other: HeartRate): Int {
        return this.value.compareTo(other.value)
    }

    override fun toString(): String {
        return value.roundToInt().toString()
    }
}