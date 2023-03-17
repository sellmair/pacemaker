package io.sellmair.broadheart.service

import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.HeartRateSensorInfo

interface UserService {
    suspend fun currentUser(): User
    suspend fun save(user: User)
    suspend fun delete(user: User)
    suspend fun linkSensor(user: User, sensorId: HeartRateSensorId)
    suspend fun unlinkSensor(sensorId: HeartRateSensorId)
    suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate)

    suspend fun findUser(sensorInfo: HeartRateSensorInfo): User? = findUser(sensorInfo.id)
    suspend fun findUser(sensorId: HeartRateSensorId): User?
    suspend fun findUpperHeartRateLimit(user: User): HeartRate?
}
