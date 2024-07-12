@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "unused", "UNUSED_PARAMETER")

package io.sellmair.pacemaker.ble

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject

internal class ApplePeripheralDelegate(
    private val scope: CoroutineScope
) : NSObject(), CBPeripheralDelegateProtocol {

    val thisDelegate = this


    class DidDiscoverServices(val peripheral: CBPeripheral, val error: NSError?)

    val didDiscoverServices = MutableSharedFlow<DidDiscoverServices>()

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        scope.launch { thisDelegate.didDiscoverServices.emit(DidDiscoverServices(peripheral, didDiscoverServices)) }
    }


    class DidModifyServices(val peripheral: CBPeripheral, val services: List<CBService>)

    val didModifyServices = MutableSharedFlow<DidModifyServices>()

    override fun peripheral(peripheral: CBPeripheral, didModifyServices: List<*>) {
        @Suppress("UNCHECKED_CAST")
        scope.launch { thisDelegate.didModifyServices.emit(DidModifyServices(peripheral, didModifyServices as List<CBService>)) }
    }


    class DidDiscoverCharacteristics(
        val peripheral: CBPeripheral, val service: CBService, val error: NSError?
    )

    val didDiscoverCharacteristics = MutableSharedFlow<DidDiscoverCharacteristics>()

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        scope.launch {
            thisDelegate.didDiscoverCharacteristics.emit(
                DidDiscoverCharacteristics(peripheral, didDiscoverCharacteristicsForService, error)
            )
        }
    }


    class DidReadRssi(val peripheral: CBPeripheral, val rssi: NSNumber, val error: NSError?)

    val didReadRssi = MutableSharedFlow<DidReadRssi>()

    override fun peripheral(peripheral: CBPeripheral, didReadRSSI: NSNumber, error: NSError?) {
        scope.launch { thisDelegate.didReadRssi.emit(DidReadRssi(peripheral, didReadRSSI, error)) }
    }


    class DidUpdateNotificationState(
        val peripheral: CBPeripheral, characteristic: CBCharacteristic, error: NSError?
    )

    val didUpdateNotificationState = MutableSharedFlow<DidUpdateNotificationState>()

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral, didUpdateNotificationStateForCharacteristic: CBCharacteristic, error: NSError?
    ) {
        scope.launch {
            thisDelegate.didUpdateNotificationState.emit(
                DidUpdateNotificationState(
                    peripheral, didUpdateNotificationStateForCharacteristic, error
                )
            )
        }
    }


    class DidUpdateValue(val peripheral: CBPeripheral, val characteristic: CBCharacteristic, val error: NSError?)

    val didUpdateValue = MutableSharedFlow<DidUpdateValue>()

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        scope.launch {
            thisDelegate.didUpdateValue.emit(
                DidUpdateValue(peripheral, didUpdateValueForCharacteristic, error)
            )
        }
    }


    class DidWriteValue(val peripheral: CBPeripheral, val characteristic: CBCharacteristic, val error: NSError?)

    val didWriteValue = MutableSharedFlow<DidWriteValue>()

    @ObjCSignatureOverride
    override fun peripheral(
        peripheral: CBPeripheral,
        didWriteValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {
        scope.launch {
            thisDelegate.didWriteValue.emit(
                DidWriteValue(peripheral, didWriteValueForCharacteristic, error)
            )
        }
    }
}
