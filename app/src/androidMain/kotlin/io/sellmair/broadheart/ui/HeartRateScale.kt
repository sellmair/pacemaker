package io.sellmair.broadheart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.broadheart.*
import kotlin.math.roundToInt


@Preview(heightDp = 200, widthDp = 100)
@Composable
fun HeartRateScale(
    @PreviewParameter(GroupStatePreviewParameterProvider::class) state: GroupState?,
    range: ClosedRange<HeartRate> = HeartRate(40)..HeartRate(200f)
) {
    Box {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val brush = SolidColor(Color.Gray)
            drawLine(brush, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 3f)

            (range step 10).forEach { heartRate ->
                val y = yOfHeartRate(HeartRate(heartRate), range, size.height)
                drawLine(brush, Offset(center.x - 20, y), Offset(center.x + 20, y), strokeWidth = 2f)
            }
        }

        (range step 5).map(::HeartRate).forEach { heartRate ->
            OnHeartRateScalePosition(heartRate, range, modifier = Modifier.padding(start = 24.dp)) {
                val isEmphasized = heartRate.value.roundToInt() % 10 == 0
                Text(
                    heartRate.toString(),
                    fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Thin,
                    fontSize = if (isEmphasized) 12.sp else 10.sp,
                )
            }
        }

        state?.members.orEmpty().forEach { memberState ->
            MemberHeartRateIndicator(memberState, range)
            MemberHeartRateLimit(memberState, range)
        }
    }
}

fun yOfHeartRate(heartRate: HeartRate, range: ClosedRange<HeartRate>, height: Float): Float {
    if (heartRate > range.endInclusive) return 0f
    if (heartRate < range.start) return height
    val rangeWidth = range.endInclusive.value - range.start.value
    val relativeInRange = (heartRate.value - range.start.value) / rangeWidth
    return height * (1f - relativeInRange)
}

fun heartRateOfY(y: Float, range: ClosedRange<HeartRate>, height: Float): HeartRate {
    if (y > height) return range.start
    if (y < 0) return range.endInclusive
    val rangeWidth = range.endInclusive.value - range.start.value
    val hr = range.endInclusive.value - (rangeWidth * y) / height
    return HeartRate(hr)
}
