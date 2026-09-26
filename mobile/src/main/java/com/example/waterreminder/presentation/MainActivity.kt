package com.example.waterreminder.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.waterreminder.presentation.theme.DrinkItMobileTheme
import com.example.waterreminder.presentation.ui.WaterReminderMobileApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrinkItMobileTheme {
                WaterReminderMobileApp()
            }
        }
    }
}
