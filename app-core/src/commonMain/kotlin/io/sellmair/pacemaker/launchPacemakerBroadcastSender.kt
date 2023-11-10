package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
internal fun CoroutineScope.launchPacemakerBroadcastSender(
    pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
) {
    /* Start broadcasting my own state to other participant  */

    /* Broadcast changes to 'me' */
    launch {
        MeState.get().mapNotNull { state -> state?.me }
            .distinctUntilChanged()
            .conflate()
            .collect { me ->
                pacemakerBluetoothService.await().write {
                    setUser(me)
                }
            }
    }

    /* Broadcast changes to 'heartRateLimit' */
    launch {
        MeState.get().mapNotNull { state -> state?.heartRateLimit }
            .sample(32.milliseconds)
            .distinctUntilChanged()
            .conflate()
            .collect { heartRateLimit ->
                pacemakerBluetoothService.await().write {
                    setHeartRateLimit(heartRateLimit)
                }
            }
    }

    /* Broadcast changes to 'heartRate' */
    launch {
        MeState.get().mapNotNull { state -> state?.heartRate }
            .sample(32.milliseconds)
            .distinctUntilChanged()
            .conflate()
            .collect { heartRate ->
                pacemakerBluetoothService.await().write {
                    setHeartRate(heartRate)
                }
            }
    }

    /* Fallback: Broadcast regardless of changes (in case a previous broadcast was lost) */
    launch {
        MeState.get().filterNotNull()
            .sample(15.seconds)
            .collect { state ->
                pacemakerBluetoothService.await().write {
                    setUser(state.me)
                    setHeartRateLimit(state.heartRateLimit)
                    state.heartRate?.let { setHeartRate(it) }
                }
            }
    }
}

