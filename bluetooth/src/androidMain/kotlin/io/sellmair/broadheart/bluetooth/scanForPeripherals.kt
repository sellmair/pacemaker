package io.sellmair.broadheart.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
internal fun BluetoothManager.scanForPeripherals(service: BleServiceDescriptor): Flow<ScanResult> = callbackFlow {
    val scanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("bluetooth", "onScanFailed(errorCode=$errorCode)")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result == null) return
            trySendBlocking(result)
        }
    }

    val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(service.uuid))
        .build()

    val scanSettings = ScanSettings.Builder()
        .build()

    adapter.bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
    awaitClose { adapter.bluetoothLeScanner.stopScan(scanCallback) }
}.buffer(Channel.CONFLATED)