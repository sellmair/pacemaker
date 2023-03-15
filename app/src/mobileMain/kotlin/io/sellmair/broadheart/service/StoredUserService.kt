package io.sellmair.broadheart.service

import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.UserId
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.randomUserId
import io.sellmair.broadheart.utils.defaultFileSystem
import io.sellmair.broadheart.utils.readUtf8OrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class StoredUserService(
    coroutineScope: CoroutineScope,
    root: Path,
    private val fs: FileSystem = defaultFileSystem(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : UserService {

    private val users = mutableMapOf<UserId, User>()
    private val userIdBySensorId = mutableMapOf<HeartRateSensorId, UserId>()
    private val heartRateLimitByUserId = mutableMapOf<UserId, HeartRate>()

    override suspend fun currentUser(): User {
        return read {
            users.values.firstOrNull { it.isMe } ?: write {
                val id = randomUserId()
                val myUser = User(isMe = true, id = id, name = "Anonymous ${id.value % 1000}")
                users[id] = myUser

                /* Set default HR limit for newly created user */
                heartRateLimitByUserId[id] = HeartRate(130)

                myUser
            }
        }
    }

    override suspend fun save(user: User) {
        write {
            users[user.id] = user
        }
    }

    override suspend fun delete(user: User) {
        write {
            users.remove(user.id)
        }
    }

    override suspend fun linkSensor(user: User, sensorId: HeartRateSensorId) {
        write {
            userIdBySensorId[sensorId] = user.id
        }
    }

    override suspend fun unlinkSensor(sensorId: HeartRateSensorId) {
        write {
            userIdBySensorId.remove(sensorId)
        }
    }

    override suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate) {
        write {
            heartRateLimitByUserId[user.id] = limit
        }
    }

    override suspend fun findUser(sensorId: HeartRateSensorId): User? {
        return read {
            val userId = userIdBySensorId[sensorId] ?: return null
            users[userId]
        }
    }

    override suspend fun findUpperHeartRateLimit(user: User): HeartRate? {
        return read {
            heartRateLimitByUserId[user.id]
        }
    }

    /* IO */

    private val mutex = Mutex()

    private suspend inline fun <T> read(readAction: () -> T): T {
        initialLoad.await()
        return mutex.withLock { readAction() }
    }

    private suspend inline fun <T> write(writeAction: () -> T): T {
        initialLoad.await()
        return try {
            mutex.withLock {
                writeAction()
            }
        } finally {
            onWriteActionPerformedChannel.send(Unit)
        }
    }

    private val usersFile = root.resolve("users.json")
    private val userIdBySensorIdFile = root.resolve("sensors.json")
    private val heartRateLimitsFile = root.resolve("heartRateLimits.json")

    private val initialLoad = coroutineScope.async(ioDispatcher) {
        /* Load users */
        run {
            val content = fs.readUtf8OrNull(usersFile) ?: return@run
            val decoded = Json.decodeFromString<List<User>>(content)
            decoded.forEach { decodedUser ->
                users[decodedUser.id] = decodedUser
            }
        }

        /* Load userIdBySensorId */
        run {
            val content = fs.readUtf8OrNull(userIdBySensorIdFile) ?: return@run
            val decoded = Json.decodeFromString<Map<HeartRateSensorId, UserId>>(content)
            userIdBySensorId.putAll(decoded)
        }

        /* Load heart rate limits */
        run {
            val content = fs.readUtf8OrNull(heartRateLimitsFile) ?: return@run
            val decoded = Json.decodeFromString<Map<UserId, HeartRate>>(content)
            heartRateLimitByUserId.putAll(decoded)
        }
    }

    private val onWriteActionPerformedChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        coroutineScope.launch {
            onWriteActionPerformedChannel.consumeEach {
                try {
                    usersFile.parent?.let { fs.createDirectories(it) }
                    userIdBySensorIdFile.parent?.let { fs.createDirectories(it) }
                    heartRateLimitsFile.parent?.let { fs.createDirectories(it) }

                    fs.write(usersFile) { writeUtf8(Json.encodeToString(users.values)) }
                    fs.write(userIdBySensorIdFile) { writeUtf8(Json.encodeToString(userIdBySensorId)) }
                    fs.write(heartRateLimitsFile) { writeUtf8(Json.encodeToString(heartRateLimitByUserId)) }
                } catch (t: Throwable) {
                    println("${StoredUserService::class.simpleName}: Failed to update storage: ${t.message}")
                }
            }
        }
    }
}
