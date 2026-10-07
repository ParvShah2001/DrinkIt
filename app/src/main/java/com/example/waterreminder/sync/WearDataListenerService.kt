package com.example.waterreminder.sync

import android.util.Log
import com.example.waterreminder.complication.WaterComplicationService
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.reminder.ReminderScheduler
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Background listener service receiving Wearable Data Layer events from paired phone.
 *
 * Automatically triggered by Google Play Services when data at `/water_data` changes.
 * Reconciles local [WaterRepository] state, updates watch complications, and reschedules
 * alarms to the peer's synchronized epoch timestamp.
 */
class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearableDataSync.PATH_WATER_DATA) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val intake = dataMap.getInt(WearableDataSync.KEY_INTAKE, 0)
                val goal = dataMap.getInt(WearableDataSync.KEY_GOAL, 2000)
                val enabled = dataMap.getBoolean(WearableDataSync.KEY_ENABLED, true)
                val interval = dataMap.getInt(WearableDataSync.KEY_INTERVAL, 60)
                val nextAlarmTime = dataMap.getLong(WearableDataSync.KEY_NEXT_ALARM_TIME, 0L)

                Log.d(TAG, "Background sync received: intake=$intake, goal=$goal, enabled=$enabled, interval=$interval, alarmTime=$nextAlarmTime")

                val repository = WaterRepository(applicationContext)
                repository.setDailyGoal(goal)
                repository.setReminderEnabled(enabled)
                repository.setReminderIntervalMinutes(interval)
                repository.setCurrentIntake(intake)

                WaterComplicationService.requestComplicationUpdate(applicationContext)

                val scheduler = ReminderScheduler(applicationContext)
                if (enabled) {
                    val validTime = if (nextAlarmTime > System.currentTimeMillis()) nextAlarmTime else null
                    scheduler.scheduleNextReminder(validTime)
                } else {
                    scheduler.cancelReminder()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WearDataListenerService"
    }
}
