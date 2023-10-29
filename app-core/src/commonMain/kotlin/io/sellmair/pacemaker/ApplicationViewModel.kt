package io.sellmair.pacemaker

import io.sellmair.pacemaker.ApplicationIntent.MainPageIntent
import io.sellmair.pacemaker.ApplicationIntent.SettingsPageIntent
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.randomUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

interface ApplicationViewModel {
    val me: StateFlow<MeState?>
    val group: StateFlow<GroupState?>
    val heartRateSensorViewModels: StateFlow<List<HeartRateSensorViewModel>>
    fun send(intent: ApplicationIntent)
}

fun ApplicationViewModel(
    coroutineScope: CoroutineScope,
    backend: ApplicationBackend,
): ApplicationViewModel {
    return ApplicationViewModelImpl(
        coroutineScope, backend.userService, backend.heartRateSensorBluetoothService,
        backend.stateBus.getState(MeState),
        backend.stateBus.getState(GroupState)
    )
}

private class ApplicationViewModelImpl(
    scope: CoroutineScope,
    private val userService: UserService,
    private val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>,
    override val me: StateFlow<MeState?>,
    override val group: StateFlow<GroupState>
) : ApplicationViewModel {

    private val intentQueue = Channel<ApplicationIntent>(Channel.UNLIMITED)

    private val viewModelsBySensors = mutableMapOf<HeartRateSensor, HeartRateSensorViewModel>()

    override val heartRateSensorViewModels: StateFlow<List<HeartRateSensorViewModel>> =
        flow { emitAll(heartRateSensorBluetoothService.await().allSensorsNearby) }
            .map { nearbySensors ->
                nearbySensors.map { sensor ->
                    viewModelsBySensors.getOrPut(sensor) {
                        HeartRateSensorViewModelImpl(scope, sensor, userService)
                    }
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())


    override fun send(intent: ApplicationIntent) {
        intentQueue.trySend(intent)
    }

    private suspend fun process(intent: ApplicationIntent): Unit = when (intent) {
        is MainPageIntent.UpdateHeartRateLimit -> {
            val me = userService.me()
            userService.saveHeartRateLimit(me, intent.heartRateLimit)
        }

        is SettingsPageIntent.LinkSensor -> {
            userService.linkSensor(intent.user, intent.sensor)
        }

        is SettingsPageIntent.UnlinkSensor -> {
            userService.unlinkSensor(intent.sensor)
        }

        is SettingsPageIntent.UpdateMe -> {
            userService.saveUser(intent.user)
        }

        is SettingsPageIntent.CreateAdhocUser -> {
            val id = randomUserId()
            val adhocUser = User(
                id = id,
                name = "Adhoc ${id.value.absoluteValue % 1000}",
                isAdhoc = true
            )
            userService.saveUser(adhocUser)
            userService.linkSensor(adhocUser, intent.sensor)
            userService.saveHeartRateLimit(adhocUser, HeartRate(130))
        }

        is SettingsPageIntent.UpdateAdhocUser -> {
            userService.saveUser(intent.user)
        }

        is SettingsPageIntent.DeleteAdhocUser -> {
            userService.deleteUser(intent.user)
        }

        is SettingsPageIntent.UpdateAdhocUserLimit -> {
            userService.saveHeartRateLimit(intent.user, intent.limit)
        }
    }

    init {
        scope.launch(Dispatchers.Main.immediate) {
            intentQueue.consumeEach { intent ->
                process(intent)
            }
        }
    }
}