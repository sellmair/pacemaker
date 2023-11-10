import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.sellmair.pacemaker.ApplicationBackend
import io.sellmair.pacemaker.SafePacemakerDatabase
import io.sellmair.pacemaker.SessionService
import io.sellmair.pacemaker.SqlSessionService
import io.sellmair.pacemaker.SqlUserService
import io.sellmair.pacemaker.UserService
import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.launchApplicationBackend
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.StateBus
import io.sellmair.pacemaker.utils.eventBus
import io.sellmair.pacemaker.utils.stateBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.CoroutineContext

class IosApplicationBackend : ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job() + EventBus() + StateBus()

    override val eventBus: EventBus get() = coroutineContext.eventBus

    override val stateBus: StateBus = coroutineContext.stateBus

    private val ble = AppleBle()

    override val pacemakerBluetoothService = async { PacemakerBluetoothService(ble) }

    override val heartRateSensorBluetoothService = async { HeartRateSensorBluetoothService(ble) }

    private val pacemakerDatabase by lazy {
        SafePacemakerDatabase(
            PacemakerDatabase(NativeSqliteDriver(PacemakerDatabase.Schema.synchronous(), "app.db"))
        )
    }

    override val userService: UserService by lazy {
        SqlUserService(pacemakerDatabase)
    }

    override val sessionService: SessionService by lazy {
        SqlSessionService(pacemakerDatabase)
    }

    override val settings: Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

    init {
        launchApplicationBackend(this)
    }
}
