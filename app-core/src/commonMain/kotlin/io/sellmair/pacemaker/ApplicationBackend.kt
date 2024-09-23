package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface ApplicationBackend {
    val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
    val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
    val sessionService: SessionService
    val userService: UserService
    val states: States
    val events: Events
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
    scope.launchApplicationFeatureActor(settings)
    scope.launchAdhocUserActor(userService)
    scope.launchHeartRateSensorLinkingActor(userService)
    scope.launchUpdateMeActor(userService)
    scope.launchHeartRateSensorsStateActor(heartRateSensorBluetoothService)
    scope.launchHeartRateSensorStateActor(userService, heartRateSensorBluetoothService)
    scope.launchHeartRateSensorConnectionStateActor(userService, heartRateSensorBluetoothService)
    scope.launchSessionStateActor(sessionService)
    scope.launchMeColorStateActor(settings)
    scope.launchBluetoothStateActor()
    launchPlatform(scope)
}

expect fun ApplicationBackend.launchPlatform(scope: CoroutineScope)
