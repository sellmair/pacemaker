package io.sellmair.pacemaker

import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerStatePoweredOff
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways


internal actual suspend fun isBluetoothEnabled(): Boolean {
    return CBCentralManager().state != CBCentralManagerStatePoweredOff
}

internal actual suspend fun isBluetoothPermissionGranted(): Boolean {
    return CBManager.authorization() == CBManagerAuthorizationAllowedAlways
}
