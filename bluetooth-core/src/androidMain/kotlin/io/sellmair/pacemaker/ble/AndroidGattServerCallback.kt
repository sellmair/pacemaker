package io.sellmair.pacemaker.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class AndroidGattServerCallback(
    private val scope: CoroutineScope
) : BluetoothGattServerCallback() {

    /* On Service Added */
    class OnServiceAdded(val status: Int, val service: BluetoothGattService)

    val onServiceAdded = MutableSharedFlow<OnServiceAdded>()

    override fun onServiceAdded(status: Int, service: BluetoothGattService) {
        scope.launch {
            onServiceAdded.emit(OnServiceAdded(status, service))
        }
    }

    /* On Connection State Change */
    class OnConnectionStateChange(val device: BluetoothDevice, val status: Int, val newState: Int)

    val onConnectionStateChange = MutableSharedFlow<OnConnectionStateChange>()

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        scope.launch {
            onConnectionStateChange.emit(OnConnectionStateChange(device, status, newState))
        }
    }

    /* On Notification Sent */

    class OnNotificationSent(val device: BluetoothDevice, val status: Int)

    val onNotificationSent = MutableSharedFlow<OnNotificationSent>()

    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
        scope.launch {
            onNotificationSent.emit(OnNotificationSent(device, status))
        }
    }

    /* On Read Request */

    class OnCharacteristicReadRequest(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val characteristic: BluetoothGattCharacteristic
    )

    val onCharacteristicReadRequest = MutableSharedFlow<OnCharacteristicReadRequest>()

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic
    ) {
        scope.launch {
            onCharacteristicReadRequest.emit(OnCharacteristicReadRequest(device, requestId, offset, characteristic))
        }
    }

    /* On Write Request */

    class OnCharacteristicWriteRequest(
        val device: BluetoothDevice,
        val requestId: Int,
        val characteristic: BluetoothGattCharacteristic,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray?
    )

    val onCharacteristicWriteRequest = MutableSharedFlow<OnCharacteristicWriteRequest>()

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        scope.launch {
            onCharacteristicWriteRequest.emit(
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
    }
}