package io.sellmair.broadheart.backend

import AndroidBleHeartRateReceiver
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.sellmair.broadheart.*
import io.sellmair.broadheart.bluetooth.AndroidBle
import io.sellmair.broadheart.bluetooth.HeartcastBluetoothSender
import io.sellmair.broadheart.bluetooth.receiveHeartcastBroadcastPackages
import io.sellmair.broadheart.hrSensor.AndroidPolarHrReceiver
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.Path.Companion.toOkioPath
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class AndroidApplicationBackend : Service(), ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    inner class MainServiceBinder(
        override val userService: UserService,
        override val groupService: GroupService
    ) : Binder(), ApplicationBackend

    private val ble by lazy { AndroidBle(this, this) }

    private val hrReceiver = HeartRateReceiver(
        AndroidPolarHrReceiver(this),
        //AndroidBleHeartRateReceiver(ble)
    )
    private val notification = AndroidHeartRateNotification(this)

    override val userService: UserService by lazy {
        StoredUserService(this, filesDir.resolve("userService").toOkioPath())
    }

    override val groupService by lazy { DefaultGroupService(userService) }

    override fun onCreate() {
        super.onCreate()
        notification.startForeground()
        launchHrLimitDaemon(this, groupService)

        /* Connecting our hr receiver with the group service */
        val hrMeasurements = hrReceiver.measurements
            .onEach { hrMeasurement -> groupService.add(hrMeasurement) }
            .onEach { groupService.invalidate() }
            .shareIn(this, SharingStarted.WhileSubscribed())

        /*
         Regularly call the updateState w/o measurements, to invalidate old ones, in case
         no measurements arrive
         */
        launch {
            while (true) {
                delay(30.seconds)
                groupService.invalidate()
            }
        }

        /* Update notification showing current users heart rate */
        launch {
            groupService.group
                .mapNotNull { it.members.find { it.user?.isMe == true } }
                .collect { currentUserState ->
                    notification.update(
                        currentUserState.currentHeartRate ?: return@collect,
                        currentUserState.heartRateLimit ?: return@collect
                    )
                }
        }

        /* Start broadcasting my own state to other participants */
        launch {
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
        launch {
            ble.receiveHeartcastBroadcastPackages().collect { received ->
                val user = User(
                    isMe = false, id = received.userId, name = received.userName
                )

                userService.save(user)
                userService.saveUpperHeartRateLimit(user, received.heartRateLimit)
                userService.linkSensor(user, received.sensorId)

                groupService.add(
                    HeartRateMeasurement(
                        heartRate = received.heartRate,
                        sensorInfo = HeartRateSensorInfo(
                            id = received.sensorId,
                            address = received.deviceId.value,
                            vendor = HeartRateSensorInfo.Vendor.Unknown
                        ),
                        receivedTime = received.receivedTime
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return MainServiceBinder(
            userService = userService,
            groupService = groupService
        )
    }
}