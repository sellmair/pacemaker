package io.sellmair.pacemaker.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.pacemaker.ApplicationViewModel
import kotlinx.coroutines.MainScope

private val viewModel by lazy { ApplicationViewModel(MainScope(), IosApplicationBackend()) }


@Suppress("Unused") // Entry point for iOS application!
object IosPacemakerViewController {
    fun create() = ComposeUIViewController {
        ApplicationWindow(viewModel)
    }
}
