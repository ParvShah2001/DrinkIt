package com.example.waterreminder.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Repository responsible for managing user hydration state, goals, and reminder preferences.
 *
 * Persists data via [SharedPreferences] and automatically checks for date transitions
 * using [Calendar.DAY_OF_YEAR] to reset the daily intake counter at midnight.
 *
 * @param context The application or component context used to access SharedPreferences.
 */
class WaterRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "water_reminder_prefs"
        private const val KEY_CURRENT_INTAKE = "current_intake"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_LAST_DAY = "last_day_of_year"

        const val DEFAULT_GOAL = 2000 // 2000 ml
        const val DEFAULT_INTERVAL = 60 // 1 hour (60 minutes)

        const val MIN_INTERVAL = 15 // 15 mins
        const val MAX_INTERVAL = 240 // 4 hours (240 mins)

        const val MIN_GOAL = 1000 // 1000 ml
        const val MAX_GOAL = 4000 // 4000 ml
    }

    init {
        checkAndResetDailyIntake()
    }

    private fun checkAndResetDailyIntake() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastDay = prefs.getInt(KEY_LAST_DAY, -1)

        if (lastDay != currentDay) {
            prefs.edit()
                .putInt(KEY_CURRENT_INTAKE, 0)
                .putInt(KEY_LAST_DAY, currentDay)
                .apply()
        }
    }

    fun getCurrentIntake(): Int {
        checkAndResetDailyIntake()
        return prefs.getInt(KEY_CURRENT_INTAKE, 0)
    }

    fun addWater(amountMl: Int): Int {
        checkAndResetDailyIntake()
        val updated = (getCurrentIntake() + amountMl).coerceAtLeast(0)
        prefs.edit().putInt(KEY_CURRENT_INTAKE, updated).apply()
        return updated
    }

    fun setCurrentIntake(intakeMl: Int) {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        prefs.edit()
            .putInt(KEY_CURRENT_INTAKE, intakeMl.coerceAtLeast(0))
            .putInt(KEY_LAST_DAY, currentDay)
            .apply()
    }

    fun resetIntake() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        prefs.edit()
            .putInt(KEY_CURRENT_INTAKE, 0)
            .putInt(KEY_LAST_DAY, currentDay)
            .apply()
    }

    fun getDailyGoal(): Int {
        return prefs.getInt(KEY_DAILY_GOAL, DEFAULT_GOAL).coerceIn(MIN_GOAL, MAX_GOAL)
    }

    fun setDailyGoal(goalMl: Int) {
        val clamped = goalMl.coerceIn(MIN_GOAL, MAX_GOAL)
        prefs.edit().putInt(KEY_DAILY_GOAL, clamped).apply()
    }

    fun increaseGoal(): Int {
        val updated = (getDailyGoal() + 250).coerceAtMost(MAX_GOAL)
        setDailyGoal(updated)
        return updated
    }

    fun decreaseGoal(): Int {
        val updated = (getDailyGoal() - 250).coerceAtLeast(MIN_GOAL)
        setDailyGoal(updated)
        return updated
    }

    fun isReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }

    fun getReminderIntervalMinutes(): Int {
        return prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL).coerceIn(MIN_INTERVAL, MAX_INTERVAL)
    }

    fun setReminderIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        prefs.edit().putInt(KEY_INTERVAL_MINUTES, clamped).apply()
    }

    fun increaseInterval(): Int {
        val updated = (getReminderIntervalMinutes() + 15).coerceAtMost(MAX_INTERVAL)
        setReminderIntervalMinutes(updated)
        return updated
    }

    fun decreaseInterval(): Int {
        val updated = (getReminderIntervalMinutes() - 15).coerceAtLeast(MIN_INTERVAL)
        setReminderIntervalMinutes(updated)
        return updated
    }
}
