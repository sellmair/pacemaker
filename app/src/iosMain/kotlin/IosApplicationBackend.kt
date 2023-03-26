@file:OptIn(FlowPreview::class)

package io.sellmair.broadheart.ui

import io.sellmair.broadheart.*
import io.sellmair.broadheart.bluetooth.DarwinBle
import io.sellmair.broadheart.bluetooth.HeartcastBluetoothSender
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

        val hrMeasurements = bluetoothService.peripherals
            .filterIsInstance<BluetoothService.Peripheral.HeartRateSensor>()
            .flatMapMerge { it.measurements }
            .onEach { hrMeasurement -> groupService.add(hrMeasurement) }
            .onEach { groupService.invalidate() }
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed())

        coroutineScope.launch {
            hrMeasurements
                .collect { hrMeasurement ->
                    groupService.add(hrMeasurement)
                    groupService.invalidate()
                }
        }


        /* Start broadcasting my own state to other participants */
        /* Connecting our hr receiver with the group service */
        coroutineScope.launch {
            val sender = HeartcastBluetoothSender(ble)
            hrMeasurements.collect { hrMeasurement ->
                val user = userService.currentUser()
                sender.updateUser(user)
                sender.updateHeartHeart(hrMeasurement.sensorInfo.id, hrMeasurement.heartRate)
                userService.findUpperHeartRateLimit(user)?.let { heartRateLimit ->
                    sender.updateHeartRateLimit(heartRateLimit)
                }
            }
        }

        /* Receive broadcasts */
        coroutineScope.launch {
            bluetoothService
                .peripherals
                .filterIsInstance<BluetoothService.Peripheral.PacemakerApp>()
                .flatMapMerge { it.broadcasts }
                .collect { received ->
                    val user = User(
                        isMe = false, id = received.userId, name = received.userName
                    )

                    userService.save(user)
                    userService.saveUpperHeartRateLimit(user, received.heartRateLimit)
                    userService.linkSensor(user, received.sensorId)

                    groupService.add(
                        HeartRateMeasurement(
                            heartRate = received.heartRate,
                            sensorInfo = HeartRateSensorInfo(id = received.sensorId),
                            receivedTime = received.receivedTime
                        )
                    )
                }
        }

    }
}