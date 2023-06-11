@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connecting
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Disconnected
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.service.UserService
import io.sellmair.pacemaker.ui.ui
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HeartRateSensorViewModelImpl(
    private val scope: CoroutineScope,
    private val userService: UserService,
    private val heartRateSensor: HeartRateSensor,
) : HeartRateSensorViewModel {

    override val name: String? = heartRateSensor.deviceName
    override val id: HeartRateSensorId = heartRateSensor.deviceId.toHeartRateSensorId()

    override val rssi: StateFlow<Int?> = MutableStateFlow(null)

    private val uiConnectionStateActor = UIConnectionStateActor(heartRateSensor)
        .apply { launchIn(scope) }

    override val connectionState: StateFlow<ConnectionState?> = uiConnectionStateActor.connectionState

    override val connectIfPossible: StateFlow<Boolean> = heartRateSensor.connectIfPossible

    override val heartRate: StateFlow<HeartRate?> =
        heartRateSensor.heartRate.map { it.heartRate }.stateIn(scope, WhileSubscribed(), null)

    override val associatedUser: StateFlow<User?> = flow {
        emit(userService.findUser(id))
        userService.onChange.collect {
            emit(userService.findUser(id))
        }
    }.stateIn(scope, WhileSubscribed(), null)

    override val associatedHeartRateLimit: StateFlow<HeartRate?> = associatedUser
        .flatMapLatest { user ->
            if (user == null) flowOf<HeartRate?>(null)
            else flow {
                emit(userService.findUpperHeartRateLimit(user))
                userService.onChange.collect {
                    emit(userService.findUpperHeartRateLimit(user))
                }
            }
        }
        .stateIn(scope, WhileSubscribed(), null)

    override fun tryConnect() {
        uiConnectionStateActor.onUiConnectClicked()
        heartRateSensor.connectIfPossible(true)
    }

    override fun tryDisconnect() {
        uiConnectionStateActor.onUiDisconnectClicked()
        heartRateSensor.connectIfPossible(false)
    }
}

/**
 * Actor encapsulating predictive UI:
 * There is a certain lag between demanding the bluetooth device to connect
 * and the device responding as 'connecting'. Therefore we predict the 'connecting' state
 * within this actor incorporating ui events
 */
private class UIConnectionStateActor(private val sensor: HeartRateSensor) {
    private val connectionStateImpl = MutableStateFlow<ConnectionState?>(null)

    val connectionState = connectionStateImpl.asStateFlow()

    private val events = Channel<Event>(Channel.UNLIMITED)


    private sealed interface Event {
        data class SensorStateChange(val state: ConnectionState) : Event {
            override fun toString(): String = "Sensor: $state"
        }

        sealed interface UIEvent : Event {
            object ConnectClicked : UIEvent {
                override fun toString(): String = "ConnectClicked"
            }

            object DisconnectClicked : UIEvent {
                override fun toString(): String = "DisconnectClicked"
            }
        }
    }


    fun onUiConnectClicked() {
        events.trySend(Event.UIEvent.ConnectClicked)
    }

    fun onUiDisconnectClicked() {
        events.trySend(Event.UIEvent.DisconnectClicked)
    }

    fun launchIn(scope: CoroutineScope) {
        scope.launch {
            sensor.connectionState.collect { state ->
                events.send(Event.SensorStateChange(state))
            }
        }

        scope.launch {
            events.consumeEach { event ->
                log.debug("event: $event")
                when (event) {
                    is Event.SensorStateChange -> {
                        connectionStateImpl.value = event.state
                    }

                    is Event.UIEvent.DisconnectClicked -> {
                        if (connectionStateImpl.value != Disconnected) {
                            connectionStateImpl.value = null
                        }
                    }

                    is Event.UIEvent.ConnectClicked -> {
                        if (connectionStateImpl.value == Disconnected) {
                            connectionStateImpl.value = Connecting
                        }
                    }
                }
            }
        }
    }

    companion object {
        val log = LogTag.ui.forClass<UIConnectionStateActor>()
    }
}