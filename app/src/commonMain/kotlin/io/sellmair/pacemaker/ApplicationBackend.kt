@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.bluetooth.broadcastPackages
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.service.GroupService
import io.sellmair.pacemaker.service.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

interface ApplicationBackend {
    val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
    val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
    val userService: UserService
    val groupService: GroupService
}

fun ApplicationBackend.launchApplicationBackend(scope: CoroutineScope) {

    /* Connecting our hr receiver with the group service */
    val hrMeasurements = flow { emitAll(heartRateSensorBluetoothService.await().newSensorsNearby) }
        .flatMapMerge { sensor -> sensor.heartRate }
        .onEach { hrMeasurement -> groupService.add(hrMeasurement) }
        .onEach { groupService.invalidate() }
        .shareIn(scope, SharingStarted.WhileSubscribed())

    /*
     Regularly call the updateState w/o measurements, to invalidate old ones, in case
     no measurements arrive
     */
    scope.launch {
        while (true) {
            delay(30.seconds)
            groupService.invalidate()
        }
    }


    /* Start broadcasting my own state to other participant  */
    scope.launch {
        hrMeasurements.collect { hrMeasurement ->
            val user = userService.currentUser()
            pacemakerBluetoothService.await().write {
                setUser(user)
                setHeartRate(hrMeasurement.sensorInfo.id, hrMeasurement.heartRate)
                userService.findUpperHeartRateLimit(user)?.let { heartRateLimit ->
                    setHeartRateLimit(heartRateLimit)
                }
            }
        }
    }


    /* Receive broadcasts */
    scope.launch {
        pacemakerBluetoothService.await().broadcastPackages().collect { received ->
            val user = User(isMe = false, id = received.userId, name = received.userName)

            userService.save(user)
            userService.saveUpperHeartRateLimit(user, received.heartRateLimit)
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
                if (userService.findUser(sensor.deviceId.toHeartRateSensorId())?.isMe == true) {
                    sensor.connectIfPossible(true)
                }
            }
    }
}