package io.sellmair.broadheart

import io.sellmair.broadheart.bluetooth.BlePeripheral
import io.sellmair.broadheart.bluetooth.Rssi
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.flow.StateFlow

sealed interface NearbyDeviceViewModel {
    val id: HeartRateSensorId
    val heartRate: StateFlow<HeartRate?>
    val rssi: StateFlow<Rssi?>
    val associatedUser: StateFlow<User?>
    val associatedHeartRateLimit: StateFlow<HeartRate?>
}

interface HeartRateSensorViewModel : NearbyDeviceViewModel {
    val state: StateFlow<BlePeripheral.State>
    fun tryConnect()
    fun tryDisconnect()
}
