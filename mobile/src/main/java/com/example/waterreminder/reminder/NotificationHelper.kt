package com.example.waterreminder.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.waterreminder.R
import com.example.waterreminder.presentation.MainActivity
import com.example.waterreminder.receiver.WaterActionReceiver

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to log daily water intake"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification() {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val log100Intent = Intent(context, WaterActionReceiver::class.java).apply {
            action = ACTION_LOG_WATER
            putExtra(EXTRA_ADD_AMOUNT, 100)
        }
        val log100PendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            log100Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop_complication)
            .setContentTitle("Time to Drink Water! 💧")
            .setContentText("Stay hydrated. Tap +100 ml to quickly log your water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(appPendingIntent)
            .addAction(
                0,
                "+100 ml",
                log100PendingIntent
            )

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val CHANNEL_NAME = "Water Intake Reminders"
        const val NOTIFICATION_ID = 1001

        const val ACTION_LOG_WATER = "com.example.waterreminder.LOG_WATER"
        const val EXTRA_ADD_AMOUNT = "extra_add_amount"
    }
}
