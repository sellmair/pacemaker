package io.sellmair.pacemaker

import io.sellmair.pacemaker.ActiveSessionIntent.Start
import io.sellmair.pacemaker.ActiveSessionIntent.Stop
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.utils.Event
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.events
import io.sellmair.pacemaker.utils.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class ActiveSessionState(val session: Session?) : State {
    companion object : State.Key<ActiveSessionState> {
        override val default: ActiveSessionState = ActiveSessionState(null)
    }
}

sealed interface ActiveSessionIntent : Event {
    data object Start : ActiveSessionIntent
    data object Stop : ActiveSessionIntent
}

internal fun CoroutineScope.launchSessionActor(userService: UserService, sessionService: SessionService) = launch {
    var activeSession: ActiveSessionService? = null
    var activeSessionActor: Job? = null

    events<ActiveSessionIntent> { event ->
        when (event) {
            Start -> {
                activeSession?.stop()
                activeSessionActor?.cancel()
                val activeSessionService = sessionService.createSession()
                activeSession = activeSessionService
                ActiveSessionState += ActiveSessionState(activeSession?.session)
                activeSessionActor = launchActiveSessionActor(userService, activeSessionService)
            }

            Stop -> {
                activeSession?.stop()
                activeSessionActor?.cancel()
                ActiveSessionState += ActiveSessionState(null)
            }
        }
    }
}


internal fun CoroutineScope.launchActiveSessionActor(
    userService: UserService,
    activeSessionService: ActiveSessionService
) = launch {
    coroutineScope {
        launch {
            events<PacemakerBroadcastPackageEvent> { event ->
                val user = userService.findUser(event.pkg.userId) ?: return@events
                activeSessionService.save(user, event.pkg.heartRate, event.pkg.heartRateLimit, event.pkg.receivedTime)
            }
        }

        launch {
            events<HeartRateMeasurementEvent> { event ->
                val user = userService.findUser(event.sensorId) ?: return@events
                val heartRateLimit = userService.findHeartRateLimit(user)
                activeSessionService.save(user, event.heartRate, heartRateLimit, event.time)
            }
        }
    }
}
