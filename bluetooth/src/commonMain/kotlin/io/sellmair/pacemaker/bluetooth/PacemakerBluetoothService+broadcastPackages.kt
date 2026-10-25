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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.yield
import kotlin.time.Clock

fun PacemakerBluetoothService.broadcastPackages(): Flow<PacemakerBroadcastPackage> {
    return newConnections.flatMapMerge { connection -> connection.broadcastPackages() }
}

fun PacemakerBluetoothConnection.broadcastPackages(): Flow<PacemakerBroadcastPackage> {
    val connectionJob = coroutineContext[Job]
    return channelFlow {
        coroutineScope {
            val receiver = launch(start = CoroutineStart.UNDISPATCHED) {
                receivedValues.broadcastPackages().collect { send(it) }
            }
            val reader = launch {
                // Let the decoder's undispatched collect register with the
                // replay=0 SharedFlow before the initial read responses arrive.
                yield()
                (this@broadcastPackages as? ReadablePacemakerBluetoothConnection)
                    ?.requestInitialRead()
            }
            val completionHandle = connectionJob?.invokeOnCompletion {
                receiver.cancel()
                reader.cancel()
            }
            try {
                receiver.join()
            } finally {
                reader.cancel()
                completionHandle?.dispose()
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
internal fun Flow<BleReceivedValue>.broadcastPackages(): Flow<PacemakerBroadcastPackage> = flow {
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
        emit(
            PacemakerBroadcastPackage(
                receivedTime = Clock.System.now(),
                deviceId = deviceId,
                userId = userId ?: return,
                // A connection may first report the numeric characteristics before
                // the name read/notification arrives. Emitting a placeholder here
                // makes the receiver persist "n/a" as the user's actual name.
                userName = userName?.takeIf(String::isNotBlank) ?: return,
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
                    val decoded = runCatching { UserId(value.data) }.getOrNull()
                    if (decoded == null) {
                        logTag.error("Failed decoding userId (${value.data.toHexString()})")
                        return@collect
                    } else {
                        if (userId != null && userId != decoded) {
                            // Characteristics belong to a user identity. Never combine
                            // a newly observed ID with the previous user's metadata.
                            userName = null
                            userColorHue = null
                            heartRate = null
                            heartRateLimit = null
                        }
                        userId = decoded
                    }
                    emitIfPossible()
                }

                userNameCharacteristic -> {
                    val decoded = runCatching { value.data.decodeToString() }.getOrNull()
                    if (decoded.isNullOrBlank()) {
                        logTag.error("Failed decoding userName (${value.data.toHexString()})")
                        return@collect
                    } else {
                        userName = decoded
                    }
                    emitIfPossible()
                }

                heartRateCharacteristic -> {
                    val decoded = HeartRate(value.data)
                    if (decoded == null) {
                        if (value.data.isEmpty()) {
                            heartRate = null
                        } else {
                            logTag.error("Failed decoding heartRate (${value.data.toHexString()})")
                        }
                        return@collect
                    } else {
                        heartRate = decoded
                    }
                    emitIfPossible()
                }

                heartRateLimitCharacteristic -> {
                    val decoded = HeartRate(value.data)
                    if (decoded == null) {
                        logTag.error("Failed decoding heartRateLimit (${value.data.toHexString()})")
                        return@collect
                    } else {
                        heartRateLimit = decoded
                    }
                    emitIfPossible()
                }

                userColorHueCharacteristic -> {
                    val decoded = Hue(value.data)
                    if (decoded == null) {
                        logTag.error("Failed decoding userColorHue (${value.data.toHexString()}")
                        return@collect
                    } else {
                        userColorHue = decoded
                    }
                    emitIfPossible()
                }
            }
        }
    }
}
