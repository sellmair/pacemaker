package io.sellmair.pacemaker.wear

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import io.sellmair.pacemaker.WATCH_COMPANION_MESSAGE_PATH
import io.sellmair.pacemaker.WatchCompanionCodec
import io.sellmair.pacemaker.WatchCompanionMessage
import io.sellmair.pacemaker.WatchCompanionDataLayer
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.ExerciseTrackedStatus

/** Process-local fanout. Live HR and group messages are never written to disk. */
internal object WearCompanionRuntime {
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<(String, WatchCompanionMessage) -> Unit>()
    private val transports = CopyOnWriteArraySet<WatchCompanionDataLayer>()
    private const val PREFS = "wear-companion"
    private val lastAlerts = mutableMapOf<String, Long>()
    private var freshnessTimerStarted = false
    private val freshnessTick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val current = WearWorkoutUiState.state.value
            val members = current.group.map { member ->
                if (member.measuredAtEpochMillis == null || now - member.measuredAtEpochMillis !in 0..60_000)
                    member.copy(bpm = null) else member
            }
            if (members != current.group) WearWorkoutUiState.state.value = current.copy(group = members)
            main.postDelayed(this, 5_000)
        }
    }

    fun addListener(listener: (String, WatchCompanionMessage) -> Unit) { listeners += listener }
    fun removeListener(listener: (String, WatchCompanionMessage) -> Unit) { listeners -= listener }
    fun addTransport(transport: WatchCompanionDataLayer) { transports += transport }
    fun removeTransport(transport: WatchCompanionDataLayer) { transports -= transport }

    fun alertIfCritical(context: Context, userId: String, bpm: Int, limit: Int?) {
        if (limit == null || bpm < limit) return
        val now = System.currentTimeMillis()
        if (now - (lastAlerts[userId] ?: 0L) < 30_000) return
        lastAlerts[userId] = now
        context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(140, 180))
    }

    fun receive(context: Context, nodeId: String, message: WatchCompanionMessage) {
        val selectedPhone = context.getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
            .getString("phone-node-id", null)
        if (nodeId != selectedPhone) return
        main.post {
        transports.forEach { it.onMessage(nodeId, message) }
        listeners.forEach { it(nodeId, message) }
        when (message) {
            is WatchCompanionMessage.Configure -> {
                context.getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                    .putString("userId", message.userId).putString("name", message.userName)
                    .putInt("limit", message.heartRateLimit).putFloat("hue", message.colorHue)
                    .putBoolean("source-selected", message.sourceSelected).apply()
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                    userId = message.userId, userName = message.userName,
                    heartRateLimit = message.heartRateLimit, colorHue = message.colorHue,
                    sourceSelected = message.sourceSelected
                )
            }
            is WatchCompanionMessage.GroupSnapshot -> {
                if (!freshnessTimerStarted) {
                    freshnessTimerStarted = true
                    main.postDelayed(freshnessTick, 5_000)
                }
                val now = System.currentTimeMillis()
                val members = message.members.map { member ->
                    val fresh = member.measuredAtEpochMillis?.let { now - it in 0..60_000 } == true
                    WearGroupMember(member.userId, member.name, member.bpm.takeIf { fresh }, member.limit, member.measuredAtEpochMillis)
                }
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(group = members)
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
        val selected = getSharedPreferences("wear-companion", Context.MODE_PRIVATE).getString("phone-node-id", null)
        if (selected != event.sourceNodeId) return
        if (message is WatchCompanionMessage.MeasurementStop) {
            if (!WearWorkoutService.isWorkoutActiveOrEnding()) {
                val future = HealthServices.getClient(this).exerciseClient.getCurrentExerciseInfoAsync()
                future.addListener({
                    val isPacemakerWorkout = runCatching { future.get()?.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS }
                        .getOrDefault(false)
                    if (isPacemakerWorkout) startMeasurementStopService(event, message)
                    else {
                        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                            .putLong("measurement-stop-generation", maxOf(
                                getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                                    .getLong("measurement-stop-generation", 0L),
                                message.generation
                            )).apply()
                        sendAck(event.sourceNodeId, message.messageId, true, message.measurementRunId)
                    }
                }, ContextCompat.getMainExecutor(this))
                return
            }
            startMeasurementStopService(event, message)
        } else {
            WearCompanionRuntime.receive(this, event.sourceNodeId, message)
        }
    }

    private fun startMeasurementStopService(event: MessageEvent, message: WatchCompanionMessage.MeasurementStop) {
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

    private fun sendAck(nodeId: String, messageId: String, accepted: Boolean, measurementRunId: String? = null) {
        val ack = WatchCompanionCodec.encode(WatchCompanionMessage.Ack(messageId, accepted, measurementRunId))
        runCatching { com.google.android.gms.wearable.Wearable.getMessageClient(this)
            .sendMessage(nodeId, WATCH_COMPANION_MESSAGE_PATH, ack) }
    }
}
