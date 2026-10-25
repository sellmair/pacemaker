package io.sellmair.pacemaker.wear

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import io.sellmair.pacemaker.WATCH_COMPANION_MESSAGE_PATH
import io.sellmair.pacemaker.WatchCompanionCodec
import io.sellmair.pacemaker.WatchCompanionMessage
import io.sellmair.pacemaker.WatchCompanionTransportAck
import io.sellmair.evas.Events
import io.sellmair.evas.States
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.ExerciseTrackedStatus
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Process-local watch state; the system listener applies messages even while the UI is closed. */
internal object WearCompanionRuntime {
    private val main = Handler(Looper.getMainLooper())
    val events = Events()
    val states = States()
    private val lastAlerts = mutableMapOf<String, Long>()

    /** The visible Activity or active workout service owns this timer and cancels it on exit. */
    suspend fun expireGroupWhileActive() {
        while (currentCoroutineContext().isActive) {
            delay(5_000)
            val now = System.currentTimeMillis()
            val current = states.workout
            val members = current.group.map { member ->
                if (member.measuredAtEpochMillis == null || now - member.measuredAtEpochMillis !in 0..60_000)
                    member.copy(bpm = null) else member
            }
            if (members != current.group) states.workout = current.copy(group = members)
        }
    }


    fun alertIfCritical(context: Context, userId: String, bpm: Int, limit: Int?) {
        if (limit == null || bpm < limit) return
        val now = System.currentTimeMillis()
        if (now - (lastAlerts[userId] ?: 0L) < 30_000) return
        lastAlerts[userId] = now
        context.getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(140, 180))
    }

    fun receive(context: Context, nodeId: String, message: WatchCompanionMessage) {
        main.post {
            val selectedPhone = context.getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                .getString("phone-node-id", null)
            if (nodeId != selectedPhone) return@post
            if (message is WatchCompanionMessage.Ack) events.emitAsync(
                WatchCompanionTransportAck(
                    nodeId,
                    message
                )
            )
            when (message) {
                is WatchCompanionMessage.Configure -> {
                    context.getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                        .putString("userId", message.userId).putString("name", message.userName)
                        .putInt("limit", message.heartRateLimit).putFloat("hue", message.colorHue)
                        .putBoolean("source-selected", message.sourceSelected).apply()
                    states.workout = states.workout.copy(
                        userId = message.userId, userName = message.userName,
                        heartRateLimit = message.heartRateLimit, colorHue = message.colorHue,
                        sourceSelected = message.sourceSelected
                    )
                }

                is WatchCompanionMessage.GroupSnapshot -> {
                    val now = System.currentTimeMillis()
                    val members = message.members.map { member ->
                        val fresh =
                            member.measuredAtEpochMillis?.let { now - it in 0..60_000 } == true
                        WearGroupMember(
                            member.userId,
                            member.name,
                            member.bpm.takeIf { fresh },
                            member.limit,
                            member.measuredAtEpochMillis
                        )
                    }
                    states.workout = states.workout.copy(group = members)
                    members.filter { it.isCritical }.forEach { member ->
                        alertIfCritical(context, member.userId, member.bpm!!, member.limit)
                    }
                }

                else -> Unit
            }
        }
    }
}

/** Android system wake-up endpoint for phone-to-watch messages while the UI is closed. */
class WearCompanionListenerService : WearableListenerService() {
    // Health Services exposes this workout ownership status for the required stop check,
    // although its RestrictTo annotation currently causes a false-positive lint error.
    @Suppress("RestrictedApi")
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WATCH_COMPANION_MESSAGE_PATH) return
        val message = WatchCompanionCodec.decode(event.data) ?: return
        val selected = getSharedPreferences("wear-companion", Context.MODE_PRIVATE).getString(
            "phone-node-id",
            null
        )
        if (selected != event.sourceNodeId) return
        if (message is WatchCompanionMessage.MeasurementStop) {
            if (!WearWorkoutService.isWorkoutActiveOrEnding()) {
                val future =
                    HealthServices.getClient(this).exerciseClient.getCurrentExerciseInfoAsync()
                future.addListener({
                    val isPacemakerWorkout =
                        runCatching { future.get()?.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS }
                            .getOrDefault(false)
                    if (isPacemakerWorkout) startMeasurementStopService(event, message)
                    else {
                        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                            .putLong(
                                "measurement-stop-generation", maxOf(
                                    getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                                        .getLong("measurement-stop-generation", 0L),
                                    message.generation
                                )
                            ).apply()
                        sendAck(
                            event.sourceNodeId,
                            message.messageId,
                            true,
                            message.measurementRunId
                        )
                    }
                }, ContextCompat.getMainExecutor(this))
                return
            }
            startMeasurementStopService(event, message)
        } else {
            WearCompanionRuntime.receive(this, event.sourceNodeId, message)
        }
    }

    private fun startMeasurementStopService(
        event: MessageEvent,
        message: WatchCompanionMessage.MeasurementStop
    ) {
        val serviceIntent = Intent(this, WearWorkoutService::class.java)
            .setAction(WearWorkoutService.ACTION_MEASUREMENT_STOP)
            .putExtra(WearWorkoutService.EXTRA_NODE_ID, event.sourceNodeId)
            .putExtra(WearWorkoutService.EXTRA_MESSAGE_ID, message.messageId)
            .putExtra(WearWorkoutService.EXTRA_RUN_ID, message.measurementRunId)
            .putExtra(WearWorkoutService.EXTRA_GENERATION, message.generation)
        try {
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (_: RuntimeException) {
            sendAck(event.sourceNodeId, message.messageId, false)
        }
    }

    private fun sendAck(
        nodeId: String,
        messageId: String,
        accepted: Boolean,
        measurementRunId: String? = null
    ) {
        val ack = WatchCompanionCodec.encode(
            WatchCompanionMessage.Ack(
                messageId,
                accepted,
                measurementRunId
            )
        )
        runCatching {
            com.google.android.gms.wearable.Wearable.getMessageClient(this)
                .sendMessage(nodeId, WATCH_COMPANION_MESSAGE_PATH, ack)
        }
    }
}
