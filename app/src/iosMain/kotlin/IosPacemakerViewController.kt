package io.sellmair.pacemaker.ui

import IosApplicationBackend
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController

private val backend by lazy { IosApplicationBackend() }

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
