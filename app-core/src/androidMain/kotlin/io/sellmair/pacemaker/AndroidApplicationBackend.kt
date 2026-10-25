package io.sellmair.pacemaker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.sellmair.evas.*
import io.sellmair.pacemaker.ble.AndroidBle
import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

class AndroidApplicationBackend : Service(), ApplicationBackend, CoroutineScope {
    private data class PendingMeasurementStop(
        val nodeId: String,
        val messageId: String,
        val generation: Long,
        val measurementRunId: String?
    )

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob() + Events() + States() +
            AndroidContextProvider(this)

    inner class MainServiceBinder(
        override val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>,
        override val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>,
        override val userService: UserService,
        override val sessionService: SessionService,
        override val settings: Settings,
    ) : Binder(), ApplicationBackend {
        override val events get() = coroutineContext.eventsOrThrow
        override val states get() = coroutineContext.statesOrThrow
    }

    override val events: Events = coroutineContext.eventsOrThrow

    override val states: States = coroutineContext.statesOrThrow

    private var bleInstance: Ble? = null
    private val pendingMeasurementStopMutex = Mutex()
    private var pendingMeasurementStopRetryJob: Job? = null
    private val ble: Deferred<Ble> = async {
        AndroidBle(this@AndroidApplicationBackend).also { bleInstance = it }
    }

    override val pacemakerBluetoothService = async {
        PacemakerBluetoothService(ble.await())
    }

    override val heartRateSensorBluetoothService = async {
        HeartRateSensorBluetoothService(ble.await())
    }

    private val notification = AndroidHeartRateNotification(this)
    private lateinit var watchCompanionDataLayer: WatchCompanionDataLayer


    override val settings: Settings by lazy {
        SharedPreferencesSettings(getSharedPreferences("app", Context.MODE_PRIVATE))
    }

    private val meId by lazy { settings.meId }

    private val pacemakerDatabase = SafePacemakerDatabase {
        PacemakerDatabase(
            AndroidSqliteDriver(
                schema = PacemakerDatabase.Schema, context = this, name = "test.db"
            )
        )
    }

    override val userService: UserService by lazy {
        SqlUserService(pacemakerDatabase, meId)
    }

    override val sessionService: SessionService by lazy {
        SqlSessionService(pacemakerDatabase)
    }

