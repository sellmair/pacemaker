package io.sellmair.pacemaker.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.nameAbbreviation
import io.sellmair.pacemaker.displayColor
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
fun UserHead(user: User, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    UserHead(
        abbreviation = user.nameAbbreviation,
        color = user.displayColor.toColor(),
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
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            style = TextStyle.Default,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(
                    align = Alignment.CenterVertically, // Default value
                    unbounded = true // Makes sense if the size less than text
                )
        )
    }
}