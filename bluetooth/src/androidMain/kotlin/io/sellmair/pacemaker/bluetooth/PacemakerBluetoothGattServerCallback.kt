package io.sellmair.pacemaker.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking


class PacemakerBluetoothGattServerCallback(private val scope: CoroutineScope) : BluetoothGattServerCallback() {

    class OnConnectionStateChange(
        device: BluetoothDevice, status: Int, newState: Int
    )

    class OnCharacteristicReadRequest(
        device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?
    )

    class OnCharacteristicWriteRequest(
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray
    )

    class OnNotificationSent(
        device: BluetoothDevice?, status: Int
    )

    val onConnectionStateChange = Channel<OnConnectionStateChange>(Channel.UNLIMITED)

    val onCharacteristicReadRequest = Channel<OnCharacteristicReadRequest>(Channel.UNLIMITED)

    val onCharacteristicWriteRequest = Channel<OnCharacteristicWriteRequest>(Channel.UNLIMITED)

    val onNotificationSent = Channel<OnNotificationSent>(Channel.UNLIMITED)

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
        onConnectionStateChange.trySendBlocking(OnConnectionStateChange(device, status, newState))
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?
    ) {
        onCharacteristicReadRequest.trySendBlocking(
            OnCharacteristicReadRequest(device, requestId, offset, characteristic)
        )
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray
    ) {
        onCharacteristicWriteRequest.trySendBlocking(
            OnCharacteristicWriteRequest(
                device = device,
                requestId = requestId,
                characteristic = characteristic,
                preparedWrite = preparedWrite,
                responseNeeded = responseNeeded,
                offset = offset,
                value = value
            )
        )
    }

    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
        onNotificationSent.trySendBlocking(
            OnNotificationSent(device, status)
        )
    }
}