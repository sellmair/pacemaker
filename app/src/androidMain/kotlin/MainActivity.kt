@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.broadheart

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import io.sellmair.broadheart.backend.AndroidApplicationBackend
import io.sellmair.broadheart.ui.ApplicationWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val mainServiceConnection = ApplicationBackendConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coroutineContext = Dispatchers.Main + Job()

        requestPermissions(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ), 0
        )
        startForegroundService()

        setContent {
            val backend = mainServiceConnection.backend.collectAsState().value
            if (backend != null) {
                ApplicationWindow(ApplicationViewModel(this.lifecycleScope, backend))
            }
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
        unbindService(mainServiceConnection)
        cancel()
    }
}
