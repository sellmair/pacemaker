package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.runtime.Composable
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.NearbyDeviceViewModel
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.BackHandlerIfAny


@Composable
internal fun SettingsPage(
    me: User,
    nearbyDevices: List<NearbyDeviceViewModel>,
    onCloseSettingsPage: () -> Unit = {},
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    BackHandlerIfAny { onCloseSettingsPage() }

    SettingsPageContent(
        me = me,
        onIntent = onIntent,
        nearbyDevices = nearbyDevices,
        onCloseSettingsPage = onCloseSettingsPage
    )
}

