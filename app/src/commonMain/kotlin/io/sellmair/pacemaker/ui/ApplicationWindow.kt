package io.sellmair.pacemaker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.ApplicationViewModel
import io.sellmair.pacemaker.ui.mainPage.MainPage
import io.sellmair.pacemaker.ui.settingsPage.SettingsPage


@Composable
internal fun ApplicationWindow(
    viewModel: ApplicationViewModel
) {


    MaterialTheme {
        var route by remember { mutableStateOf(Route.MainPage) }
        val groupState by viewModel.group.collectAsState(null)
        val nearbyDevices by viewModel.heartRateSensorViewModels.collectAsState()
        val meState by viewModel.me.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = route == Route.MainPage,
                enter = slideInHorizontally(tween(250, 250)) + fadeIn(tween(500, 250)),
                exit = slideOutHorizontally(tween(500)) + fadeOut(tween(250))
            ) {
                MainPage(
                    meState = meState,
                    groupState = groupState,
                    onSettingsClicked = { route = Route.SettingsPage },
                    onMyHeartRateLimitChanged = { newHeartRateLimit ->
                        viewModel.send(ApplicationIntent.MainPageIntent.UpdateHeartRateLimit(newHeartRateLimit))
                    }
                )
            }

            AnimatedVisibility(
                visible = route == Route.SettingsPage,
                enter = slideInHorizontally(tween(250, 250), initialOffsetX = { it }) + fadeIn(tween(500, 250)),
                exit = slideOutHorizontally(tween(500), targetOffsetX = { it }) + fadeOut(tween(250))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )

                if (meState == null) {
                    println("Missing: me")
                }
                meState?.let { me ->
                    SettingsPage(
                        me = me.me,
                        heartRateSensors = nearbyDevices,
                        onCloseSettingsPage = { route = Route.MainPage },
                        onIntent = viewModel::send
                    )
                }
            }
        }
    }
}
