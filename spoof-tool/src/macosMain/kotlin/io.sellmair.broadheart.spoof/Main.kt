@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker.spoof

import io.sellmair.pacemaker.bluetooth.DarwinBle
import io.sellmair.pacemaker.bluetooth.PacemakerBle
import io.sellmair.pacemaker.bluetooth.receivePacemakerBroadcastPackages
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.CoreFoundation.CFRunLoopRun
import kotlin.math.roundToInt

private val ble = DarwinBle(MainScope())


fun main() {
    launchSendBroadcasts()
    launchReceiveBroadcasts()
    launchReceiveHeartRates()
    CFRunLoopRun()
}

private fun launchSendBroadcasts() = MainScope().launch {
    MainScope().launch(Dispatchers.Default) {
        val user = User(
            isMe = true,
            id = UserId(2412),
            name = "Felix Werner"
        )

        val pacemakerBle = PacemakerBle(ble)
        pacemakerBle.updateUser(user)

        while (isActive) {
            val line = readln()
            if (line.startsWith("l")) {
                val heartRateLimit = line.removePrefix("l").toIntOrNull() ?: continue
                pacemakerBle.updateHeartRateLimit(HeartRate(heartRateLimit))
                println("Updated spoof hr-limit: $heartRateLimit")
            } else {
                pacemakerBle.updateHeartHeart(
                    HeartRateSensorId("👻"), HeartRate(line.toIntOrNull() ?: continue)
                )
                println("Updated spoof hr: ${line.toIntOrNull()}")
            }
        }

    }
}

private fun launchReceiveBroadcasts() = MainScope().launch(Dispatchers.Default) {
    val pacemakerBle = PacemakerBle(ble)
    pacemakerBle.connections.flatMapMerge { connection ->
        println("Found pacemaker peripheral: ${connection.id}")
        connection.receivePacemakerBroadcastPackages()
    }.collect { pkg ->
        println("${pkg.userName}: ${pkg.heartRate.value.roundToInt()}/${pkg.heartRateLimit.value.roundToInt()}")
    }
}

private fun launchReceiveHeartRates() = MainScope().launch(Dispatchers.Default) {/*
    Ble(this).receiveHeartRateMeasurements().collect { measurement ->
        println("HR: ${measurement.heartRate} | Device: ${measurement.sensorInfo.id}")
    }*/
}
