@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker

import io.sellmair.pacemaker.BluetoothService.Device.HeartRateSensor
import io.sellmair.pacemaker.BluetoothService.Device.PacemakerAppDevice
import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.bluetooth.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun BluetoothService(
    ble: Ble
): BluetoothService {
    return BluetoothServiceImpl(ble)
}

private class BluetoothServiceImpl(
    private val ble: Ble
) : BluetoothService {

    private val pacemakerCentral = ble.scope.async { PacemakerCentralService(ble) }

    private val pacemakerPeripheral = ble.scope.async { PacemakerPeripheralService(ble) }

    override suspend fun pacemaker(): PacemakerBleWritable {
        // TODO: Write to central!
        return pacemakerPeripheral.await()
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
        emitAll(BluetoothHeartRateSensorService(ble).sensors.map { sensor ->
            HeartRateSensorImpl(sensor)
        })
    }

    private fun pacemakerPeripherals(): Flow<PacemakerAppDevice> = flow {
        emitAll(pacemakerCentral.await().connections.map { connection ->
            PacemakerAppDeviceImpl(connection)
        })
    }

    init {
        ble.scope.launch {
            pacemakerPeripheral.await().startAdvertising()
        }
    }
}

private class HeartRateSensorImpl(
    private val sensor: BluetoothHeartRateSensor
) : HeartRateSensor, BluetoothHeartRateSensor by sensor {
    override fun toString(): String {
        return "Heart Rate Sensor: ${sensor.deviceId}"
    }
}

private class PacemakerAppDeviceImpl(
    private val connection: BleConnection,
) : PacemakerAppDevice {

    override val id: BleDeviceId = connection.deviceId

    override val broadcasts: SharedFlow<PacemakerBroadcastPackage> = connection.receivePacemakerBroadcastPackages()
        .shareIn(connection.scope, SharingStarted.WhileSubscribed())

    override fun toString(): String {
        return "Pacemaker App: ${connection.deviceId}"
    }
}