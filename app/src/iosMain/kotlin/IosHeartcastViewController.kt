package io.sellmair.broadheart.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.broadheart.ApplicationViewModel
import kotlinx.coroutines.MainScope

private val viewModel by lazy { ApplicationViewModel(MainScope(), IosApplicationBackend()) }


@Suppress("Unused") // Entry point for iOS application!
object IosHeartcastViewController {
    fun create() = ComposeUIViewController {
        ApplicationWindow(viewModel)
    }
}
