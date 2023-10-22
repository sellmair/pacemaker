package io.sellmair.pacemaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.sellmair.pacemaker.model.HeartRate

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
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Pacemaker")
            .setContentText("Running")
            .setContentIntent(contentPendingIntent)
            .addAction(NotificationCompat.Action.Builder(null, "stop", stopPendingIntent).build())
            .setOngoing(true)
    }


    fun startForeground() {
        service.startForeground(notificationId, createDefaultNotification().build())
    }

    fun update(myHeartRate: HeartRate, myHeartRateLimit: HeartRate) {
        notificationManager.notify(
            notificationId, createDefaultNotification()
                .setContentText("Current HR: $myHeartRate Limit: $myHeartRateLimit")
                .build()
        )
    }
}