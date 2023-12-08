package io.sellmair.pacemaker

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.set
import kotlinx.coroutines.launch


fun AndroidPermissionsState(deniedPermissions: Set<String>): AndroidPermissionsState {
    return if (deniedPermissions.isEmpty()) AndroidPermissionsState.Granted
    else AndroidPermissionsState.Denied(deniedPermissions)
}

sealed class AndroidPermissionsState : State {

    data object Granted : AndroidPermissionsState()

    data class Denied(val deniedPermissions: Set<String>) : AndroidPermissionsState()

    companion object Key : State.Key<AndroidPermissionsState?> {
        override val default: AndroidPermissionsState? = null
    }
}

private val requiredPermissions = setOfNotNull(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_ADVERTISE,
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null,
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.BODY_SENSORS else null
)

fun MainActivity.launchAndroidPermissionStateActor() {
    val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.isEmpty()) return@registerForActivityResult

        launchWithApplicationBackend {
            assert(result.keys.containsAll(requiredPermissions))
            val deniedPermissions = result.filterValues { !it }.keys
            AndroidPermissionsState set AndroidPermissionsState(deniedPermissions)
        }
    }

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            requestPermissions.launch(requiredPermissions.toTypedArray())
        }
    }
}