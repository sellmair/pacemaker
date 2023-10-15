@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.service.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class HeartRateSensorViewModelImpl(
    scope: CoroutineScope,
    heartRateSensor: HeartRateSensor,
    private val userService: UserService,
) : HeartRateSensorViewModel {

    override val name: String? = heartRateSensor.deviceName
    override val id: HeartRateSensorId = heartRateSensor.deviceId.toHeartRateSensorId()

    override val rssi: StateFlow<Int?> = heartRateSensor.rssi

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

    override val connection = HeartRateRateSensorConnectionViewModelImpl(scope, heartRateSensor, userService)
}
