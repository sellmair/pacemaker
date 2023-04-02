package io.sellmair.pacemaker.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import android.os.ParcelUuid
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.error
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

@SuppressLint("MissingPermission")
internal class AndroidPeripheralController(
    private val scope: CoroutineScope,
    private val hardware: AndroidPeripheralHardware
) : BlePeripheralController {

    override fun startAdvertising() {
        val callback = object : AdvertiseCallback() {}

        hardware.manager.adapter.bluetoothLeAdvertiser.startAdvertising(
            AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .build(),
            AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(hardware.serviceDescriptor.uuid))
                .build(),
            callback
        )

        scope.coroutineContext.job.invokeOnCompletion {
            hardware.manager.adapter.bluetoothLeAdvertiser.stopAdvertising(callback)
        }
    }

    override suspend fun respond(request: BlePeripheralController.WriteRequest, statusCode: BleStatusCode): Boolean {
        request as MyWriteRequest
        return hardware.gattServer.sendResponse(
            /* device = */ request.event.device,
            /* requestId = */ request.event.requestId,
            /* status = */ statusCode.toInt(),
            /* offset = */ 0,
            /* value = */ null
        )
    }

    override suspend fun respond(request: BlePeripheralController.ReadRequest, statusCode: BleStatusCode): Boolean {
        request as MyReadRequest
        return hardware.gattServer.sendResponse(
            /* device = */ request.event.device,
            /* requestId = */ request.event.requestId,
            /* status = */ statusCode.toInt(),
            /* offset = */ request.event.offset,
            /* value = */ null
        )
    }

    override suspend fun respond(
        request: BlePeripheralController.ReadRequest,
        value: ByteArray,
        statusCode: BleStatusCode
    ): Boolean {
        request as MyReadRequest
        return hardware.gattServer.sendResponse(
            /* device = */ request.event.device,
            /* requestId = */ request.event.requestId,
            /* status = */ statusCode.toInt(),
            /* offset = */ request.event.offset,
            /* value = */ value
        )
    }

    override suspend fun sendNotification(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
        require(characteristic.isNotificationsEnabled) { "Expected $characteristic 'isNotificationsEnabled' " }

        val bluetoothGattCharacteristic = hardware.service.getCharacteristic(characteristic.uuid)
            ?: throw IllegalArgumentException("$characteristic not found")


        connectedDevices.toList().forEach { device ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val rawStatus = hardware.gattServer.notifyCharacteristicChanged(
                    device, bluetoothGattCharacteristic, false, value
                )

                val status = BleStatusCode(rawStatus)
                if (!status.isSuccess) {
                    log.error("Failed sending notification to '${device.deviceId}' ($status)")
                    return@forEach
                }

            } else @Suppress("DEPRECATION") {
                bluetoothGattCharacteristic.value = value
                val success = hardware.gattServer.notifyCharacteristicChanged(
                    device, bluetoothGattCharacteristic, false
                )
                if (!success) {
                    log.error("Failed sending notification to '${device.deviceId}")
                    return@forEach
                }
            }

            val result = hardware.gattServerCallback.onNotificationSent.first { it.device == device }
            val resultStatus = BleStatusCode(result.status)
            if (!resultStatus.isSuccess) {
                log.error("Failed sending notification to '${device.deviceId}' ($resultStatus")
            }
        }
    }

    override val writeRequests: Channel<BlePeripheralController.WriteRequest> = Channel()

    override val readRequests: Channel<BlePeripheralController.ReadRequest> = Channel()

    private val connectedDevices: Set<BluetoothDevice> = run {
        val connectedDevices = mutableSetOf<BluetoothDevice>()
        hardware.gattServerCallback.onConnectionStateChange
            .onEach { event ->
                val status = BleStatusCode(event.status)
                if (status == BleKnownStatusCode.Success && event.newState == BluetoothGatt.STATE_CONNECTED) {
                    if (connectedDevices.add(event.device)) {
                        log.info("Device connected: ${event.device.deviceId}")
                    }
                } else {
                    if (connectedDevices.remove(event.device)) {
                        log.info("Device disconnected: ${event.device.deviceId} ($status)")
                    }
                }
            }
            .launchIn(scope)
        connectedDevices
    }

    private inner class MyWriteRequest(
        val event: AndroidGattServerCallback.OnCharacteristicWriteRequest
    ) : BlePeripheralController.WriteRequest {
        override val deviceId: BleDeviceId = event.device.deviceId
        override val characteristicUuid: BleUUID = event.characteristic.uuid
        override val value: ByteArray? = event.value
    }

    private inner class MyReadRequest(
        val event: AndroidGattServerCallback.OnCharacteristicReadRequest
    ) : BlePeripheralController.ReadRequest {
        override val deviceId: BleDeviceId = event.device.deviceId
        override val characteristicUuid: BleUUID = event.characteristic.uuid
        override val offset: Int = event.offset
    }

    companion object {
        val log = LogTag.ble.forClass<AndroidPeripheralController>()
    }

    init {
        @OptIn(ExperimentalStdlibApi::class)
        require(scope.coroutineContext[CoroutineDispatcher.Key] == Dispatchers.ble)

        /* Receive read requests */
        scope.launch {
            hardware.gattServerCallback.onCharacteristicReadRequest.collect { request ->
                readRequests.send(MyReadRequest(request))
            }
        }

        /* Receive write requests */
        scope.launch {
            hardware.gattServerCallback.onCharacteristicWriteRequest.collect { request ->
                writeRequests.send(MyWriteRequest(request))
            }
        }
    }
}