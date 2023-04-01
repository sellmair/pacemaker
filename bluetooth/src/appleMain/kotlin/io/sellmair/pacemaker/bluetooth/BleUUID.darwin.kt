package io.sellmair.pacemaker.bluetooth

import platform.CoreBluetooth.CBUUID
import platform.CoreBluetooth.CBUUID.Companion.UUIDWithString

actual typealias BleUUID = CBUUID

actual fun BleUUID(value: String): BleUUID = UUIDWithString(value)