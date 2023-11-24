package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import kotlin.math.absoluteValue

object UserColors {
    const val saturation = .5f
    const val lightness = .4f

    const val saturationLight = .7f
    const val lightnessLight = .75f

    fun defaultHue(userId: UserId): Float = hashCode().toFloat().absoluteValue % 360f

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


val User.displayColorLight: HSLColor
    get() = HSLColor(
        hue = UserColors.defaultHue(id),
        saturation = UserColors.saturationLight,
        lightness = UserColors.lightnessLight
    )

val User.displayColor: HSLColor
    get() = HSLColor(
        hue = UserColors.defaultHue(id),
        saturation = UserColors.saturation,
        lightness = UserColors.lightness
    )

val UserState.displayColor: HSLColor
    get() = user.displayColor

val UserState.displayColorLight: HSLColor
    get() = user.displayColorLight

