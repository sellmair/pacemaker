package io.sellmair.pacemaker.ble

import android.Manifest.permission.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.getSystemService

internal val BluetoothDevice.deviceId: BleDeviceId get() = BleDeviceId(address)

fun Context.isBluetoothPermissionGranted(): Boolean {
    return checkSelfPermission(BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
}

fun Context.isBluetoothEnabled(): Boolean {
    return getSystemService<BluetoothManager>()?.adapter?.isEnabled == true
}