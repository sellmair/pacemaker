package io.sellmair.broadheart.ui.mainPage

import androidx.compose.runtime.Composable
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.service.GroupState
import io.sellmair.broadheart.ui.Route

@Composable
fun MainPage(
    groupState: GroupState?,
    onRoute: (Route) -> Unit,
    onMyHeartRateLimitChanged: (HeartRate) -> Unit = {}
) {
    GroupHeartRateOverview(
        state = groupState,
        onMyHeartRateLimitChanged = onMyHeartRateLimitChanged
    )
    MyStatusHeader(
        state = groupState,
        onSettingsClicked = { onRoute(Route.SettingsPage) })
}