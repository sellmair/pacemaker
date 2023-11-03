package io.sellmair.pacemaker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.ApplicationViewModel
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage
import io.sellmair.pacemaker.ui.timelinePage.TimelinePage


@Composable
internal fun ApplicationWindow(
    viewModel: ApplicationViewModel
) {
    val groupState by viewModel.group.collectAsState(null)
    val nearbyDevices by viewModel.heartRateSensorViewModels.collectAsState()
    val meState by viewModel.me.collectAsState()
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
            onSurfaceVariant = meColor
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

                Page.TimelinePage -> TimelinePage()

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
