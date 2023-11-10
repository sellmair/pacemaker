package io.sellmair.pacemaker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.ApplicationViewModel
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePageViewModel


@Composable
internal fun ApplicationWindow(
    viewModel: ApplicationViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val groupState by viewModel.group.collectAsState(null)
    val nearbyDevices by viewModel.heartRateSensorViewModels.collectAsState()
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
                    onMyHeartRateLimitChanged = { newHeartRateLimit ->
                        viewModel.send(ApplicationIntent.MainPageIntent.UpdateHeartRateLimit(newHeartRateLimit))
                    }
                )

                Page.TimelinePage -> {
                    timelinePageViewModel?.let { viewModel ->
                        TimelinePage(viewModel)
                    }
                }

                Page.SettingsPage -> meState?.let { me ->
                    SettingsPage(
                        me = me.me,
                        heartRateSensors = nearbyDevices,
                        onIntent = viewModel::send
                    )
                }
            }
        }
    }
}
