@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import io.sellmair.pacemaker.ui.*
import io.sellmair.pacemaker.ui.ApplicationWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override lateinit var coroutineContext: CoroutineContext

    private val applicationBackendConnection = ApplicationBackendConnection()

    init {
        launchAndroidPermissionStateActor()
        launchEnableBluetoothActor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
        )

        coroutineContext = Dispatchers.Main + Job()

        startForegroundService()

        setContent {
            val backend = applicationBackendConnection.backend.collectAsState().value
            if (backend != null) {
                CompositionLocalProvider(
                    LocalStateBus provides backend.stateBus,
                    LocalEventBus provides backend.eventBus,
                    LocalSessionService provides backend.sessionService
                ) {
                    ApplicationWindow()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == AndroidHeartRateNotification.stopAction) {
            unbindService(applicationBackendConnection)
            stopService(Intent(this, AndroidApplicationBackend::class.java))
            finish()
            Runtime.getRuntime().exit(0)
        }
    }

    suspend fun <T> withApplicationBackend(block: suspend () -> T): T {
        /* await service connection */
        val backend = applicationBackendConnection.backend.filterNotNull().first()
        return withContext(backend.stateBus + backend.eventBus) {
            block()
        }
    }

    fun launchWithApplicationBackend(block: suspend () -> Unit): Job {
        return launch {
            withApplicationBackend(block)
        }
    }

    private inner class ApplicationBackendConnection : ServiceConnection {

        private val _backend = MutableStateFlow<ApplicationBackend?>(null)

        val backend = _backend.asStateFlow()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AndroidApplicationBackend.MainServiceBinder) {
                this._backend.value = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _backend.value = null
        }
    }

    private fun startForegroundService() {
        bindService(
            Intent(this, AndroidApplicationBackend::class.java),
            applicationBackendConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
