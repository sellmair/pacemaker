package io.sellmair.pacemaker.bluetooth

import android.bluetooth.BluetoothDevice
import io.sellmair.pacemaker.ble.BleDeviceId

internal val BluetoothDevice.deviceId: BleDeviceId get() = BleDeviceId(address)
