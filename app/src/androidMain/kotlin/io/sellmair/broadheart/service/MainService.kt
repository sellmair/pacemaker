package io.sellmair.broadheart.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.sellmair.broadheart.bluetooth.BroadheartBluetoothReceiver
import io.sellmair.broadheart.bluetooth.BroadheartBluetoothSender
import io.sellmair.broadheart.hrSensor.HrReceiver
import io.sellmair.broadheart.hrSensor.polar.PolarHrReceiver
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorInfo
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.Path.Companion.toOkioPath
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class MainService : Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    data class Services(
        val userService: UserService,
        val groupService: GroupService
    )

    inner class MainServiceBinder : Binder() {
        val services = Services(
            userService = userService,
            groupService = groupService
        )
    }

    private val hrReceiver = HrReceiver(PolarHrReceiver(this))
    private val notification = MainServiceNotification(this)

    private val userService: UserService by lazy {
        StoredUserService(this, filesDir.resolve("userService").toOkioPath())
    }

    private val groupService by lazy { DefaultGroupService(userService) }


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
            val sender = BroadheartBluetoothSender(this@MainService, this@MainService, userService.currentUser())
            coroutineScope {
                launch {
                    hrMeasurements.collect { hrMeasurement ->
                        val user = userService.currentUser()
                        sender.updateUser(user)
                        sender.updateHeartHeart(hrMeasurement.sensorInfo.id, hrMeasurement.heartRate)
                    }
                }
            }
        }


        /* Receive broadcasts */
        launch {
            BroadheartBluetoothReceiver(this@MainService, this@MainService).received.collect { received ->
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
                            address = received.address,
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
        return MainServiceBinder()
    }
}