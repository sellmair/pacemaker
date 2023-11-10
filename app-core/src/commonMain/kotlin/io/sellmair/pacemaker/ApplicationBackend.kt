package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.StateBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface ApplicationBackend {
    val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
    val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
    val sessionService: SessionService
    val userService: UserService
    val stateBus: StateBus
    val eventBus: EventBus
    val settings: Settings
}

fun ApplicationBackend.launchApplicationBackend(scope: CoroutineScope) {
    scope.launchGroupStateActor(userService)
    scope.launchMeStateActor(userService)
    scope.launchPacemakerBroadcastSender(pacemakerBluetoothService)
    scope.launchPacemakerBroadcastReceiver(userService, pacemakerBluetoothService)
    scope.launchHeartRateSensorMeasurement(heartRateSensorBluetoothService)
    scope.launchHeartRateSensorAutoConnector(userService, heartRateSensorBluetoothService)
    scope.launchCriticalGroupStateActor()
    scope.launchSessionActor(userService, sessionService)
    scope.launchHeartRateUtteranceProducer()
    scope.launchUtteranceSettingsActor(settings)
    launchPlatform(scope)
}

expect fun ApplicationBackend.launchPlatform(scope: CoroutineScope)
