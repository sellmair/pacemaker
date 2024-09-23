package io.sellmair.pacemaker

import io.sellmair.evas.flow
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.model.Hue
import io.sellmair.evas.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
internal fun CoroutineScope.launchPacemakerBroadcastSender(
    pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
) {
    /* Start broadcasting my own state to other participant  */

    /* Broadcast changes to 'me' */
    launch {
        MeState.flow().mapNotNull { state -> state?.me }
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
        MeState.flow().mapNotNull { state -> state?.heartRateLimit }
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
        MeState.flow().mapNotNull { state -> state?.heartRate }
            .sample(32.milliseconds)
            .distinctUntilChanged()
            .conflate()
            .collect { heartRate ->
                pacemakerBluetoothService.await().write {
                    setHeartRate(heartRate)
                }
            }
    }

    /* Broadcast my current color */
    launch {
        MeColorState.flow().mapNotNull { state -> state?.color }
            .sample(32.milliseconds)
            .distinctUntilChanged()
            .conflate()
            .collect { meColor ->
                pacemakerBluetoothService.await().write {
                    setColorHue(Hue.safe(meColor.hue))
                }
            }
    }

    /* Fallback: Broadcast regardless of changes (in case a previous broadcast was lost) */
    launch {
        while (isActive) {
            delay(15.seconds)

            MeState.value()?.let { state ->
                pacemakerBluetoothService.await().write {
                    setUser(state.me)
                    setHeartRateLimit(state.heartRateLimit)
                    state.heartRate?.let { setHeartRate(it) }
                }
            }

            MeColorState.value()?.let { state ->
                pacemakerBluetoothService.await().write {
                    setColorHue(Hue.safe(state.color.hue))
                }
            }
        }
    }
}
