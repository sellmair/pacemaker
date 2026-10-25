package io.sellmair.pacemaker

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import io.sellmair.pacemaker.ble.AndroidBle
import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.sql.PacemakerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class AndroidApplicationBackend : Service(), ApplicationBackend, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob() + Events() + States() +
            AndroidContextProvider(this)

    inner class MainServiceBinder(
        override val pacemakerBluetoothService: Deferred<PacemakerBluetoothService>,
        override val heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>,
        override val userService: UserService,
        override val settings: Settings,
    ) : Binder(), ApplicationBackend {
        override val events get() = coroutineContext.eventsOrThrow
        override val states get() = coroutineContext.statesOrThrow
    }

    override val events: Events = coroutineContext.eventsOrThrow

    override val states: States = coroutineContext.statesOrThrow

    private var bleInstance: Ble? = null
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
        SharedPreferencesSettings(appSettingsPreferences())
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

    override fun onCreate() {
        super.onCreate()
        notification.startForeground(this)
        launchApplicationBackend(this)
        watchCompanionDataLayer = WatchCompanionDataLayer(this, this, WATCH_COMPANION_CAPABILITY)
        val watchPreferences = phoneWatchPreferences()
        val stopDelivery = WatchMeasurementStopDelivery(
            SharedPreferencesWatchMeasurementStopStore(watchPreferences), this, watchCompanionDataLayer
        )
        WatchCompanionStatePublisher(watchPreferences, this, watchCompanionDataLayer, stopDelivery).start()
        WatchCompanionMessageActor(watchPreferences, this, watchCompanionDataLayer, stopDelivery, userService).start()
        watchCompanionDataLayer.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DATA_LAYER_MESSAGE) {
            val peerId = intent.getStringExtra(EXTRA_NODE_ID)
            val payload = intent.getByteArrayExtra(EXTRA_PAYLOAD)
            if (peerId != null && payload != null) {
                WatchCompanionCodec.decode(payload)?.let { message ->
                    events.emitAsync(WatchCompanionInboundMessage(peerId, message))
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


    override fun onBind(intent: Intent?): IBinder {
        return MainServiceBinder(
            userService = userService,
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
