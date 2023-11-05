package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

internal class BlePeripheralServiceImpl(
    private val queue: BleQueue,
    private val controller: BlePeripheralController,
    override val service: BleServiceDescriptor,
    coroutineScope: CoroutineScope
) : BlePeripheralService {

    private val characteristicValues = mutableMapOf<BleUUID, ByteArray>()

    override val receivedWrites: SharedFlow<BleReceivedValue> = controller.writeRequests
        .consumeAsFlow()
        .mapNotNull { request -> handleWriteRequest(request) }
        .shareIn(coroutineScope, SharingStarted.Eagerly)

    @Suppress("unused")
    private val readRequestHandler = controller.readRequests.consumeAsFlow()
        .onEach { request -> handleReadRequest(request) }
        .launchIn(coroutineScope)


    override suspend fun setValue(
        characteristic: BleCharacteristicDescriptor, value: ByteArray
    ) = withContext(Dispatchers.Main) {
        if (characteristic.isReadable) {
            characteristicValues[characteristic.uuid] = value
        }

        if (characteristic.isNotificationsEnabled) {
            queue enqueue SendNotificationBleOperation(characteristic) {
                controller.sendNotification(characteristic, value)
                BleSuccess()
            }
        } else BleSuccess()
    }

    override suspend fun startAdvertising() {
        queue enqueue StartAdvertisingBleOperation(service) {
            controller.startAdvertising()
            BleSuccess()
        }
    }

    private suspend fun handleWriteRequest(request: BlePeripheralController.WriteRequest): BleReceivedValue? {
        val value = request.value
        if (value == null) {
            queue enqueue RespondToWriteRequestBleOperation(service, request.deviceId, request.characteristicUuid) {
                controller.respond(request, BleKnownStatusCode.GattError)
                BleSuccess()
            }
            return null
        }

        val characteristic = service.findCharacteristic(request.characteristicUuid)
        if (characteristic == null) {
            queue enqueue RespondToWriteRequestBleOperation(service, request.deviceId, request.characteristicUuid) {
                controller.respond(request, BleKnownStatusCode.UnknownAttribute)
                BleSuccess()
            }
            return null
        }

        return BleReceivedValue(request.deviceId, characteristic, value)
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
                BleSuccess()
            }
            return
        }

        if (request.offset >= value.size) {
            queue enqueue RespondToReadRequestBleOperation(service, request.deviceId, characteristic) {
                controller.respond(request, BleKnownStatusCode.IllegalOffset)
                BleSuccess()
            }
        }

        queue enqueue RespondToReadRequestBleOperation(service, request.deviceId, characteristic) {
            controller.respond(request, value)
            BleSuccess()
        }
    }
}
