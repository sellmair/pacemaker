package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

internal class BlePeripheralServiceImpl(
    private val queue: BleQueue,
    private val controller: BlePeripheralController,
    override val service: BleServiceDescriptor
) : BlePeripheralService {

    private val characteristicValues = mutableMapOf<BleUUID, ByteArray>()

    override val receivedWrites: SharedFlow<BlePeripheralService.ReceivedWrite> = controller.writeRequests
        .consumeAsFlow()
        .mapNotNull { request -> handleWriteRequest(request) }
        .shareIn(queue.scope, SharingStarted.Eagerly)

    @Suppress("unused")
    private val readRequestHandler = controller.readRequests.consumeAsFlow()
        .onEach { request -> handleReadRequest(request) }
        .launchIn(queue.scope)


    override suspend fun setValue(
        characteristic: BleCharacteristicDescriptor, value: ByteArray
    ) = withContext(Dispatchers.ble) {
        if (characteristic.isReadable) {
            characteristicValues[characteristic.uuid] = value
        }

        if (characteristic.isNotificationsEnabled) {
            queue enqueue SendNotificationBleOperation(characteristic) {
                controller.sendNotification(characteristic, value)
                BleResult.Success
            }
        }
    }

    override suspend fun startAdvertising() {
        queue enqueue StartAdvertisingBleOperation(service) {
            controller.startAdvertising()
            BleResult.Success
        }
    }

    private suspend fun handleWriteRequest(request: BlePeripheralController.WriteRequest): BlePeripheralService.ReceivedWrite? {
        val value = request.value
        if (value == null) {
            queue enqueue RespondToWriteRequestBleOperation(service, request.deviceId, request.characteristicUuid) {
                controller.respond(request, BleKnownStatusCode.GattError)
                BleResult.Success
            }
            return null
        }

        val characteristic = service.findCharacteristic(request.characteristicUuid)
        if (characteristic == null) {
            queue enqueue RespondToWriteRequestBleOperation(service, request.deviceId, request.characteristicUuid) {
                controller.respond(request, BleKnownStatusCode.UnknownAttribute)
                BleResult.Success
            }
            return null
        }

        queue enqueue RespondToWriteRequestBleOperation(service, request.deviceId, request.characteristicUuid) {
            controller.respond(request, BleKnownStatusCode.Success)
            BleResult.Success
        }

        return BlePeripheralService.ReceivedWrite(request.deviceId, characteristic, value)
    }

    private suspend fun handleReadRequest(request: BlePeripheralController.ReadRequest) {
        val characteristic = service.findCharacteristic(request.characteristicUuid)
        val value = characteristicValues[request.characteristicUuid]

        if (characteristic == null || value == null) {
            queue enqueue RespondToReadRequestBleOperation(
                service, request.deviceId,
                characteristic = characteristic?.toString() ?: request.characteristicUuid
            ) {
                controller.respond(request, BleKnownStatusCode.UnknownAttribute)
                BleResult.Success
            }
            return
        }

        if (request.offset >= value.size) {
            queue enqueue RespondToReadRequestBleOperation(service, request.deviceId, characteristic) {
                controller.respond(request, BleKnownStatusCode.IllegalOffset)
                BleResult.Success
            }
        }

        queue enqueue RespondToReadRequestBleOperation(service, request.deviceId, characteristic) {
            controller.respond(request, value)
            BleResult.Success
        }
    }
}