package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.UpdateMeIntent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.ui.collectAsState
import io.sellmair.pacemaker.ui.widget.*
import io.sellmair.pacemaker.ui.widget.ChangeableMemberHeartRateLimit
import io.sellmair.pacemaker.ui.widget.HeartRateScale
import io.sellmair.pacemaker.ui.widget.MemberHeartRateIndicator
import io.sellmair.pacemaker.ui.widget.MemberHeartRateLimit
import io.sellmair.pacemaker.utils.emit

@Composable
fun GroupHeartRateOverview(
    range: ClosedRange<HeartRate> = HeartRate(40)..HeartRate(200f),
) {
    GroupHeartRateOverview(
        state = GroupState.collectAsState().value,
        range = range
    )
}

@Composable
internal fun GroupHeartRateOverview(
    state: GroupState?,
    range: ClosedRange<HeartRate> = HeartRate(40)..HeartRate(200f),
) {
    HeartRateScale(range = range) {
        state?.members.orEmpty().forEach { memberState ->
            key(memberState.user.id.value) {
                MemberHeartRateIndicator(memberState, range)

                if (memberState.isMe) {
                    ChangeableMemberHeartRateLimit(
                        state = memberState,
                        range = range,
                        onLimitChanged = Launching { heartRate ->
                            UpdateMeIntent.UpdateHeartRateLimit(heartRate).emit()
                        }
                    )
                } else {
                    MemberHeartRateLimit(memberState, range)
                }
            }
        }
    }
}