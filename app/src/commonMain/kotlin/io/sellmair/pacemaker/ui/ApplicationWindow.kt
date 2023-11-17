package io.sellmair.pacemaker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePageViewModel


@Composable
internal fun ApplicationWindow() {
    val coroutineScope = rememberCoroutineScope()
    val groupState by GroupState.get().collectAsState()
    val meState by MeState.collectAsState()
    val meColor = meState?.me?.displayColor?.toColor() ?: Color.Gray
    val sessionService = LocalSessionService.current
    val timelinePageViewModel = remember {
        TimelinePageViewModel(
            coroutineScope,
            sessionService ?: return@remember null
        )
    }

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

                Page.TimelinePage -> {
                    timelinePageViewModel?.let { viewModel ->
                        TimelinePage(viewModel)
                    }
                }

                Page.SettingsPage -> meState?.let { me ->
                    SettingsPage(me = me.me)
                }
            }
        }
    }
}
