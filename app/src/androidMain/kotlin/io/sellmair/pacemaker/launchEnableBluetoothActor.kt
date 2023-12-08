package io.sellmair.pacemaker.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.sellmair.pacemaker.AndroidPermissionsState
import io.sellmair.pacemaker.MainActivity
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

fun MainActivity.launchEnableBluetoothActor() {
    val startActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        /* User denied to enable bluetooth */
        if (result.resultCode != ComponentActivity.RESULT_OK) {
            finish()
            exitProcess(0)
        }
    }

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {

            /* Request to enable bluetooth */
            withApplicationBackend {
                /* Wait for permission being granted */
                AndroidPermissionsState.get().first { state -> state is AndroidPermissionsState.Granted }

                val bluetoothManager = getSystemService<BluetoothManager>()
                val adapter = bluetoothManager?.adapter
                if (adapter?.isEnabled == false) {
                    val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult.launch(enableBluetoothIntent)
                }
            }
        }
    }
}
