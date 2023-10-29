package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.model.HeartRate

@Composable
internal fun MainPage(
    meState: MeState?,
    groupState: GroupState?,
    onSettingsClicked: () -> Unit,
    onMyHeartRateLimitChanged: (HeartRate) -> Unit = {}
) {
    GroupHeartRateOverview(
        state = groupState,
        onMyHeartRateLimitChanged = onMyHeartRateLimitChanged
    )
    MyStatusHeader(
        state = meState,
        onSettingsClicked = onSettingsClicked
    )
}