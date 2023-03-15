package io.sellmair.broadheart.ui.mainPage

import androidx.compose.runtime.Composable

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.service.GroupState
import io.sellmair.broadheart.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.broadheart.ui.widget.HeartRateScale
import io.sellmair.broadheart.ui.widget.MemberHeartRateIndicator
import io.sellmair.broadheart.ui.widget.MemberHeartRateLimit

@Composable
internal fun GroupHeartRateOverview(
    state: GroupState?,
    range: ClosedRange<HeartRate> = HeartRate(40)..HeartRate(200f),
    onMyHeartRateLimitChanged: (HeartRate) -> Unit = {}
) {
    HeartRateScale(range = range) {
        state?.members.orEmpty().forEach { memberState ->
            MemberHeartRateIndicator(memberState, range)

            if (memberState.user?.isMe == true) {
                ChangeableMemberHeartRateLimit(
                    state = memberState,
                    range = range,
                    onLimitChanged = onMyHeartRateLimitChanged
                )
            } else {
                MemberHeartRateLimit(memberState, range)
            }
        }
    }
}