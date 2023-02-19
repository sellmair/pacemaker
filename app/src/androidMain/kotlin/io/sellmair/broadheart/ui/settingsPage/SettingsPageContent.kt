package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.sellmair.broadheart.User
import io.sellmair.broadheart.service.GroupMemberState
import io.sellmair.broadheart.service.GroupState
import io.sellmair.broadheart.ui.preview.UserPreviewParameterProvider

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun SettingsPageContent(
    @PreviewParameter(UserPreviewParameterProvider::class)
    me: User,
    groupState: GroupState? = null,
    onEvent: (SettingsPageEvent) -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        SettingsPageHeader(
            me = me,
            onEvent = onEvent
        )

        Spacer(Modifier.height(24.dp))

        Box(Modifier.padding(horizontal = 24.dp)) {
            SettingsPageDevicesList(
                me = me,
                groupState = groupState,
                onEvent = onEvent
            )
        }
    }
}

@Composable
fun SettingsPageDevicesList(
    me: User,
    groupState: GroupState?,
    onEvent: (SettingsPageEvent) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Nearby Devices",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        val memberStates = groupState?.members.orEmpty()
            .sortedWith(
                compareBy<GroupMemberState> { it.user?.isMe?.let { 0 } ?: 1 }
                    .then(compareBy { it.sensorInfo?.id?.value.orEmpty() })
            )
        LazyColumn(
        ) {
            items(memberStates) { member ->
                Box(
                    modifier = Modifier
                        .animateContentSize()
                ) {
                    NearbyDeviceCard(
                        me = me,
                        state = member,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}
