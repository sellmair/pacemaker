package io.sellmair.broadheart

import io.sellmair.broadheart.bluetooth.BlePeripheral
import io.sellmair.broadheart.bluetooth.HeartcastBroadcastPackage
import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface BluetoothService {

    val peripherals: SharedFlow<Peripheral>

    enum class ConnectionState {
        NotConnected,
        Connectable,
        Connected,
    }

    sealed interface Peripheral {
        val id: BlePeripheral.Id
        val state: ConnectionState

        interface HeartRateSensor : Peripheral {
            val measurements: Flow<HeartRateMeasurement>
            fun tryConnect()
            fun tryDisconnect()
        }

        interface PacemakerApp : Peripheral {
            val broadcasts: Flow<HeartcastBroadcastPackage>
        }
    }
}

