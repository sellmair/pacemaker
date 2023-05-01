package io.sellmair.pacemaker.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class AndroidGattCallback(private val scope: CoroutineScope) : BluetoothGattCallback() {

    /* On Connection State Change */
    class OnConnectionStateChange(
        val gatt: BluetoothGatt, val status: Int, val newState: Int
    )

    val onConnectionStateChange = MutableSharedFlow<OnConnectionStateChange>()

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        scope.launch { onConnectionStateChange.emit(OnConnectionStateChange(gatt, status, newState)) }
    }


    /* On Services Discovered */

    class OnServicesDiscovered(val gatt: BluetoothGatt, val status: Int)

    val onServicesDiscovered = MutableSharedFlow<OnServicesDiscovered>()

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        scope.launch { onServicesDiscovered.emit(OnServicesDiscovered(gatt, status)) }
    }


    /* On Descriptor Write */

    class OnDescriptorWrite(
        val gatt: BluetoothGatt, val descriptor: BluetoothGattDescriptor, val status: Int
    )

    val onDescriptorWrite = MutableSharedFlow<OnDescriptorWrite>()

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        scope.launch { onDescriptorWrite.emit(OnDescriptorWrite(gatt, descriptor, status)) }
    }


    /* On Characteristic Read */

    class OnCharacteristicRead(
        val gatt: BluetoothGatt, val characteristic: BluetoothGattCharacteristic, val value: ByteArray, val status: Int
    )

    val onCharacteristicRead = MutableSharedFlow<OnCharacteristicRead>()

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        scope.launch { onCharacteristicRead.emit(OnCharacteristicRead(gatt, characteristic, value, status)) }
    }

    /* On Characteristic Write */

    class OnCharacteristicWrite(
        val gatt: BluetoothGatt, val characteristic: BluetoothGattCharacteristic, val status: Int
    )

    val onCharacteristicWrite = MutableSharedFlow<OnCharacteristicWrite>()

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        scope.launch { onCharacteristicWrite.emit(OnCharacteristicWrite(gatt, characteristic, status)) }
    }

    /* On Characteristic Changed */

    class OnCharacteristicChanged(
        val gatt: BluetoothGatt, val characteristic: BluetoothGattCharacteristic, val value: ByteArray
    )

    val onCharacteristicChanged = MutableSharedFlow<OnCharacteristicChanged>()

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        scope.launch { onCharacteristicChanged.emit(OnCharacteristicChanged(gatt, characteristic, value)) }
    }
}