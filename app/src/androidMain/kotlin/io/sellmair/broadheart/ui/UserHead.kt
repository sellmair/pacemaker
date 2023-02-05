package io.sellmair.broadheart.ui

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
import io.sellmair.broadheart.User
import io.sellmair.broadheart.displayColor
import io.sellmair.broadheart.nameAbbreviation
import io.sellmair.broadheart.toColor

@Composable
fun UserHead(
    user: User,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(SolidColor(user.displayColor.toColor()))
        }
        Text(text = user.nameAbbreviation, color = Color.White, fontSize = 10.sp)
    }
}
