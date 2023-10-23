@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.bluetooth.broadcastPackages
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

interface ApplicationBackend {
    val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
    val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
    val userService: UserService
    val groupService: GroupService
}

fun ApplicationBackend.launchApplicationBackend(scope: CoroutineScope) {

    val meUserId = scope.async { userService.me().id }
    suspend fun User.isMe(): Boolean = this.id == meUserId.await()

    /* Connecting our hr receiver with the group service */
    val hrMeasurements = flow { emitAll(heartRateSensorBluetoothService.await().newSensorsNearby) }
        .flatMapMerge { sensor -> sensor.heartRate }
        .onEach { hrMeasurement -> groupService.add(hrMeasurement) }
        .shareIn(scope, SharingStarted.Eagerly)


    /* Start broadcasting my own state to other participant  */
    scope.launch {
        hrMeasurements.collect { hrMeasurement ->
            val user = userService.findUser(hrMeasurement.sensorInfo.id) ?: return@collect
            if (!user.isMe()) return@collect
            pacemakerBluetoothService.await().write {
                setUser(user)
                setHeartRate(hrMeasurement.sensorInfo.id, hrMeasurement.heartRate)
                userService.findHeartRateLimit(user)?.let { heartRateLimit ->
                    setHeartRateLimit(heartRateLimit)
                }
            }
        }
    }

    /* Receive broadcasts */
    scope.launch {
        pacemakerBluetoothService.await().broadcastPackages().collect { received ->
            val user = User(id = received.userId, name = received.userName)

            userService.saveUser(user)
            userService.saveHeartRateLimit(user, received.heartRateLimit)
            userService.linkSensor(user, received.sensorId)

            groupService.add(
                HeartRateMeasurement(
                    heartRate = received.heartRate,
                    sensorInfo = HeartRateSensorInfo(id = received.sensorId),
                    receivedTime = received.receivedTime
                )
            )
        }
    }

    /* Auto connect to hr sensors */
    scope.launch {
        heartRateSensorBluetoothService.await().newSensorsNearby
            .collect { sensor ->
                if (userService.findUser(sensor.deviceId.toHeartRateSensorId())?.isMe() == true) {
                    sensor.connectIfPossible(true)
                }
            }
    }
}