package io.sellmair.broadheart


import kotlin.math.absoluteValue

data class User(
    val uuid: UUID,
    val name: String,
    val imageUrl: String? = null
)

val User.nameAbbreviation: String get() = name.split(Regex("\\s")).joinToString("") { it.first().uppercase() }

data class HSLColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
)

val User.displayColorLight: HSLColor
    get() = HSLColor(
        hue = name.hashCode().toFloat().absoluteValue % 360f,
        saturation = 0.7f,
        lightness = 0.75f
    )

val User.displayColor: HSLColor
    get() = HSLColor(
        hue = name.hashCode().toFloat().absoluteValue % 360f,
        saturation = .5f,
        lightness = .4f
    )
