package io.sellmair.broadheart.spoof

import io.sellmair.broadheart.bluetooth.BroadheartBluetoothSender
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.UserId
import kotlinx.coroutines.*
import platform.CoreFoundation.CFRunLoopRun

var sender: BroadheartBluetoothSender? = null
fun main() {
    MainScope().launch(Dispatchers.Main) {
        val user = User(
            isMe = true,
            id = UserId(2412),
            name = "Felix Werner"
        )

        sender = BroadheartBluetoothSender(user)

        withContext(Dispatchers.Default) {
            while (isActive) {
                print("Heart Rate: ")
                sender?.updateHeartHeart(
                    HeartRateSensorId("spoof-sensor"),
                    HeartRate(readln().toIntOrNull() ?: continue)
                )

                print("Heart Rate Limit: ")
                sender?.updateHeartRateLimit(HeartRate(readln().toIntOrNull() ?: continue))
            }
        }
    }

    CFRunLoopRun()
}
