package io.sellmair.pacemaker.ui

import io.sellmair.pacemaker.GroupMember
import io.sellmair.pacemaker.NearbyDeviceViewModel
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


val GroupMember.displayColor: HSLColor
    get() = user?.displayColor ?: HSLColor(
        hue = (this.sensorInfo?.id?.value?.hashCode() ?: 0).toFloat().absoluteValue % 360f,
        saturation = .5f,
        lightness = .4f
    )



val GroupMember.displayColorLight: HSLColor
    get() = user?.displayColorLight
        ?: HSLColor(
            hue = (this.sensorInfo?.id?.value?.hashCode() ?: 0).toFloat().absoluteValue % 360f,
            saturation = .7f,
            lightness = .75f
        )

val NearbyDeviceViewModel.displayColor: HSLColor
    get() = this.associatedUser.value?.displayColor ?: HSLColor(
        hue = this.id.value.hashCode().toFloat().absoluteValue % 360f,
        saturation = .5f,
        lightness = .4f
    )

val NearbyDeviceViewModel.displayColorLight: HSLColor
    get() = this.associatedUser.value?.displayColorLight ?: HSLColor(
        hue = this.id.value.hashCode().toFloat().absoluteValue % 360f,
        saturation = .7f,
        lightness = .75f
    )
