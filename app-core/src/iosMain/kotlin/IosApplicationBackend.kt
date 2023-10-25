import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.sellmair.pacemaker.ApplicationBackend
import io.sellmair.pacemaker.GroupService
import io.sellmair.pacemaker.SqliteUserService
import io.sellmair.pacemaker.UserService
import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.launchApplicationBackend
import io.sellmair.pacemaker.launchGroupService
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext

class IosApplicationBackend : ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job() + EventBus()

    private val ble = AppleBle()

    override val pacemakerBluetoothService = async { PacemakerBluetoothService(ble) }

    override val heartRateSensorBluetoothService = async { HeartRateSensorBluetoothService(ble) }


    override val userService: UserService by lazy {
        SqliteUserService(
            PacemakerDatabase(NativeSqliteDriver(PacemakerDatabase.Schema.synchronous(), "app.db"))
        )
    }

    override val groupService: GroupService by lazy {
        launchGroupService(userService)
    }

    private val hrDeamon = async { launchHrLimitDaemon(groupService) }

    init {
        launchApplicationBackend(this)
    }
}