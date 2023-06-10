@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connected
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connecting
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Disconnected
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.service.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class HeartRateSensorViewModelImpl(
    private val scope: CoroutineScope,
    private val userService: UserService,
    private val heartRateSensor: HeartRateSensor,
) : HeartRateSensorViewModel {

    override val name: String? = heartRateSensor.deviceName
    override val id: HeartRateSensorId = heartRateSensor.deviceId.toHeartRateSensorId()

    // TODO
    override val rssi: StateFlow<Int?> = MutableStateFlow(null)

    private val uiPredictiveConnectionState = MutableStateFlow(Disconnected)

    override val connectionState: StateFlow<BleConnectable.ConnectionState> =
        flowOf(uiPredictiveConnectionState, heartRateSensor.connectionState).flattenMerge()
            .distinctUntilChanged()
            .debounce { if (it == Connected) 1000L else 0L }
            .flowOn(Dispatchers.Main.immediate)
            .stateIn(scope, SharingStarted.Eagerly, heartRateSensor.connectionState.value)

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
        uiPredictiveConnectionState.value = Connecting
        heartRateSensor.connectIfPossible(true)
    }

    override fun tryDisconnect() {
        uiPredictiveConnectionState.value = Disconnected
        heartRateSensor.connectIfPossible(false)
    }
}
