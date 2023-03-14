package io.sellmair.broadheart.ui

import androidx.compose.ui.window.Application
import io.sellmair.broadheart.ui.mainPage.MainPage

fun HeartcastViewController() = Application {
    MainPage(
        groupState = null,
        onRoute = {},
        onMyHeartRateLimitChanged = {}
    )
}