package io.sellmair.broadheart.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.broadheart.*

@Composable
fun MyLimit(state: GroupMemberState, range: ClosedRange<HeartRate>) {
    var isDragging: Boolean by remember { mutableStateOf(false) }
    var parentSize: IntSize? = null

    OnHeartRateScalePosition(
        state.upperLimitHeartRate, range,
        side = ScaleSide.Right,
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
                    Me.myLimit = newHeartRate
                    Log.d("Logic", "new HR: $newHeartRate")
                }
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(.3f)
                    .height(if (isDragging) 2.dp else 1.5.dp)
            ) {
                drawRect(SolidColor(state.user.displayColorLight.toColor()))
            }

            Box(modifier = Modifier.size(if (isDragging) 30.dp else 20.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(SolidColor(state.user.displayColorLight.toColor()))
                }
            }
        }

        if (isDragging) {
            Text(
                state.upperLimitHeartRate.toString(), lineHeight = 12.sp,
                modifier = Modifier.padding(start = 45.dp)
            )
        }
    }
}
