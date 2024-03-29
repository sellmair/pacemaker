package io.sellmair.pacemaker.spoof

import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.*
import platform.CoreFoundation.CFRunLoopRun


private val ble = AppleBle()


fun main() {
    launchSendBroadcasts()
    //launchReceiveBroadcasts()
    /* launchReceiveHeartRates() */
    CFRunLoopRun()
}

private fun launchSendBroadcasts() = MainScope().launch {
    MainScope().launch(Dispatchers.Default) {
        val user = User(
            id = UserId(2412),
            name = "Felix Werner"
        )

        val pacemakerPeripheral = PacemakerBluetoothService(ble)
        pacemakerPeripheral.write {
            setUser(user)
            setHeartRateLimit(HeartRate(120))
            setHeartRate(HeartRate(130))
        }


        while (isActive) {
            yield()
            val line = readln()
            if (line.startsWith("l")) {
                val heartRateLimit = line.removePrefix("l").toIntOrNull() ?: continue
                pacemakerPeripheral.write {
                    setHeartRateLimit(HeartRate(heartRateLimit))
                    println("Updated spoof hr-limit: $heartRateLimit")
                }

            } else {
                pacemakerPeripheral.write {
                    setHeartRate(HeartRate(line.toIntOrNull() ?: return@write))
                    println("Updated spoof hr: ${line.toIntOrNull()}")
                }
            }
        }
    }
}

private fun launchReceiveBroadcasts() = MainScope().launch(Dispatchers.Default) {
    val pacemaker = PacemakerBluetoothService(ble)
    pacemaker.newConnections.collect { connection ->
        LogTag("spoof").info("Received connection ${connection.deviceId}")
    }
}

/*
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


 */