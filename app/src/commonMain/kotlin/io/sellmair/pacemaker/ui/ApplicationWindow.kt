package io.sellmair.pacemaker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePage


@Composable
internal fun ApplicationWindow() {
    val groupState by GroupState.get().collectAsState()
    val meState by MeState.collectAsState()
    val meColor = meState?.me?.displayColor?.toColor() ?: Color.Gray

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = meColor,
            primaryContainer = meColor,
            secondaryContainer = meColor,
            onSecondaryContainer = Color.White,
            onPrimaryContainer = meColor,
            onTertiaryContainer = meColor,
            onSurface = meColor,
            onSurfaceVariant = meColor,
        )
    ) {
        PageRouter { page ->
            when (page) {
                Page.MainPage -> MainPage(
                    meState = meState,
                    groupState = groupState,
                )

                Page.TimelinePage -> TimelinePage()

                Page.SettingsPage -> meState?.let { me ->
                    SettingsPage(me = me.me)
                }
            }
        }
    }
}
