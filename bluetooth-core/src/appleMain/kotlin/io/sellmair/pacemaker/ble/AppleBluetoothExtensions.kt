package io.sellmair.pacemaker.ble

import platform.CoreBluetooth.CBCentral
import platform.CoreBluetooth.CBPeripheral

val CBPeripheral.deviceId: BleDeviceId get() = BleDeviceId(this.identifier.UUIDString)
val CBCentral.deviceId: BleDeviceId get() = BleDeviceId(this.identifier.UUIDString)
