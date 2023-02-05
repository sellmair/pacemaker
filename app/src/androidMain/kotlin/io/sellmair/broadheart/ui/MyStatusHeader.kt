package io.sellmair.broadheart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
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
import io.sellmair.broadheart.GroupMemberState
import io.sellmair.broadheart.displayColor
import io.sellmair.broadheart.displayColorLight
import io.sellmair.broadheart.toColor


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
                if (state.currentHeartRate != null)
                    Text(
                        state.currentHeartRate.toString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 48.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    ) else {
                    Icon(Icons.Default.Send, "Searchign")
                }
            }

            Row {
                Text(
                    state.upperLimitHeartRate.toString(),
                    Modifier.offset(y = (-4).dp),
                    fontWeight = FontWeight.Light,
                    fontSize = 10.sp,
                    color = state.user.displayColor.toColor()
                )
            }

            Row {
                Icon(
                    Icons.Outlined.FavoriteBorder, "Heart",
                    Modifier.offset(y = (-4).dp),
                    tint = state.user.displayColorLight.toColor()
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().height(248.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Icon(
                Icons.Default.Settings, "Settings",
                Modifier.padding(vertical = 42.dp, horizontal = 24.dp)
            )
        }
    }
}