package io.sellmair.broadheart.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.broadheart.User
import io.sellmair.broadheart.hrSensor.HrSensorInfo
import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.UserService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    userService: UserService,
    groupService: GroupService,
) {
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<User?>(null) }
    val groupState by groupService.groupState.collectAsState(null)

    LaunchedEffect(Unit) {
        currentUser = userService.currentUser()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentUser?.name.orEmpty(),
                onValueChange = { text ->
                    currentUser = currentUser?.copy(name = text)
                    currentUser?.let { updateUser ->
                        coroutineScope.launch { userService.save(updateUser) }
                    }
                },
                label = {},
                textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
                singleLine = true,
                shape = TextFieldDefaults.outlinedShape,
            )
        }

        LazyColumn {
            items(groupState?.members.orEmpty()) { memberState ->

                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clickable {
                            coroutineScope.launch {
                                userService.saveSensorId(
                                    userService.currentUser(),
                                    memberState.sensorInfo?.id ?: return@launch
                                )
                            }
                        },
                    elevation = CardDefaults.cardElevation(),
                ) {

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        UserHead(
                            memberState = memberState,
                            size = 48.dp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Column(Modifier.padding(horizontal = 24.dp)) {
                            if (memberState.user != null) {
                                Text(memberState.user.name)
                            }
                            if (memberState.sensorInfo != null) {
                                Column {
                                    if (memberState.currentHeartRate != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Favorite, "HR", Modifier.size(12.dp))
                                            Text(memberState.currentHeartRate.value.roundToInt().toString())
                                        }
                                    }

                                    if (memberState.sensorInfo.rssi != null) {
                                        Text("${memberState.sensorInfo.rssi} db")
                                    }

                                    Text(memberState.sensorInfo.id.value)

                                    when (memberState.sensorInfo.vendor) {
                                        HrSensorInfo.Vendor.Polar -> Text("Polar")
                                        HrSensorInfo.Vendor.Garmin -> Text("Garmin")
                                        HrSensorInfo.Vendor.Unknown -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}