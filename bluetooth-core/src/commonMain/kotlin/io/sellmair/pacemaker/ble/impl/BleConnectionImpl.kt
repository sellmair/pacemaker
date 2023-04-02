package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive

class BleConnectionImpl(
    override val scope: CoroutineScope,
    private val queue: BleQueue,
    private val controller: BleConnectableController,
    override val service: BleServiceDescriptor
) : BleConnection {
    override val deviceId: BleDeviceId = controller.deviceId

    override val receivedValues: SharedFlow<BleReceivedValue>
        get() = controller.values

    override suspend fun enableNotifications(characteristic: BleCharacteristicDescriptor): BleQueue.Result<Unit> {
        if (!scope.isActive) BleQueue.Result.Failure.NotEnqueued
        return queue enqueue EnableNotificationsBleOperation(controller.deviceId, characteristic) {
            controller.enableNotification(characteristic)
        }
    }

    override suspend fun requestRead(characteristic: BleCharacteristicDescriptor): BleQueue.Result<ByteArray> {
        if (!scope.isActive) return BleQueue.Result.Failure.NotEnqueued
        return queue enqueue ReadCharacteristicBleOperation(controller.deviceId, characteristic) {
            controller.readValue(characteristic)
        }
    }

    override suspend fun setValue(
        characteristic: BleCharacteristicDescriptor, value: ByteArray
    ): BleQueue.Result<Unit> {
        if (!scope.isActive) return BleQueue.Result.Failure.NotEnqueued
        return queue enqueue WriteCharacteristicBleOperation(controller.deviceId, characteristic) {
            controller.writeValue(characteristic, value)
        }
    }
}