package io.sellmair.broadheart.service

import android.content.Context
import io.sellmair.broadheart.User
import io.sellmair.broadheart.UserId
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.hrSensor.HrSensorInfo

class AndroidUserService(private val context: Context) : UserService {
    override suspend fun currentUser(): User {
        return User(
            isMe = true,
            uuid = UserId(0L),
            name = "Sebastian Sellmair",
            imageUrl = null
        )
    }

    override suspend fun save(user: User) {
    }

    override suspend fun saveSensorId(user: User, sensorId: HrSensorInfo.HrSensorId) {
    }

    override suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate) {
        return
    }

    override suspend fun findUser(sensorId: HrSensorInfo.HrSensorId): User? {
        //return currentUser()
        return null
    }

    override suspend fun findUpperHeartRateLimit(user: User): HeartRate? {
        return null
    }
}