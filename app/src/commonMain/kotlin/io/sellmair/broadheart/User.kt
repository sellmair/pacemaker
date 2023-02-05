package io.sellmair.broadheart

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlin.math.absoluteValue

data class User(
    val uuid: UUID,
    val name: String,
    val imageUrl: String? = null
)

val User.nameAbbreviation: String get() = name.split(Regex("\\s")).joinToString("") { it.first().uppercase() }

@get:ColorInt
val User.displayColorInt: Int
    get() {
        val hue = uuid.value.fold(0) { acc, char -> (char.code + acc * 37)  % 360 }
        val saturation = 0.5f
        val lightness = 0.4f
        return ColorUtils.HSLToColor(floatArrayOf(hue.absoluteValue.toFloat(), saturation, lightness))
    }

@get:ColorInt
val User.displayColorHsl: Color
    get() {
        val hue = uuid.value.fold(0f) { acc, char -> (char.code.toFloat() + acc * 37f) % 360f }
        val saturation = 0.7f
        val lightness = 0.8f
        return Color.hsl(hue, saturation, lightness)
    }