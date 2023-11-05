package io.sellmair.pacemaker.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor
import kotlinx.coroutines.launch

/* How can this be done using remember? 🤷 */
private val animationByUser = mutableMapOf<UserId?, Animatable<Float, AnimationVector1D>>()

@Composable
internal fun MemberHeartRateIndicator(member: UserState, range: ClosedRange<HeartRate>) {
    val side = if (member.isMe) ScaleSide.Right else ScaleSide.Left
    val memberCurrentHeartRate = member.heartRate

    val animatedHeartRate = animationByUser.getOrPut(member.user.id) { Animatable(memberCurrentHeartRate.value) }
    rememberCoroutineScope().launch {
        animatedHeartRate.animateTo(memberCurrentHeartRate.value)
    }

    /* Can be null for myself */
    OnHeartRateScalePosition(
        HeartRate(animatedHeartRate.value), range, side = side, modifier = Modifier.padding(
            start = if (side == ScaleSide.Right) 96.dp else 0.dp,
            end = if (side == ScaleSide.Left) 48.dp else 0.dp
        )
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            UserHead(
                userState = member,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .alpha(0.75f)
            )

            member.heartRateLimit?.let { memberHeartRateLimit ->
                if (memberCurrentHeartRate > memberHeartRateLimit) {
                    Icon(
                        Icons.Default.Warning, "Too high",
                        modifier = Modifier.size(12.dp),
                        tint = Color.Red
                    )
                } else {
                    Icon(
                        Icons.Default.ThumbUp, "OK",
                        modifier = Modifier.size(12.dp),
                        tint = member.displayColorLight.toColor()
                    )
                }
            }
        }
    }
}
