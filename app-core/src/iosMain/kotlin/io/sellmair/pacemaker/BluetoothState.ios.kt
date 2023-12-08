package io.sellmair.pacemaker

import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways


internal actual suspend fun isBluetoothEnabled(): Boolean {
    return true
    //return CBCentralManager().state != CBCentralManagerStatePoweredOff
}

internal actual suspend fun isBluetoothPermissionGranted(): Boolean {
    return CBManager.authorization == CBManagerAuthorizationAllowedAlways
}
