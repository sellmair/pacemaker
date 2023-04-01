package io.sellmair.pacemaker.bluetooth

import platform.CoreBluetooth.CBCentral
import platform.CoreBluetooth.CBPeripheral

internal val CBPeripheral.deviceId: BleDeviceId get() = BleDeviceId(this.identifier.UUIDString)

internal val CBCentral.deviceId: BleDeviceId get() = BleDeviceId(this.identifier.UUIDString)

