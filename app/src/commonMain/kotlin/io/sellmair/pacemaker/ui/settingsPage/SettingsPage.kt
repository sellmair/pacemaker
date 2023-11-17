package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.sellmair.pacemaker.HeartRateSensorViewModelState
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.get


@Composable
internal fun SettingsPage(
    me: User,
) {
    SettingsPageContent(
        me = me,
        heartRateSensors = HeartRateSensorViewModelState.get().collectAsState().value.sensors,
    )
}
