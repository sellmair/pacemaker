package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.ConfigurationKey
import kotlinx.coroutines.flow.Flow

interface UserService {
    suspend fun me(): User
    suspend fun saveUser(user: User)
    suspend fun deleteUser(user: User)

    suspend fun findUser(userId: UserId): User?
    suspend fun findUser(sensorId: HeartRateSensorId): User?
    fun findUserFlow(sensorId: HeartRateSensorId): Flow<User?>

    suspend fun linkSensor(user: User, sensorId: HeartRateSensorId)
    suspend fun unlinkSensor(sensorId: HeartRateSensorId)

    suspend fun findHeartRateLimit(user: User): HeartRate?
    fun findHeartRateLimitFlow(user: User): Flow<HeartRate?>
    suspend fun saveHeartRateLimit(user: User, limit: HeartRate)

    val onChange: Flow<Unit>
    val onSaveUser: Flow<User>

    object NewUserHeartRateLimit : ConfigurationKey.WithDefault<HeartRate> {
        override val default: HeartRate = HeartRate(145)
    }
}
