package io.sellmair.pacemaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.sellmair.app.core.R
import io.sellmair.evas.flow
import io.sellmair.pacemaker.model.HeartRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
        val notificationIntent = service.packageManager.getLaunchIntentForPackage(service.packageName)
            ?: Intent(service, service.javaClass)
        val contentPendingIntent = PendingIntent.getActivity(
            service, openActivityRequestCode, notificationIntent, FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(service, notificationChannel.id)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Pacemaker")
            .setContentText("Running")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
        if (!service.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH)) {
            notificationIntent.component?.let { component ->
                val stopIntent = Intent().setComponent(component).setAction(stopAction)
                val stopPendingIntent = PendingIntent.getActivity(service, stopAppRequestCode, stopIntent, FLAG_IMMUTABLE)
                builder.addAction(NotificationCompat.Action.Builder(null, "stop", stopPendingIntent).build())
            }
        }
        return builder
    }


    fun startForeground(coroutineScope: CoroutineScope) {
        /* Update notification showing current users heart rate */
        coroutineScope.launch {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            }
            ServiceCompat.startForeground(
                service, notificationId, createDefaultNotification().build(), foregroundServiceType
            )

            MeState.flow().collect { meState ->
                if (meState == null) return@collect
                if (meState.hasFreshHeartRate(Clock.System.now().toEpochMilliseconds())) {
                    meState.heartRate?.let { update(it, meState.heartRateLimit) }
                } else updateUnavailable(meState.heartRateLimit)
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

    private fun updateUnavailable(myHeartRateLimit: HeartRate) {
        notificationManager.notify(
            notificationId, createDefaultNotification()
                .setContentText("Heart rate unavailable Limit: $myHeartRateLimit")
                .build()
        )
    }
}
