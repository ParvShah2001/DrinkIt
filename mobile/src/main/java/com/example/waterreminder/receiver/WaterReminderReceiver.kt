package com.example.waterreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.reminder.NotificationHelper
import com.example.waterreminder.reminder.ReminderScheduler

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Water reminder alarm received on mobile phone!")

        val repository = WaterRepository(context)
        if (repository.isReminderEnabled()) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification()

            val scheduler = ReminderScheduler(context)
            scheduler.scheduleNextReminder()
        }
    }

    companion object {
        private const val TAG = "WaterReminderReceiver"
    }
}
