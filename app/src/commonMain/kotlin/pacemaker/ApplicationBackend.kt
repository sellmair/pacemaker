@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import io.sellmair.pacemaker.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

interface ApplicationBackend {
    val bluetoothService: BluetoothService
    val userService: UserService
    val groupService: GroupService
}

fun ApplicationBackend.launchApplicationBackend(scope: CoroutineScope) {

    /* Connecting our hr receiver with the group service */
    val hrMeasurements = bluetoothService.devices
        .filterIsInstance<BluetoothService.Device.HeartRateSensor>()
        .flatMapMerge { it.measurements }
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
            bluetoothService.pacemakerBle().updateUser(user)
            bluetoothService.pacemakerBle().updateHeartHeart(hrMeasurement.sensorInfo.id, hrMeasurement.heartRate)
            userService.findUpperHeartRateLimit(user)?.let { heartRateLimit ->
                bluetoothService.pacemakerBle().updateHeartRateLimit(heartRateLimit)
            }
        }
    }


    /* Receive broadcasts */
    scope.launch {
        bluetoothService
            .devices
            .filterIsInstance<BluetoothService.Device.PacemakerAppDevice>()
            .flatMapMerge { it.broadcasts }
            .collect { received ->
                val user = User(
                    isMe = false, id = received.userId, name = received.userName
                )

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
}