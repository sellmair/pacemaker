package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.MeState

@Composable
internal fun MainPage(
    meState: MeState?,
    groupState: GroupState?,
) {
    GroupHeartRateOverview(
        state = groupState,
    )
    MyStatusHeader(
        state = meState,
    )
}