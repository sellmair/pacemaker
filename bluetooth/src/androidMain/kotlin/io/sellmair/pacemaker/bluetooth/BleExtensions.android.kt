package io.sellmair.pacemaker.bluetooth

import android.bluetooth.BluetoothDevice

internal val BluetoothDevice.deviceId: BleDeviceId get() = BleDeviceId(address)
