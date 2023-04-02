package io.sellmair.pacemaker.ble

fun BleStatusCode(value: Int): BleStatusCode {
    return BleKnownStatusCode.byCode[value] ?: BleUnknownStatusCode(value)
}

sealed interface BleStatusCode {
    val code: Int
}

val BleStatusCode.isSuccess get() = this == BleKnownStatusCode.Success
fun BleStatusCode.toInt() = code
fun BleStatusCode.toLong() = code.toLong()

enum class BleKnownStatusCode(override val code: Int) : BleStatusCode {
    Success(0),
    IllegalOffset(7),
    GattError(133),
    InternalError(129),
    UnknownAttribute(0x010A);

    override fun toString(): String {
        return "BleStatusCode($name)"
    }

    companion object {
        val values = values().toList()
        val codes = values.map { it.code }.toSet()
        val byCode = values.associateBy { it.code }
    }
}

class BleUnknownStatusCode internal constructor(override val code: Int) : BleStatusCode {
    init {
        val knownStatusCode = BleKnownStatusCode.byCode[code]
        if (knownStatusCode != null) {
            throw IllegalArgumentException("code '$code' is known as '$knownStatusCode'")
        }
    }

    override fun toString(): String {
        return "BleStatusCode($code)"
    }
}
