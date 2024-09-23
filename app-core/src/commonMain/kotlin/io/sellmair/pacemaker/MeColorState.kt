package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.sellmair.evas.Event
import io.sellmair.evas.State
import io.sellmair.evas.collectEvents
import io.sellmair.evas.launchState
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
) = launchState(MeColorState, context = Dispatchers.Main.immediate) {
    val initialHue = settings.storedUserHue ?: UserColors.defaultHue(settings.meId)
    MeColorState(UserColors.fromHue(initialHue)).emit()

    collectEvents<MeColorIntent.ChangeHue> { event ->
        MeColorState(UserColors.fromHue(event.hue)).emit()
        settings.storedUserHue = event.hue
    }
}

private const val hueSettingsKey = "me.hue"

private var Settings.storedUserHue: Float?
    get() = getFloatOrNull(hueSettingsKey)
    set(value) {
        if (value != null) set(hueSettingsKey, value)
        else remove(hueSettingsKey)
    }
  