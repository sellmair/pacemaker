@file:OptIn(FlowPreview::class)

package io.sellmair.broadheart

import io.sellmair.broadheart.BluetoothService.Peripheral.HeartRateSensor
import io.sellmair.broadheart.BluetoothService.Peripheral.PacemakerApp
import io.sellmair.broadheart.bluetooth.Ble
import io.sellmair.broadheart.bluetooth.BlePeripheral
import io.sellmair.broadheart.bluetooth.HeartRateBlePeripheral
import io.sellmair.broadheart.bluetooth.startHeartRateBleCentralService
import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

fun BluetoothService(ble: Ble): BluetoothService {
    return BluetoothServiceImpl(ble)
}

private class BluetoothServiceImpl(private val ble: Ble) : BluetoothService {

    override val peripherals: SharedFlow<BluetoothService.Peripheral> = flowOf(
        heartRateSensorPeripherals(),
        pacemakerPeripherals()
    ).flattenMerge()
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    override val allPeripherals: SharedFlow<List<BluetoothService.Peripheral>> = peripherals
        .runningFold(emptyList<BluetoothService.Peripheral>()) { list, peripheral -> list + peripheral }
        .shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    private fun heartRateSensorPeripherals(): Flow<HeartRateSensor> = flow {
        emitAll(ble.startHeartRateBleCentralService().peripherals.map { peripheral ->
            HeartRateSensorImpl(ble.scope, peripheral)
        })
    }

    private fun pacemakerPeripherals(): Flow<PacemakerApp> {
        return flowOf()
    }
}

private class HeartRateSensorImpl(
    scope: CoroutineScope,
    peripheral: HeartRateBlePeripheral
) : HeartRateSensor, BlePeripheral by peripheral {

    override val measurements: SharedFlow<HeartRateMeasurement> =
        peripheral.heartRateMeasurements.shareIn(scope, SharingStarted.Eagerly)

}
