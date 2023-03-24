@file:OptIn(FlowPreview::class)

import io.sellmair.broadheart.BluetoothService
import io.sellmair.broadheart.bluetooth.Ble
import io.sellmair.broadheart.bluetooth.receiveHeartRateMeasurements
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class BleBluetoothService(private val ble: Ble) : BluetoothService {

    override val peripherals: SharedFlow<BluetoothService.Peripheral> = flowOf(
        heartRateSensorPeripherals(),
        pacemakerPeripherals()
    ).flattenMerge().shareIn(ble.scope, SharingStarted.WhileSubscribed(), replay = Channel.UNLIMITED)

    private fun heartRateSensorPeripherals(): Flow<BluetoothService.Peripheral.HeartRateSensor> {
        TODO()
    }

    private fun pacemakerPeripherals(): Flow<BluetoothService.Peripheral.PacemakerApp> {
        TODO()
    }
}