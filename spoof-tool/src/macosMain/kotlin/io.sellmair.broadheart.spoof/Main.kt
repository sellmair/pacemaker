package io.sellmair.pacemaker.spoof

import io.sellmair.pacemaker.ble.AppleBle
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.Configuration
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.CoreFoundation.CFRunLoopRun


private val ble = with(Configuration()) {
    AppleBle()
}


fun main() {
    //launchSendBroadcasts()
    launchReceiveBroadcasts()
    /* launchReceiveHeartRates() */
    CFRunLoopRun()
}

private fun launchSendBroadcasts() = MainScope().launch {
    MainScope().launch(Dispatchers.Default) {
        val user = User(
            isMe = true,
            id = UserId(2412),
            name = "Felix Werner"
        )

        val pacemakerPeripheral = PacemakerBluetoothService(ble)
        pacemakerPeripheral.write {
            setUser(user)
            setHeartRateLimit(HeartRate(120))
        }


        while (isActive) {
            val line = readln()
            if (line.startsWith("l")) {
                val heartRateLimit = line.removePrefix("l").toIntOrNull() ?: continue
                pacemakerPeripheral.write {
                    setHeartRateLimit(HeartRate(heartRateLimit))
                    println("Updated spoof hr-limit: $heartRateLimit")
                }

            } else {
                pacemakerPeripheral.write {
                    setHeartRate(HeartRateSensorId("👻"), HeartRate(line.toIntOrNull() ?: return@write))
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