@file:OptIn(FlowPreview::class)

import io.sellmair.pacemaker.ApplicationBackend
import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.launchApplicationBackend
import io.sellmair.pacemaker.service.GroupService
import io.sellmair.pacemaker.service.UserService
import io.sellmair.pacemaker.service.impl.StoredUserService
import io.sellmair.pacemaker.utils.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

class IosApplicationBackend : ApplicationBackend, Configuration by Configuration(), CoroutineScope by MainScope() {

    private val ble = AppleBle()

    override val pacemakerBluetoothService = async { PacemakerBluetoothService(ble) }

    override val heartRateSensorBluetoothService = async { HeartRateSensorBluetoothService(ble) }

    override val userService: UserService by lazy {
        val fileManager = NSFileManager.defaultManager()
        val documents = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
        val root = documents.path!!.toPath().resolve("users")
        StoredUserService(this, root)
    }

    override val groupService: GroupService by lazy {
        GroupService(userService)
    }


    init {
        launchApplicationBackend(this)
    }
}