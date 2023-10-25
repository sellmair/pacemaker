package io.sellmair.pacemaker

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.sellmair.pacemaker.ble.AndroidBle
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class AndroidApplicationBackend : Service(), ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job() + EventBus()

    inner class MainServiceBinder(
        override val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>,
        override val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>,
        override val userService: UserService,
        override val groupService: GroupService,
    ) : Binder(), ApplicationBackend

    private val ble by lazy { AndroidBle(this) }

    override val pacemakerBluetoothService = async {
        PacemakerBluetoothService(ble)
    }

    override val heartRateSensorBluetoothService = async {
        HeartRateSensorBluetoothService(ble)
    }

    private val notification = AndroidHeartRateNotification(this)

    override val userService: UserService by lazy {
        val driver = AndroidSqliteDriver(
            schema = PacemakerDatabase.Schema.synchronous(), context = this, name = "test.db"
        )
        SqliteUserService(PacemakerDatabase(driver))
    }

    override val groupService by lazy { launchGroupService(userService) }

    override fun onCreate() {
        super.onCreate()
        notification.startForeground()
        launchHrLimitDaemon(this, groupService)

        /* Update notification showing current users heart rate */
        launch {
            groupService.group
                .mapNotNull { it.members.find { it.isMe } }
                .collect { currentUserState ->
                    notification.update(
                        currentUserState.heartRate,
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
            pacemakerBluetoothService = pacemakerBluetoothService,
            heartRateSensorBluetoothService = heartRateSensorBluetoothService,
        )
    }
}