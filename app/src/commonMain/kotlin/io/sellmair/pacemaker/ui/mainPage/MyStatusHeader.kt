package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.ui.collectAsState
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.ui.widget.experimentalFeatureToggle

@Composable
fun MyStatusHeader() {
    MyStatusHeader(MeState.collectAsState().value)
}


@Composable
 fun MyStatusHeader(state: MeState?) {
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .height(248.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color.White, Color.White, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /* Ensure big HR number and Settings icon are aligned vertically */
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                SessionStartStopButton(
                    Modifier.align(Alignment.CenterStart)
                )

                Text(
                    state?.heartRate?.toString() ?: "🤷‍♂️",
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Center)
                        .experimentalFeatureToggle()
                )

                UtteranceControlButton(
                    Modifier.align(Alignment.CenterEnd)
                )
            }

            if (state?.heartRateLimit != null)
                Text(
                    state.heartRateLimit.toString(),
                    Modifier.offset(y = (-4).dp),
                    fontWeight = FontWeight.Light,
                    fontSize = 10.sp,
                    color = state.me.displayColor.toColor()
                )

            Icon(
                Icons.Outlined.FavoriteBorder, "Heart",
                Modifier.offset(y = (-4).dp),
                tint = state?.me?.displayColorLight?.toColor() ?: Color.Gray
            )
        }
    }
}