package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.sellmair.pacemaker.utils.get
import io.sellmair.pacemaker.utils.set
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
    
    UtteranceState.get().collectLatest { state ->
        settings[UtteranceState::class.qualifiedName!!] = state.name
    }
}
