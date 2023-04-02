package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.bluetooth.*
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothService {
    suspend fun pacemakerBle(): PacemakerBle
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
            val broadcasts: SharedFlow<PacemakerBroadcastPackage>
        }
    }
}

