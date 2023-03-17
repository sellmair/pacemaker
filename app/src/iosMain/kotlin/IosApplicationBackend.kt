package io.sellmair.broadheart.ui

import io.sellmair.broadheart.backend.ApplicationBackend
import io.sellmair.broadheart.bluetooth.BroadheartBluetoothReceiver
import io.sellmair.broadheart.bluetooth.BroadheartBluetoothSender
import io.sellmair.broadheart.hrSensor.HeartRateReceiver
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.service.DefaultGroupService
import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.StoredUserService
import io.sellmair.broadheart.service.UserService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import platform.Foundation.*
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

    private val heartRateReceiver = HeartRateReceiver()

    init {
        coroutineScope.launch {
            while (true) {
                delay(30.seconds)
                groupService.invalidate()
            }
        }

        /* Broadcast my HR measurements */
        coroutineScope.launch {
            val sender = BroadheartBluetoothSender(userService.currentUser())
            heartRateReceiver.measurements.collect { measurement ->
                val user = userService.currentUser()
                sender.updateUser(user)
                sender.updateHeartHeart(measurement.sensorInfo.id, measurement.heartRate)
            }
        }
    }
}