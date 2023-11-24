package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.sellmair.pacemaker.HeartRateSensorsState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.ui.collectAsState
import io.sellmair.pacemaker.ui.get


@Composable
internal fun SettingsPage() {
    val heartRateSensors = HeartRateSensorsState.get().collectAsState().value.nearbySensors
    val me = MeState.collectAsState().value?.me ?: return

    SettingsPageContent(
        me = me,
        heartRateSensors = heartRateSensors
    )
}
