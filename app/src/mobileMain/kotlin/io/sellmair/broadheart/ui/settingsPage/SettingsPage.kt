package io.sellmair.broadheart.ui.settingsPage

import androidx.compose.runtime.*
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.randomUserId
import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.UserService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.math.absoluteValue

sealed interface SettingsPageEvent {
    object Back : SettingsPageEvent
    data class UpdateMe(val user: User) : SettingsPageEvent
    data class LinkSensor(val user: User, val sensor: HeartRateSensorId) : SettingsPageEvent
    data class UnlinkSensor(val sensor: HeartRateSensorId) : SettingsPageEvent
    data class CreateAdhocUser(val sensor: HeartRateSensorId) : SettingsPageEvent
    data class UpdateAdhocUser(val user: User) : SettingsPageEvent
    data class DeleteAdhocUser(val user: User) : SettingsPageEvent
    data class UpdateAdhocUserLimit(val user: User, val limit: HeartRate) : SettingsPageEvent
}

@Composable
fun SettingsPage(
    userService: UserService,
    groupService: GroupService,
    onBack: () -> Unit = {}
) {
    //HCBackHandler { onBack() }

    var me by remember { mutableStateOf<User?>(null) }
    val groupState by groupService.groupState.collectAsState(null)

    val eventChannel = remember { Channel<SettingsPageEvent>(Channel.UNLIMITED) }

    /* Process incoming events */
    LaunchedEffect(Unit) {
        eventChannel.consumeEach { event ->
            when (event) {
                is SettingsPageEvent.Back -> {
                    onBack()
                }

                is SettingsPageEvent.LinkSensor -> {
                    userService.linkSensor(event.user, event.sensor)
                }

                is SettingsPageEvent.UnlinkSensor -> {
                    userService.unlinkSensor(event.sensor)
                }

                is SettingsPageEvent.UpdateMe -> {
                    userService.save(event.user)
                    me = event.user
                }

                is SettingsPageEvent.CreateAdhocUser -> {
                    val id = randomUserId()
                    val adhocUser = User(
                        isMe = false,
                        id = id,
                        name = "Adhoc ${id.value.absoluteValue % 1000}",
                        isAdhoc = true
                    )
                    userService.save(adhocUser)
                    userService.linkSensor(adhocUser, event.sensor)
                    userService.saveUpperHeartRateLimit(adhocUser, HeartRate(130))
                    groupService.updateState()
                }

                is SettingsPageEvent.UpdateAdhocUser -> {
                    userService.save(event.user)
                    groupService.updateState()
                }

                is SettingsPageEvent.DeleteAdhocUser -> {
                    userService.delete(event.user)
                    groupService.updateState()
                }

                is SettingsPageEvent.UpdateAdhocUserLimit -> {
                    userService.saveUpperHeartRateLimit(event.user, event.limit)
                    groupService.updateState()
                }
            }

            groupService.updateState()
        }
    }

    /* Initially load 'me' */
    LaunchedEffect(Unit) {
        me = userService.currentUser()
    }

    me?.let { nonNullMe ->
        SettingsPageContent(
            me = nonNullMe,
            groupState = groupState,
            onEvent = { event -> eventChannel.trySend(event) }
        )
    }
}

