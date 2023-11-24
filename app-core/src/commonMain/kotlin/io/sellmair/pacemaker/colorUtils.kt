package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.UserId
import kotlin.math.absoluteValue

object UserColors {
    const val saturation = .5f
    const val lightness = .4f
    const val saturationLight = .7f
    const val lightnessLight = .75f


    fun default(userId: UserId?): HSLColor = fromHue(defaultHue(userId))

    fun defaultHue(userId: UserId?): Float = userId.hashCode().toFloat().absoluteValue % 360f

    fun fromHue(hue: Float) = HSLColor(
        hue = hue, saturation = saturation, lightness = lightness
    )

    fun fromHueLight(hue: Float) = HSLColor(
        hue = hue, saturation = saturationLight, lightness = lightnessLight
    )
}

fun HSLColor.toUserLight() = UserColors.fromHueLight(hue)

data class HSLColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
)


val UserState.displayColor: HSLColor
    get() = color

val UserState.displayColorLight: HSLColor
    get() = color.toUserLight()

