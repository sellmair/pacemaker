package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.Event
import io.sellmair.pacemaker.utils.collectEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


sealed class UpdateMeIntent : Event {
    data class UpdateMe(val me: User): UpdateMeIntent()
    data class UpdateHeartRateLimit(val heartRateLimit: HeartRate): UpdateMeIntent()
}

internal fun CoroutineScope.launchUpdateMeActor(userService: UserService): Job = launch(Dispatchers.Main.immediate) {
    var me = userService.me()
    collectEvents<UpdateMeIntent> { intent ->
        when(intent) {
            is UpdateMeIntent.UpdateHeartRateLimit -> userService.saveHeartRateLimit(me, intent.heartRateLimit)
            is UpdateMeIntent.UpdateMe -> {
                me = intent.me
                userService.saveUser(intent.me)
            }
        }
    }
}
