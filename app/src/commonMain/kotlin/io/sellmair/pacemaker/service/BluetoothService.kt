package io.sellmair.pacemaker.service

import io.sellmair.pacemaker.bluetooth.BluetoothHeartRateSensor
import kotlinx.coroutines.flow.SharedFlow

interface BluetoothService {
    val devices: SharedFlow<Device>
    val allDevices: SharedFlow<List<Device>>

    sealed interface Device {

        interface HeartRateSensor : Device, BluetoothHeartRateSensor

    }
}

