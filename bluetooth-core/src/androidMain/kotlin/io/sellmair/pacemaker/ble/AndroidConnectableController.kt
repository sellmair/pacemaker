@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.*

@SuppressLint("MissingPermission")
internal class AndroidConnectableController(
    private val scope: CoroutineScope,
    private val service: BleServiceDescriptor,
    private val hardware: AndroidConnectableHardware,
) : BleConnectableController {
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

    override val isConnected: StateFlow<Boolean> = MutableStateFlow(false)

    private var gatt: BluetoothGatt? = null

    override suspend fun connect(): BleSimpleResult {
        if (gatt != null) {
            gatt?.disconnect()
            gatt?.close()
        }

        gatt = hardware.device.connectGatt(hardware.context, true, hardware.callback, BluetoothDevice.TRANSPORT_LE)
        return hardware.callback.onConnectionStateChange.mapNotNull { connectionStateChange ->
            val statusCode = BleStatusCode(connectionStateChange.status)

            if (statusCode.isSuccess && connectionStateChange.newState == BluetoothGatt.STATE_CONNECTED) {
                log.debug("'$deviceId': Connection state changed: 'Connected'")
                return@mapNotNull BleSimpleResult.Success
            }

            if (!statusCode.isSuccess) {
                return@mapNotNull BleResult.Failure.Message("'$deviceId: Failed connecting: status=$statusCode")
            }

            null
        }.first()
    }

    override suspend fun disconnect(): BleSimpleResult {
        val gatt = gatt ?: return BleSimpleResult.Success
        gatt.disconnect()
        hardware.callback.onConnectionStateChange.first()
        gatt.close()
        return BleSimpleResult.Success
    }

    override suspend fun discoverService(): BleSimpleResult {
        val gatt = gatt ?: return BleResult.Failure.Message("No 'gatt' connected")
        gatt.discoverServices()
        val status = BleStatusCode(hardware.callback.onServicesDiscovered.first().status)
        return if (status.isSuccess) BleSimpleResult.Success
        else BleResult.Failure.Code(status)
    }

    override suspend fun discoverCharacteristics(): BleSimpleResult {
        /* Android does not have a dedicated function for this */
        return requireGattService().map { }
    }

    @Suppress("DEPRECATION")
    override suspend fun enableNotification(characteristicDescriptor: BleCharacteristicDescriptor): BleSimpleResult {
        val gatt = requireGatt().getOr { return it }
        val characteristic = requireGattCharacteristic(characteristicDescriptor).getOr { return it }
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return BleResult.Failure.Message("'setCharacteristicNotification' failed for $characteristic")
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
            return BleResult.Failure.Message("Failed reading '$characteristicDescriptor")
        }

        val read = hardware.callback.onCharacteristicRead.first { it.characteristic == characteristic }
        val statusCode = BleStatusCode(read.status)
        if (!statusCode.isSuccess) return BleResult.Failure.Code(statusCode)
        return BleResult(read.value)
    }

    override suspend fun writeValue(
        characteristicDescriptor: BleCharacteristicDescriptor,
        value: ByteArray
    ): BleSimpleResult {
        val gatt = requireGatt().getOr { return it }
        val characteristic = requireGattCharacteristic(characteristicDescriptor).getOr { return it }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else run {
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }

        val write = hardware.callback.onCharacteristicWrite.first { it.characteristic == characteristic }
        return BleResult(BleStatusCode(write.status))
    }


    private fun requireGatt(): BleResult<BluetoothGatt> {
        val gatt = this.gatt ?: return BleResult.Failure.Message("Missing 'gatt'. Is device connected?")
        return BleResult.Success(gatt)
    }

    private fun requireGattService(): BleResult<BluetoothGattService> {
        return requireGatt().flatMap { gatt ->
            @Suppress("EQUALITY_NOT_APPLICABLE") // TODO: Create ticket!
            val service = gatt.services.find { it.uuid == service.uuid }
            return if (service == null) BleResult.Failure.Message("Missing '$service' in 'gatt'")
            else BleResult.Success(service)
        }
    }

    private fun requireGattCharacteristic(
        descriptor: BleCharacteristicDescriptor
    ): BleResult<BluetoothGattCharacteristic> = requireGattService().flatMap { service ->
        val characteristic = service.getCharacteristic(descriptor.uuid)
            ?: return@flatMap BleResult.Failure.Message("Missing $descriptor in gatt service")
        BleResult.Success(characteristic)
    }

    companion object {
        val log = LogTag.ble.forClass<AndroidConnectableController>()
    }
}