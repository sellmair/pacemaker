package io.sellmair.broadheart.ui

import androidx.compose.runtime.Composable
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.service.GroupState

@Composable
fun MainPage(
    groupState: GroupState?,
    onRoute: (Route) -> Unit,
    onMyHeartRateLimitChanged: (HeartRate) -> Unit = {}
) {
    GroupHeartRateScale(
        state = groupState,
        onMyHeartRateLimitChanged = onMyHeartRateLimitChanged
    )
    MyStatusHeader(
        state = groupState,
        onSettingsClicked = { onRoute(Route.SettingsPage) })
}