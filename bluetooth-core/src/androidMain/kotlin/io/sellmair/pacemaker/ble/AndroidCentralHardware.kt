package io.sellmair.pacemaker.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.delay

internal suspend fun AndroidCentralHardware(context: Context): AndroidCentralHardware {
    /* Wait for bluetooth permission */
    while (
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) delay(1000)

    val manager = context.getSystemService(BluetoothManager::class.java)
    return AndroidCentralHardware(context, manager)
}

internal class AndroidCentralHardware(
    val context: Context,
    val manager: BluetoothManager
)
