package io.sellmair.broadheart.bluetooth

import platform.CoreBluetooth.CBUUID
import platform.CoreBluetooth.CBUUID.Companion.UUIDWithString

actual typealias BleUUID = CBUUID

actual fun BleUUID(value: String): BleUUID = UUIDWithString(value)