package io.sellmair.broadheart

import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.UserId
import io.sellmair.broadheart.service.GroupMemberState
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue

@Serializable
data class User(
    val isMe: Boolean,
    val id: UserId,
    val name: String,
    val isAdhoc: Boolean = false,
    val imageUrl: String? = null
)

@Serializable
@JvmInline
value class UserId(val value: Long)


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


val GroupMemberState.displayColor: HSLColor
    get() = user?.displayColor ?: HSLColor(
        hue = (this.sensorInfo?.id?.value?.hashCode() ?: 0).toFloat().absoluteValue % 360f,
        saturation = .5f,
        lightness = .4f
    )

val GroupMemberState.displayColorLight: HSLColor
    get() = user?.displayColor
        ?: HSLColor(
            hue = (this.sensorInfo?.id?.value?.hashCode() ?: 0).toFloat().absoluteValue % 360f,
            saturation = .7f,
            lightness = .75f
        )