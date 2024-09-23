package io.sellmair.pacemaker

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.sellmair.evas.*
import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.sql.PacemakerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.CoroutineContext

class IosApplicationBackend : ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob() + Events() + States()

    override val events: Events get() = coroutineContext.eventsOrThrow

    override val states: States = coroutineContext.statesOrThrow

    private val ble = AppleBle()

    override val pacemakerBluetoothService = async { PacemakerBluetoothService(ble) }

    override val heartRateSensorBluetoothService = async { HeartRateSensorBluetoothService(ble) }

    override val settings: Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

    private val meId by lazy { settings.meId }

    private val pacemakerDatabase = SafePacemakerDatabase {
        PacemakerDatabase(NativeSqliteDriver(PacemakerDatabase.Schema, "app.db"))
    }

    override val userService: UserService by lazy {
        SqlUserService(pacemakerDatabase, meId)
    }

    override val sessionService: SessionService by lazy {
        SqlSessionService(pacemakerDatabase)
    }


    init {
        launchApplicationBackend(this)
    }
}
