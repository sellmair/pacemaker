package io.sellmair.broadheart.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.sellmair.broadheart.*
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.service.GroupMemberState
import io.sellmair.broadheart.ui.toColor

@Composable
fun MemberHeartRateLimit(
    memberState: GroupMemberState,
    range: ClosedRange<HeartRate>
) {
    if (memberState.user == null) return
    if (memberState.upperHeartRateLimit == null) return
    OnHeartRateScalePosition(memberState.upperHeartRateLimit, range, side = ScaleSide.Left) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(.4f)
                .height(1.dp)
        ) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, memberState.user.displayColorLight.toColor()),
                )
            )
        }
    }
}