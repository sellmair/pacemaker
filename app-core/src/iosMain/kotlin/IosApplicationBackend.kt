
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.sellmair.pacemaker.ApplicationBackend
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
import kotlin.coroutines.CoroutineContext

class IosApplicationBackend : ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job() + EventBus() + StateBus()

    override val eventBus: EventBus get() = coroutineContext.eventBus

    override val stateBus: StateBus = coroutineContext.stateBus

    private val ble = AppleBle()

    override val pacemakerBluetoothService = async { PacemakerBluetoothService(ble) }

    override val heartRateSensorBluetoothService = async { HeartRateSensorBluetoothService(ble) }

    override val userService: UserService by lazy {
        SqlUserService(
            PacemakerDatabase(NativeSqliteDriver(PacemakerDatabase.Schema.synchronous(), "app.db"))
        )
    }

    init {
        launchApplicationBackend(this)
    }
}
