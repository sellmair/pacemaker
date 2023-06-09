package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.HeartRateSensorViewModel
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.BackHandlerIfAny


@Composable
internal fun SettingsPage(
    me: User,
    heartRateSensors: List<HeartRateSensorViewModel>,
    onCloseSettingsPage: () -> Unit = {},
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    BackHandlerIfAny { onCloseSettingsPage() }

    SettingsPageContent(
        me = me,
        onIntent = onIntent,
        heartRateSensors = heartRateSensors,
        onCloseSettingsPage = onCloseSettingsPage
    )
}

