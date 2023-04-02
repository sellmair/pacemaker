package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleCharacteristicDescriptor
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleServiceDescriptor
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

