package io.sellmair.pacemaker.ble

import android.bluetooth.BluetoothDevice

internal val BluetoothDevice.deviceId: BleDeviceId get() = BleDeviceId(address)
