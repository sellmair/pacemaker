package io.sellmair.broadheart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.sellmair.broadheart.*

@Composable
fun MemberHeartRateLimit(memberState: GroupMemberState, range: ClosedRange<HeartRate>) {
    /* Render the respective limits */
    if (memberState.user == Me.user) {
        MyLimit(memberState, range)
        MyStatusHeader(memberState)
    } else {
        OnHeartRateScalePosition(memberState.upperLimitHeartRate, range, side = ScaleSide.Left) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(.4f)
                    .height(1.dp)
            ) {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, memberState.user.displayColorLight.toColor()),
                    ),
                )
            }
        }
    }
}