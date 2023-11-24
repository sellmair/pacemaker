package io.sellmair.pacemaker.ui

import androidx.compose.ui.graphics.Color
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.User
import kotlin.math.absoluteValue

data class HSLColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
)

val User.displayColorLight: HSLColor
    get() = HSLColor(
        hue = id.hashCode().toFloat().absoluteValue % 360f,
        saturation = 0.7f,
        lightness = 0.75f
    )

val User.displayColor: HSLColor
    get() = HSLColor(
        hue = id.hashCode().toFloat().absoluteValue % 360f,
        saturation = .5f,
        lightness = .4f
    )

val UserState.displayColor: HSLColor
    get() = user.displayColor

val UserState.displayColorLight: HSLColor
    get() = user.displayColorLight

fun HSLColor.toColor(alpha: Float = 1f): Color {
    return Color.hsl(hue, saturation, lightness, alpha = alpha)
}
