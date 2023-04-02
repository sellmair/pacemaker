package io.sellmair.pacemaker.ble

import kotlinx.coroutines.channels.ReceiveChannel

internal interface BleCentralController {

    fun startScanning()

    val scanResults: ReceiveChannel<ScanResult>

    fun createConnectableController(result: ScanResult): BleConnectableController

    interface ScanResult {
        val deviceId: BleDeviceId
        val rssi: Int
        val isConnectable: Boolean
    }
}