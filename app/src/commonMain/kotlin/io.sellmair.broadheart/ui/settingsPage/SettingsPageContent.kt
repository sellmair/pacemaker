package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.sellmair.broadheart.ApplicationIntent
import io.sellmair.broadheart.NearbyDeviceViewModel
import io.sellmair.broadheart.model.User

@Composable
internal fun SettingsPageContent(
    me: User,
    nearbyDevices: List<NearbyDeviceViewModel>,
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit = {},
    onCloseSettingsPage: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        SettingsPageHeader(
            me = me,
            onIntent = onIntent,
            onCloseSettingsPage = onCloseSettingsPage
        )

        Spacer(Modifier.height(24.dp))

        Box(Modifier.padding(horizontal = 24.dp)) {
            SettingsPageDevicesList(
                me = me,
                nearbyDevices = nearbyDevices,
                onIntent = onIntent
            )
        }
    }
}

@Composable
internal fun SettingsPageDevicesList(
    me: User,
    nearbyDevices: List<NearbyDeviceViewModel>,
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Nearby Devices",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn {
            items(nearbyDevices) { sensor ->
                Box(
                    modifier = Modifier
                        .animateContentSize()
                ) {
                    NearbyDeviceCard(
                        me = me,
                        device = sensor,
                        onEvent = onIntent
                    )
                }
            }
        }
    }
}
