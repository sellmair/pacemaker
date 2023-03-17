package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.runtime.*
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.Group
import io.sellmair.broadheart.ui.HCBackHandler
import io.sellmair.broadheart.ApplicationIntent


@Composable
internal fun SettingsPage(
    me: User,
    groupState: Group?,
    onCloseSettingsPage: () -> Unit = {},
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    HCBackHandler { onCloseSettingsPage() }


    SettingsPageContent(
        me = me,
        groupState = groupState,
        onIntent = onIntent,
        onCloseSettingsPage = onCloseSettingsPage
    )

}

