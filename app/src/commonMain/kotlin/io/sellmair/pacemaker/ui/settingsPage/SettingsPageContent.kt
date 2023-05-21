package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.ApplicationIntent
import io.sellmair.pacemaker.NearbyDeviceViewModel
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.toColor

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

        if (nearbyDevices.isEmpty()) {

            Column(
                Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {

                CatImage(
                    Modifier.fillMaxWidth()
                        .height(256.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))


                    Text(
                        "Searching for nearby devices",
                        fontSize = 12.sp
                    )

                    Text(
                        "Please stand by 👍",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Light
                    )

                    Spacer(Modifier.height(24.dp))

                    CircularProgressIndicator(
                        color = me.displayColor.toColor(),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 1.dp
                    )
                }
            }

        }

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
