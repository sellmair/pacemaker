package io.sellmair.broadheart.ui

import androidx.compose.ui.graphics.Color
import io.sellmair.broadheart.HSLColor

fun HSLColor.toColor(alpha: Float = 1f): Color {
    return Color.hsl(hue, saturation, lightness, alpha = alpha)
}