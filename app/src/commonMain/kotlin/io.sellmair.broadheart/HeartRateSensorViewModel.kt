package io.sellmair.broadheart

import io.sellmair.broadheart.bluetooth.BlePeripheral
import io.sellmair.broadheart.bluetooth.toHeartRateSensorId
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class HeartRateSensorViewModelImpl(
    private val scope: CoroutineScope,
    private val userService: UserService,
    private val heartRateSensor: BluetoothService.Peripheral.HeartRateSensor,
) : HeartRateSensorViewModel {
    override val id: HeartRateSensorId = heartRateSensor.id.toHeartRateSensorId()

    override val rssi: StateFlow<BlePeripheral.Rssi> = heartRateSensor.rssi

    override val state: StateFlow<BlePeripheral.State> = heartRateSensor.state

    override val heartRate: StateFlow<HeartRate?> =
        heartRateSensor.measurements.map { it.heartRate }.stateIn(scope, WhileSubscribed(), null)

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
        heartRateSensor.tryConnect()
    }

    override fun tryDisconnect() {
        heartRateSensor.tryDisconnect()
    }
}
