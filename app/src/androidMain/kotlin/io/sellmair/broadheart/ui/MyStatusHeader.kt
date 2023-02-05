package io.sellmair.broadheart.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import io.sellmair.broadheart.*


@Composable
fun MyStatusHeader(state: GroupMemberState) {
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
                Text(
                    state.currentHeartRate?.toString() ?: "N/A",
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Center)
                )

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Me.user.displayColor.toColor()),
                    onClick = { Log.d("x", "click")},
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(24.dp),
                ) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }

            Text(
                state.upperLimitHeartRate.toString(),
                Modifier.offset(y = (-4).dp),
                fontWeight = FontWeight.Light,
                fontSize = 10.sp,
                color = state.user.displayColor.toColor()
            )

            Icon(
                Icons.Outlined.FavoriteBorder, "Heart",
                Modifier.offset(y = (-4).dp),
                tint = state.user.displayColorLight.toColor()
            )
        }
    }
}