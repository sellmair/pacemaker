package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.sellmair.evas.flow
import io.sellmair.evas.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchUtteranceSettingsActor(settings: Settings) = launch {
    val storedUtterance = settings.getStringOrNull(UtteranceState::class.qualifiedName!!)
    if (storedUtterance != null) {
        val resolvedStoredUtterance = UtteranceState.entries.find { state -> state.name == storedUtterance }
        if (resolvedStoredUtterance != null) {
            UtteranceState.set(resolvedStoredUtterance)
        }
    }
    
    UtteranceState.flow().collectLatest { state ->
        settings[UtteranceState::class.qualifiedName!!] = state.name
    }
}
