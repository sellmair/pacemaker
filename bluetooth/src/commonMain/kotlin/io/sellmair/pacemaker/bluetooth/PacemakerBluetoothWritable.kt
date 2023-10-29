package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User

interface PacemakerBluetoothWritable {
    suspend fun setUser(user: User)
    suspend fun setHeartRate(heartRate: HeartRate)
    suspend fun setHeartRateLimit(heartRate: HeartRate)
}
