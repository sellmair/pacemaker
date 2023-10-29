package io.sellmair.pacemaker

import app.cash.sqldelight.coroutines.asFlow
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.model.randomNewUser
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map


internal class SqlUserService(private val database: PacemakerDatabase) : UserService {
    override suspend fun me(): User = transaction {
        val me = database.userQueries.findMe().executeAsOneOrNull()
        if (me != null) return@transaction User(id = UserId(me.id), name = me.name, isAdhoc = false)


        val user = randomNewUser()
        database.userQueries.saveUser(user.toDbUser())
        database.userQueries.saveUserSettings(
            Db_user_settings(
                id = 0, /* position 0 is reserved for 'me' */
                user_id = user.id.value,
                heart_rate_limit = UserService.NewUserHeartRateLimit.value().value.toDouble()
            )
        )
        user
    }

    override suspend fun saveUser(user: User) = transaction {
        database.userQueries.saveUser(user.toDbUser())
    }

    override suspend fun deleteUser(user: User) = transaction {
        database.userQueries.deleteUser(user.id.value)
        database.userQueries.deleteUserSensors(user.id.value)
        database.userQueries.delteUserSettings(user.id.value)
    }

    override suspend fun linkSensor(user: User, sensorId: HeartRateSensorId) = transaction {
        database.userQueries.saveSensor(Db_sensor(id = sensorId.value, user_id = user.id.value))
    }

    override suspend fun unlinkSensor(sensorId: HeartRateSensorId) = transaction {
        database.userQueries.deleteSensor(id = sensorId.value)
    }

    override suspend fun saveHeartRateLimit(user: User, limit: HeartRate) = transaction {
        database.userQueries.saveHeartRateLimit(
            user_id_ = user.id.value,
            user_id = user.id.value,
            heart_rate_limit = limit.value.toDouble(),
        )
    }

    override suspend fun findUser(userId: UserId): User? {
        return database.userQueries.findUserById(userId.value).executeAsOneOrNull()?.toUser()
    }


    override suspend fun findUser(sensorId: HeartRateSensorId): User? {
        return database.userQueries.findUserBySensorId(sensorId.value).executeAsOneOrNull()?.let { result ->
            User(id = UserId(result.id), name = result.name, isAdhoc = result.is_adhoc > 0)
        }
    }

    override fun findUserFlow(sensorId: HeartRateSensorId): Flow<User?> {
        return database.userQueries.findUserBySensorId(sensorId.value).asFlow().map { query ->
            query.executeAsOneOrNull()?.toUser()
        }
    }

    override suspend fun findHeartRateLimit(user: User): HeartRate? {
        return database.userQueries.findUserSettingsForUserId(user.id.value).executeAsOneOrNull()?.let { result ->
            HeartRate(result.heart_rate_limit?.toFloat() ?: return null)
        }
    }

    override fun findHeartRateLimitFlow(user: User): Flow<HeartRate?> {
        return database.userQueries.findUserSettingsForUserId(user.id.value).asFlow()
            .map { query -> query.executeAsOneOrNull() }
            .map { result -> HeartRate(result?.heart_rate_limit?.toFloat() ?: return@map null) }
    }

    private suspend fun <T> transaction(transaction: suspend () -> T): T {
        try {
            return database.transactionWithResult {
                transaction()
            }
        } finally {
            onChange.emit(Unit)
        }
    }

    override val onChange = MutableSharedFlow<Unit>()
}

private fun User.toDbUser(): Db_user {
    return Db_user(id = id.value, name = name, is_adhoc = if (isAdhoc) 1 else 0)
}

private fun Db_user.toUser(): User {
    return User(id = UserId(id), name = name, isAdhoc = is_adhoc > 0)
}

private fun FindUserBySensorId.toUser(): User {
    return User(id = UserId(id), name = name, isAdhoc = is_adhoc > 0)
}