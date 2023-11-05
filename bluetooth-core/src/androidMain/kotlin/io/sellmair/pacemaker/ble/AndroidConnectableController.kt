@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Build
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

@SuppressLint("MissingPermission")
internal class AndroidConnectableController(
    scope: CoroutineScope,
    private val service: BleServiceDescriptor,
    private val hardware: AndroidConnectableHardware,
) : BleConnectableController {

    override val deviceName: String? = hardware.device.name

    override val deviceId: BleDeviceId = hardware.device.deviceId

    private val valuesFromRead = hardware.callback
        .onCharacteristicRead
        .mapNotNull { read ->
            val characteristic = service.findCharacteristic(read.characteristic.uuid) ?: return@mapNotNull null
            BleReceivedValue(deviceId, characteristic, read.value)
        }

    private val valuesFromNotifications = hardware.callback
        .onCharacteristicChanged
        .mapNotNull { notification ->
            val characteristic = service.findCharacteristic(notification.characteristic.uuid) ?: return@mapNotNull null
            BleReceivedValue(deviceId, characteristic, notification.value)
        }

    override val values: SharedFlow<BleReceivedValue> = flowOf(valuesFromRead, valuesFromNotifications)
        .flattenMerge()
        .shareIn(scope, SharingStarted.Eagerly)

    override val rssi: SharedFlow<Int> = MutableStateFlow(-1)

    override val isConnected: StateFlow<Boolean> = hardware.callback.onConnectionStateChange
        .map { change -> change.newState == BluetoothGatt.STATE_CONNECTED && BleStatusCode(change.status).isSuccess }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    private var gatt: BluetoothGatt? = null

    override suspend fun connect(): BleUnit {
        log.info("$deviceId: connect() ($service)")
        if (gatt != null) {
            gatt?.disconnect()
            gatt?.close()
        }

        gatt = hardware.device.connectGatt(hardware.context, false, hardware.callback, BluetoothDevice.TRANSPORT_LE)
        return hardware.callback.onConnectionStateChange.mapNotNull { connectionStateChange ->
            val statusCode = BleStatusCode(connectionStateChange.status)

            if (statusCode.isSuccess && connectionStateChange.newState == BluetoothGatt.STATE_CONNECTED) {
                log.debug("'$deviceId': Connection state changed: 'Connected'")
                return@mapNotNull BleSuccess()
            }

            if (!statusCode.isSuccess) {
                return@mapNotNull BleFailure.Message("'$deviceId: Failed connecting: status=$statusCode")
            }

            null
        }.first()
    }

    override suspend fun disconnect(): BleUnit {
        val gatt = gatt ?: return BleSuccess()
        gatt.disconnect()
        hardware.callback.onConnectionStateChange.first()
        gatt.close()
        return BleSuccess()
    }

    override suspend fun discoverService(): BleUnit {
        val gatt = gatt ?: return BleFailure.Message("No 'gatt' connected")
        gatt.discoverServices()
        val status = BleStatusCode(hardware.callback.onServicesDiscovered.first().status)
        return if (status.isSuccess) BleSuccess()
        else BleFailure.StatusCode(status)
    }

    override suspend fun discoverCharacteristics(): BleUnit {
        /* Android does not have a dedicated function for this */
        return requireGattService().map { }
    }

    @Suppress("DEPRECATION")
    override suspend fun enableNotification(characteristicDescriptor: BleCharacteristicDescriptor): BleUnit {
        val gatt = requireGatt().getOr { return it }
        val characteristic = requireGattCharacteristic(characteristicDescriptor).getOr { return it }
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return BleFailure.Message("'setCharacteristicNotification' failed for $characteristic")
        }

        val ccdUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val ccdDescriptor: BluetoothGattDescriptor = characteristic.getDescriptor(ccdUUID)
        ccdDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        gatt.writeDescriptor(ccdDescriptor)

        val write = hardware.callback.onDescriptorWrite.filter { it.descriptor == ccdDescriptor }.first()
        return BleResult(BleStatusCode(write.status))
    }

    override suspend fun readValue(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<ByteArray> {
        val gatt = requireGatt().getOr { return it }
        val characteristic = requireGattCharacteristic(characteristicDescriptor).getOr { return it }
        if (!gatt.readCharacteristic(characteristic)) {
            return BleFailure.Message("Failed reading '$characteristicDescriptor")
        }

        val read = hardware.callback.onCharacteristicRead.first { it.characteristic == characteristic }
        val statusCode = BleStatusCode(read.status)
        if (!statusCode.isSuccess) return BleFailure.StatusCode(statusCode)
        return BleSuccess(read.value)
    }

    override suspend fun writeValue(
        characteristicDescriptor: BleCharacteristicDescriptor,
        value: ByteArray
    ): BleUnit {
        val gatt = requireGatt().getOr { return it }
        val characteristic = requireGattCharacteristic(characteristicDescriptor).getOr { return it }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else run {
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }

        val write = hardware.callback.onCharacteristicWrite.first { it.characteristic == characteristic }
        return BleResult(BleStatusCode(write.status))
    }


    private fun requireGatt(): BleResult<BluetoothGatt> {
        val gatt = this.gatt ?: return BleFailure.Message("Missing 'gatt'. Is device connected?")
        return BleSuccess(gatt)
    }

    private fun requireGattService(): BleResult<BluetoothGattService> {
        return requireGatt().flatMap { gatt ->
            @Suppress("EQUALITY_NOT_APPLICABLE") // TODO: Create ticket!
            val service = gatt.services.find { it.uuid == service.uuid }
            return if (service == null) BleFailure.Message("Missing '$service' in 'gatt'")
            else BleSuccess(service)
        }
    }

    private fun requireGattCharacteristic(
        descriptor: BleCharacteristicDescriptor
    ): BleResult<BluetoothGattCharacteristic> = requireGattService().flatMap { service ->
        val characteristic = service.getCharacteristic(descriptor.uuid)
            ?: return@flatMap BleFailure.Message("Missing $descriptor in gatt service")
        BleSuccess(characteristic)
    }

    companion object {
        val log = LogTag.ble.forClass<AndroidConnectableController>()
    }
}