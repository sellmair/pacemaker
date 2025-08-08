package io.sellmair.pacemaker

internal actual suspend fun isBluetoothEnabled(): Boolean {
    return true
}

internal actual suspend fun isBluetoothPermissionGranted(): Boolean {
    return true
}
