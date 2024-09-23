package io.sellmair.pacemaker

import io.sellmair.evas.State
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

data class BluetoothState(
    val isBluetoothEnabled: Boolean,
    val isBluetoothPermissionGranted: Boolean
) : State {
    companion object Key : State.Key<BluetoothState?> {
        override val default: BluetoothState? = null
    }
}

internal fun CoroutineScope.launchBluetoothStateActor(): Job = launchState(BluetoothState) {
    val isBluetoothEnabledFlow = flow {
        while (true) {
            val isBluetoothEnabled = isBluetoothEnabled()
            emit(isBluetoothEnabled)
            delay(1.seconds)
        }
    }

    val isBluetoothPermissionGrantedFlow = flow {
        while (true) {
            val isBluetoothPermissionGranted = isBluetoothPermissionGranted()
            emit(isBluetoothPermissionGranted)
            /* Cannot be revoked w/o restarting the process. Once granted we can assume that this is it */
            if (isBluetoothPermissionGranted) break
            delay(0.5.seconds)
        }
    }

    this emitAll combine(isBluetoothEnabledFlow, isBluetoothPermissionGrantedFlow) { isBluetoothEnabled, isBluetoothPermissionGranted ->
        BluetoothState(isBluetoothEnabled, isBluetoothPermissionGranted)
    }
}

internal expect suspend fun isBluetoothEnabled(): Boolean

internal expect suspend fun isBluetoothPermissionGranted(): Boolean