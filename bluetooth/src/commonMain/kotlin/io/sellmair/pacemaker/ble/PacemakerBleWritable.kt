package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User

interface PacemakerBleWritable {
    suspend fun setUser(user: User)
    suspend fun setHeartRate(sensorId: HeartRateSensorId, heartRate: HeartRate)
    suspend fun setHeartRateLimit(heartRate: HeartRate)
}