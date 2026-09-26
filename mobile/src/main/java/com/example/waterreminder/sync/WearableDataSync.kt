package com.example.waterreminder.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*

class WearableDataSync(private val context: Context) {

    private val dataClient: DataClient? by lazy {
        try {
            Wearable.getDataClient(context)
        } catch (e: Exception) {
            Log.d(TAG, "Wearable API unavailable on device", e)
            null
        }
    }

    fun syncToPeer(
        currentIntake: Int,
        dailyGoal: Int,
        isReminderEnabled: Boolean,
        intervalMinutes: Int,
        nextAlarmTimeMs: Long = 0L
    ) {
        val client = dataClient ?: return
        try {
            val putDataMapReq = PutDataMapRequest.create(PATH_WATER_DATA).apply {
                dataMap.putInt(KEY_INTAKE, currentIntake)
                dataMap.putInt(KEY_GOAL, dailyGoal)
                dataMap.putBoolean(KEY_ENABLED, isReminderEnabled)
                dataMap.putInt(KEY_INTERVAL, intervalMinutes)
                dataMap.putLong(KEY_NEXT_ALARM_TIME, nextAlarmTimeMs)
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val request = putDataMapReq.asPutDataRequest().setUrgent()
            client.putDataItem(request)
                .addOnSuccessListener {
                    Log.d(TAG, "Synced state to peer device: intake=$currentIntake, goal=$dailyGoal, alarmTime=$nextAlarmTimeMs")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Standalone mode (no peer device connected): ${e.message}")
                }
        } catch (e: Exception) {
            Log.d(TAG, "Standalone fallback sync bypass", e)
        }
    }

    fun startListening(onDataReceived: (intake: Int, goal: Int, enabled: Boolean, interval: Int, nextAlarmTime: Long) -> Unit): DataClient.OnDataChangedListener? {
        val client = dataClient ?: return null
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            try {
                for (event in dataEvents) {
                    if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == PATH_WATER_DATA) {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val intake = dataMap.getInt(KEY_INTAKE, 0)
                        val goal = dataMap.getInt(KEY_GOAL, 2000)
                        val enabled = dataMap.getBoolean(KEY_ENABLED, true)
                        val interval = dataMap.getInt(KEY_INTERVAL, 60)
                        val nextAlarmTime = dataMap.getLong(KEY_NEXT_ALARM_TIME, 0L)
                        Log.d(TAG, "Received peer update: intake=$intake, goal=$goal, alarmTime=$nextAlarmTime")
                        onDataReceived(intake, goal, enabled, interval, nextAlarmTime)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error processing incoming data event", e)
            }
        }
        try {
            client.addListener(listener)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to attach DataClient listener in standalone mode", e)
        }
        return listener
    }

    fun stopListening(listener: DataClient.OnDataChangedListener?) {
        if (listener == null) return
        try {
            dataClient?.removeListener(listener)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to remove DataClient listener", e)
        }
    }

    companion object {
        private const val TAG = "WearableDataSync"
        const val PATH_WATER_DATA = "/water_data"

        const val KEY_INTAKE = "intake"
        const val KEY_GOAL = "goal"
        const val KEY_ENABLED = "enabled"
        const val KEY_INTERVAL = "interval"
        const val KEY_NEXT_ALARM_TIME = "next_alarm_time"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
