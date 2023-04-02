package io.sellmair.pacemaker.ble

import kotlinx.coroutines.channels.ReceiveChannel

internal interface BlePeripheralController {

    fun startAdvertising()

    suspend fun respond(request: WriteRequest, statusCode: BleStatusCode): Boolean

    suspend fun respond(request: ReadRequest, statusCode: BleStatusCode): Boolean

    suspend fun respond(request: ReadRequest, value: ByteArray, statusCode: BleStatusCode = BleKnownStatusCode.Success)

    suspend fun sendNotification(characteristic: BleCharacteristicDescriptor, value: ByteArray)


    val writeRequests: ReceiveChannel<WriteRequest>

    val readRequests: ReceiveChannel<ReadRequest>

    interface WriteRequest {
        val deviceId: BleDeviceId
        val characteristicUuid: BleUUID
        val value: ByteArray?
    }

    interface ReadRequest {
        val deviceId: BleDeviceId
        val characteristicUuid: BleUUID
        val offset: Int
    }
}