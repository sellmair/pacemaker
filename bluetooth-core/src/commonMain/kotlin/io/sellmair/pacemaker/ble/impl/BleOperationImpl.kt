package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.*

internal class ReadCharacteristicBleOperation(
    private val deviceId: BleDeviceId,
    private val characteristic: BleCharacteristicDescriptor,
    private val readCharacteristic: suspend () -> BleResult<ByteArray>
) : BleOperation<ByteArray> {

    override val description: String
        get() = "'$deviceId': Read $characteristic'"

    override suspend fun BleQueue.Context.invoke(): BleResult<ByteArray> {
        check(characteristic.isReadable) { "Expected $characteristic to be readable" }
        return readCharacteristic()
    }
}

internal class WriteCharacteristicBleOperation(
    private val deviceId: BleDeviceId,
    private val characteristic: BleCharacteristicDescriptor,
    private val writeCharacteristic: suspend () -> BleSimpleResult,
) : BleSimpleOperation {
    override val description: String
        get() = "'$deviceId': Write $characteristic"

    override suspend fun BleQueue.Context.invoke(): BleSimpleResult {
        check(characteristic.isWritable) { "Expected $characteristic to be writeable" }
        return writeCharacteristic()
    }
}

internal class SendNotificationBleOperation(
    private val characteristic: BleCharacteristicDescriptor,
    private val sendNotification: suspend () -> BleSimpleResult,
) : BleSimpleOperation {
    override val description: String
        get() = "'Send notification for '$characteristic'"

    override suspend fun BleQueue.Context.invoke(): BleSimpleResult {
        check(characteristic.isNotificationsEnabled) { "Expected $characteristic to have notifications enabled" }
        return sendNotification()
    }
}

internal class StartAdvertisingBleOperation(
    private val service: BleServiceDescriptor,
    private val startAdvertising: suspend () -> BleSimpleResult
) : BleSimpleOperation {
    override val description: String
        get() = "Start advertising for '$service'"

    override suspend fun BleQueue.Context.invoke(): BleResult<Unit> {
        return startAdvertising()
    }
}

internal class EnableNotificationsBleOperation(
    private val deviceId: BleDeviceId,
    private val characteristic: BleCharacteristicDescriptor,
    private val enableNotification: suspend () -> BleSimpleResult
) : BleSimpleOperation {

    override val description: String
        get() = "'$deviceId': Enable notifications on $characteristic"

    override suspend fun BleQueue.Context.invoke(): BleSimpleResult {
        return enableNotification()
    }
}

internal class DiscoverServicesBleOperation(
    private val deviceId: BleDeviceId,
    private val discover: suspend () -> BleSimpleResult
) : BleSimpleOperation {

    override val description: String
        get() = "'$deviceId' Discover services'"

    override suspend fun BleQueue.Context.invoke(): BleResult<Unit> {
        return discover()
    }
}

internal class DiscoverCharacteristicsBleOperation(
    private val deviceId: BleDeviceId,
    private val discover: suspend () -> BleSimpleResult
) : BleSimpleOperation {

    override val description: String
        get() = "'$deviceId' Discover characteristics'"

    override suspend fun BleQueue.Context.invoke(): BleResult<Unit> {
        return discover()
    }
}

internal class ConnectPeripheralBleOperation(
    private val deviceId: BleDeviceId,
    private val connect: suspend () -> BleSimpleResult
) : BleSimpleOperation {
    override val description: String
        get() = "'$deviceId': Connect"

    override suspend fun BleQueue.Context.invoke(): BleSimpleResult {
        return connect()
    }
}

internal class DisconnectPeripheralBleOperation(
    private val deviceId: BleDeviceId,
    private val disconnect: suspend () -> BleSimpleResult
) : BleSimpleOperation {
    override val description: String
        get() = "'$deviceId': Disconnect"

    override suspend fun BleQueue.Context.invoke(): BleSimpleResult {
        return disconnect()
    }
}

internal class AddPeripheralServiceBleOperation(
    private val service: BleServiceDescriptor,
    private val triggerAddService: suspend () -> Unit,
    private val awaitResult: suspend () -> BleSimpleResult
) : BleSimpleOperation {

    override val description: String
        get() = "'Adding peripheral service: '${service.name}'"

    override suspend fun BleQueue.Context.invoke(): BleResult<Unit> {
        triggerAddService()
        return awaitResult()
    }
}

internal class RespondToWriteRequestBleOperation(
    private val service: BleServiceDescriptor,
    private val deviceId: BleDeviceId,
    private val characteristicUUID: BleUUID,
    private val respondToWriteRequest: suspend () -> BleSimpleResult,
) : BleSimpleOperation {
    override val description: String
        get() = "$service: '$deviceId': Respond to write request of '$characteristicUUID'"

    override suspend fun BleQueue.Context.invoke(): BleResult<Unit> {
        return respondToWriteRequest()
    }
}

internal class RespondToReadRequestBleOperation(
    private val service: BleServiceDescriptor,
    private val deviceId: BleDeviceId,
    private val characteristic: Any,
    private val respondToReadRequest: suspend () -> BleSimpleResult
) : BleSimpleOperation {
    override val description: String
        get() = "$service: '$deviceId': Respond to read request of $characteristic"

    override suspend fun BleQueue.Context.invoke(): BleResult<Unit> {
        return respondToReadRequest()
    }
}