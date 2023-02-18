package io.sellmair.broadheart.service

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CombinedVibration
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import io.sellmair.broadheart.HeartRate
import io.sellmair.broadheart.MainActivity
import io.sellmair.broadheart.Me
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.asFlow

class MainService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        val notification = MainServiceNotification(this)

        /* Load my limit from shared preferences */
        Me.myLimit = HeartRate(
            getSharedPreferences("limit", MODE_PRIVATE)
                .getFloat("limit", Me.myLimit.value)
        )

        notification.startForeground()


        /* Update notification and vibrate if we're over limit */
        scope.launch {
            while (true) {
                delay(1000)

                val myHeartRate = Me.myHeartRate ?: continue
                notification.update(myHeartRate, Me.myLimit)

                if (myHeartRate > Me.myLimit) {
                    val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.vibrate(
                        CombinedVibration.createParallel(
                            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    )
                }
            }
        }

        /* Start receiving HR updates */
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            val api = PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.FEATURE_HR)

            scope.launch {
                api.startListenForPolarHrBroadcasts(null).asFlow().collect { broadcast ->
                    Me.myHeartRate = HeartRate(broadcast.hr)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /* Store limit in shared preferences */
        getSharedPreferences("limit", MODE_PRIVATE).edit(true) {
            putFloat("limit", Me.myLimit.value)
        }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}