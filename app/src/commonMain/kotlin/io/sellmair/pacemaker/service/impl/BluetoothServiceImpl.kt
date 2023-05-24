@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker.service.impl

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.bluetooth.BluetoothHeartRateSensor
import io.sellmair.pacemaker.bluetooth.BluetoothHeartRateSensorService
import io.sellmair.pacemaker.service.BluetoothService
import io.sellmair.pacemaker.service.BluetoothService.Device.HeartRateSensor
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

fun BluetoothService(
    ble: Ble
): BluetoothService {
    return BluetoothServiceImpl(ble)
}

private class BluetoothServiceImpl(
    private val ble: Ble
) : BluetoothService {

    override val devices: SharedFlow<BluetoothService.Device> = flowOf(
        heartRateSensorPeripherals(),
    ).flattenMerge()
        .onEach { log.info("Discovered: $it") }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    override val allDevices: SharedFlow<List<BluetoothService.Device>> = devices
        .runningFold(emptyList<BluetoothService.Device>()) { list, peripheral -> list + peripheral }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)


    private fun heartRateSensorPeripherals(): Flow<HeartRateSensor> = flow {
        emitAll(BluetoothHeartRateSensorService(ble).sensors.map { sensor ->
            HeartRateSensorImpl(sensor)
        })
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