    override fun onCreate() {
        super.onCreate()
        notification.startForeground(this)
        // Clear the legacy ownership bit during upgrade. Companion watches always belong
        // to this phone, so this value must never suppress phone broadcasts.
        getSharedPreferences("watch-companion", Context.MODE_PRIVATE).edit().remove("watch-owns-participant").apply()
        getSharedPreferences("watch-companion", Context.MODE_PRIVATE).edit()
            .remove("wear-address").remove("wear-name").remove("pair-window-until")
            .remove("pair-window-device").remove("blocked")
            .remove("last-published-run-id").remove("last-published-node-id")
            .remove("last-published-revision").remove("session-outbox-v1")
            .remove("session-attempted-v1").remove("wear-stopped-runs").apply()
        launchApplicationBackend(this)
        getSharedPreferences("app", Context.MODE_PRIVATE).edit()
            .remove("watch.import.name").remove("watch.import.limit").apply()
        watchCompanionDataLayer = WatchCompanionDataLayer(
            this, this, WATCH_COMPANION_CAPABILITY, ::selectedWatchNodeId,
            { state -> onWatchCompanionPresence(state) }, ::sendWatchCompanionSnapshot
        ).also { it.start() }
        launchWatchCompanionStatePublisher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DATA_LAYER_MESSAGE) {
            val peerId = intent.getStringExtra(EXTRA_NODE_ID)
            val payload = intent.getByteArrayExtra(EXTRA_PAYLOAD)
            if (peerId != null && payload != null) launch {
                val message = WatchCompanionCodec.decode(payload) ?: return@launch
                val accepted = handleWatchCompanionMessage(peerId, message)
                if (message is WatchCompanionMessage.Hello) {
                    watchCompanionDataLayer.sendTo(peerId, WatchCompanionMessage.Ack(message.messageId, accepted))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (::watchCompanionDataLayer.isInitialized) watchCompanionDataLayer.close()
        (bleInstance as? java.io.Closeable)?.close()
        super.onDestroy()
        cancel()
    }

    private fun selectedWatchNodeId(): String? = states.getState(SelectedHeartRateSourceState).value?.source
        ?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }?.companionDeviceId

    private suspend fun onWatchCompanionPresence(state: WatchCompanionDeviceState) {
        states.setState(WatchCompanionDeviceState, state)
        val prefs = getSharedPreferences("watch-companion", Context.MODE_PRIVATE)
        if (pendingMeasurementStops(prefs).any { pending -> state.candidates.any { it.id == pending.nodeId && it.connected } }) {
            drainPendingMeasurementStop()
        }
        val source = states.getState(SelectedHeartRateSourceState).value?.source
            ?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH } ?: return
        val selectedReachable = state.candidates.firstOrNull { it.id == source.companionDeviceId }
        val legacyCompanionId = isLegacyBluetoothCompanionId(source.companionDeviceId)
        if (selectedReachable == null && state.candidates.size == 1 && legacyCompanionId) {
            // Migrate the old MAC-based selection only when one OS-authenticated
            // companion app node is available. Never overwrite a chest strap source.
            events.emit(HeartRateSourceSelectionIntent(source.copy(
                companionDeviceId = state.candidates.single().id,
                connectionName = state.candidates.single().name,
                availability = if (state.candidates.single().connected) HeartRateSource.Availability.AVAILABLE
                    else HeartRateSource.Availability.DISCONNECTED,
                heartRate = null, measuredAtEpochMillis = null
            )))
            return
        }
        val availability = if (selectedReachable?.connected == true) HeartRateSource.Availability.AVAILABLE
            else HeartRateSource.Availability.DISCONNECTED
        if (source.availability != availability || (availability == HeartRateSource.Availability.DISCONNECTED && source.heartRate != null)) {
            states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(source.copy(
                availability = availability,
                heartRate = if (availability == HeartRateSource.Availability.AVAILABLE) source.heartRate else null,
                measuredAtEpochMillis = if (availability == HeartRateSource.Availability.AVAILABLE) source.measuredAtEpochMillis else null
            )))
        }
    }

    private suspend fun handleWatchCompanionMessage(peerId: String, message: WatchCompanionMessage): Boolean = when (message) {
            is WatchCompanionMessage.Ack -> {
                if (message.accepted && message.measurementRunId != null) {
                    val prefs = getSharedPreferences("watch-companion", Context.MODE_PRIVATE)
                    val queue = pendingMeasurementStops(prefs)
                    val updated = queue.map { pending ->
                        if (pending.messageId == message.messageId && pending.nodeId == peerId)
                            pending.copy(measurementRunId = message.measurementRunId) else pending
                    }
                    if (updated != queue) savePendingMeasurementStops(prefs, updated)
                }
                watchCompanionDataLayer.onMessage(peerId, message)
                true
            }
            is WatchCompanionMessage.Hello -> {
                sendWatchCompanionSnapshot(peerId)
                true
            }
            is WatchCompanionMessage.HeartRate -> {
                val me = userService.me()
                if (message.userId != me.id.value.toString()) return false
                val sensorId = HeartRateSensorId(WATCH_COMPANION_SOURCE_ID)
                val selected = states.getState(SelectedHeartRateSourceState).value?.source
                if (selected?.id != sensorId.value || !acceptsWatchMeasurementFrom(selected.companionDeviceId, peerId)) return false
                val companionPrefs = getSharedPreferences("watch-companion", Context.MODE_PRIVATE)
                val stoppedRunIds = companionPrefs.getStringSet("watch-measurement-stopped-run-ids", emptySet()).orEmpty()
                if (message.bpm == null) {
                    val activeRunId = companionPrefs.getString("watch-measurement-run-id", null)
                    val activeRunNodeId = companionPrefs.getString("watch-measurement-run-node-id", null)
                    val incomingRunId = message.measurementRunId
                    val activeRunBelongsToPeer = activeRunNodeId == null || activeRunNodeId == peerId
                    if (incomingRunId != null) {
                        val editor = companionPrefs.edit().putStringSet(
                            "watch-measurement-stopped-run-ids",
                            (stoppedRunIds + incomingRunId).toList().takeLast(64).toSet()
                        )
                        if (activeRunBelongsToPeer && activeRunId == incomingRunId) {
                            editor.remove("watch-measurement-run-id").remove("watch-measurement-run-node-id")
                        }
                        editor.apply()
                        if (activeRunBelongsToPeer && activeRunId != null && activeRunId != incomingRunId) return true
                    }
                    states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(selected.copy(
                        availability = HeartRateSource.Availability.DISCONNECTED, heartRate = null,
                        measuredAtEpochMillis = null
                    )))
                    return true
                }
                val bpm = message.bpm ?: return false
                if (bpm !in 25..240 || Clock.System.now().toEpochMilliseconds() - message.measuredAtEpochMillis !in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds) return false
                if (message.measurementRunId != null && message.measurementRunId in stoppedRunIds) return false
                message.measurementRunId?.let { runId ->
                    val previousRun = companionPrefs.getString("watch-measurement-run-id", null)
                    if (previousRun != null && previousRun != runId) {
                        companionPrefs.edit().putStringSet(
                            "watch-measurement-stopped-run-ids",
                            (stoppedRunIds + previousRun).toList().takeLast(64).toSet()
                        ).apply()
                    }
                    companionPrefs.edit().putString("watch-measurement-run-id", runId)
                        .putString("watch-measurement-run-node-id", peerId).apply()
                }
                userService.linkSensor(me, sensorId)
                val source = states.getState(SelectedHeartRateSourceState).value?.source
                states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(source?.copy(
                    availability = HeartRateSource.Availability.AVAILABLE,
                    heartRate = HeartRate(bpm), measuredAtEpochMillis = message.measuredAtEpochMillis
                )))
                events.emit(HeartRateMeasurementEvent(HeartRate(bpm), sensorId,
                    kotlin.time.Instant.fromEpochMilliseconds(message.measuredAtEpochMillis)))
                true
            }
            is WatchCompanionMessage.Session, is WatchCompanionMessage.MeasurementStop,
            is WatchCompanionMessage.Configure, is WatchCompanionMessage.GroupSnapshot -> false
        }

    private fun launchWatchCompanionStatePublisher() {
        launch {
            MeState.flow().collect { meState ->
                if (meState == null) return@collect
                val hue = states.getState(MeColorState).value?.color?.hue ?: UserColors.defaultHue(meState.me.id)
                watchCompanionDataLayer.send(WatchCompanionMessage.Configure(
                    messageId = "config-${meState.me.id.value}-${meState.me.name.hashCode()}-${meState.heartRateLimit.value.toInt()}",
                    userId = meState.me.id.value.toString(), userName = meState.me.name, colorHue = hue,
                    heartRateLimit = meState.heartRateLimit.value.toInt()
                ))
            }
        }
        launch {
            var lastSnapshotAt = 0L
            GroupState.flow().collect { group ->
                val now = Clock.System.now().toEpochMilliseconds()
                if (now - lastSnapshotAt < 2_000) return@collect
                lastSnapshotAt = now
                val members = group.members.filterNot { it.isMe }
                    .sortedByDescending { it.heartRateLimit != null && it.heartRate >= it.heartRateLimit }
                    .take(5)
                    .map { member -> WatchCompanionMessage.GroupSnapshot.Member(
                        userId = member.user.id.value.toString(), name = member.user.name,
                        bpm = member.heartRate.value.toInt(), limit = member.heartRateLimit?.value?.toInt(),
                        measuredAtEpochMillis = member.measuredAtEpochMillis
                    ) }
                watchCompanionDataLayer.send(WatchCompanionMessage.GroupSnapshot(
                    messageId = "group-${Clock.System.now().toEpochMilliseconds()}", members = members
                ))
            }
        }
        launch {
            val prefs = getSharedPreferences("watch-companion", Context.MODE_PRIVATE)
            var lastNodeId = prefs.getString("last-selected-watch-node-id", selectedWatchNodeId())
            SelectedHeartRateSourceState.flow().collect { state ->
                val nextNodeId = state?.source?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }
                    ?.companionDeviceId
                if (nextNodeId == lastNodeId) return@collect
                val previousNodeId = lastNodeId
                if (previousNodeId != null) {
                    val generation = prefs.getLong("measurement-stop-generation", 0L) + 1L
                    val messageId = "measurement-stop-$generation-" + java.util.UUID.randomUUID()
                    val queue = pendingMeasurementStops(prefs).toMutableList()
                    queue += PendingMeasurementStop(
                        previousNodeId, messageId, generation,
                        prefs.getString("watch-measurement-run-id", null).takeIf {
                            prefs.getString("watch-measurement-run-node-id", null) == previousNodeId
                        }
                    )
                    prefs.edit().putLong("measurement-stop-generation", generation).apply()
                    savePendingMeasurementStops(prefs, queue)
                }
                lastNodeId = nextNodeId
                prefs.edit().putString("last-selected-watch-node-id", nextNodeId).apply()
                if (nextNodeId != null && pendingMeasurementStops(prefs).none { it.nodeId == nextNodeId }) {
                    sendWatchCompanionSnapshot(nextNodeId)
                }
                if (previousNodeId != null) drainPendingMeasurementStop()
            }
        }
    }

    /** Keep the immutable target and command ID until the watch confirms Health Services actually stopped. */
    private suspend fun drainPendingMeasurementStop(): Boolean = pendingMeasurementStopMutex.withLock {
        val prefs = getSharedPreferences("watch-companion", Context.MODE_PRIVATE)
        while (true) {
            val pending = pendingMeasurementStops(prefs).firstOrNull() ?: break
            val result = watchCompanionDataLayer.sendReliableToOutcome(
                pending.nodeId,
                WatchCompanionMessage.MeasurementStop(pending.messageId, pending.generation, pending.measurementRunId),
                retries = 3
            )
            if (result != true) {
                scheduleMeasurementStopRetry()
                return@withLock false
            }
            val queue = pendingMeasurementStops(prefs)
            val acknowledged = queue.firstOrNull()?.takeIf {
                it.nodeId == pending.nodeId && it.messageId == pending.messageId && it.generation == pending.generation
            } ?: return@withLock false
            savePendingMeasurementStops(prefs, queue.drop(1))
            val stoppedRuns = prefs.getStringSet("watch-measurement-stopped-run-ids", emptySet()).orEmpty()
            val editor = prefs.edit()
            if (acknowledged.measurementRunId != null) {
                editor.putStringSet("watch-measurement-stopped-run-ids",
                    (stoppedRuns + acknowledged.measurementRunId).toList().takeLast(64).toSet())
                if (prefs.getString("watch-measurement-run-id", null) == acknowledged.measurementRunId) {
                    if (prefs.getString("watch-measurement-run-node-id", null) == pending.nodeId) {
                        editor.remove("watch-measurement-run-id").remove("watch-measurement-run-node-id")
                    }
                }
            }
            editor.apply()
            sendWatchCompanionSnapshot(pending.nodeId)
        }
        selectedWatchNodeId()?.let { sendWatchCompanionSnapshot(it) }
        true
    }

    private fun scheduleMeasurementStopRetry() {
        if (pendingMeasurementStopRetryJob?.isActive == true) return
        pendingMeasurementStopRetryJob = launch {
            var waitMillis = 5_000L
            repeat(5) {
                delay(waitMillis)
                if (drainPendingMeasurementStop()) return@launch
                waitMillis = (waitMillis * 2).coerceAtMost(30_000L)
            }
        }
    }

    private fun pendingMeasurementStops(prefs: android.content.SharedPreferences): List<PendingMeasurementStop> {
        val encoded = prefs.getString("measurement-stop-queue-v1", null)
        if (!encoded.isNullOrBlank()) return runCatching {
            val array = org.json.JSONArray(encoded)
            (0 until array.length()).mapNotNull { index ->
                val row = array.optJSONObject(index) ?: return@mapNotNull null
                PendingMeasurementStop(
                    row.getString("node"), row.getString("message"), row.getLong("generation"),
                    row.optString("run").takeIf { it.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
        val nodeId = prefs.getString("measurement-stop-node", null) ?: return emptyList()
        val messageId = prefs.getString("measurement-stop-id", null) ?: return emptyList()
        return listOf(PendingMeasurementStop(nodeId, messageId,
            prefs.getLong("measurement-stop-command-generation", 0L),
            prefs.getString("watch-measurement-run-id", null)))
    }

    private fun savePendingMeasurementStops(
        prefs: android.content.SharedPreferences,
        queue: List<PendingMeasurementStop>
    ) {
        val editor = prefs.edit().remove("measurement-stop-node").remove("measurement-stop-id")
            .remove("measurement-stop-command-generation")
        if (queue.isEmpty()) editor.remove("measurement-stop-queue-v1")
        else editor.putString("measurement-stop-queue-v1", org.json.JSONArray().apply {
            queue.forEach { pending ->
                put(org.json.JSONObject().put("node", pending.nodeId).put("message", pending.messageId)
                    .put("generation", pending.generation).put("run", pending.measurementRunId ?: ""))
            }
        }.toString())
        editor.apply()
    }

    private suspend fun sendWatchCompanionSnapshot(peerId: String? = null) {
        val sourceSelected = peerId == null || selectedWatchNodeId() == peerId
        val me = states.getState(MeState).value ?: return
        val hue = states.getState(MeColorState).value?.color?.hue ?: UserColors.defaultHue(me.me.id)
        val configure = WatchCompanionMessage.Configure(
            "config-${me.me.id.value}-${me.me.name.hashCode()}-${me.heartRateLimit.value.toInt()}",
            me.me.id.value.toString(), me.me.name, hue, me.heartRateLimit.value.toInt(), sourceSelected
        )
        if (peerId != null) watchCompanionDataLayer.sendTo(peerId, configure) else watchCompanionDataLayer.send(configure)
        if (peerId != null && !sourceSelected) return
        val members = states.getState(GroupState.Key).value?.members.orEmpty().filterNot { it.isMe }
            .sortedByDescending { it.heartRateLimit != null && it.heartRate >= it.heartRateLimit }
            .take(5)
            .map { member -> WatchCompanionMessage.GroupSnapshot.Member(
                member.user.id.value.toString(), member.user.name, member.heartRate.value.toInt().takeUnless { member.isConnectionLost },
                member.heartRateLimit?.value?.toInt(), member.measuredAtEpochMillis
            ) }
        val groupSnapshot = WatchCompanionMessage.GroupSnapshot(
            "group-${Clock.System.now().toEpochMilliseconds()}", members
        )
        if (peerId != null) watchCompanionDataLayer.sendTo(peerId, groupSnapshot) else watchCompanionDataLayer.send(groupSnapshot)
    }

    override fun onBind(intent: Intent?): IBinder {
        return MainServiceBinder(
            userService = userService,
            sessionService = sessionService,
            pacemakerBluetoothService = pacemakerBluetoothService,
            heartRateSensorBluetoothService = heartRateSensorBluetoothService,
            settings = settings
        )
    }

    companion object {
        const val ACTION_DATA_LAYER_MESSAGE = "io.sellmair.pacemaker.DATA_LAYER_MESSAGE"
        const val EXTRA_NODE_ID = "dataLayerNodeId"
        const val EXTRA_PAYLOAD = "dataLayerPayload"
    }
}
