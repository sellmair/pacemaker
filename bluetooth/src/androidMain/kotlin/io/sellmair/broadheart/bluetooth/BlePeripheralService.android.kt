@file:Suppress("FunctionName")

package io.sellmair.broadheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
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
            (BluetoothGattCharacteristic.PROPERTY_READ.takeIf { characteristic.isReadable } ?: 0)
                    or (BluetoothGattCharacteristic.PROPERTY_NOTIFY.takeIf { characteristic.isNotificationsEnabled }
                ?: 0),
            BluetoothGattCharacteristic.PERMISSION_READ
        ))
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
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build(),
        AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(HeartcastBleServiceConstants.serviceUuidString)))
            .build(),
        advertiseCallback
    )

    currentCoroutineContext().job.invokeOnCompletion {
        manager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    return AndroidBleServer(
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

    class NotificationSent(
        val device: BluetoothDevice,
        val status: Int
    )

    val characteristicReadRequests = Channel<CharacteristicReadRequest>(Channel.UNLIMITED)
    val notificationsSent = Channel<NotificationSent>(Channel.UNLIMITED)

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic
    ) {
        characteristicReadRequests.trySend(CharacteristicReadRequest(device, requestId, offset, characteristic))
    }

    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
        notificationsSent.trySend(NotificationSent(device, status))
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private class AndroidBleServer(
    scope: CoroutineScope,
    override val service: BleServiceDescriptor,
    private val manager: BluetoothManager,
    private val gattServer: BluetoothGattServer,
    private val gattServerCallback: BluetoothGattServerCallback,
    private val gattService: BluetoothGattService,
) : BlePeripheralService {

    class SendNotificationCommand(val characteristic: BleCharacteristicDescriptor, val value: ByteArray)

    private val sendNotificationChannel = Channel<SendNotificationCommand>()


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

        /* Coroutine handling send notification requests */
        scope.launch {
            @Suppress("DEPRECATION")
            sendNotificationChannel.consumeEach { command ->
                val characteristic = gattService.getCharacteristic(command.characteristic.uuid)
                characteristic.setValue(command.value)
                manager.getConnectedDevices(BluetoothProfile.GATT).forEach { device ->
                    gattServer.notifyCharacteristicChanged(device, characteristic, false)
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
