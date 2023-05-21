package io.sellmair.pacemaker.service.impl

import io.sellmair.pacemaker.model.*
import io.sellmair.pacemaker.service.UserService
import io.sellmair.pacemaker.utils.defaultFileSystem
import io.sellmair.pacemaker.utils.readUtf8OrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

class StoredUserService(
    coroutineScope: CoroutineScope,
    root: Path,
    private val fs: FileSystem = defaultFileSystem(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : UserService {

    private val mutex = Mutex()
    private val users = mutableMapOf<UserId, User>()
    private val userIdBySensorId = mutableMapOf<HeartRateSensorId, UserId>()
    private val heartRateLimitByUserId = mutableMapOf<UserId, HeartRate>()

    private val usersSerializer = ListSerializer(User.serializer())
    private val userIdBySensorIdSerializer = MapSerializer(HeartRateSensorId.serializer(), UserId.serializer())
    private val heartRateLimitByUserIdSerializer = MapSerializer(UserId.serializer(), HeartRate.serializer())

    override suspend fun currentUser(): User {
        return withLock {
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
        writeWithLock {
            users[user.id] = user
        }
    }

    override suspend fun delete(user: User) {
        writeWithLock {
            users.remove(user.id)
        }
    }

    override suspend fun linkSensor(user: User, sensorId: HeartRateSensorId) {
        writeWithLock {
            userIdBySensorId[sensorId] = user.id
        }
    }

    override suspend fun unlinkSensor(sensorId: HeartRateSensorId) {
        writeWithLock {
            userIdBySensorId.remove(sensorId)
        }
    }

    override suspend fun saveUpperHeartRateLimit(user: User, limit: HeartRate) {
        writeWithLock {
            heartRateLimitByUserId[user.id] = limit
        }
    }

    override suspend fun findUser(sensorId: HeartRateSensorId): User? {
        return withLock {
            val userId = userIdBySensorId[sensorId] ?: return@withLock null
            users[userId]
        }
    }

    override suspend fun findUpperHeartRateLimit(user: User): HeartRate? {
        return withLock {
            heartRateLimitByUserId[user.id]
        }
    }

    override val onChange: MutableSharedFlow<Unit> = MutableSharedFlow()

    /* IO */

    private suspend fun <T> withLock(readAction: suspend () -> T): T {
        initialLoad.await()
        return mutex.withLock { readAction() }
    }

    private suspend fun <T> writeWithLock(writeAction: suspend () -> T): T {
        return withLock { write { writeAction() } }.also { onChange.emit(Unit) }
    }

    private suspend fun <T> write(writeAction: suspend () -> T): T {
        return try {
            writeAction()
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
            decoded.forEach { decodedUser -> users[decodedUser.id] = decodedUser }
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

                    fs.write(usersFile) {
                        writeUtf8(Json.encodeToString(usersSerializer, users.values.toList()))
                    }

                    fs.write(userIdBySensorIdFile) {
                        writeUtf8(Json.encodeToString(userIdBySensorIdSerializer, userIdBySensorId))
                    }

                    fs.write(heartRateLimitsFile) {
                        writeUtf8(Json.encodeToString(heartRateLimitByUserIdSerializer, heartRateLimitByUserId))
                    }

                } catch (t: Throwable) {
                    println("${StoredUserService::class.simpleName}: Failed to update storage: ${t.message}")
                }
            }
        }
    }
}
