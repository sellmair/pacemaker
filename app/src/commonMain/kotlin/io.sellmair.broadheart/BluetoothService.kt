package io.sellmair.broadheart

import io.sellmair.broadheart.bluetooth.BlePeripheral
import io.sellmair.broadheart.bluetooth.HeartcastBroadcastPackage
import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.flow.SharedFlow

interface BluetoothService {
    val peripherals: SharedFlow<Peripheral>
    val allPeripherals: SharedFlow<List<Peripheral>>

    sealed interface Peripheral: BlePeripheral {

        interface HeartRateSensor : Peripheral {
            val measurements: SharedFlow<HeartRateMeasurement>
        }

        interface PacemakerApp : Peripheral {
            val broadcasts: SharedFlow<HeartcastBroadcastPackage>
        }
    }
}

