package io.sellmair.pacemaker

import io.sellmair.evas.Event
import io.sellmair.evas.State
import io.sellmair.evas.collectEvents
import io.sellmair.evas.launchState
import io.sellmair.evas.value
import io.sellmair.pacemaker.ActiveSessionIntent.Start
import io.sellmair.pacemaker.ActiveSessionIntent.Stop
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.model.Session
import kotlin.time.Clock
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

internal fun CoroutineScope.launchSessionActor(
    userService: UserService, sessionService: SessionService, clock: Clock = Clock.System
) = launchState(ActiveSessionState) {
    var activeSession: ActiveSessionService? = null
    var activeSessionActor: Job? = null

    collectEvents<ActiveSessionIntent> { event ->
        when (event) {
            Start -> {
                if (activeSession != null) return@collectEvents
                activeSession?.stop()
                activeSessionActor?.cancel()
                val activeSessionService = sessionService.createSession()
                activeSession = activeSessionService
                ActiveSessionState(activeSessionService.session).emit()
                activeSessionActor = launchActiveSessionActor(userService, activeSessionService, clock)
            }

            Stop -> {
                activeSession?.stop()
                activeSessionActor?.cancel()
                activeSession = null
                activeSessionActor = null
                ActiveSessionState(null).emit()
            }
        }
    }
}


internal fun CoroutineScope.launchActiveSessionActor(
    userService: UserService,
    activeSessionService: ActiveSessionService,
    clock: Clock = Clock.System
) = launch {
    coroutineScope {
        launch {
            collectEvents<PacemakerBroadcastPackageEvent> { event ->
                val user = userService.findUser(event.pkg.userId) ?: return@collectEvents
                activeSessionService.save(user, event.pkg.heartRate, event.pkg.heartRateLimit, event.pkg.receivedTime)
            }
        }

        launch {
            collectEvents<HeartRateMeasurementEvent> { event ->
                val age = clock.now() - event.time
                if (age < kotlin.time.Duration.ZERO || age > HeartRateSource.MAX_AGE) return@collectEvents
                val user = userService.findUser(event.sensorId) ?: return@collectEvents
                val selected = SelectedHeartRateSourceState.value()?.source
                val ownId = MeState.value()?.me?.id?.value?.toString().orEmpty()
                if (!acceptsHeartRateMeasurement(selected, ownId, user.id.value.toString(), event.sensorId.value)) return@collectEvents
                val heartRateLimit = userService.findHeartRateLimit(user)
                activeSessionService.save(user, event.heartRate, heartRateLimit, event.time)
            }
        }
    }
}
