package io.sellmair.pacemaker

import io.sellmair.pacemaker.ApplicationIntent.MainPageIntent
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.randomUserId
import io.sellmair.pacemaker.service.GroupService
import io.sellmair.pacemaker.service.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

interface ApplicationViewModel {
    val me: StateFlow<User?>
    val group: StateFlow<Group?>
    val heartRateSensorViewModels: StateFlow<List<HeartRateSensorViewModel>>
    fun send(intent: ApplicationIntent)
}

fun ApplicationViewModel(
    coroutineScope: CoroutineScope,
    backend: ApplicationBackend,
): ApplicationViewModel {
    return ApplicationViewModelImpl(
        coroutineScope, backend.userService, backend.groupService, backend.heartRateSensorBluetoothService
    )
}

private class ApplicationViewModelImpl(
    scope: CoroutineScope,
    private val userService: UserService,
    private val groupService: GroupService,
    private val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>,
) : ApplicationViewModel {

    private val intentQueue = Channel<ApplicationIntent>(Channel.UNLIMITED)
    private val _me = MutableStateFlow<User?>(null)
    override val me = _me.asStateFlow()

    override val group = groupService.group

    private val viewModelsBySensors = mutableMapOf<HeartRateSensor, HeartRateSensorViewModel>()

    override val heartRateSensorViewModels: StateFlow<List<HeartRateSensorViewModel>> =
        flow { emitAll(heartRateSensorBluetoothService.await().allSensorsNearby) }
            .map { nearbySensors ->
                nearbySensors.map { sensor ->
                    viewModelsBySensors.getOrPut(sensor) {
                        HeartRateSensorViewModelImpl(scope, userService, sensor)
                    }
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())


    override fun send(intent: ApplicationIntent) {
        intentQueue.trySend(intent)
    }

    private suspend fun process(intent: ApplicationIntent): Unit = when (intent) {
        is MainPageIntent.UpdateHeartRateLimit -> {
            val me = userService.currentUser()
            userService.saveUpperHeartRateLimit(me, intent.heartRateLimit)
            groupService.invalidate()
        }

        is SettingsPageIntent.LinkSensor -> {
            userService.linkSensor(intent.user, intent.sensor)
        }

        is SettingsPageIntent.UnlinkSensor -> {
            userService.unlinkSensor(intent.sensor)
        }

        is SettingsPageIntent.UpdateMe -> {
            userService.save(intent.user)
            _me.value = intent.user
        }

        is SettingsPageIntent.CreateAdhocUser -> {
            val id = randomUserId()
            val adhocUser = User(
                isMe = false,
                id = id,
                name = "Adhoc ${id.value.absoluteValue % 1000}",
                isAdhoc = true
            )
            userService.save(adhocUser)
            userService.linkSensor(adhocUser, intent.sensor)
            userService.saveUpperHeartRateLimit(adhocUser, HeartRate(130))
            groupService.invalidate()
        }

        is SettingsPageIntent.UpdateAdhocUser -> {
            userService.save(intent.user)
            groupService.invalidate()
        }

        is SettingsPageIntent.DeleteAdhocUser -> {
            userService.delete(intent.user)
            groupService.invalidate()
        }

        is SettingsPageIntent.UpdateAdhocUserLimit -> {
            userService.saveUpperHeartRateLimit(intent.user, intent.limit)
            groupService.invalidate()
        }
    }

    init {
        scope.launch(Dispatchers.Main.immediate) {
            println("Launched user load")
            _me.value = userService.currentUser()
            println("Loaded user: ${_me.value}")
            intentQueue.consumeEach { intent ->
                process(intent)
                _me.value = userService.currentUser()
                groupService.invalidate()
            }
        }
    }
}