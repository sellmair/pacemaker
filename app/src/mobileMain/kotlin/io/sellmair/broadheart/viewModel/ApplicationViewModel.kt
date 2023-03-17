package io.sellmair.broadheart.viewModel

import io.sellmair.broadheart.Group
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.randomUserId
import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.UserService
import io.sellmair.broadheart.viewModel.ApplicationIntent.MainPageIntent
import io.sellmair.broadheart.viewModel.ApplicationIntent.SettingsPageIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

interface ApplicationViewModel {
    val me: StateFlow<User?>
    val group: StateFlow<Group?>
    fun send(intent: ApplicationIntent)
}

fun ApplicationViewModel(
    coroutineScope: CoroutineScope,
    userService: UserService,
    groupService: GroupService
): ApplicationViewModel {
    return ApplicationViewModelImpl(
        coroutineScope, userService, groupService
    )
}

private class ApplicationViewModelImpl(
    coroutineScope: CoroutineScope,
    private val userService: UserService,
    private val groupService: GroupService
) : ApplicationViewModel {

    private val intentQueue = Channel<ApplicationIntent>(Channel.UNLIMITED)
    private val _me = MutableStateFlow<User?>(null)
    override val me = _me.asStateFlow()

    override val group = groupService.group

    override fun send(intent: ApplicationIntent) {
        intentQueue.trySend(intent)
    }

    private suspend fun process(intent: ApplicationIntent): Unit = when (intent) {
        is MainPageIntent.UpdateHeartRateLimit -> {
            val me = userService.currentUser()
            userService.saveUpperHeartRateLimit(me, intent.heartRateLimit)
            groupService.invalidate()
        }

        is SettingsPageIntent.LinkSensor -> {
            userService.linkSensor(intent.user, intent.sensor)
        }

        is SettingsPageIntent.UnlinkSensor -> {
            userService.unlinkSensor(intent.sensor)
        }

        is SettingsPageIntent.UpdateMe -> {
            userService.save(intent.user)
            _me.value = intent.user
        }

        is SettingsPageIntent.CreateAdhocUser -> {
            val id = randomUserId()
            val adhocUser = User(
                isMe = false,
                id = id,
                name = "Adhoc ${id.value.absoluteValue % 1000}",
                isAdhoc = true
            )
            userService.save(adhocUser)
            userService.linkSensor(adhocUser, intent.sensor)
            userService.saveUpperHeartRateLimit(adhocUser, HeartRate(130))
            groupService.invalidate()
        }

        is SettingsPageIntent.UpdateAdhocUser -> {
            userService.save(intent.user)
            groupService.invalidate()
        }

        is SettingsPageIntent.DeleteAdhocUser -> {
            userService.delete(intent.user)
            groupService.invalidate()
        }

        is SettingsPageIntent.UpdateAdhocUserLimit -> {
            userService.saveUpperHeartRateLimit(intent.user, intent.limit)
            groupService.invalidate()
        }
    }

    init {
        coroutineScope.launch(Dispatchers.Main.immediate) {
            _me.value = userService.currentUser()
            intentQueue.consumeEach { intent ->
                process(intent)
                _me.value = userService.currentUser()
                groupService.invalidate()
            }
        }
    }
}