package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePage


@Composable
internal fun ApplicationWindow() {
    PacemakerTheme {
        PageRouter { page ->
            when (page) {
                Page.MainPage -> MainPage()
                Page.TimelinePage -> TimelinePage()
                Page.SettingsPage -> SettingsPage()
            }
        }
    }
}
