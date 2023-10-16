package io.sellmair.pacemaker.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.nameAbbreviation
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.toColor

@Composable
internal fun UserHead(
    userState: UserState,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    UserHead(
        abbreviation = userState.user.nameAbbreviation,
        color = userState.displayColor.toColor(),
        modifier = modifier,
        size = size
    )
}


@Composable
internal fun UserHead(
    abbreviation: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(SolidColor(color))
        }
        Text(
            text = abbreviation,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}