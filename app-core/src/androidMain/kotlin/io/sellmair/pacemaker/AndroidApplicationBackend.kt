package io.sellmair.pacemaker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.sellmair.pacemaker.ble.AndroidBle
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.StateBus
import io.sellmair.pacemaker.utils.eventBus
import io.sellmair.pacemaker.utils.stateBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class AndroidApplicationBackend : Service(), ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job() + EventBus() + StateBus()

    inner class MainServiceBinder(
        override val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>,
        override val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>,
        override val userService: UserService,
        override val sessionService: SessionService,
        override val settings: Settings,
    ) : Binder(), ApplicationBackend {
        override val eventBus get() = coroutineContext.eventBus
        override val stateBus get() = coroutineContext.stateBus
    }

    override val eventBus: EventBus = coroutineContext.eventBus

    override val stateBus: StateBus = coroutineContext.stateBus

    private val ble by lazy { AndroidBle(this) }

    override val pacemakerBluetoothService = async {
        PacemakerBluetoothService(ble)
    }

    override val heartRateSensorBluetoothService = async {
        HeartRateSensorBluetoothService(ble)
    }

    private val notification = AndroidHeartRateNotification(this)

    private val pacemakerDatabase by lazy {
        SafePacemakerDatabase(
            PacemakerDatabase(
                AndroidSqliteDriver(
                    schema = PacemakerDatabase.Schema.synchronous(), context = this, name = "test.db"
                )
            )
        )
    }

    override val userService: UserService by lazy {
        SqlUserService(pacemakerDatabase)
    }

    override val sessionService: SessionService by lazy {
        SqlSessionService(pacemakerDatabase)
    }

    override val settings: Settings by lazy {
        SharedPreferencesSettings(getSharedPreferences("app", Context.MODE_PRIVATE))
    }

    override fun onCreate() {
        super.onCreate()
        notification.startForeground()
        launchApplicationBackend(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return MainServiceBinder(
            userService = userService,
            sessionService = sessionService,
            pacemakerBluetoothService = pacemakerBluetoothService,
            heartRateSensorBluetoothService = heartRateSensorBluetoothService,
            settings = settings
        )
    }
}