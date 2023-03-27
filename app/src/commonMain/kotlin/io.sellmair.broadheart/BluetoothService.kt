package io.sellmair.broadheart

import io.sellmair.broadheart.bluetooth.*
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothService {
    suspend fun heartcastBle(): HeartcastBle
    val devices: SharedFlow<Device>
    val allDevices: SharedFlow<List<Device>>

    sealed interface Device {

        interface HeartRateSensor : Device {
            val id: HeartRateSensorId
            val measurements: SharedFlow<HeartRateMeasurement>
            val rssi: StateFlow<Rssi>
            val state: StateFlow<BlePeripheral.State>
            fun tryConnect()
            fun tryDisconnect()
        }

        interface PacemakerAppDevice : Device {
            val id: BleDeviceId
            val broadcasts: SharedFlow<HeartcastBroadcastPackage>
        }
    }
}

