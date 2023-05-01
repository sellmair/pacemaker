package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.bluetooth.Rssi
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed

internal class HeartRateSensorViewModelImpl(
    private val scope: CoroutineScope,
    private val userService: UserService,
    private val heartRateSensor: BluetoothService.Device.HeartRateSensor,
) : HeartRateSensorViewModel {
    override val id: HeartRateSensorId = heartRateSensor.deviceId.toHeartRateSensorId()

    // TODO
    override val rssi: StateFlow<Rssi?> = MutableStateFlow(null)

    override val state: StateFlow<BleConnectable.ConnectionState> = heartRateSensor.connectionState

    override val heartRate: StateFlow<HeartRate?> =
        heartRateSensor.heartRate.map { it.heartRate }.stateIn(scope, WhileSubscribed(), null)

    override val associatedUser: StateFlow<User?> = flow {
        emit(userService.findUser(id))
        userService.onChange.collect {
            emit(userService.findUser(id))
        }
    }.stateIn(scope, WhileSubscribed(), null)

    override val associatedHeartRateLimit: StateFlow<HeartRate?> = associatedUser
        .map { user -> if (user == null) return@map null else userService.findUpperHeartRateLimit(user) }
        .stateIn(scope, WhileSubscribed(), null)

    override fun tryConnect() {
        heartRateSensor.connectIfPossible(true)
    }

    override fun tryDisconnect() {
        heartRateSensor.connectIfPossible(false)
    }
}
