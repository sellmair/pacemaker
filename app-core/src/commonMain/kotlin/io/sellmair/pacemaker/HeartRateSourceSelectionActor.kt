package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import io.sellmair.evas.collectEvents
import io.sellmair.evas.launchState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/** Stores the user's source choice, without persisting any measurement. */
internal fun CoroutineScope.launchHeartRateSourceSelectionActor(settings: Settings) = launchState(SelectedHeartRateSourceState) {
    val savedId = settings.getStringOrNull(SELECTED_SOURCE_KEY)
    var selected = when {
        savedId.isNullOrBlank() -> null
        savedId == WATCH_COMPANION_SOURCE_ID -> HeartRateSource(
            savedId, HeartRateSource.Kind.COMPANION_WATCH, HeartRateSource.Availability.CONNECTING,
            connectionName = "Wrist watch", companionDeviceId = settings.getStringOrNull(SELECTED_COMPANION_KEY)
        )
        else -> HeartRateSource(savedId, HeartRateSource.Kind.BLE_SENSOR, HeartRateSource.Availability.CONNECTING)
    }
    SelectedHeartRateSourceState(selected).emit()

    suspend fun setSelection(source: HeartRateSource?) {
        selected = source
        SelectedHeartRateSourceState(selected).emit()
        val id = selected?.id
        if (id == null) settings.remove(SELECTED_SOURCE_KEY)
        else settings.putString(SELECTED_SOURCE_KEY, id)
        val companionDeviceId = selected?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }?.companionDeviceId
        if (companionDeviceId != null) {
            settings.putString(SELECTED_COMPANION_KEY, companionDeviceId)
        } else settings.remove(SELECTED_COMPANION_KEY)
    }
    launch { collectEvents<HeartRateSourceSelectionIntent> { setSelection(it.source) } }
}

private const val SELECTED_SOURCE_KEY = "heart-rate.selected-source"
private const val SELECTED_COMPANION_KEY = "heart-rate.selected-companion-device"
