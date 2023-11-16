package io.sellmair.pacemaker.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor
import kotlinx.coroutines.launch

@Composable
internal fun MemberHeartRateLimit(
    userState: UserState,
    range: ClosedRange<HeartRate>
) {
    val user = userState.user
    val heartRateLimit = userState.heartRateLimit ?: return

    val animatableHeartRateLimit = remember(user.id.value) { Animatable(heartRateLimit.value) }
    rememberCoroutineScope().launch {
        animatableHeartRateLimit.animateTo(
            heartRateLimit.value,
            animationSpec = onHeartRateScaleSpring()
        )
    }

    OnHeartRateScalePosition(HeartRate(animatableHeartRateLimit.value), range, side = ScaleSide.Left) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(.4f)
                .height(1.dp)
        ) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, user.displayColorLight.toColor()),
                )
            )
        }
    }
}