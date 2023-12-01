package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.isBluetoothEnabled
import io.sellmair.pacemaker.ble.isBluetoothPermissionGranted

internal actual suspend fun isBluetoothEnabled(): Boolean {
    return androidContext().isBluetoothEnabled()
}

internal actual suspend fun isBluetoothPermissionGranted(): Boolean {
    return androidContext().isBluetoothPermissionGranted()
}
