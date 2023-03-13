package io.sellmair.broadheart.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.sellmair.broadheart.ui.mainPage.MainPage

fun HeartcastViewController() = ComposeUIViewController {
    MainPage(
        groupState = null,
        onRoute = {},
        onMyHeartRateLimitChanged = {}
    )
}