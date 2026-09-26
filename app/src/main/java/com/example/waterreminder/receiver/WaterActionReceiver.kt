package com.example.waterreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.waterreminder.complication.WaterComplicationService
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.reminder.NotificationHelper
import com.example.waterreminder.reminder.ReminderScheduler
import com.example.waterreminder.sync.WearableDataSync

class WaterActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == NotificationHelper.ACTION_LOG_WATER) {
            val amount = intent.getIntExtra(NotificationHelper.EXTRA_ADD_AMOUNT, 100)
            Log.d(TAG, "Quick logging $amount ml from notification action on watch")

            val repository = WaterRepository(context)
            val newIntake = repository.addWater(amount)

            // Update Wear OS watch face complications
            WaterComplicationService.requestComplicationUpdate(context)

            // Reset/Schedule next alarm from this point
            val scheduler = ReminderScheduler(context)
            val nextAlarm = scheduler.scheduleNextReminder()

            // Sync updated state and exact next alarm timestamp to peer device
            val sync = WearableDataSync(context)
            sync.syncToPeer(
                currentIntake = newIntake,
                dailyGoal = repository.getDailyGoal(),
                isReminderEnabled = repository.isReminderEnabled(),
                intervalMinutes = repository.getReminderIntervalMinutes(),
                nextAlarmTimeMs = nextAlarm
            )

            val notificationHelper = NotificationHelper(context)
            notificationHelper.dismissNotification()
        }
    }

    companion object {
        private const val TAG = "WaterActionReceiver"
    }
}
