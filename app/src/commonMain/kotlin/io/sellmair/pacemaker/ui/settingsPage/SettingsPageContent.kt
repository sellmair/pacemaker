@file:OptIn(ExperimentalFoundationApi::class)

package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.sellmair.pacemaker.HeartRateSensorViewModel
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.toColor

@Composable
internal fun SettingsPageContent(
    me: User,
    heartRateSensors: List<HeartRateSensorViewModel>,
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        SettingsPageHeader(
            me = me,
            onIntent = onIntent,
        )

        Spacer(Modifier.height(24.dp))

        Box {
            SettingsPageDevicesList(
                me = me,
                heartRateSensors = heartRateSensors,
                onIntent = onIntent
            )
        }
    }
}

@Composable
internal fun SettingsPageDevicesList(
    me: User,
    heartRateSensors: List<HeartRateSensorViewModel>,
    onIntent: (ApplicationIntent.SettingsPageIntent) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Nearby Devices",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))


        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {

            if (heartRateSensors.isEmpty()) {
                item(key = "empty placeholder") {
                    Spacer(Modifier.height(48.dp))
                    Column(
                        Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {

                        CatImage(
                            Modifier.fillMaxWidth()
                                .height(200.dp)
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

                        }
                    }
                }
            }

            items(heartRateSensors, key = { it.id.value }) { sensor ->
                Box(
                    modifier = Modifier.padding(24.dp).animateItemPlacement()
                ) {
                    HeartRateSensorCard(
                        me = me,
                        viewModel = sensor,
                        onEvent = onIntent,
                    )
                }
            }

            item(key = "Progress") {
                Spacer(Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = me.displayColor.toColor(),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 1.dp
                )

                Spacer(Modifier.height(128.dp))
            }
        }
    }
}
