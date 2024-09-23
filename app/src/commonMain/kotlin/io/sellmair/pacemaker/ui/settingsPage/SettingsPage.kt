package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import io.sellmair.evas.compose.composeValue
import io.sellmair.pacemaker.HeartRateSensorsState
import io.sellmair.pacemaker.MeState


@Composable
internal fun SettingsPage() {
    val heartRateSensors = HeartRateSensorsState.composeValue().nearbySensors
    val me = MeState.composeValue()?.me ?: return

    SettingsPageContent(
        me = me,
        heartRateSensors = heartRateSensors
    )
}
