package io.sellmair.pacemaker.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.SampleDataPoint
import io.sellmair.evas.emit
import io.sellmair.evas.flow
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.WatchCompanionMessage
import io.sellmair.pacemaker.WatchCompanionDataLayer
import io.sellmair.pacemaker.WATCH_PHONE_CAPABILITY
import io.sellmair.pacemaker.WatchCompanionCodec
import io.sellmair.pacemaker.awaitPacemaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.isActive
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

data class WearWorkoutState(
    val isRunning: Boolean = false,
    val isStarting: Boolean = false,
    val companionConnected: Boolean = false,
    val sourceSelected: Boolean = false,
    val heartRate: Int? = null,
    val status: String = "Ready",
    val userId: String = "",
    val userName: String = "Pacemaker",
    val colorHue: Float = 0f,
    val heartRateLimit: Int? = null,
    val group: List<WearGroupMember> = emptyList(),
    val measuredAtEpochMillis: Long? = null
)

data class WearGroupMember(val userId: String, val name: String, val bpm: Int?, val limit: Int?, val measuredAtEpochMillis: Long?) {
    val isCritical get() = bpm != null && limit != null && bpm >= limit
}

object WearWorkoutUiState {
    val state = kotlinx.coroutines.flow.MutableStateFlow(WearWorkoutState())
}

