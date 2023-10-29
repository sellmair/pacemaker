@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.bluetooth.toEvent
import io.sellmair.pacemaker.bluetooth.toHeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.StateBus
import io.sellmair.pacemaker.utils.emit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface ApplicationBackend {
    val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
    val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
    val userService: UserService
    val stateBus: StateBus
    val eventBus: EventBus
}

fun ApplicationBackend.launchApplicationBackend(scope: CoroutineScope) {

    val meUserId = scope.async { userService.me().id }
    suspend fun User.isMe(): Boolean = this.id == meUserId.await()

    scope.launchGroupStateActor(userService)
    scope.launchMeStateActor(userService)
    scope.launchPacemakerBroadcastSender(userService, pacemakerBluetoothService)
    scope.launchPacemakerBroadcastReceiver(userService, pacemakerBluetoothService)


    /* Connecting our hr receiver and emit hr measurement events  */
    flow { emitAll(heartRateSensorBluetoothService.await().newSensorsNearby) }
        .flatMapMerge { sensor -> sensor.heartRate }
        .onEach { measurement -> measurement.toEvent().emit() }
        .launchIn(scope)


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