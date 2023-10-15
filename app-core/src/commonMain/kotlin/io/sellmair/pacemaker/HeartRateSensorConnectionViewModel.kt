package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import io.sellmair.pacemaker.service.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface HeartRateSensorConnectionViewModel {
    val connectionState: StateFlow<BleConnectable.ConnectionState?>
    val connectIfPossible: StateFlow<Boolean>

    fun onConnectClicked()
    fun onDisconnectClicked()
}

/**
 * Actor encapsulating predictive UI:
 * There is a certain lag between demanding the bluetooth device to connect
 * and the device responding as 'connecting'. Therefore we predict the 'connecting' state
 * within this actor incorporating ui events
 */
class HeartRateRateSensorConnectionViewModelImpl(
    private val scope: CoroutineScope,
    private val sensor: HeartRateSensor,
    private val userService: UserService,
) : HeartRateSensorConnectionViewModel {
    private val connectionStateImpl = MutableStateFlow<BleConnectable.ConnectionState?>(null)

    override val connectionState = connectionStateImpl.asStateFlow()

    override val connectIfPossible: StateFlow<Boolean> = sensor.connectIfPossible

    private val events = Channel<Event>(Channel.UNLIMITED)

    private sealed interface Event {
        data class SensorStateChange(val state: BleConnectable.ConnectionState) : Event {
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

    override fun onConnectClicked() {
        events.trySend(Event.UIEvent.ConnectClicked)
        sensor.connectIfPossible(true)
        scope.launch(Dispatchers.Main.immediate) {
            if (userService.findUser(sensor.deviceId.toHeartRateSensorId()) == null) {
                userService.linkSensor(userService.currentUser(), sensor.deviceId.toHeartRateSensorId())
            }
        }
    }

    override fun onDisconnectClicked() {
        events.trySend(Event.UIEvent.DisconnectClicked)
        sensor.connectIfPossible(false)
        scope.launch(Dispatchers.Main.immediate) {
            if(userService.findUser(sensor.deviceId.toHeartRateSensorId())?.isMe == true) {
                userService.unlinkSensor(sensor.deviceId.toHeartRateSensorId())
            }
        }
    }

    init {
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
                        if (connectionStateImpl.value != BleConnectable.ConnectionState.Disconnected) {
                            connectionStateImpl.value = null
                        }
                    }

                    is Event.UIEvent.ConnectClicked -> {
                        if (connectionStateImpl.value == BleConnectable.ConnectionState.Disconnected) {
                            connectionStateImpl.value = BleConnectable.ConnectionState.Connecting
                        }
                    }
                }
            }
        }
    }

    companion object {
        val log = LogTag.appCore.forClass<HeartRateRateSensorConnectionViewModelImpl>()
    }
}