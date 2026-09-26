package com.example.waterreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.reminder.ReminderScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Phone booted / package updated ($action). Re-scheduling water reminder if enabled...")
            val repository = WaterRepository(context)
            if (repository.isReminderEnabled()) {
                val scheduler = ReminderScheduler(context)
                scheduler.scheduleNextReminder()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
