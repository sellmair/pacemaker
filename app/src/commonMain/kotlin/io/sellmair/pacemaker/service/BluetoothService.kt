package io.sellmair.pacemaker.service

import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.bluetooth.BluetoothHeartRateSensor
import io.sellmair.pacemaker.bluetooth.PacemakerBleWritable
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackage
import kotlinx.coroutines.flow.SharedFlow

interface BluetoothService {
    suspend fun pacemaker(): PacemakerBleWritable
    val devices: SharedFlow<Device>
    val allDevices: SharedFlow<List<Device>>

    val broadcasts: SharedFlow<PacemakerBroadcastPackage>

    sealed interface Device {

        interface HeartRateSensor : Device, BluetoothHeartRateSensor

        interface PacemakerAppDevice : Device {
            val id: BleDeviceId
            val broadcasts: SharedFlow<PacemakerBroadcastPackage>
        }
    }
}

