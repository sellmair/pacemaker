package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.AppleCentralManagerDelegate.DidDiscoverPeripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBAdvertisementDataIsConnectable
import platform.CoreBluetooth.CBPeripheral

internal class AppleCentralController(
    private val scope: CoroutineScope,
    private val hardware: AppleCentralHardware
) : BleCentralController {

    override fun startScanning() {
        /* Search for already connected devices and emit them */
        val connectedPeripherals = hardware.manager.retrieveConnectedPeripheralsWithServices(listOf(hardware.serviceDescriptor.uuid))
        scope.launch {
            @Suppress("UNCHECKED_CAST")
            connectedPeripherals  as List<CBPeripheral>
            connectedPeripherals.forEach { device ->
                connectedDevices.send(MyConnectedDevice(device)) }
        }

        hardware.manager.scanForPeripheralsWithServices(
            listOf(hardware.serviceDescriptor.uuid),
            mutableMapOf<Any?, Any>()
        )
    }

    override val scanResults = Channel<BleCentralController.ScanResult>()

    override val connectedDevices = Channel<BleCentralController.ConnectedDevice>()

    override fun createConnectableController(result: BleCentralController.ScanResult): BleConnectableController {
        result as MyScanResult
        val delegate = ApplePeripheralDelegate(scope)
        result.event.peripheral.delegate = delegate

        return AppleConnectableController(
            scope, hardware, AppleConnectableHardware(result.event.peripheral, delegate, hardware.serviceDescriptor)
        )

    }

    override fun createConnectableController(device: BleCentralController.ConnectedDevice): BleConnectableController {
        device as MyConnectedDevice
        val delegate = ApplePeripheralDelegate(scope)
        device.peripheral.delegate = delegate
        return AppleConnectableController(
            scope, hardware, AppleConnectableHardware(device.peripheral, delegate, hardware.serviceDescriptor)
        )
    }

    private class MyScanResult(val event: DidDiscoverPeripheral) : BleCentralController.ScanResult {
        override val deviceId: BleDeviceId = event.peripheral.deviceId
        override val rssi: Int = event.RSSI.intValue
        override val isConnectable: Boolean =
            (event.advertisementData[CBAdvertisementDataIsConnectable] as? Boolean) ?: true
    }

    private class MyConnectedDevice(val peripheral: CBPeripheral): BleCentralController.ConnectedDevice {
        override val deviceId: BleDeviceId = peripheral.deviceId
    }


    init {
        scope.launch {
            hardware.delegate.didDiscoverPeripheral.collect { event ->
                scanResults.send(MyScanResult(event))
            }
        }
    }
}