package io.sellmair.pacemaker

import app.cash.sqldelight.coroutines.asFlow
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.model.newUser
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.error
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map


internal class SqlUserService(
    private val database: SafePacemakerDatabase,
    private val meId: UserId,
) : UserService {

    override suspend fun me(): User {
        val newUserHeartRateLimit = UserService.NewUserHeartRateLimit.value()

        val user = transaction {
            val me = userQueries.findUserById(meId.value).executeAsOneOrNull()
            if (me != null) return@transaction User(id = UserId(me.id), name = me.name, isAdhoc = false)

            val user = newUser(meId)
            userQueries.saveUser(user.toDbUser())
            userQueries.saveHeartRateLimit(
                Db_heart_rate_limit(
                    user_id = user.id.value,
                    heart_rate_limit = newUserHeartRateLimit.value.toDouble()
                )
            )
            user
        }

        onSaveUser.emit(user)
        return user
    }

    override suspend fun saveUser(user: User) {
        transaction {
            userQueries.saveUser(user.toDbUser())
        }
        onSaveUser.emit(user)
    }

    override suspend fun deleteUser(user: User) = transaction {
        userQueries.deleteUser(user.id.value)
        userQueries.deleteUserSensors(user.id.value)
        userQueries.deleteHeartRateLimit(user.id.value)
    }

    override suspend fun linkSensor(user: User, sensorId: HeartRateSensorId) = transaction {
        userQueries.saveSensor(Db_sensor(id = sensorId.value, user_id = user.id.value))
    }

    override suspend fun unlinkSensor(sensorId: HeartRateSensorId) = transaction {
        userQueries.deleteSensor(id = sensorId.value)
    }

    override suspend fun saveHeartRateLimit(user: User, limit: HeartRate) = transaction {
        userQueries.saveHeartRateLimit(
            Db_heart_rate_limit(
                user_id = user.id.value,
                heart_rate_limit = limit.value.toDouble()
            ),
        )
    }

    override suspend fun findUser(userId: UserId): User? = database {
        userQueries.findUserById(userId.value).executeAsOneOrNull()?.toUser()
    }


    override suspend fun findUser(sensorId: HeartRateSensorId): User? = database {
        userQueries.findUserBySensorId(sensorId.value).executeAsOneOrNull()?.let { result ->
            User(id = UserId(result.id), name = result.name, isAdhoc = result.is_adhoc > 0)
        }
    }

    override fun findUserFlow(sensorId: HeartRateSensorId): Flow<User?> = database.flow {
        userQueries.findUserBySensorId(sensorId.value).asFlow().map { query ->
            query.executeAsOneOrNull()?.toUser()
        }
    }

    override suspend fun findHeartRateLimit(user: User): HeartRate? = database {
        userQueries.findUserSettingsForUserId(user.id.value).executeAsOneOrNull()?.let { result ->
            HeartRate(result.heart_rate_limit?.toFloat() ?: return@database null)
        }
    }

    override fun findHeartRateLimitFlow(user: User): Flow<HeartRate?> = database.flow {
        userQueries.findUserSettingsForUserId(user.id.value).asFlow()
            .map { query -> query.executeAsOneOrNull() }
            .map { result -> HeartRate(result?.heart_rate_limit?.toFloat() ?: return@map null) }
    }

    private suspend fun <T> transaction(transaction: PacemakerDatabase.() -> T): T {
        try {
            return database {
                transactionWithResult {
                    transaction()
                }
            }
        } finally {
            onChange.emit(Unit)
        }
    }

    override val onChange = MutableSharedFlow<Unit>()

    override val onSaveUser = MutableSharedFlow<User>()
}

internal fun User.toDbUser(): Db_user {
    return Db_user(id = id.value, name = name, is_adhoc = if (isAdhoc) 1 else 0)
}

internal fun Db_user.toUser(): User {
    return User(id = UserId(id), name = name, isAdhoc = is_adhoc > 0)
}

internal fun FindUserBySensorId.toUser(): User {
    return User(id = UserId(id), name = name, isAdhoc = is_adhoc > 0)
}