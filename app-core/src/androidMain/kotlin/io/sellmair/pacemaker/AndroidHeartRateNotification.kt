package io.sellmair.pacemaker

import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.sellmair.pacemaker.model.HeartRate

class AndroidHeartRateNotification(private val service: Service) {

    private companion object {
        const val notificationId = 1
        const val notificationChannelId = "Service Notification Channel"
        const val notificationChannelName = "Service Status"
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
        val notificationIntent = Intent(service, Class.forName("io.sellmair.pacemaker.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(service, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)


        return NotificationCompat.Builder(service, notificationChannel.id)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Pacemaker")
            .setContentText("Running")
            .setContentIntent(pendingIntent)
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