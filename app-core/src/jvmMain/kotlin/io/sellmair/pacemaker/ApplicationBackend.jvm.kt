package io.sellmair.pacemaker

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.sql.PacemakerDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.prefs.Preferences

actual fun ApplicationBackend.launchPlatform(scope: CoroutineScope) {
    scope.launchHeartRateSensorEmulation()
}

object JvmApplicationBackend : ApplicationBackend {
    override val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
        get() = CompletableDeferred()

    override val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
        get() = CompletableDeferred(JvmHeartRateSensorBluetoothService)

    private val database by lazy { createInMemoryDatabase() }

    override val sessionService: SessionService by lazy {
        SqlSessionService(database)
    }

    private val meId by lazy {
        settings.meId
    }

    override val userService: UserService by lazy {
        SqlUserService(database, meId)
    }

    override val states: States = States()
    override val events: Events = Events()
    override val settings: Settings = PreferencesSettings(Preferences.userRoot())
}


internal fun createInMemoryDatabase(): SafePacemakerDatabase = SafePacemakerDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    PacemakerDatabase.Schema.create(driver)
    PacemakerDatabase(driver)
}

object JvmHeartRateSensorBluetoothService : HeartRateSensorBluetoothService {
    fun addSensor(sensor: HeartRateSensor) {
        allSensorsNearby.update { sensors -> sensors + sensor }
        newSensorsNearby.tryEmit(sensor)
    }

    override val newSensorsNearby = MutableSharedFlow<HeartRateSensor>(replay = 1)


    override val allSensorsNearby = MutableStateFlow<List<HeartRateSensor>>(emptyList())
}
