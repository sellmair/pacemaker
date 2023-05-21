@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker.service.impl

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.bluetooth.*
import io.sellmair.pacemaker.service.BluetoothService
import io.sellmair.pacemaker.service.BluetoothService.Device.HeartRateSensor
import io.sellmair.pacemaker.service.BluetoothService.Device.PacemakerAppDevice
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.info
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
        val writable = pacemakerCentral.await().currentConnections()
            .map { connection -> PacemakerBleWritable(connection) }
            .plus(pacemakerPeripheral.await())

        return PacemakerBleWritable(ble.scope, writable)
    }

    override val devices: SharedFlow<BluetoothService.Device> = flowOf(
        heartRateSensorPeripherals(),
        pacemakerPeripherals()
    ).flattenMerge()
        .onEach { log.info("Discovered: $it") }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    override val allDevices: SharedFlow<List<BluetoothService.Device>> = devices
        .runningFold(emptyList<BluetoothService.Device>()) { list, peripheral -> list + peripheral }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    override val broadcasts: SharedFlow<PacemakerBroadcastPackage> = flowOf(
        devices
            .filterIsInstance<PacemakerAppDevice>()
            .flatMapMerge { it.broadcasts },
        flow { emitAll(pacemakerPeripheral.await().broadcasts) }
    ).flattenMerge()
        .shareIn(ble.scope, SharingStarted.Eagerly)


    private fun heartRateSensorPeripherals(): Flow<HeartRateSensor> = flow {
        emitAll(BluetoothHeartRateSensorService(ble).sensors.map { sensor ->
            HeartRateSensorImpl(sensor)
        })
    }

    private fun pacemakerPeripherals(): Flow<PacemakerAppDevice> = flow {
        emitAll(pacemakerCentral.await().connections.map { connection ->
            PacemakerPeripheralAppDeviceImpl(connection)
        })
    }

    init {
        ble.scope.launch {
            pacemakerPeripheral.await().startAdvertising()
        }
    }

    companion object {
        val log = LogTag.ble.forClass<BluetoothService>()
    }
}

private class HeartRateSensorImpl(
    private val sensor: BluetoothHeartRateSensor
) : HeartRateSensor, BluetoothHeartRateSensor by sensor {
    override fun toString(): String {
        return "Heart Rate Sensor: ${sensor.deviceId}"
    }
}

private class PacemakerPeripheralAppDeviceImpl(
    private val connection: BleConnection,
) : PacemakerAppDevice {

    override val id: BleDeviceId = connection.deviceId

    override val broadcasts: SharedFlow<PacemakerBroadcastPackage> = connection.receivePacemakerBroadcastPackages()
        .shareIn(connection.scope, SharingStarted.WhileSubscribed())

    override fun toString(): String {
        return "Pacemaker App: ${connection.deviceId}"
    }
}

private class PacemakerCentralAppDeviceImpl(
    private val id: BleDeviceId
)