@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker

import io.sellmair.pacemaker.BluetoothService.Device.HeartRateSensor
import io.sellmair.pacemaker.BluetoothService.Device.PacemakerAppDevice
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.bluetooth.*
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

fun BluetoothService(ble: BleV1): BluetoothService {
    return BluetoothServiceImpl(ble)
}

private class BluetoothServiceImpl(private val ble: BleV1) : BluetoothService {

    private val pacemakerBle = ble.scope.async { PacemakerBle(ble) }

    override suspend fun pacemakerBle(): PacemakerBle {
        return pacemakerBle.await()
    }

    override val devices: SharedFlow<BluetoothService.Device> = flowOf(
        heartRateSensorPeripherals(),
        pacemakerPeripherals()
    ).flattenMerge()
        .onStart { println("BleService: started scanning for peripherals") }
        .onEach { println("BleService: discovered: $it") }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    override val allDevices: SharedFlow<List<BluetoothService.Device>> = devices
        .runningFold(emptyList<BluetoothService.Device>()) { list, peripheral -> list + peripheral }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    private fun heartRateSensorPeripherals(): Flow<HeartRateSensor> = flow {
        emitAll(ble.startHeartRateBleCentralService().peripherals.map { peripheral ->
            HeartRateSensorImpl(ble.scope, peripheral)
        })
    }

    private fun pacemakerPeripherals(): Flow<PacemakerAppDevice> = flow {
        emitAll(pacemakerBle.await().connections.map { PacemakerAppDeviceImpl(ble.scope, it) })
    }
}

private class HeartRateSensorImpl(
    scope: CoroutineScope,
    private val peripheral: HeartRateBlePeripheral
) : HeartRateSensor {
    override val id: HeartRateSensorId = peripheral.id.toHeartRateSensorId()

    override val state: StateFlow<BlePeripheral.State>
        get() = peripheral.state

    override val rssi: StateFlow<Rssi>
        get() = peripheral.rssi

    override val measurements: SharedFlow<HeartRateMeasurement> =
        peripheral.heartRateMeasurements.shareIn(scope, SharingStarted.Eagerly)

    override fun tryConnect() {
        peripheral.tryConnect()
    }

    override fun tryDisconnect() {
        peripheral.tryDisconnect()
    }

    override fun toString(): String {
        return "Heart Rate Sensor: $id"
    }
}

private class PacemakerAppDeviceImpl(
    scope: CoroutineScope,
    private val connection: BleConnection,
) : PacemakerAppDevice {

    override val id: BleDeviceId = connection.id

    override val broadcasts: SharedFlow<PacemakerBroadcastPackage> = connection.receivePacemakerBroadcastPackages()
        .shareIn(scope, SharingStarted.WhileSubscribed())

    override fun toString(): String {
        return "Pacemaker App: ${connection.id}"
    }
}