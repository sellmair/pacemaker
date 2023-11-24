package io.sellmair.pacemaker

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.sellmair.pacemaker.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.INFINITE

enum class ApplicationFeature(val default: Boolean) {
    /**
     * Allow to record and then view sessions
     */
    Sessions(false);

    val state get() = ApplicationFeatureState.Key(this)

    suspend fun enable() {
        ApplicationFeatureEvent.Enable(this).emit()
    }

    suspend fun disable() {
        ApplicationFeatureEvent.Disable(this).emit()
    }

    suspend fun toggle() {
        ApplicationFeatureEvent.Toggle(this).emit()
    }
}

data class ApplicationFeatureState(
    val feature: ApplicationFeature,
    val enabled: Boolean,
) : State {
    data class Key(val feature: ApplicationFeature) : State.Key<ApplicationFeatureState> {
        override val default: ApplicationFeatureState = when (feature) {
            ApplicationFeature.Sessions -> ApplicationFeatureState(feature, feature.default)
        }
    }
}

sealed class ApplicationFeatureEvent : Event {
    data class Enable(val feature: ApplicationFeature) : ApplicationFeatureEvent()
    data class Disable(val feature: ApplicationFeature) : ApplicationFeatureEvent()
    data class Toggle(val feature: ApplicationFeature) : ApplicationFeatureEvent()
}

internal fun CoroutineScope.launchApplicationFeatureActor(settings: Settings) = launchStateProducer(
    coroutineContext = Dispatchers.Main.immediate,
    keepActive = INFINITE
) { key: ApplicationFeatureState.Key ->
    val settingsKey = "${ApplicationFeature::class.qualifiedName}:${key.feature.name}"
    var enabled = settings.getBooleanOrNull(settingsKey) ?: key.default.enabled
    ApplicationFeatureState(key.feature, enabled).emit()

    collectEvents<ApplicationFeatureEvent> { event ->
        enabled = when (event) {
            is ApplicationFeatureEvent.Disable -> false
            is ApplicationFeatureEvent.Enable -> true
            is ApplicationFeatureEvent.Toggle -> !enabled
        }
        ApplicationFeatureState(key.feature, enabled).emit()
        settings[settingsKey] = enabled
    }
}

