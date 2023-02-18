package io.sellmair.broadheart.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.sellmair.broadheart.hrSensor.HrReceiver
import io.sellmair.broadheart.hrSensor.polar.PolarHrReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

class MainService : Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    data class Services(
        val userService: UserService,
        val groupService: GroupService
    )

    inner class MainServiceBinder : Binder() {
        val services = Services(
            userService = userService,
            groupService = groupService
        )
    }

    private val hrReceiver = HrReceiver(PolarHrReceiver(this))
    private val notification = MainServiceNotification(this)
    private val userService: UserService = AndroidUserService(this)
    private val groupService = DefaultGroupService(userService)


    override fun onCreate() {
        super.onCreate()
        notification.startForeground()
        launchHrLimitDaemon(this, groupService)

        /* Connecting our hr receiver with the group service */
        hrReceiver.measurements
            .onEach { hrMeasurement -> groupService.push(hrMeasurement) }
            .launchIn(this)

        /* Update notification showing current users heart rate */
        launch {
            val currentUser = userService.currentUser()
            groupService.groupState
                .mapNotNull { it.members.find { it.user?.uuid == currentUser.uuid } }
                .collect { currentUserState ->
                    notification.update(
                        currentUserState.currentHeartRate ?: return@collect,
                        currentUserState.upperHeartRateLimit ?: return@collect
                    )
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return MainServiceBinder()
    }
}