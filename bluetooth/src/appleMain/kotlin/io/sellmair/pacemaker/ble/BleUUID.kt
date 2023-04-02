package io.sellmair.pacemaker.ble

import platform.CoreBluetooth.CBUUID
import platform.CoreBluetooth.CBUUID.Companion.UUIDWithString

actual fun BleUUID(value: String): BleUUID = UUIDWithString(value)
actual typealias BleUUID = CBUUID