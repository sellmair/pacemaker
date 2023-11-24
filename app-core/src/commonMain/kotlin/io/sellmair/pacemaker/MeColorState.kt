package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.sellmair.pacemaker.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MeColorState(val color: HSLColor) : State {
    companion object Key : State.Key<MeColorState?> {
        override val default: MeColorState? = null
    }
}

sealed class MeColorIntent : Event {
    data class ChangeHue(val hue: Float) : MeColorIntent()
}

internal fun CoroutineScope.launchMeColorStateActor(
    settings: Settings
) = launchStateProducer(MeColorState, coroutineContext = Dispatchers.Main.immediate) {
    val initialHue = settings.storedUserHue ?: UserColors.defaultHue(settings.meId)
    MeColorState(UserColors.fromHue(initialHue)).emit()

    collectEvents<MeColorIntent.ChangeHue> { event ->
        MeColorState(UserColors.fromHue(event.hue)).emit()
        settings.storedUserHue = event.hue
    }
}

private const val hueSetingsKey = "me.hue"

private var Settings.storedUserHue: Float?
    get() = getFloatOrNull(hueSetingsKey)
    set(value) {
        if (value != null) set(hueSetingsKey, value)
        else remove(hueSetingsKey)
    }
  