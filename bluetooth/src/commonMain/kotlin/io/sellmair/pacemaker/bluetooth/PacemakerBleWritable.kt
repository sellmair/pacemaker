package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User

interface PacemakerBleWritable {
    suspend fun setUser(user: User)
    suspend fun setHeartRate(sensorId: HeartRateSensorId, heartRate: HeartRate)
    suspend fun setHeartRateLimit(heartRate: HeartRate)
}

fun PacemakerBleWritable(underlying: List<PacemakerBleWritable>): PacemakerBleWritable {
    return CompositePacemakerBleWritable(underlying)
}

private class CompositePacemakerBleWritable(private val underlying: List<PacemakerBleWritable>) : PacemakerBleWritable {
    override suspend fun setUser(user: User) {
        underlying.forEach { it.setUser(user) }
    }

    override suspend fun setHeartRate(sensorId: HeartRateSensorId, heartRate: HeartRate) {
        underlying.forEach { it.setHeartRate(sensorId, heartRate) }
    }

    override suspend fun setHeartRateLimit(heartRate: HeartRate) {
        underlying.forEach { it.setHeartRateLimit(heartRate) }
    }
}