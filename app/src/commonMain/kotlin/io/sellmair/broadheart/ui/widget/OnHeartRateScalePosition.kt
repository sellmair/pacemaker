package io.sellmair.broadheart.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import io.sellmair.broadheart.model.HeartRate
import kotlin.math.roundToInt

enum class ScaleSide {
    Left, Right
}

@Composable
fun OnHeartRateScalePosition(
    heartRate: HeartRate,
    range: ClosedRange<HeartRate>,
    modifier: Modifier = Modifier,
    horizontalCenterBias: Float = .5f,
    side: ScaleSide = ScaleSide.Right,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                placeable.placeRelative(
                    x = when (side) {
                        ScaleSide.Right -> constraints.maxWidth.times(horizontalCenterBias).roundToInt()
                        ScaleSide.Left -> (constraints.maxWidth.times(horizontalCenterBias) - placeable.width).roundToInt()
                    },
                    y = yOfHeartRate(
                        HeartRate(heartRate.value), range, constraints.maxHeight.toFloat()
                    ).roundToInt() - placeable.height / 2
                )
            }
        }
    }
}

