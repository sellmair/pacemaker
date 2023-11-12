package io.sellmair.pacemaker.ui.widget

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun Headline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TextStyle.Headline,
        modifier = modifier,
        color = Color.Black
    )
}


val TextStyle.Companion.Headline get() = TextStyle(
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold
)
