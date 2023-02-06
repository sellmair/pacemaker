package io.sellmair.broadheart

import android.Manifest
import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import io.sellmair.broadheart.ui.HeartRateScale
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.asFlow


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


class MainService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()

        /* Load my limit from shared preferences */
        Me.myLimit = HeartRate(
            getSharedPreferences("limit", Context.MODE_PRIVATE)
                .getFloat("limit", Me.myLimit.value)
        )

        /* Create foreground notification and start foreground service */
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, FLAG_IMMUTABLE)

        val chan = NotificationChannel("myChannelId", "myChannelName", NotificationManager.IMPORTANCE_LOW)
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)

        val builder = NotificationCompat.Builder(this, "myChannelId")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Heartcast")
            .setContentText("Running")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        startForeground(1, builder.build())

        /* Update notification and vibrate if we're over limit */
        scope.launch {
            while (true) {
                delay(1000)

                val myHeartRate = Me.myHeartRate ?: continue
                val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(
                    1, builder.setContentText("Current HR: $myHeartRate Limit: ${Me.myLimit}").build()
                )

                if (myHeartRate > Me.myLimit) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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
        getSharedPreferences("limit", Context.MODE_PRIVATE).edit(true) {
            putFloat("limit", Me.myLimit.value)
        }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}