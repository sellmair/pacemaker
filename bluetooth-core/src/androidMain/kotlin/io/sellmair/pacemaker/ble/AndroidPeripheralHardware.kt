package io.sellmair.pacemaker.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job


internal class AndroidPeripheralHardware(
    val manager: BluetoothManager,
    val gattServer: BluetoothGattServer,
    val gattServerCallback: AndroidGattServerCallback,
    val service: BluetoothGattService,
    val serviceDescriptor: BleServiceDescriptor
) {
    companion object {
        val log = LogTag.ble.forClass<AndroidPeripheralHardware>()
    }
}

@SuppressLint("MissingPermission")
internal suspend fun AndroidPeripheralHardware(
    context: Context,
    scope: CoroutineScope,
    serviceDescriptor: BleServiceDescriptor,
): AndroidPeripheralHardware {
    val manager = context.getSystemService(BluetoothManager::class.java)
    val gattServerCallback = AndroidGattServerCallback(scope)
    val gattServer = manager.openGattServer(context, gattServerCallback)

    val gattService = BluetoothGattService(serviceDescriptor)
    gattServer.addService(gattService)

    gattServerCallback.onServiceAdded.first { it.service == gattService }.status.also { status ->
        val statusCode = BleStatusCode(status)
        if (statusCode != BleKnownStatusCode.Success) {
            AndroidPeripheralHardware.log.error("Failed adding service: $statusCode")
        }
    }

    scope.coroutineContext.job.invokeOnCompletion {
        gattServer.close()
    }

    return AndroidPeripheralHardware(
        manager = manager,
        gattServer = gattServer,
        gattServerCallback = gattServerCallback,
        service = gattService,
        serviceDescriptor = serviceDescriptor
    )
}

private fun BluetoothGattService(descriptor: BleServiceDescriptor): BluetoothGattService {
    val bluetoothGattService = BluetoothGattService(descriptor.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)

    descriptor.characteristics.forEach { characteristic ->
        bluetoothGattService.addCharacteristic(BluetoothGattCharacteristic(characteristic))
    }

    return bluetoothGattService
}

private fun BluetoothGattCharacteristic(descriptor: BleCharacteristicDescriptor): BluetoothGattCharacteristic {
    return BluetoothGattCharacteristic(
        descriptor.uuid,
        (PROPERTY_READ.takeIf { descriptor.isReadable } ?: 0) or
            (PROPERTY_NOTIFY.takeIf { descriptor.isNotificationsEnabled } ?: 0) or
            (PROPERTY_WRITE_NO_RESPONSE.takeIf { descriptor.isWritable } ?: 0),
        PERMISSION_READ or (PERMISSION_WRITE.takeIf { descriptor.isWritable } ?: 0)
    ).also { if(descriptor.isWritable) it.writeType = WRITE_TYPE_NO_RESPONSE }
}