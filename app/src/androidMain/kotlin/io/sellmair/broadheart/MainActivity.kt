package io.sellmair.broadheart

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupState = GroupService.instance.state
        setContent {
            HeartRateScale(groupState.collectAsState(null).value)
        }
    }
}

class GroupStatePreviewParameterProvider : PreviewParameterProvider<GroupState?> {
    override val values: Sequence<GroupState?>
        get() = sequenceOf(dummyGroupState)
}


@Preview(heightDp = 200, widthDp = 100)
@Composable
fun HeartRateScale(@PreviewParameter(GroupStatePreviewParameterProvider::class) state: GroupState?) {
    Box {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val brush = SolidColor(Color.Gray)
            drawLine(brush, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 3f)

            (100..200 step 10).forEach { heartRate ->
                val y = yOfHeartRate(heartRate.toFloat(), size.height)
                drawLine(brush, Offset(center.x - 20, y), Offset(center.x + 20, y), strokeWidth = 2f)
            }
        }

        (100..200 step 5).map(::HeartRate).forEach { heartRate ->
            OnScale(heartRate, modifier = Modifier.padding(start = 24.dp)) {
                val isEmphasized = heartRate.value.roundToInt() % 10 == 0
                Text(
                    heartRate.toString(),
                    fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Thin,
                    fontSize = if (isEmphasized) 12.sp else 10.sp,
                )
            }
        }

        state?.members.orEmpty().forEach { memberState ->
            /* Render current heart rate */
            val side = if (memberState.user == Me.user) ScaleSide.Right else ScaleSide.Left
            OnScale(
                memberState.currentHeartRate, side = side, modifier = Modifier.padding(
                    start = if (side == ScaleSide.Right) 96.dp else 0.dp,
                    end = if (side == ScaleSide.Left) 48.dp else 0.dp
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserHead(memberState.user, modifier = Modifier.padding(horizontal = 4.dp))
                    if (memberState.currentHeartRate > memberState.upperLimitHeartRate) {
                        Icon(
                            Icons.Default.Warning, "Too high",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Red
                        )
                    } else {
                        Icon(
                            Icons.Default.ThumbUp, "OK",
                            modifier = Modifier.size(12.dp),
                            tint = memberState.user.displayColorHsl
                        )
                    }
                }
            }

            /* Render the respective limits */
            if (memberState.user == Me.user) {
                MyStatusHeader(memberState)
                MyLimit(memberState)
            } else {
                OnScale(memberState.upperLimitHeartRate, side = side) {
                    Canvas(modifier = Modifier.fillMaxWidth(.4f).height(1.dp)) {
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, memberState.user.displayColorHsl),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyStatusHeader(state: GroupMemberState) {
    Box {
        Column(
            Modifier.fillMaxWidth().height(248.dp).background(
                Brush.linearGradient(
                    listOf(Color.White, Color.White, Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    state.currentHeartRate.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row {
                Text(
                    state.upperLimitHeartRate.toString(),
                    Modifier.offset(y = (-4).dp),
                    fontWeight = FontWeight.Light,
                    fontSize = 10.sp,
                    color = state.user.displayColorHsl
                )
            }

            Row {
                Icon(
                    Icons.Outlined.FavoriteBorder, "Heart",
                    Modifier.offset(y = (-4).dp),
                    tint = state.user.displayColorHsl
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().height(248.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Icon(Icons.Default.Settings, "Settings",
                Modifier.padding(vertical = 42.dp, horizontal = 24.dp))
        }
    }
}

@Composable
fun MyLimit(state: GroupMemberState) {
    var isDragging: Boolean by remember { mutableStateOf(false) }
    var parentSize: IntSize? = null

    OnScale(
        state.upperLimitHeartRate,
        side = ScaleSide.Right,
        modifier = Modifier.onPlaced { coordinates ->
            parentSize = coordinates.parentCoordinates?.size
        }.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { isDragging = true },
                onDragEnd = { isDragging = false }
            ) { change, _ ->
                val newY = change.position.y
                val parentHeight = parentSize?.height ?: return@detectVerticalDragGestures
                val newHeartRate = heartRateOfY(newY, parentHeight.toFloat())
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
                drawRect(SolidColor(state.user.displayColorHsl))
            }

            Box(modifier = Modifier.size(if (isDragging) 30.dp else 20.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(SolidColor(state.user.displayColorHsl))
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


@Composable
fun UserHead(
    user: User,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val color = remember(user) {
            Color(user.displayColorInt)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(SolidColor(color))
        }
        Text(text = user.nameAbbreviation, color = Color.White, fontSize = 10.sp)
    }
}

enum class ScaleSide {
    Left, Right
}


@Composable
fun OnScale(
    heartRate: HeartRate,
    modifier: Modifier = Modifier,
    side: ScaleSide = ScaleSide.Right,
    content: @Composable () -> Unit
) {

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                placeable.placeRelative(
                    x = when (side) {
                        ScaleSide.Right -> (constraints.maxWidth / 2f).roundToInt()
                        ScaleSide.Left -> ((constraints.maxWidth / 2f) - placeable.width).roundToInt()
                    },
                    y = yOfHeartRate(
                        heartRate.value,
                        constraints.maxHeight.toFloat()
                    ).roundToInt() - placeable.height / 2
                )
            }
        }
    }
}

private fun yOfHeartRate(heartRate: Float, height: Float): Float {
    // 100 is on the bottom (size.height)
    // 200 is on the top (0)

    if (heartRate < 100) return height
    if (heartRate > 200) return 0f
    return height * (1f - ((heartRate - 100f) / 100f))
}

private fun heartRateOfY(y: Float, height: Float): HeartRate {
    // y == 0 -> 200
    // y == height -> 100

    if (y <= 0) return HeartRate(200)
    if (y >= height) return HeartRate(100)
    return HeartRate((100f * (1 - (y / height)) + 100f))
}