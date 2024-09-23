package io.sellmair.pacemaker.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.evas.compose.LocalEvents
import io.sellmair.evas.compose.LocalStates
import io.sellmair.pacemaker.LocalSessionService
import io.sellmair.pacemaker.backend


@Suppress("Unused") // Entry point for iOS application!
object IosPacemakerViewController {
    fun create() = ComposeUIViewController {
        CompositionLocalProvider(
            LocalEvents provides backend.events,
            LocalStates provides backend.states,
            LocalSessionService provides backend.sessionService
        ) {
            ApplicationWindow()
        }
    }
}
