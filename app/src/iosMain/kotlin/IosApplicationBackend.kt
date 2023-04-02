@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker.ui

import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.bluetooth.DarwinBleV1
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

class IosApplicationBackend : ApplicationBackend {

    private val coroutineScope = MainScope()

    private val ble = DarwinBleV1(coroutineScope)

    override val bluetoothService: BluetoothService by lazy { BluetoothService(ble) }

    override val userService: UserService by lazy {
        val fileManager = NSFileManager.defaultManager()
        val documents = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
        val root = documents.path!!.toPath().resolve("users")
        StoredUserService(coroutineScope, root)
    }

    override val groupService: GroupService by lazy {
        DefaultGroupService(userService)
    }


    init {
        launchApplicationBackend(coroutineScope)
    }
}