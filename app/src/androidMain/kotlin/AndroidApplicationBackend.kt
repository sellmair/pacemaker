@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker.backend

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.bluetooth.AndroidBleV1
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.mapNotNull
import okio.Path.Companion.toOkioPath
import kotlin.coroutines.CoroutineContext

class AndroidApplicationBackend : Service(), ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    inner class MainServiceBinder(
        override val bluetoothService: BluetoothService,
        override val userService: UserService,
        override val groupService: GroupService
    ) : Binder(), ApplicationBackend

    private val ble by lazy { AndroidBleV1(this, this) }


    override val bluetoothService by lazy { BluetoothService(ble) }

    private val notification = AndroidHeartRateNotification(this)

    override val userService: UserService by lazy {
        StoredUserService(this, filesDir.resolve("userService").toOkioPath())
    }

    override val groupService by lazy { DefaultGroupService(userService) }

    override fun onCreate() {
        super.onCreate()
        notification.startForeground()
        launchHrLimitDaemon(this, groupService)

        /* Update notification showing current users heart rate */
        launch {
            groupService.group
                .mapNotNull { it.members.find { it.user?.isMe == true } }
                .collect { currentUserState ->
                    notification.update(
                        currentUserState.currentHeartRate ?: return@collect,
                        currentUserState.heartRateLimit ?: return@collect
                    )
                }
        }

        launchApplicationBackend(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return MainServiceBinder(
            userService = userService,
            groupService = groupService,
            bluetoothService = bluetoothService
        )
    }
}