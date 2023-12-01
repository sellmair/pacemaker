package io.sellmair.pacemaker.ble

import android.bluetooth.BluetoothManager
import android.content.Context

internal fun AndroidCentralHardware(context: Context): AndroidCentralHardware {
    val manager = context.getSystemService(BluetoothManager::class.java)
    return AndroidCentralHardware(context, manager)
}

internal class AndroidCentralHardware(
    val context: Context,
    val manager: BluetoothManager
)
