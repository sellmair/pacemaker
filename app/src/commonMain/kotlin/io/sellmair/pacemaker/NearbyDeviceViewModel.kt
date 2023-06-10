package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.flow.StateFlow

interface HeartRateSensorViewModel {
    val id: HeartRateSensorId
    val name: String?
    val heartRate: StateFlow<HeartRate?>
    val rssi: StateFlow<Int?>
    val connectionState: StateFlow<BleConnectable.ConnectionState>
    val associatedUser: StateFlow<User?>
    val associatedHeartRateLimit: StateFlow<HeartRate?>

    fun tryConnect()
    fun tryDisconnect()
}
