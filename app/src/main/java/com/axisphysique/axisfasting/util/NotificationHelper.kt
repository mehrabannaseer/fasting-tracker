package com.axisphysique.axisfasting.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.axisphysique.axisfasting.R

object NotificationHelper {
    private const val CHANNEL_ID = "fasting_notifications"
    private const val CHANNEL_NAME = "Fasting Alerts"
    private const val CHANNEL_DESC = "Notifications for fasting start and end"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendFastCompleteNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setContentTitle("Fast Complete!")
            .setContentText("Congratulations! You've reached your fasting goal.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // Check for permission in Android 13+ before calling notify
            // For now, we'll assume it's handled in the UI
            try {
                notify(1, builder.build())
            } catch (e: SecurityException) {
                // Handle missing permission
            }
        }
    }
}
