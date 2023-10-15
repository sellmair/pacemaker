package io.sellmair.pacemaker.ble

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleConnectableController {

    val deviceName: String?

    val deviceId: BleDeviceId

    val values: SharedFlow<BleReceivedValue>

    val rssi: SharedFlow<Int>

    val isConnected: StateFlow<Boolean>

    suspend fun connect(): BleUnit

    suspend fun disconnect(): BleUnit

    suspend fun discoverService(): BleUnit

    suspend fun discoverCharacteristics(): BleUnit

    suspend fun enableNotification(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<Unit>

    suspend fun readValue(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<ByteArray>

    suspend fun writeValue(characteristicDescriptor: BleCharacteristicDescriptor, value: ByteArray): BleResult<Unit>

}