@file:Suppress("OPT_IN_USAGE")

package io.sellmair.broadheart.spoof

import io.sellmair.broadheart.bluetooth.DarwinBle
import io.sellmair.broadheart.bluetooth.HeartcastBluetoothSender
import io.sellmair.broadheart.bluetooth.startHeartcastBleCentralService
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.UserId
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

        val sender = HeartcastBluetoothSender(ble)
        sender.updateUser(user)

        while (isActive) {
            val line = readln()
            if (line.startsWith("l")) {
                val heartRateLimit = line.removePrefix("l").toIntOrNull() ?: continue
                sender.updateHeartRateLimit(HeartRate(heartRateLimit))
                println("Updated spoof hr-limit: $heartRateLimit")
            } else {
                sender.updateHeartHeart(
                    HeartRateSensorId("👻"), HeartRate(line.toIntOrNull() ?: continue)
                )
                println("Updated spoof hr: ${line.toIntOrNull()}")
            }
        }

    }
}

private fun launchReceiveBroadcasts() = MainScope().launch(Dispatchers.Default) {
    ble.startHeartcastBleCentralService().peripherals.flatMapMerge { peripheral ->
        peripheral.tryConnect()
        println("Found heartcast peripheral: ${peripheral.id}")
        peripheral.broadcasts
    }.collect { pkg ->
        println("${pkg.userName}: ${pkg.heartRate.value.roundToInt()}/${pkg.heartRateLimit.value.roundToInt()}")
    }
}

private fun launchReceiveHeartRates() = MainScope().launch(Dispatchers.Default) {/*
    Ble(this).receiveHeartRateMeasurements().collect { measurement ->
        println("HR: ${measurement.heartRate} | Device: ${measurement.sensorInfo.id}")
    }*/
}
