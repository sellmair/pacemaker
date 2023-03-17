package io.sellmair.broadheart.ui.mainPage

import androidx.compose.runtime.Composable
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.Group

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