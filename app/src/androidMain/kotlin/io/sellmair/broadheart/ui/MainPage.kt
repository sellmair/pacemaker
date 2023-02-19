package io.sellmair.broadheart.ui

import androidx.compose.runtime.Composable
import io.sellmair.broadheart.service.GroupState

@Composable
fun MainPage(groupState: GroupState?, onRoute: (Route) -> Unit) {
    HeartRateScale(groupState)
    MyStatusHeader(groupState, onSettingsClicked = { onRoute(Route.SettingsPage) })
}