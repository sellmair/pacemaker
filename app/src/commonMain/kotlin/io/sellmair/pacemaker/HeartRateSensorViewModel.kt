package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.flow.StateFlow

interface HeartRateSensorViewModel {
    val id: HeartRateSensorId
    val name: String?
    val heartRate: StateFlow<HeartRate?>
    val rssi: StateFlow<Int?>
    val associatedUser: StateFlow<User?>
    val associatedHeartRateLimit: StateFlow<HeartRate?>
    val connection: HeartRateSensorConnectionViewModel
}
