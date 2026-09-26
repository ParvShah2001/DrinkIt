package com.example.waterreminder.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.presentation.theme.WaterReminderTheme
import com.example.waterreminder.presentation.ui.WaterReminderApp
import com.example.waterreminder.reminder.ReminderScheduler

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleInitialReminder()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()
        scheduleInitialReminder()

        setContent {
            WaterReminderTheme {
                WaterReminderApp()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleInitialReminder() {
        val repository = WaterRepository(this)
        if (repository.isReminderEnabled()) {
            val scheduler = ReminderScheduler(this)
            scheduler.scheduleNextReminder()
        }
    }
}
