package io.sellmair.pacemaker.service

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.flow.Flow

interface UserService {
    suspend fun currentUser(): User
    suspend fun save(user: User)
    suspend fun delete(user: User)
    suspend fun linkSensor(user: User, sensorId: HeartRateSensorId)
    suspend fun unlinkSensor(sensorId: HeartRateSensorId)
    suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate)

    suspend fun findUser(sensorInfo: HeartRateSensorInfo): User = findUser(sensorInfo.id)
    suspend fun findUser(sensorId: HeartRateSensorId): User
    suspend fun findUpperHeartRateLimit(user: User): HeartRate?

    val onChange: Flow<Unit>
}
