package io.sellmair.pacemaker.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

interface Ble {
    val scope: CoroutineScope
    suspend fun startPeripheralService(service: BleServiceDescriptor): BlePeripheralService
    suspend fun startCentralService(service: BleServiceDescriptor): BleCentralService
}

interface BleService {
    val service: BleServiceDescriptor
    suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray)

}

interface BlePeripheralService : BleService {
    val centrals: Flow<BleConnection>
}

interface BleCentralService : BleService {
    val peripherals: Flow<BlePeripheral>
}

@JvmInline
value class BleDeviceId(val value: String)

@JvmInline
value class Rssi(val value: Int)

interface BleConnection {
    val id: BleDeviceId
    fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray>
}

interface BlePeripheral : BleConnection {
    enum class State {
        Disconnected,
        Connectable,
        Connecting,
        Connected
    }

    val rssi: StateFlow<Rssi>
    val state: StateFlow<State>
    fun tryConnect()
    fun tryDisconnect()
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
    val isWritable: Boolean = false,
    val isNotificationsEnabled: Boolean = false
) {
    override fun toString(): String {
        return "${BleCharacteristicDescriptor::class.simpleName}(${name ?: uuid})"
    }
}
