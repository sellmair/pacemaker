package io.sellmair.pacemaker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
    var route by remember { mutableStateOf(Route.MainPage) }
    val groupState by viewModel.group.collectAsState(null)
    val nearbyDevices by viewModel.nearbyDevices.collectAsState()
    val me by viewModel.me.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = route == Route.MainPage,
            enter = slideInHorizontally(tween(250, 250)) + fadeIn(tween(500, 250)),
            exit = slideOutHorizontally(tween(500)) + fadeOut(tween(250))
        ) {
            MainPage(
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

            if (me == null) {
                println("Missing: me")
            }
            me?.let { me ->
                SettingsPage(
                    me = me,
                    nearbyDevices = nearbyDevices,
                    onCloseSettingsPage = { route = Route.MainPage },
                    onIntent = viewModel::send
                )
            }
        }
    }
}
