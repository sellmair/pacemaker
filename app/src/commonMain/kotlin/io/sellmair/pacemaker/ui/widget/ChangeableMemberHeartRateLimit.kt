package io.sellmair.pacemaker.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.UserColors
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.displayColorLight
import io.sellmair.pacemaker.toUserLight
import io.sellmair.pacemaker.ui.toColor
import kotlin.math.roundToInt

@Composable
internal fun ChangeableMemberHeartRateLimit(
    state: UserState,
    range: ClosedRange<HeartRate>,
    horizontalCenterBias: Float = .5f,
    side: ScaleSide = if (state.isMe) ScaleSide.Right else ScaleSide.Left,
    onLimitChanged: (HeartRate) -> Unit = {}
) {
    val heartRateLimit = state.heartRateLimit ?:  return
    ChangeableMemberHeartRateLimit(
        user = state,
        heartRateLimit = heartRateLimit,
        range = range,
        horizontalCenterBias = horizontalCenterBias,
        side = side,
        onLimitChanged = onLimitChanged
    )
}

@Composable
internal fun ChangeableMemberHeartRateLimit(
    user: UserState,
    heartRateLimit: HeartRate,
    range: ClosedRange<HeartRate>,
    horizontalCenterBias: Float = .5f,
    side: ScaleSide = if (user.isMe) ScaleSide.Right else ScaleSide.Left,
    onLimitChanged: (HeartRate) -> Unit = {}
) {
    ChangeableMemberHeartRateLimit(
        color = user.color.toUserLight().toColor(),
        heartRateLimit = heartRateLimit,
        range = range,
        horizontalCenterBias = horizontalCenterBias,
        side = side,
        onLimitChanged = onLimitChanged
    )
}

@Composable
internal fun ChangeableMemberHeartRateLimit(
    color: Color,
    heartRateLimit: HeartRate,
    range: ClosedRange<HeartRate>,
    horizontalCenterBias: Float = .5f,
    side: ScaleSide = ScaleSide.Right,
    onLimitChanged: (HeartRate) -> Unit = {}
) {
    var myHeartRateLimit by remember { mutableStateOf(heartRateLimit) }
    var isDragging: Boolean by remember { mutableStateOf(false) }
    var parentSize: IntSize? = null

    OnHeartRateScalePosition(
        heartRate = myHeartRateLimit,
        range = range,
        side = side,
        horizontalCenterBias = horizontalCenterBias,
        modifier = Modifier
            .onPlaced { coordinates ->
                parentSize = coordinates.parentCoordinates?.size
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false }
                ) { change, _ ->
                    val newY = change.position.y
                    val parentHeight = parentSize?.height ?: return@detectVerticalDragGestures
                    val newHeartRate = heartRateOfY(newY, range, parentHeight.toFloat())
                    myHeartRateLimit = newHeartRate
                    onLimitChanged(newHeartRate)
                }
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(.3f)
                    .height(if (isDragging) 2.dp else 1.5.dp)
            ) {
                drawRect(SolidColor(color))
            }

            Box(modifier = Modifier.size(if (isDragging) 30.dp else 20.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(SolidColor(color))
                }
            }
        }

        if (isDragging) {
            Text(
                myHeartRateLimit.value.roundToInt().toString(), lineHeight = 12.sp,
                modifier = Modifier.padding(start = 45.dp)
            )
        }
    }
}
