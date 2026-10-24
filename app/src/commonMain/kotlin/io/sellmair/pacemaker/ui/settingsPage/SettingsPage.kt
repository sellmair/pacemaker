package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import io.sellmair.evas.compose.composeValue
import io.sellmair.pacemaker.HeartRateSensorsState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.SelectedHeartRateSourceState
import io.sellmair.pacemaker.WatchCompanionDeviceState


@Composable
internal fun SettingsPage() {
    val heartRateSensors = HeartRateSensorsState.composeValue().nearbySensors
    val me = MeState.composeValue()?.me ?: return
    val selectedSource = SelectedHeartRateSourceState.composeValue().source
    val companionState = WatchCompanionDeviceState.composeValue()

    SettingsPageContent(
        me = me,
        heartRateSensors = heartRateSensors,
        selectedSource = selectedSource,
        companionState = companionState
    )
}
