package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage


@Composable
fun ApplicationWindow() {
    PacemakerTheme {
        PageRouter { page ->
            when (page) {
                Page.MainPage -> MainPage()
                Page.SettingsPage -> SettingsPage()
            }
        }
    }
}
