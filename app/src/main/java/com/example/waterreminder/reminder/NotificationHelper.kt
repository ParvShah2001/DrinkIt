package com.example.waterreminder.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.presentation.MainActivity
import com.example.waterreminder.receiver.WaterActionReceiver

/**
 * Helper class for creating, displaying, and managing hydration reminder notifications on Wear OS.
 *
 * Configures notification channel, custom water drop icons, high-priority heads-up display,
 * custom vibrations, and quick action buttons. Sets [NotificationCompat.Builder.setLocalOnly] to true
 * to prevent duplicate Bluetooth mirroring between watch and phone.
 *
 * @param context The application context used to build and post notifications.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val CHANNEL_NAME = "Drink It! Reminders"
        const val NOTIFICATION_ID = 1001
        const val ACTION_LOG_WATER = "com.main.waterreminder.ACTION_LOG_WATER"
        const val EXTRA_ADD_AMOUNT = "extra_add_amount"
    }

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
                description = "Configurable reminders to drink water on your Wear OS watch"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(currentIntake: Int, goal: Int, intervalMinutes: Int): Notification {
        // Open Main Activity Intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct Action: Quick Log +100ml
        val logWaterIntent = Intent(context, WaterActionReceiver::class.java).apply {
            action = ACTION_LOG_WATER
            putExtra(EXTRA_ADD_AMOUNT, 100)
        }
        val logWaterPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            logWaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = if (goal > 0) ((currentIntake.toFloat() / goal) * 100).toInt() else 0
        val intervalText = if (intervalMinutes % 60 == 0) "${intervalMinutes / 60}h" else "${intervalMinutes}m"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.waterreminder.R.drawable.ic_water_drop_complication)
            .setContentTitle("Drink It! 💧 ($intervalText interval)")
            .setContentText("Logged: ${currentIntake} / ${goal} ml ($progressPercent%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                "+100 ml",
                logWaterPendingIntent
            )
            .setLocalOnly(true) // Wear OS local notification
            .build()
    }

    fun showNotification() {
        val repository = WaterRepository(context)
        val intake = repository.getCurrentIntake()
        val goal = repository.getDailyGoal()
        val interval = repository.getReminderIntervalMinutes()
        notificationManager.notify(NOTIFICATION_ID, buildNotification(intake, goal, interval))
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
