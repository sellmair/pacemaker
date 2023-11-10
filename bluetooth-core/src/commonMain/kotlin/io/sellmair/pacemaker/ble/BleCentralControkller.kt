package io.sellmair.pacemaker.ble

import kotlinx.coroutines.channels.ReceiveChannel

internal interface BleCentralController {

    fun startScanning()

    val scanResults: ReceiveChannel<ScanResult>

    val connectedDevices: ReceiveChannel<ConnectedDevice>

    fun createConnectableController(result: ScanResult): BleConnectableController

    fun createConnectableController(device: ConnectedDevice): BleConnectableController

    interface ScanResult {
        val deviceId: BleDeviceId
        val rssi: Int
        val isConnectable: Boolean
    }

    interface ConnectedDevice {
        val deviceId: BleDeviceId
    }
}