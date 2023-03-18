package io.sellmair.broadheart.bluetooth

import kotlinx.coroutines.flow.Flow

interface Ble {
    suspend fun startServer(service: BleServiceDescriptor): BleServer
    suspend fun startClient(service: BleServiceDescriptor): BleClient
}

interface BleServer {
    val service: BleServiceDescriptor
    suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray)
}

interface BleClient {
    val service: BleServiceDescriptor
    val peripherals: Flow<BlePeripheral>
}

data class BlePeripheralId(val value: String)

interface BlePeripheral {
    val peripheralId: BlePeripheralId
    suspend fun connect(): BleClientConnection
}

interface BleClientConnection {
    val peripheralId: BlePeripheralId
    fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray>
}

expect fun BleUUID(value: String): BleUUID

expect class BleUUID

data class BleServiceDescriptor(
    val name: String? = null,
    val uuid: BleUUID,
    val characteristics: Set<BleCharacteristicDescriptor>
) {
    override fun toString(): String {
        return "${BleServiceDescriptor::class.simpleName}(${name ?: uuid})"
    }
}

data class BleCharacteristicDescriptor(
    val name: String? = null,
    val uuid: BleUUID,
    val isReadable: Boolean = true,
    val isNotificationsEnabled: Boolean = false
) {
    override fun toString(): String {
        return "${BleCharacteristicDescriptor::class.simpleName}(${name ?: uuid})"
    }
}
