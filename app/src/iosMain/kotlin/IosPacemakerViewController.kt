package io.sellmair.pacemaker.ui

import IosApplicationBackend
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.pacemaker.ApplicationViewModel
import kotlinx.coroutines.MainScope

private val backend by lazy { IosApplicationBackend() }

private val viewModel by lazy { ApplicationViewModel(MainScope(), backend) }

@Suppress("Unused") // Entry point for iOS application!
object IosPacemakerViewController {
    fun create() = ComposeUIViewController {
        CompositionLocalProvider(
            LocalEventBus provides backend.eventBus,
            LocalStateBus provides backend.stateBus,
            LocalSessionService provides backend.sessionService
        ) {
            ApplicationWindow(viewModel)
        }
    }
}
