package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.AppleCentralManagerDelegate.DidDiscoverPeripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBAdvertisementDataIsConnectable

internal class AppleCentralController(
    private val scope: CoroutineScope,
    private val hardware: AppleCentralHardware
) : BleCentralController {

    override fun startScanning() {
        hardware.manager.scanForPeripheralsWithServices(
            listOf(hardware.serviceDescriptor.uuid),
            mutableMapOf<Any?, Any>()
        )
    }

    override val scanResults = Channel<BleCentralController.ScanResult>()

    override fun createConnectableController(result: BleCentralController.ScanResult): BleConnectableController {
        result as MyScanResult
        val delegate = ApplePeripheralDelegate(scope)
        result.event.peripheral.delegate = delegate

        return AppleConnectableController(
            scope, hardware, AppleConnectableHardware(result.event.peripheral, delegate, hardware.serviceDescriptor)
        )
    }

    private class MyScanResult(val event: DidDiscoverPeripheral) : BleCentralController.ScanResult {
        override val deviceId: BleDeviceId = event.peripheral.deviceId
        override val rssi: Int = event.RSSI.intValue
        override val isConnectable: Boolean =
            (event.advertisementData[CBAdvertisementDataIsConnectable] as? Boolean) ?: true
    }

    init {
        scope.launch {
            hardware.delegate.didDiscoverPeripheral.collect { event ->
                scanResults.send(MyScanResult(event))
            }
        }
    }
}