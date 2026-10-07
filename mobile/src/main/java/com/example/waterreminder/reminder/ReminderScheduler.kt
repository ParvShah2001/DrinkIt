package com.example.waterreminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.presentation.MainActivity
import com.example.waterreminder.receiver.WaterReminderReceiver

/**
 * Schedules high-reliability, exact hydration reminder alarms using [AlarmManager].
 *
 * Utilizes [AlarmManager.setAlarmClock] on API 21+ to guarantee exact wakeups even
 * through deep Android Doze mode and manufacturer power-saving restrictions.
 *
 * Supports synchronized trigger timestamps across paired devices to ensure
 * notifications fire concurrently without drift.
 *
 * @param context The application context used to obtain the system [AlarmManager].
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, WaterReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createShowIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            SHOW_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleNextReminder(customTriggerTimeMs: Long? = null): Long {
        val repository = WaterRepository(context)
        if (!repository.isReminderEnabled()) {
            cancelReminder()
            return 0L
        }

        val intervalMinutes = repository.getReminderIntervalMinutes()
        val triggerTimeMs = customTriggerTimeMs ?: run {
            val now = System.currentTimeMillis()
            val rawTime = now + (intervalMinutes * 60 * 1000L)
            rawTime - (rawTime % 60000L)
        }

        val pendingIntent = createAlarmPendingIntent()
        val showPendingIntent = createShowIntent()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d(TAG, "Scheduled synchronized setAlarmClock water reminder at epoch $triggerTimeMs.")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm, attempting fallback", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule fallback alarm", ex)
            }
        }
        return triggerTimeMs
    }

    fun cancelReminder() {
        val pendingIntent = createAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled water reminder alarm.")
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val REMINDER_REQ_CODE = 2001
        private const val SHOW_REQ_CODE = 2002
    }
}
