@file:Suppress("FunctionName")

package io.sellmair.broadheart.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.broadheart.ui.mainPage.MainPage

@Suppress("Unused") // Entry point for iOS application!
fun HeartcastViewController() = ComposeUIViewController {
    MainPage(
        groupState = null,
        onRoute = {},
        onMyHeartRateLimitChanged = {}
    )
}