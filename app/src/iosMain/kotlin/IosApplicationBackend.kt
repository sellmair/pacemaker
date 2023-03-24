@file:OptIn(FlowPreview::class)

package io.sellmair.broadheart.ui

import io.sellmair.broadheart.*
import io.sellmair.broadheart.bluetooth.DarwinBle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.time.Duration.Companion.seconds

class IosApplicationBackend : ApplicationBackend {

    private val coroutineScope = MainScope()

    private val ble = DarwinBle(coroutineScope)

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
        coroutineScope.launch {
            while (true) {
                delay(30.seconds)
                groupService.invalidate()
            }
        }

        coroutineScope.launch {
            bluetoothService.peripherals.filterIsInstance<BluetoothService.Peripheral.HeartRateSensor>()
                .flatMapMerge { it.measurements }
                .collect { hrMeasurement ->
                    groupService.add(hrMeasurement)
                    groupService.invalidate()
                }
        }
    }
}