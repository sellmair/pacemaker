package io.sellmair.broadheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import io.sellmair.broadheart.bluetooth.ServiceConstants.heartRateCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.heartRateLimitCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.sensorIdCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.serviceUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.userIdCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.userNameCharacteristicUuidString
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.utils.encodeToByteArray
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
suspend fun BroadheartBluetoothSender(
    context: Context, scope: CoroutineScope, user: User
): BroadheartBluetoothSender {
    /* Wait for bluetooth permission */
    while (
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        delay(1000)
    }

    val onNotificationSentChannel = Channel<BluetoothDevice>()

    var currentUser: User = user
    var currentHearRate: HeartRate? = null
    var currentHeartRateLimit: HeartRate? = null
    var currentHeartRateSensorId: HeartRateSensorId? = null

    val manager = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
    lateinit var server: BluetoothGattServer
    server = manager.openGattServer(context, object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic
        ) {
            val data = when (characteristic.uuid.toString()) {
                userIdCharacteristicUuidString -> currentUser.id.value.encodeToByteArray()
                userNameCharacteristicUuidString -> currentUser.name.encodeToByteArray()
                sensorIdCharacteristicUuidString -> currentHeartRateSensorId?.value?.encodeToByteArray()
                heartRateCharacteristicUuidString -> currentHearRate?.value?.roundToInt()?.encodeToByteArray()
                heartRateLimitCharacteristicUuidString ->
                    currentHeartRateLimit?.value?.roundToInt()?.encodeToByteArray()

                else -> null
            }

            if (data == null) {
                server.sendResponse(
                    device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf()
                )
                return
            }

            if (offset > data.size) {
                server.sendResponse(
                    device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, 0, byteArrayOf()
                )
                return
            }

            server.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS, offset, data
            )
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            scope.launch { onNotificationSentChannel.send(device) }
        }
    })

    val userIdCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(userIdCharacteristicUuidString),
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    val userNameCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(userNameCharacteristicUuidString),
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    val sensorIdCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(sensorIdCharacteristicUuidString),
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    val heartRateCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(heartRateCharacteristicUuidString),
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    val heartRateLimitCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(heartRateLimitCharacteristicUuidString),
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    val service = BluetoothGattService(UUID.fromString(serviceUuidString), SERVICE_TYPE_PRIMARY)
    service.addCharacteristic(userIdCharacteristic)
    service.addCharacteristic(userNameCharacteristic)
    service.addCharacteristic(sensorIdCharacteristic)
    service.addCharacteristic(heartRateCharacteristic)
    service.addCharacteristic(heartRateLimitCharacteristic)
    server.addService(service)


    val advertiseCallback = object : AdvertiseCallback() {}
    manager.adapter.bluetoothLeAdvertiser.startAdvertising(
        AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build(),
        AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(serviceUuidString)))
            .build(),
        advertiseCallback
    )

    currentCoroutineContext().job.invokeOnCompletion {
        server.close()
        manager.adapter.bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    @Suppress("DEPRECATION")
    return object : BroadheartBluetoothSender {
        override fun updateUser(user: User) {
            currentUser = user
        }

        override fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate) {
            currentHeartRateSensorId = sensorId
            currentHearRate = heartRate
            scope.launch {
                server.connectedDevices.forEach { device ->
                    heartRateCharacteristic.setValue(heartRate.value.roundToInt().encodeToByteArray())
                    server.notifyCharacteristicChanged(device, heartRateCharacteristic, false)
                    onNotificationSentChannel.receiveAsFlow().filter { it == device }.first()
                }
            }
        }

        override fun updateHeartRateLimit(heartRate: HeartRate) {
            currentHeartRateLimit = heartRate
            scope.launch {
                server.connectedDevices.forEach { device ->
                    heartRateLimitCharacteristic.setValue(heartRate.value.roundToInt().encodeToByteArray())
                    server.notifyCharacteristicChanged(device, heartRateLimitCharacteristic, false)
                    onNotificationSentChannel.receiveAsFlow().filter { it == device }.first()
                }
            }
        }
    }
}
