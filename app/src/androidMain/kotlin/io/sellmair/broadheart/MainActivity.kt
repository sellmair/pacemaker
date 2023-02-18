package io.sellmair.broadheart

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import io.sellmair.broadheart.service.MainService
import io.sellmair.broadheart.ui.HeartRateScale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 0)
        startForegroundService()
        val groupState = GroupService.instance.state
        setContent {
            HeartRateScale(groupState.collectAsState(null).value)
        }
    }

    private fun startForegroundService() {
        startService(Intent(this, MainService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MainService::class.java))
    }
}


