package io.sellmair.pacemaker.ui

import IosApplicationBackend
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val backend by lazy {
    IosApplicationBackend().also { backend ->
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + backend.eventBus + backend.stateBus)
        scope.launchHeartRateUtteranceActor()
    }
}

@Suppress("Unused") // Entry point for iOS application!
object IosPacemakerViewController {
    fun create() = ComposeUIViewController {
        CompositionLocalProvider(
            LocalEventBus provides backend.eventBus,
            LocalStateBus provides backend.stateBus,
            LocalSessionService provides backend.sessionService
        ) {
            ApplicationWindow()
        }
    }
}