class WearWorkoutService : Service(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main.immediate + SupervisorJob()
    private val exerciseClient: ExerciseClient by lazy { HealthServices.getClient(this).exerciseClient }
    private var companionClient: WatchCompanionDataLayer? = null
    private val sessionCommandGate = io.sellmair.pacemaker.WatchSessionCommandGate()
    private val pendingPhoneSessionCommands = mutableSetOf<String>()
    private var runId = ""
    private var sessionRevision = 0L
    private var sessionSourcePhone = false
    private var sessionCommandDrainJob: Job? = null
    private var heartRateExpiryJob: Job? = null
    private var startingWorkout = false
    private var exerciseStartInFlight = false
    private var companionEnabled = false
    private var groupExpiryJob: Job? = null
    private var workoutGeneration = 0L
    private var registeredUpdateCallback: ExerciseUpdateCallback? = null

    private fun updateCallbackFor(generation: Long) = object : ExerciseUpdateCallback {
        override fun onRegistered() = Unit
        override fun onRegistrationFailed(throwable: Throwable) {
            if (generation != workoutGeneration) return
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                isRunning = false, heartRate = null, status = "Heart rate sensor unavailable"
            )
            startingWorkout = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            if (generation != workoutGeneration) return
            val point = update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()
            val bpm = point?.value?.toInt()
            val sampleTime = (point as? SampleDataPoint<*>)?.timeDurationFromBoot?.toMillis()
            if (bpm != null && bpm in 25..240 && sampleTime != null) {
                val measuredAt = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime() + sampleTime
                onHeartRate(bpm, measuredAt, generation)
            }
            if (update.exerciseStateInfo.state.isEnded) {
                stopWorkout(endExercise = false)
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
            .remove("session-outbox-v1").remove("session-attempted-v1")
            .remove("phone-stopped-runs").apply()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MEASUREMENT_STOP -> {
                val nodeId = intent.getStringExtra(EXTRA_NODE_ID)
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
                val stopGeneration = intent.getLongExtra(EXTRA_GENERATION, 0L)
                val targetRunId = intent.getStringExtra(EXTRA_RUN_ID)
                if (nodeId != null && messageId != null) {
                    val prefs = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                    val previousStop = prefs.getLong("measurement-stop-generation", 0L)
                    val activeRun = prefs.getString("active-measurement-run-id", null)
                    val gate = io.sellmair.pacemaker.WatchMeasurementRunGate(activeRun, previousStop)
                    if (!gate.shouldStop(targetRunId, stopGeneration)) {
                        if (stopGeneration > previousStop) prefs.edit().putLong("measurement-stop-generation", stopGeneration).apply()
                        val anotherRunIsActive = activeServiceExercise || startingWorkout || exerciseStartInFlight
                        startPhoneTransport(false, null, null)
                        launch {
                            companionClient?.sendTo(nodeId, WatchCompanionMessage.Ack(messageId, true, targetRunId))
                            if (!anotherRunIsActive) stopSelf()
                        }
                        return START_NOT_STICKY
                    }
                    val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification("Stopping heart rate"), type)
                    startPhoneTransport(false, null, null)
                    val stoppedRunId = targetRunId ?: activeRun
                    stopWorkout(sendToPhone = false) { stopped ->
                        launch {
                            if (stopped) prefs.edit().putLong("measurement-stop-generation", stopGeneration).apply()
                            companionClient?.sendTo(nodeId, WatchCompanionMessage.Ack(
                                messageId, stopped, stoppedRunId.takeIf { stopped }
                            ))
                            if (stopped) stopSelf()
                        }
                    }
                }
            }
            ACTION_START -> startWorkout(
                fromPhone = intent.getBooleanExtra(EXTRA_FROM_PHONE, false),
                phoneRunId = intent.getStringExtra(EXTRA_RUN_ID),
                phoneRevision = intent.getLongExtra(EXTRA_REVISION, 0L).takeIf { it > 0 }
            )
            ACTION_STOP -> stopWorkout()
        }
        return START_STICKY
    }

    @Suppress("RestrictedApi") // Health Services exposes this ownership status for safe workout adoption.
    private fun startWorkout(fromPhone: Boolean = false, phoneRunId: String? = null, phoneRevision: Long? = null,
                             completion: (Boolean) -> Unit = {}) {
        if (fromPhone && !hasBackgroundHealthPermission()) {
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                isStarting = false, status = "Open watch to allow heart rate access"
            )
            completion(false)
            launch { delay(500); stopSelf() }
            return
        }
        if (WearWorkoutUiState.state.value.isRunning || startingWorkout || exerciseStartInFlight) {
            completion(WearWorkoutUiState.state.value.isRunning && runId == phoneRunId)
            return
        }
        if (!fromPhone && (!WearWorkoutUiState.state.value.companionConnected || WearWorkoutUiState.state.value.userId.isBlank())) {
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(status = "Open Pacemaker on your phone to connect")
            completion(false)
            return
        }
        val generation = ++workoutGeneration
        runId = java.util.UUID.randomUUID().toString()
        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
            .putString("active-measurement-run-id", runId).apply()
        activeServiceExercise = true
        startingWorkout = true
        WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
            status = "Checking heart rate", isStarting = true
        )
        val foregroundType = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification("Starting Pacemaker"), foregroundType)

        val capabilitiesFuture = exerciseClient.getCapabilitiesAsync()
        val exerciseInfoFuture = exerciseClient.getCurrentExerciseInfoAsync()
        exerciseInfoFuture.addListener({
            if (generation != workoutGeneration) { completion(false); return@addListener }
            val info = runCatching { exerciseInfoFuture.get() }.getOrNull()
            if (info == null) {
                startingWorkout = false
                activeServiceExercise = false
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                    isStarting = false, isRunning = false, status = "Workout status unavailable"
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                completion(false)
                return@addListener
            }
            if (info?.exerciseTrackedStatus == ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS) {
                startingWorkout = false
                activeServiceExercise = false
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                    status = "Stop the other workout first", isRunning = false, isStarting = false
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                completion(false)
                return@addListener
            }
            if (info?.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
                startingWorkout = false
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                    status = "Pacemaker workout already active", isRunning = true, isStarting = false
                )
                val callback = updateCallbackFor(generation)
                registeredUpdateCallback = callback
                exerciseClient.setUpdateCallback(callback)
                if (fromPhone && phoneRunId != null) {
                    runId = phoneRunId
                    sessionRevision = phoneRevision ?: 1L
                    sessionSourcePhone = true
                }
                startPhoneTransport(fromPhone, phoneRunId, phoneRevision)
                completion(true)
                return@addListener
            }
            capabilitiesFuture.addListener({
                if (generation != workoutGeneration) { completion(false); return@addListener }
                val capabilities = runCatching { capabilitiesFuture.get() }.getOrNull()
                val workoutType = listOf(ExerciseType.RUNNING, ExerciseType.WALKING, ExerciseType.WORKOUT)
                    .firstOrNull { type ->
                        capabilities?.typeToCapabilities?.get(type)?.supportedDataTypes?.contains(DataType.HEART_RATE_BPM) == true
                    }
                if (workoutType == null) {
                    startingWorkout = false
                    activeServiceExercise = false
                    getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit().remove("active-measurement-run-id").apply()
                    getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit().remove("active-measurement-run-id").apply()
                    WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                        isRunning = false, isStarting = false, status = "Heart rate sensor unavailable"
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    completion(false)
                    return@addListener
                }
                val callback = updateCallbackFor(generation)
                registeredUpdateCallback = callback
                exerciseClient.setUpdateCallback(callback)
                val config = ExerciseConfig.builder(workoutType)
                    .setDataTypes(setOf(DataType.HEART_RATE_BPM))
                    .setIsAutoPauseAndResumeEnabled(false)
                    .setIsGpsEnabled(false)
                    .build()
                val startFuture = exerciseClient.startExerciseAsync(config)
                exerciseStartInFlight = true
                startFuture.addListener({
                exerciseStartInFlight = false
                if (generation != workoutGeneration) {
                    if (runCatching { startFuture.get() }.isSuccess) exerciseClient.endExerciseAsync()
                    getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit().remove("active-measurement-run-id").apply()
                    completion(false)
                    return@addListener
                }
                if (runCatching { startFuture.get() }.isFailure) {
                    startingWorkout = false
                    activeServiceExercise = false
                    getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit().remove("active-measurement-run-id").apply()
                    WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                        isRunning = false, isStarting = false, status = "Could not start workout"
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    completion(false)
                    return@addListener
                }
                startingWorkout = false
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                    isRunning = true, isStarting = false, status = "With phone"
                )
                startPhoneTransport(fromPhone, phoneRunId, phoneRevision)
                completion(true)
                }, ContextCompat.getMainExecutor(this))
            }, ContextCompat.getMainExecutor(this))
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startPhoneTransport(fromPhone: Boolean, phoneRunId: String?, phoneRevision: Long?) {
        companionEnabled = true
        if (fromPhone && phoneRunId != null) {
            runId = phoneRunId
            sessionRevision = phoneRevision ?: 1L
            sessionSourcePhone = true
        }
        if (companionClient == null) {
            val prefs = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
            companionClient = WatchCompanionDataLayer(
                this, this, WATCH_PHONE_CAPABILITY,
                selectedNodeId = { prefs.getString("phone-node-id", null) },
                onPresenceChanged = { state ->
                    WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                        companionConnected = state.connected,
                        status = if (state.connected) "With phone" else "Phone connection lost"
                    )
                },
                onNodeAvailable = { Unit }
            ).also { it.start(); WearCompanionRuntime.addTransport(it) }
            WearCompanionRuntime.addListener(::onCompanionMessage)
        }
        companionEnabled = true
        launch { companionClient?.refreshPresence() }
    }

    private fun onHeartRate(bpm: Int, measuredAt: Long) {
        onHeartRate(bpm, measuredAt, workoutGeneration)
    }

    private fun onHeartRate(bpm: Int, measuredAt: Long, generation: Long) {
        if (generation != workoutGeneration || (!startingWorkout && !WearWorkoutUiState.state.value.isRunning)) return
        val age = System.currentTimeMillis() - measuredAt
        if (age !in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds) return
        heartRateExpiryJob?.cancel()
        heartRateExpiryJob = launch {
            delay((HeartRateSource.MAX_AGE.inWholeMilliseconds - age).coerceAtLeast(0))
            val current = WearWorkoutUiState.state.value
            if (current.measuredAtEpochMillis == measuredAt) {
                WearWorkoutUiState.state.value = current.copy(heartRate = null, status = "Heart rate unavailable")
            }
        }
        WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
            isRunning = true, heartRate = bpm,
            measuredAtEpochMillis = measuredAt,
            status = "With phone"
        )
        val identity = WearWorkoutUiState.state.value
        if (identity.sourceSelected && identity.userId.isNotBlank()) launch { companionClient?.send(WatchCompanionMessage.HeartRate(
            messageId = "hr-${java.util.UUID.randomUUID()}", userId = identity.userId, bpm = bpm,
            measuredAtEpochMillis = measuredAt, measurementRunId = runId
        )) }
        alertIfCritical("me", bpm, identity.heartRateLimit)
    }

    private fun onCompanionMessage(nodeId: String, message: WatchCompanionMessage) {
        when (message) {
            is WatchCompanionMessage.Configure -> {
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                    userId = message.userId, userName = message.userName, heartRateLimit = message.heartRateLimit,
                    colorHue = message.colorHue, sourceSelected = message.sourceSelected
                )
                getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                    .putString("userId", message.userId).putString("name", message.userName)
                    .putInt("limit", message.heartRateLimit).putFloat("hue", message.colorHue)
                    .putBoolean("source-selected", message.sourceSelected).apply()
            }
            is WatchCompanionMessage.GroupSnapshot -> {
                val now = Clock.System.now().toEpochMilliseconds()
                val members = message.members.map { WearGroupMember(it.userId, it.name, it.bpm, it.limit, it.measuredAtEpochMillis) }
                    .map { member -> if (member.measuredAtEpochMillis == null || now - member.measuredAtEpochMillis !in 0..60_000) member.copy(bpm = null) else member }
                WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(group = members)
                members.filter { it.isCritical }.forEach { alertIfCritical(it.userId, it.bpm!!, it.limit) }
                groupExpiryJob?.cancel()
                groupExpiryJob = launch {
                    while (isActive && companionEnabled) {
                        delay(5_000)
                        val nowMs = System.currentTimeMillis()
                        val rows = WearWorkoutUiState.state.value.group.map { member ->
                            if (member.measuredAtEpochMillis == null || nowMs - member.measuredAtEpochMillis !in 0..60_000)
                                member.copy(bpm = null) else member
                        }
                        WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(group = rows)
                    }
                }
            }
            is WatchCompanionMessage.Hello -> Unit
            is WatchCompanionMessage.Session -> Unit
            is WatchCompanionMessage.MeasurementStop -> Unit
            is WatchCompanionMessage.Ack -> companionClient?.onMessage(nodeId, message)
            is WatchCompanionMessage.HeartRate -> Unit
        }
    }

    private fun acknowledgeStop(nodeId: String, messageId: String, accepted: Boolean) {
        launch {
            companionClient?.sendTo(nodeId, WatchCompanionMessage.Ack(messageId, accepted))
            delay(300)
            stopSelf()
        }
    }

    private fun alertIfCritical(userId: String, bpm: Int, limit: Int?) {
        WearCompanionRuntime.alertIfCritical(this, userId, bpm, limit)
    }

    private fun stopHeartRateTransport() {
        companionEnabled = false
        groupExpiryJob?.cancel()
        companionClient?.let { WearCompanionRuntime.removeTransport(it); it.close() }
        companionClient?.close()
        WearCompanionRuntime.removeListener(::onCompanionMessage)
        companionClient = null
    }

    @Suppress("RestrictedApi") // Health Services exposes this ownership status for safely ending an orphan workout.
    private fun stopWorkout(sendToPhone: Boolean = true, endExercise: Boolean = true, completion: ((Boolean) -> Unit)? = null) {
        if (sendToPhone) {
            val userId = WearWorkoutUiState.state.value.userId
            if (userId.isNotBlank()) launch { companionClient?.send(WatchCompanionMessage.HeartRate(
                messageId = "hr-clear-${java.util.UUID.randomUUID()}", userId = userId, bpm = null,
                measuredAtEpochMillis = Clock.System.now().toEpochMilliseconds(), measurementRunId = runId
            )) }
        }
        val wasRunning = WearWorkoutUiState.state.value.isRunning
        val endingFuture = if (endExercise && wasRunning) exerciseClient.endExerciseAsync() else null
        val shouldCheckOrphan = endExercise && !wasRunning && completion != null
        workoutGeneration++
        startingWorkout = false
        registeredUpdateCallback?.let { runCatching { exerciseClient.clearUpdateCallbackAsync(it) } }
        registeredUpdateCallback = null
        heartRateExpiryJob?.cancel()
        WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(isRunning = false, isStarting = false, heartRate = null, status = "Ready")
        fun finishStop(ended: Boolean) {
            activeServiceExercise = !ended
            if (ended) {
                getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                    .remove("active-measurement-run-id").apply()
                runId = ""
                stopForeground(STOP_FOREGROUND_REMOVE)
                completion?.invoke(true)
                if (completion == null) stopSelf()
                return
            }
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                isRunning = true, isStarting = false, status = "Could not stop workout"
            )
            val callback = updateCallbackFor(workoutGeneration)
            registeredUpdateCallback = callback
            exerciseClient.setUpdateCallback(callback)
            completion?.invoke(false)
        }
        if (endingFuture != null) {
            endingFuture.addListener({ finishStop(runCatching { endingFuture.get() }.isSuccess) }, ContextCompat.getMainExecutor(this))
        } else if (shouldCheckOrphan) {
            val infoFuture = exerciseClient.getCurrentExerciseInfoAsync()
            infoFuture.addListener({
                val info = runCatching { infoFuture.get() }.getOrNull()
                if (info == null) finishStop(false)
                else if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
                    val orphanStop = exerciseClient.endExerciseAsync()
                    orphanStop.addListener({ finishStop(runCatching { orphanStop.get() }.isSuccess) }, ContextCompat.getMainExecutor(this))
                } else finishStop(true)
            }, ContextCompat.getMainExecutor(this))
        } else finishStop(true)
    }

    private fun hasBackgroundHealthPermission(): Boolean {
        val heartRatePermission = if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE"
            else android.Manifest.permission.BODY_SENSORS
        if (ContextCompat.checkSelfPermission(this, heartRatePermission) != android.content.pm.PackageManager.PERMISSION_GRANTED) return false
        val backgroundPermission = when {
            Build.VERSION.SDK_INT >= 36 -> "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
            Build.VERSION.SDK_INT >= 33 -> android.Manifest.permission.BODY_SENSORS_BACKGROUND
            else -> null
        }
        return backgroundPermission == null || ContextCompat.checkSelfPermission(this, backgroundPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pacemaker workout", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("Pacemaker")
        .setContentText(text)
        .setOngoing(true)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        stopHeartRateTransport()
        cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile private var activeServiceExercise = false
        fun isWorkoutActiveOrEnding(): Boolean = activeServiceExercise

        const val ACTION_START = "io.sellmair.pacemaker.wear.START"
        const val ACTION_STOP = "io.sellmair.pacemaker.wear.STOP"
        const val ACTION_MEASUREMENT_STOP = "io.sellmair.pacemaker.wear.MEASUREMENT_STOP"
        const val EXTRA_NODE_ID = "nodeId"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_MESSAGE_ID = "messageId"
        const val EXTRA_GENERATION = "measurementStopGeneration"
        const val EXTRA_FROM_PHONE = "fromPhone"
        const val EXTRA_RUN_ID = "runId"
        const val EXTRA_REVISION = "revision"
        private const val NOTIFICATION_ID = 50
        private const val CHANNEL_ID = "pacemaker_watch_workout"
    }
}
