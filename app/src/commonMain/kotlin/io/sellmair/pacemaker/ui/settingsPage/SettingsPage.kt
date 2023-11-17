package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.sellmair.pacemaker.HeartRateSensorsState
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.get


@Composable
internal fun SettingsPage(
    me: User,
) {
    val heartRateSensors = HeartRateSensorsState.get().collectAsState().value.nearbySensors
    SettingsPageContent(
        me = me,
        heartRateSensors = heartRateSensors
    )
}
