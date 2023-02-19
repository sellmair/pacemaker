package io.sellmair.broadheart.service

import android.content.Context
import io.sellmair.broadheart.User
import io.sellmair.broadheart.UserId
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.hrSensor.HrSensorInfo

class AndroidUserService(private val context: Context) : UserService {

    private val users = mutableMapOf(
        UserId(0L) to User(
            isMe = true,
            uuid = UserId(0L),
            name = "Sebastian Sellmair",
            imageUrl = null
        )
    )

    private val sensors = mutableMapOf<HrSensorInfo.HrSensorId, User>()

    private val limits = mutableMapOf<UserId, HeartRate>()

    override suspend fun currentUser(): User {
        return users.values.first { it.isMe }
    }

    override suspend fun save(user: User) {
        users[user.uuid] = user
    }

    override suspend fun saveSensorId(user: User, sensorId: HrSensorInfo.HrSensorId) {
        sensors[sensorId] = user
    }

    override suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate) {
        limits[user.uuid] = limit
    }

    override suspend fun findUser(sensorId: HrSensorInfo.HrSensorId): User? {
        return sensors[sensorId]
    }

    override suspend fun findUpperHeartRateLimit(user: User): HeartRate? {
        if(user.isMe) return HeartRate(130)
        return limits[user.uuid]
    }
}