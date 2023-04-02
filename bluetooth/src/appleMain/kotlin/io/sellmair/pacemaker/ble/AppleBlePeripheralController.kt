package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.bluetooth.deviceId
import io.sellmair.pacemaker.utils.toNSData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.ByteString.Companion.toByteString
import platform.CoreBluetooth.CBATTRequest
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.Foundation.*

internal class AppleBlePeripheralController(
    scope: CoroutineScope,
    private val hardware: PeripheralHardware
) : BlePeripheralController {


    override fun startAdvertising() {
        val serviceName = hardware.serviceDescriptor.name
        val serviceUUID = hardware.serviceDescriptor.uuid

        hardware.manager.startAdvertising(
            mapOf(
                CBAdvertisementDataLocalNameKey to NSString.create(string = serviceName),
                CBAdvertisementDataServiceUUIDsKey to NSArray.create(listOf(serviceUUID))
            )
        )
    }

    override suspend fun respond(request: BlePeripheralController.WriteRequest, statusCode: BleStatusCode): Boolean {
        request as MyWriteRequest
        hardware.manager.respondToRequest(request.underlying, statusCode.toLong())
        return true
    }

    override suspend fun respond(request: BlePeripheralController.ReadRequest, statusCode: BleStatusCode): Boolean {
        request as MyReadRequest
        hardware.manager.respondToRequest(request.underlying, statusCode.toLong())
        return true
    }

    override suspend fun respond(
        request: BlePeripheralController.ReadRequest, value: ByteArray, statusCode: BleStatusCode
    ) {
        request as MyReadRequest
        val nsData = value.toNSData()
        val nsDataWithOffset = nsData.subdataWithRange(
            NSMakeRange(request.offset.toULong(), nsData.length - request.offset.toULong())
        )
        request.underlying.setValue(nsDataWithOffset)
        hardware.manager.respondToRequest(request.underlying, statusCode.toLong())
    }

    override suspend fun sendNotification(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
        require(characteristic.isNotificationsEnabled) { "Expected $characteristic 'isNotificationsEnabled' " }

        val cbCharacteristic = hardware.service.characteristics.orEmpty().filterIsInstance<CBMutableCharacteristic>()
            .find { it.UUID == characteristic.uuid } ?: throw IllegalArgumentException("$characteristic not found")

        while (!hardware.manager.updateValue(value.toNSData(), cbCharacteristic, null)) {
            hardware.delegate.isReadyToUpdateSubscribers.first()
        }
    }

    override val writeRequests: Channel<BlePeripheralController.WriteRequest> = Channel()

    override val readRequests: Channel<BlePeripheralController.ReadRequest> = Channel()

    private inner class MyWriteRequest(
        val underlying: CBATTRequest,
    ) : BlePeripheralController.WriteRequest {
        override val deviceId: BleDeviceId = underlying.central.deviceId
        override val value: ByteArray? = underlying.value?.toByteString()?.toByteArray()
        override val characteristicUuid: BleUUID = underlying.characteristic.UUID
    }

    private inner class MyReadRequest(
        val underlying: CBATTRequest
    ) : BlePeripheralController.ReadRequest {
        override val deviceId: BleDeviceId = underlying.central.deviceId
        override val offset: Int = underlying.offset.toInt()
        override val characteristicUuid: BleUUID = underlying.characteristic.UUID
    }

    init {
        /* Receive read requests */
        scope.launch(Dispatchers.ble) {
            hardware.delegate.didReceiveReadRequest.collect { didReceiveReadRequest ->
                readRequests.send(MyReadRequest(didReceiveReadRequest.request))
            }
        }

        /* Receive write requests */
        scope.launch {
            hardware.delegate.didReceiveWriteRequest.collect { didReceiveWriteRequest ->
                writeRequests.send(MyWriteRequest(didReceiveWriteRequest.request))
            }
        }
    }
}
