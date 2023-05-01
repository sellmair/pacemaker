package io.sellmair.pacemaker.ble

import java.util.*

actual fun BleUUID(value: String): BleUUID = UUID.fromString(value)

actual typealias BleUUID = UUID

fun BleUUID(uuid: UUID) : BleUUID = uuid