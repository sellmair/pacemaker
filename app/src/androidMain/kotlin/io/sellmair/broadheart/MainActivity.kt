@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.broadheart

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.MainService
import io.sellmair.broadheart.service.UserService
import io.sellmair.broadheart.ui.HeartRateScale
import io.sellmair.broadheart.ui.MyStatusHeader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val mainServiceConnection = MainServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coroutineContext = Dispatchers.Main + Job()

        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 0)
        startForegroundService()

        val groupStates = mainServiceConnection.groupServices
            .flatMapLatest { servicesOrNull -> servicesOrNull?.groupState ?: flowOf(null) }

        setContent {
            val groupState by groupStates.collectAsState(null)
            HeartRateScale(groupState)
            MyStatusHeader(groupState)
        }
    }

    private inner class MainServiceConnection : ServiceConnection {
        private val _userService = MutableStateFlow<UserService?>(null)
        private val _groupService = MutableStateFlow<GroupService?>(null)

        val userService = _userService.asStateFlow()
        val groupServices = _groupService.asStateFlow()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MainService.MainServiceBinder) {
                _userService.tryEmit(service.services.userService)
                _groupService.tryEmit(service.services.groupService)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _userService.tryEmit(null)
            _groupService.tryEmit(null)
        }
    }

    private fun startForegroundService() {
        bindService(Intent(this, MainService::class.java), mainServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mainServiceConnection)
        cancel()
    }
}


