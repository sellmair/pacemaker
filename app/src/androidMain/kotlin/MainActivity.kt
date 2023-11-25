@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import io.sellmair.pacemaker.ui.ApplicationWindow
import io.sellmair.pacemaker.ui.LocalEventBus
import io.sellmair.pacemaker.ui.LocalSessionService
import io.sellmair.pacemaker.ui.LocalStateBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val mainServiceConnection = ApplicationBackendConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
        )

        coroutineContext = Dispatchers.Main + Job()

        requestPermissions(
            listOfNotNull(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                if(Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null,
                if(Build.VERSION.SDK_INT >= 33) Manifest.permission.BODY_SENSORS else null
            ).toTypedArray(), 0
        )
        startForegroundService()

        setContent {
            val backend = mainServiceConnection.backend.collectAsState().value
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
            unbindService(mainServiceConnection)
            stopService(Intent(this, AndroidApplicationBackend::class.java))
            finish()
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
            mainServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
