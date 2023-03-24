package io.sellmair.broadheart.ui

import io.sellmair.broadheart.*
import io.sellmair.broadheart.bluetooth.DarwinBle
import io.sellmair.broadheart.bluetooth.HeartcastBluetoothSender
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.time.Duration.Companion.seconds

class IosApplicationBackend : ApplicationBackend {

    private val coroutineScope = MainScope()

    override val userService: UserService by lazy {
        val fileManager = NSFileManager.defaultManager()
        val documents = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
        val root = documents.path!!.toPath().resolve("users")
        StoredUserService(coroutineScope, root)
    }

    override val groupService: GroupService by lazy {
        DefaultGroupService(userService)
    }

    private val ble = DarwinBle(coroutineScope)

    private val heartRateReceiver = HeartRateReceiver(
        BleHeartRateReceiver(ble)
    )

    init {
        coroutineScope.launch {
            while (true) {
                delay(30.seconds)
                groupService.invalidate()
            }
        }

        /* Broadcast my HR measurements */
        coroutineScope.launch {
            val sender = HeartcastBluetoothSender(ble)
            heartRateReceiver.measurements.collect { measurement ->
                val user = userService.currentUser()
                sender.updateUser(user)
                sender.updateHeartHeart(measurement.sensorInfo.id, measurement.heartRate)
            }
        }
    }
}