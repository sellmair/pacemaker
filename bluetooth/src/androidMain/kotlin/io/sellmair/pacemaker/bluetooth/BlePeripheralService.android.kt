@file:Suppress("FunctionName")

package io.sellmair.pacemaker.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import io.sellmair.pacemaker.ble.BleCharacteristicDescriptor
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import io.sellmair.pacemaker.ble.BleUUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import java.util.*

@SuppressLint("MissingPermission")
internal suspend fun BlePeripheralService(
    scope: CoroutineScope,
    context: Context,
    service: BleServiceDescriptor
): BlePeripheralService {
    /* Wait for bluetooth permission */
    while (
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        delay(1000)
    }


    val manager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothGattServerCallback = BluetoothGattServerCallback()

    /* Create Bluetooth Gatt Server */
    val bluetoothGattServer = manager.openGattServer(context, bluetoothGattServerCallback)
    val bluetoothGattService = BluetoothGattService(service.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

    service.characteristics.forEach { characteristic ->
        bluetoothGattService.addCharacteristic(BluetoothGattCharacteristic(
            characteristic.uuid,
            (PROPERTY_READ.takeIf { characteristic.isReadable } ?: 0) or
                    (PROPERTY_NOTIFY.takeIf { characteristic.isNotificationsEnabled } ?: 0) or
                    (PROPERTY_WRITE.takeIf { characteristic.isWritable } ?: 0),
            PERMISSION_READ or (PERMISSION_WRITE.takeIf { characteristic.isWritable } ?: 0))
        )
    }

    bluetoothGattServer.addService(bluetoothGattService)

    currentCoroutineContext().job.invokeOnCompletion {
        bluetoothGattServer.close()
    }

    /* Start advertising */

    val advertiseCallback = object : AdvertiseCallback() {}
    manager.adapter.bluetoothLeAdvertiser.startAdvertising(
        AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .build(),
        AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(PacemakerBleServiceConstants.serviceUuidString)))
            .build(),
        advertiseCallback
    )

    currentCoroutineContext().job.invokeOnCompletion {
        manager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    return AndroidBlePeripheralService(
        scope, service, manager, bluetoothGattServer, bluetoothGattServerCallback, bluetoothGattService
    )
}

private class BluetoothGattServerCallback : android.bluetooth.BluetoothGattServerCallback() {

    class CharacteristicReadRequest(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val characteristic: BluetoothGattCharacteristic
    )

    class CharacteristicWriteRequest(
        val device: BluetoothDevice,
        val requestId: Int,
        val characteristic: BluetoothGattCharacteristic,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray
    )

    class NotificationSent(
        val device: BluetoothDevice,
        val status: Int
    )

