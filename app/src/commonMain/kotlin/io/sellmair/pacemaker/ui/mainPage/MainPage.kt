package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.Group

@Composable
internal fun MainPage(
    groupState: Group?,
    onSettingsClicked: () -> Unit,
    onMyHeartRateLimitChanged: (HeartRate) -> Unit = {}
) {
    GroupHeartRateOverview(
        state = groupState,
        onMyHeartRateLimitChanged = onMyHeartRateLimitChanged
    )
    MyStatusHeader(
        state = groupState,
        onSettingsClicked = onSettingsClicked
    )
}