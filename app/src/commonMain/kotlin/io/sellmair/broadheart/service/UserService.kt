package io.sellmair.broadheart.service

import io.sellmair.broadheart.User
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.hrSensor.HrSensorId
import io.sellmair.broadheart.hrSensor.HrSensorInfo

interface UserService {
    suspend fun currentUser(): User
    suspend fun save(user: User)
    suspend fun delete(user: User)
    suspend fun linkSensor(user: User, sensorId: HrSensorId)
    suspend fun unlinkSensor(sensorId: HrSensorId)
    suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate)

    suspend fun findUser(sensorInfo: HrSensorInfo): User? = findUser(sensorInfo.id)
    suspend fun findUser(sensorId: HrSensorId): User?
    suspend fun findUpperHeartRateLimit(user: User): HeartRate?
}

