package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.runtime.Composable

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.pacemaker.ui.widget.HeartRateScale
import io.sellmair.pacemaker.ui.widget.MemberHeartRateIndicator
import io.sellmair.pacemaker.ui.widget.MemberHeartRateLimit

@Composable
internal fun GroupHeartRateOverview(
    state: Group?,
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