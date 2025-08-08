@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleReceivedValue
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.bluetooth.PacemakerServiceDescriptors.heartRateCharacteristic
import io.sellmair.pacemaker.bluetooth.PacemakerServiceDescriptors.heartRateLimitCharacteristic
import io.sellmair.pacemaker.bluetooth.PacemakerServiceDescriptors.userColorHueCharacteristic
import io.sellmair.pacemaker.bluetooth.PacemakerServiceDescriptors.userIdCharacteristic
import io.sellmair.pacemaker.bluetooth.PacemakerServiceDescriptors.userNameCharacteristic
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Hue
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.error
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlin.time.Clock

fun PacemakerBluetoothService.broadcastPackages(): Flow<PacemakerBroadcastPackage> {
    return newConnections.flatMapMerge { connection -> connection.broadcastPackages() }
}

fun PacemakerBluetoothConnection.broadcastPackages(): Flow<PacemakerBroadcastPackage> = receivedValues
    .broadcastPackages()

@OptIn(ExperimentalStdlibApi::class)
internal fun Flow<BleReceivedValue>.broadcastPackages(): Flow<PacemakerBroadcastPackage> = channelFlow {
    val logTag = LogTag.ble.with("decode PacemakerBroadcastPackage")

    class State(
        val deviceId: BleDeviceId,
        var userId: UserId? = null,
        var userName: String? = null,
        var userColorHue: Hue? = null,
        var heartRate: HeartRate? = null,
        var heartRateLimit: HeartRate? = null
    )

    val states = mutableMapOf<BleDeviceId, State>()

    fun stateOf(deviceId: BleDeviceId): State {
        return states.getOrPut(deviceId) { State(deviceId) }
    }

    suspend fun State.emitIfPossible() {
        send(
            PacemakerBroadcastPackage(
                receivedTime = Clock.System.now(),
                deviceId = deviceId,
                userId = userId ?: return,
                userName = userName ?: "n/a",
                heartRate = heartRate ?: return,
                heartRateLimit = heartRateLimit ?: return,
                userColorHue = userColorHue,
            )
        )
    }

    collect { value ->
        with(stateOf(value.deviceId)) {
            when (value.characteristic) {
                userIdCharacteristic -> {
                    userId = runCatching { UserId(value.data) }.getOrNull()
                    if (userId == null) {
                        logTag.error("Failed decoding userId (${value.data.toHexString()})")
                    }
                    emitIfPossible()
                }

                userNameCharacteristic -> {
                    userName = runCatching { value.data.decodeToString() }.getOrNull()
                    if (userName == null) {
                        logTag.error("Failed decoding userName (${value.data.toHexString()})")
                    }
                    emitIfPossible()
                }

                heartRateCharacteristic -> {
                    heartRate = HeartRate(value.data)
                    if (heartRate == null) {
                        logTag.error("Failed decoding heartRate (${value.data.toHexString()})")
                    }
                    emitIfPossible()
                }

                heartRateLimitCharacteristic -> {
                    heartRateLimit = HeartRate(value.data)
                    if (heartRateLimit == null) {
                        logTag.error("Failed decoding heartRateLimit (${value.data.toHexString()})")
                    }
                    emitIfPossible()
                }

                userColorHueCharacteristic -> {
                    userColorHue = Hue(value.data)
                    if (userColorHue == null) {
                        logTag.error("Failed decoding userColorHue (${value.data.toHexString()}")
                    }
                    emitIfPossible()
                }
            }
        }
    }
}
