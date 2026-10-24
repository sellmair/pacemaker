package io.sellmair.pacemaker.spoof

import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFRunLoopGetMain
import platform.CoreFoundation.CFRunLoopRun
import platform.CoreFoundation.CFRunLoopStop

private val ble = AppleBle()

@OptIn(ExperimentalForeignApi::class)
fun main() {
    launchSendBroadcasts()
    CFRunLoopRun()
}

@OptIn(ExperimentalForeignApi::class)
private fun launchSendBroadcasts() = MainScope().launch {
    val service = PacemakerBluetoothService(ble)
    var user = User(id = UserId(2412), name = "Felix Werner")
    var heartRate = HeartRate(130)
    var heartRateLimit = HeartRate(120)

    service.write {
        setUser(user)
        setHeartRateLimit(heartRateLimit)
        setHeartRate(heartRate)
    }
    println("READY")

    withContext(Dispatchers.Default) {
        while (isActive) {
            val line = readlnOrNull() ?: break
            when {
                line.startsWith("heart-rate ") -> {
                    val value = line.removePrefix("heart-rate ").toIntOrNull() ?: continue
                    heartRate = HeartRate(value)
                    service.write { setHeartRate(heartRate) }
                    println("Heart rate set to $value bpm")
                }

                line.startsWith("heart-rate-limit ") -> {
                    val value = line.removePrefix("heart-rate-limit ").toIntOrNull() ?: continue
                    heartRateLimit = HeartRate(value)
                    service.write { setHeartRateLimit(heartRateLimit) }
                    println("Heart rate limit set to $value bpm")
                }

                line.startsWith("user-id ") -> {
                    val value = line.removePrefix("user-id ").toLongOrNull() ?: continue
                    user = user.copy(id = UserId(value))
                    service.write { setUser(user) }
                    println("User ID set to $value")
                }

                line.startsWith("user-name ") -> {
                    user = user.copy(name = line.removePrefix("user-name "))
                    service.write { setUser(user) }
                    println("User name set to ${user.name}")
                }
            }
        }
    }

    ble.close()
    CFRunLoopStop(CFRunLoopGetMain())
}
