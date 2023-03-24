@file:Suppress("FunctionName")

package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.utils.toNSData
import kotlinx.coroutines.flow.*
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject

suspend fun DarwinBlePeripheralService(service: BleServiceDescriptor): BlePeripheralService {
    val peripheralDelegate = BPeripheralManagerDelegate()
    val cbPeripheralManager = CBPeripheralManager(peripheralDelegate, null)
    peripheralDelegate.awaitPoweredOnState()

    val cbService = CBMutableService(service.uuid, true)
    val cbCharacteristics = service.characteristics.map { characteristic ->
        CBMutableCharacteristic(
            type = characteristic.uuid,
            properties = (CBCharacteristicPropertyRead.takeIf { characteristic.isReadable } ?: 0.toULong()).or
                (CBCharacteristicPropertyNotify.takeIf { characteristic.isNotificationsEnabled } ?: 0.toULong()),
            value = null,
            permissions = CBAttributePermissionsReadable
        )
    }
    cbService.setCharacteristics(cbCharacteristics)

    cbPeripheralManager.addService(cbService)


    cbPeripheralManager.startAdvertising(
        mapOf(
            CBAdvertisementDataLocalNameKey to NSString.create(string = service.name.orEmpty()),
            CBAdvertisementDataServiceUUIDsKey to NSArray.create(listOf(service.uuid)),
        )
    )

    return object : BlePeripheralService {
        override val service: BleServiceDescriptor = service

        override suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
            val cbCharacteristic = cbCharacteristics.find { it.UUID == characteristic.uuid } ?: return
            val nsData = value.toNSData()

            if (characteristic.isReadable) {
                cbCharacteristic.setValue(nsData)
            }

            if (characteristic.isNotificationsEnabled) {
                cbPeripheralManager.updateValue(nsData, cbCharacteristic, null)
            }
        }
    }
}

private class BPeripheralManagerDelegate : NSObject(), CBPeripheralManagerDelegateProtocol {
    private val _state = MutableStateFlow<Long?>(null)
    val state: StateFlow<Long?> = _state.asStateFlow()

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        _state.value = peripheral.state
    }

    suspend fun awaitPoweredOnState() {
        state.filter { it == CBPeripheralManagerStatePoweredOn }.first()
    }

    override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
        val data = didReceiveReadRequest.characteristic.value
            ?: return peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorUnlikelyError)

        if (didReceiveReadRequest.offset > data.length) {
            peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorInvalidOffset)
            return
        }

        didReceiveReadRequest.setValue(
            data.subdataWithRange(
                NSMakeRange(
                    didReceiveReadRequest.offset,
                    data.length - didReceiveReadRequest.offset
                )
            )
        )
        peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
    }
}