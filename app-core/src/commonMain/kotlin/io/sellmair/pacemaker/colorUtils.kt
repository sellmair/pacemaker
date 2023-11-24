package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.UserId
import kotlin.math.absoluteValue

object UserColors {
    const val saturation = .5f
    const val lightness = .4f
    const val saturationLight = .7f
    const val lightnessLight = .75f

    fun defaultHue(userId: UserId?): Float = userId.hashCode().toFloat().absoluteValue % 360f

    fun fromHue(hue: Float) = HSLColor(
        hue = hue, saturation = saturation, lightness = lightness
    )

    fun fromHueLight(hue: Float) = HSLColor(
        hue = hue, saturation = saturationLight, lightness = lightnessLight
    )
}

data class HSLColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
)


val UserState.displayColor: HSLColor
    get() = UserColors.fromHue(UserColors.defaultHue(user.id))

val UserState.displayColorLight: HSLColor
    get() = UserColors.fromHueLight(UserColors.defaultHue(user.id))

