package io.sellmair.pacemaker.ui

import androidx.compose.ui.graphics.Color

fun HSLColor.toColor(alpha: Float = 1f): Color {
    return Color.hsl(hue, saturation, lightness, alpha = alpha)
}