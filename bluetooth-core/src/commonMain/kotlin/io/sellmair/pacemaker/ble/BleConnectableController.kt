package io.sellmair.pacemaker.ble

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleConnectableController {

    val deviceId: BleDeviceId

    val values: SharedFlow<BleReceivedValue>

    val rssi: SharedFlow<Int>

    val isConnected: StateFlow<Boolean>

    suspend fun connect(): BleSimpleResult

    suspend fun disconnect(): BleSimpleResult

    suspend fun discoverService(): BleSimpleResult

    suspend fun discoverCharacteristics(): BleSimpleResult

    suspend fun enableNotification(characteristicDescriptor: BleCharacteristicDescriptor): BleSimpleResult

    suspend fun readValue(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<ByteArray>

    suspend fun writeValue(characteristicDescriptor: BleCharacteristicDescriptor, value: ByteArray): BleSimpleResult

}