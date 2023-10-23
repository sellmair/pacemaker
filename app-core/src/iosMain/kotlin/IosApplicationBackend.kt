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
import io.sellmair.pacemaker.sql.PacemakerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async

class IosApplicationBackend : ApplicationBackend, CoroutineScope by MainScope() {

    private val ble = AppleBle()

    override val pacemakerBluetoothService = async { PacemakerBluetoothService(ble) }

    override val heartRateSensorBluetoothService = async { HeartRateSensorBluetoothService(ble) }

    override val userService: UserService by lazy {
        SqliteUserService(
            PacemakerDatabase(NativeSqliteDriver(PacemakerDatabase.Schema.synchronous(), "app.db"))
        )
    }

    override val groupService: GroupService by lazy {
        GroupService(userService)
    }


    init {
        launchApplicationBackend(this)
    }
}