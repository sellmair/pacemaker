package io.sellmair.broadheart.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.GroupMember
import io.sellmair.broadheart.ui.displayColorLight
import io.sellmair.broadheart.ui.toColor

@Composable
internal fun MemberHeartRateLimit(
    memberState: GroupMember,
    range: ClosedRange<HeartRate>
) {
    if (memberState.user == null) return
    if (memberState.heartRateLimit == null) return
    OnHeartRateScalePosition(memberState.heartRateLimit, range, side = ScaleSide.Left) {
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