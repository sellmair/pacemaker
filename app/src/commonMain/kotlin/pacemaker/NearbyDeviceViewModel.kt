package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.bluetooth.Rssi
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.flow.StateFlow

sealed interface NearbyDeviceViewModel {
    val id: HeartRateSensorId
    val heartRate: StateFlow<HeartRate?>
    val rssi: StateFlow<Rssi?>
    val associatedUser: StateFlow<User?>
    val associatedHeartRateLimit: StateFlow<HeartRate?>
}

interface HeartRateSensorViewModel : NearbyDeviceViewModel {
    val state: StateFlow<BleConnectable.ConnectionState>
    fun tryConnect()
    fun tryDisconnect()
}
