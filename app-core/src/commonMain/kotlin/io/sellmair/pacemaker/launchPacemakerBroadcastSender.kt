package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
internal fun CoroutineScope.launchPacemakerBroadcastSender(
    userService: UserService, pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
) {
    /* Start broadcasting my own state to other participant  */
    launch {
        MeState.get().debounce(32.milliseconds).collect { meState ->
            if (meState == null) return@collect
            userService.me()
            pacemakerBluetoothService.await().write {
                setUser(userService.me())
                setHeartRate(meState.heartRate ?: return@write)
                setHeartRateLimit(meState.heartRateLimit)
            }
        }
    }
}