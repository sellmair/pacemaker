package io.sellmair.pacemaker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.sellmair.app.core.R
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class AndroidHeartRateNotification(private val service: Service) {

    companion object {
        const val notificationId = 1
        const val notificationChannelId = "Service Notification Channel"
        const val notificationChannelName = "Service Status"

        const val openActivityRequestCode = 0
        const val stopAppRequestCode = 1
        const val stopAction = "Action: Stop"
    }

    private val notificationManager by lazy {
        service.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationChannel by lazy {
        NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_LOW).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(this)
        }
    }

    private fun createDefaultNotification(): NotificationCompat.Builder {
        val mainActivityClz = Class.forName("io.sellmair.pacemaker.MainActivity")

        val notificationIntent = Intent(service, mainActivityClz)
        val contentPendingIntent = PendingIntent.getActivity(
            service, openActivityRequestCode, notificationIntent, FLAG_IMMUTABLE
        )

        val stopIntent = Intent(service, mainActivityClz)
        stopIntent.setAction(stopAction)
        val stopPendingIntent = PendingIntent.getActivity(
            service, stopAppRequestCode, stopIntent, FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(service, notificationChannel.id)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Pacemaker")
            .setContentText("Running")
            .setContentIntent(contentPendingIntent)
            .addAction(NotificationCompat.Action.Builder(null, "stop", stopPendingIntent).build())
            .setOngoing(true)
    }


    fun startForeground(coroutineScope: CoroutineScope) {
        /* Update notification showing current users heart rate */
        coroutineScope.launch(Dispatchers.Main.immediate) {
            while(Build.VERSION.SDK_INT >= 33 && service.checkSelfPermission(Manifest.permission.BODY_SENSORS) != PERMISSION_GRANTED) {
                delay(1.seconds)
            }

            val foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                if(Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0

            ServiceCompat.startForeground(
                service, notificationId, createDefaultNotification().build(), foregroundServiceType
            )

            MeState.get().filterNotNull().collect { meState ->
                update(
                    meState.heartRate ?: return@collect,
                    meState.heartRateLimit
                )
            }
        }
    }

    private fun update(myHeartRate: HeartRate, myHeartRateLimit: HeartRate) {
        notificationManager.notify(
            notificationId, createDefaultNotification()
                .setContentText("Current HR: $myHeartRate Limit: $myHeartRateLimit")
                .build()
        )
    }
}