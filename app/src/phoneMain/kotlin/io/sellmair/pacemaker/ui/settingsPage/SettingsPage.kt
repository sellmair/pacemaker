package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.HeartRateSensorViewModel
import io.sellmair.pacemaker.model.User


@Composable
internal fun SettingsPage(
    me: User,
    heartRateSensors: List<HeartRateSensorViewModel>,
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    SettingsPageContent(
        me = me,
        onIntent = onIntent,
        heartRateSensors = heartRateSensors,
    )
}

