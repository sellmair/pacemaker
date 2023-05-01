package io.sellmair.pacemaker.ble

import android.bluetooth.BluetoothDevice
import android.content.Context

internal class AndroidConnectableHardware(
    val context: Context,
    val device: BluetoothDevice,
    val callback: AndroidGattCallback
)

