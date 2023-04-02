package io.sellmair.pacemaker.ble

import kotlin.jvm.JvmInline

@JvmInline
value class BleDeviceId(val value: String) {
    override fun toString(): String = value
}