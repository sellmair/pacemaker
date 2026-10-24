package io.sellmair.pacemaker

import io.sellmair.evas.flow
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.model.Hue
import io.sellmair.evas.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock

@OptIn(FlowPreview::class)
internal fun CoroutineScope.launchPacemakerBroadcastSender(
    pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
) {
    fun MeState.hasFreshOwnHeartRate() = hasFreshHeartRate(Clock.System.now().toEpochMilliseconds())
    launch {
        val ownState = MeState.flow().map { state -> state?.takeIf { it.hasFreshOwnHeartRate() } }
    /* Start broadcasting my own state to other participant  */

    /* Broadcast changes to 'me' */
    launch {
        ownState.mapNotNull { state -> state?.me }
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
        ownState.mapNotNull { state -> state?.heartRateLimit }
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
        ownState.map { state -> state?.heartRate }
            .sample(32.milliseconds)
            .distinctUntilChanged()
            .filterNotNull()
            .conflate()
            .collect { heartRate ->
                pacemakerBluetoothService.await().write {
                    setHeartRate(heartRate)
                }
            }
    }

    /* Empty HR characteristic tells new readers there is no current measurement. */
    launch {
        MeState.flow().map { state -> state?.takeIf { !it.hasFreshOwnHeartRate() }?.me?.id }
            .distinctUntilChanged()
            .filterNotNull()
            .collect {
                pacemakerBluetoothService.await().write { clearHeartRate() }
            }
    }

    /* Broadcast my current color */
    launch {
        combine(MeColorState.flow(), MeState.flow()) { colorState, me ->
            colorState?.color?.takeIf { me?.hasFreshOwnHeartRate() == true }
        }
            .sample(32.milliseconds)
            .distinctUntilChanged()
            .filterNotNull()
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

            MeState.value()?.takeIf { it.hasFreshOwnHeartRate() }?.let { state ->
                pacemakerBluetoothService.await().write {
                    setUser(state.me)
                    setHeartRateLimit(state.heartRateLimit)
                    state.heartRate?.let { setHeartRate(it) }
                }
            }

            MeColorState.value()?.takeIf { MeState.value()?.hasFreshOwnHeartRate() == true }?.let { state ->
                pacemakerBluetoothService.await().write {
                    setColorHue(Hue.safe(state.color.hue))
                }
            }
        }
    }
    }
}
