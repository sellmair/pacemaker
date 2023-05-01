package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface PacemakerBleWritable {
    suspend fun setUser(user: User)
    suspend fun setHeartRate(sensorId: HeartRateSensorId, heartRate: HeartRate)
    suspend fun setHeartRateLimit(heartRate: HeartRate)
}

fun PacemakerBleWritable(scope: CoroutineScope, underlying: List<PacemakerBleWritable>): PacemakerBleWritable {
    return CompositePacemakerBleWritable(scope, underlying)
}

private class CompositePacemakerBleWritable(
    private val scope: CoroutineScope,
    private val underlying: List<PacemakerBleWritable>
) : PacemakerBleWritable {
    override suspend fun setUser(user: User) {
        underlying.forEach { scope.launch { it.setUser(user) } }
    }

    override suspend fun setHeartRate(sensorId: HeartRateSensorId, heartRate: HeartRate) {
        underlying.forEach { scope.launch { it.setHeartRate(sensorId, heartRate) } }
    }

    override suspend fun setHeartRateLimit(heartRate: HeartRate) {
        underlying.forEach { scope.launch { it.setHeartRateLimit(heartRate) } }
    }
}