    val currentlyConnectedDevices = mutableSetOf<BluetoothDevice>()
    val connectedDevices = Channel<BluetoothDevice>(Channel.UNLIMITED)
    val characteristicReadRequests = Channel<CharacteristicReadRequest>(Channel.UNLIMITED)
    val characteristicWriteRequests = Channel<CharacteristicWriteRequest>(Channel.UNLIMITED)
    val notificationsSent = Channel<NotificationSent>(Channel.UNLIMITED)

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic
    ) {
        characteristicReadRequests.trySend(CharacteristicReadRequest(device, requestId, offset, characteristic))
    }

    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
        notificationsSent.trySend(NotificationSent(device, status))
    }

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
            println("Ble: Connected central ${device.deviceId}")
            connectedDevices.trySend(device)
            currentlyConnectedDevices.add(device)
        } else currentlyConnectedDevices.remove(device)
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?, requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        characteristicWriteRequests.trySend(
            CharacteristicWriteRequest(
                device = device ?: return,
                requestId = requestId,
                characteristic = characteristic ?: return,
                preparedWrite = preparedWrite,
                responseNeeded = responseNeeded,
                offset = offset,
                value = value ?: return
            )
        )
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private class AndroidBlePeripheralService(
    scope: CoroutineScope,
    override val service: BleServiceDescriptor,
    private val manager: BluetoothManager,
    private val gattServer: BluetoothGattServer,
    private val gattServerCallback: BluetoothGattServerCallback,
    private val gattService: BluetoothGattService,
) : BlePeripheralService {

    class SendNotificationCommand(val characteristic: BleCharacteristicDescriptor, val value: ByteArray)

    private val sendNotificationChannel = Channel<SendNotificationCommand>()

    private val centralConnections = mutableMapOf<BleDeviceId, AndroidBleCentralConnection>()

    override val centrals: Flow<BleConnection> = gattServerCallback.connectedDevices.consumeAsFlow().map { device ->
        centralConnections.getOrPut(device.deviceId) { AndroidBleCentralConnection(device) }
    }.shareIn(scope, SharingStarted.Eagerly)


    override suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
        val androidCharacteristic = gattService.getCharacteristic(characteristic.uuid)

        if (characteristic.isReadable) {
            androidCharacteristic.value = value
        }

        if (characteristic.isNotificationsEnabled) {
            sendNotificationChannel.send(SendNotificationCommand(characteristic, value))
        }
    }


    init {
        /* Coroutine handling read requests */
        scope.launch {
            gattServerCallback.characteristicReadRequests.consumeEach request@{ readRequest ->
                val data = readRequest.characteristic.value

                if (data == null) {
                    gattServer.sendResponse(
                        readRequest.device, readRequest.requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf()
                    )
                    return@request
                }

                if (readRequest.offset > data.size) {
                    gattServer.sendResponse(
                        readRequest.device, readRequest.requestId, BluetoothGatt.GATT_INVALID_OFFSET, 0, byteArrayOf()
                    )
                    return@request
                }

                gattServer.sendResponse(
                    readRequest.device, readRequest.requestId, BluetoothGatt.GATT_SUCCESS, readRequest.offset, data
                )
            }
        }

        /* Coroutine handling write request */
        scope.launch {
            gattServerCallback.characteristicWriteRequests.consumeEach { writeRequest ->
                val connection = centralConnections.getOrPut(writeRequest.device.deviceId) {
                    AndroidBleCentralConnection(writeRequest.device)
                }

                connection.valueFlowOf(writeRequest.characteristic.uuid).emit(writeRequest.value)
            }
        }

        /* Coroutine handling send notification requests */
        scope.launch {
            sendNotificationChannel.consumeEach { command ->
                val characteristic = gattService.getCharacteristic(command.characteristic.uuid)
                characteristic.setValue(command.value)

                gattServerCallback.currentlyConnectedDevices.toList().forEach { device ->

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notifyCharacteristicChanged = gattServer.notifyCharacteristicChanged(
                            device, characteristic, false, command.value
                        )

                        if (notifyCharacteristicChanged != BluetoothStatusCodes.SUCCESS) {
                            Log.d(
                                "Ble",
                                "Failed sending notification for ${command.characteristic.name} to ${device.deviceId}"
                            )
                            return@forEach
                        }
                    } else {
                        val notifyCharacteristicChanged = gattServer.notifyCharacteristicChanged(
                            device, characteristic, false
                        )

                        if (!notifyCharacteristicChanged) {
                            Log.d(
                                "Ble",
                                "Failed sending notification for ${command.characteristic.name} to ${device.deviceId}"
                            )
                            return@forEach
                        }
                    }


                    val sent = gattServerCallback.notificationsSent.receive()
                    assert(sent.device == device)
                    if (sent.status != BluetoothGatt.GATT_SUCCESS) {
                        Log.d("Ble", "failed sending notification to '$device'. Status '${sent.status}'")

                    }
                }
            }
        }
    }
}

private class AndroidBleCentralConnection(device: BluetoothDevice) : BleConnection {
    override val id: BleDeviceId = device.deviceId

    private val valueFlows = mutableMapOf<BleUUID, MutableSharedFlow<ByteArray>>()

    fun valueFlowOf(uuid: BleUUID): MutableSharedFlow<ByteArray> {
        return valueFlows.getOrPut(uuid) {
            MutableSharedFlow(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }
    }

    override fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray> {
        return valueFlowOf(characteristic.uuid)
    }
}