package io.sellmair.broadheart.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.service.GroupState

@Preview(heightDp = 400, widthDp = 200)
@Composable
fun GroupHeartRateScale(
    @PreviewParameter(GroupStatePreviewParameterProvider::class) state: GroupState?,
    range: ClosedRange<HeartRate> = HeartRate(40)..HeartRate(200f),
    onMyHeartRateLimitChanged: (HeartRate) -> Unit = {}
) {
    HeartRateScale(range) {
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