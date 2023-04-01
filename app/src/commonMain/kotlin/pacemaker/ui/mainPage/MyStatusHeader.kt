package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.foundation.background
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
import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.displayColorLight
import io.sellmair.pacemaker.ui.toColor


@Composable
internal fun MyStatusHeader(
    state: Group?,
    onSettingsClicked: () -> Unit
) {
    val myState = state?.members.orEmpty()
        .find { groupMemberState -> groupMemberState.user?.isMe == true }

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
                    myState?.currentHeartRate?.toString() ?: "🤷‍♂️",
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Center)
                )

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = myState?.displayColor?.toColor() ?: Color.Gray
                    ),
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(24.dp),
                ) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }

            if (myState?.heartRateLimit != null)
                Text(
                    myState.heartRateLimit.toString(),
                    Modifier.offset(y = (-4).dp),
                    fontWeight = FontWeight.Light,
                    fontSize = 10.sp,
                    color = myState.displayColor.toColor()
                )

            Icon(
                Icons.Outlined.FavoriteBorder, "Heart",
                Modifier.offset(y = (-4).dp),
                tint = myState?.displayColorLight?.toColor() ?: Color.Gray
            )
        }
    }
}