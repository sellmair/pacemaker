package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.runtime.Composable
import io.sellmair.broadheart.ApplicationIntent
import io.sellmair.broadheart.NearbyDeviceViewModel
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.ui.HCBackHandler


@Composable
internal fun SettingsPage(
    me: User,
    nearbyDevices: List<NearbyDeviceViewModel>,
    onCloseSettingsPage: () -> Unit = {},
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    HCBackHandler { onCloseSettingsPage() }

    SettingsPageContent(
        me = me,
        onIntent = onIntent,
        nearbyDevices = nearbyDevices,
        onCloseSettingsPage = onCloseSettingsPage
    )
}

