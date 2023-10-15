package io.sellmair.pacemaker.service

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import kotlinx.coroutines.flow.Flow

interface UserService {
    suspend fun currentUser(): User
    suspend fun save(user: User)
    suspend fun delete(user: User)
    suspend fun linkSensor(user: User, sensorId: HeartRateSensorId)
    suspend fun unlinkSensor(sensorId: HeartRateSensorId)
    suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate)

    suspend fun findUser(userId: UserId): User?
    suspend fun findUser(sensorId: HeartRateSensorId): User?
    suspend fun findHeartRateLimit(user: User): HeartRate?

    val onChange: Flow<Unit>
}
