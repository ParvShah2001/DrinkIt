package com.example.waterreminder.presentation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.waterreminder.complication.WaterComplicationService
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.presentation.theme.AquaElectric
import com.example.waterreminder.presentation.theme.ContainerDark
import com.example.waterreminder.presentation.theme.ContainerElevated
import com.example.waterreminder.presentation.theme.SurfaceDark
import com.example.waterreminder.reminder.NotificationHelper
import com.example.waterreminder.reminder.ReminderScheduler
import com.example.waterreminder.sync.WearableDataSync

@Composable
fun WaterReminderApp() {
    val context = LocalContext.current
    val repository = remember { WaterRepository(context) }
    val scheduler = remember { ReminderScheduler(context) }
    val notificationHelper = remember { NotificationHelper(context) }
    val sync = remember { WearableDataSync(context) }

    var currentIntake by remember { mutableStateOf(repository.getCurrentIntake()) }
    var dailyGoal by remember { mutableStateOf(repository.getDailyGoal()) }
    var isReminderEnabled by remember { mutableStateOf(repository.isReminderEnabled()) }
    var intervalMinutes by remember { mutableStateOf(repository.getReminderIntervalMinutes()) }

    var showResetDialog by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableStateOf(0) }

    // Listen for real-time state changes from paired Mobile Phone
    DisposableEffect(Unit) {
        val listener = sync.startListening { intake, goal, enabled, interval, nextAlarmTime ->
            currentIntake = intake
            dailyGoal = goal
            isReminderEnabled = enabled
            intervalMinutes = interval
            WaterComplicationService.requestComplicationUpdate(context)
        }
        onDispose { sync.stopListening(listener) }
    }

    val progress = remember(currentIntake, dailyGoal) {
        if (dailyGoal > 0) (currentIntake.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    }
    val progressPercent = remember(progress) { (progress * 100).toInt() }
    val formattedInterval = remember(intervalMinutes) {
        val hours = intervalMinutes / 60
        val mins = intervalMinutes % 60
        when {
            hours == 0 -> "$mins mins"
            mins == 0 -> "$hours hr${if (hours > 1) "s" else ""}"
            else -> "${hours}h ${mins}m"
        }
    }

    val listState = rememberScalingLazyListState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (isReminderEnabled) {
            val nextAlarm = scheduler.scheduleNextReminder()
            sync.syncToPeer(currentIntake, dailyGoal, isReminderEnabled, intervalMinutes, nextAlarm)
        }
    }

    fun addWaterAndSync(amountMl: Int) {
        currentIntake = repository.addWater(amountMl)
        val nextAlarm = if (isReminderEnabled) scheduler.scheduleNextReminder() else 0L
        sync.syncToPeer(currentIntake, dailyGoal, isReminderEnabled, intervalMinutes, nextAlarm)
        WaterComplicationService.requestComplicationUpdate(context)
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceDark),
                state = listState,
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.95f,
                    edgeAlpha = 0.95f,
                    minElementHeight = 0f
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 10.dp, end = 10.dp)
            ) {
                // Header Badge
                item(key = "header_badge") {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF142436))
                            .clickable {
                                devTapCount++
                                if (devTapCount >= 3) {
                                    devTapCount = 0
                                    notificationHelper.showNotification()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Drink It! 💧",
                            style = MaterialTheme.typography.caption1,
                            color = AquaElectric,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Central Gauge Ring & Water Intake Progress Stats
                item(key = "progress_ring") {
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            startAngle = 270f,
                            endAngle = 270f,
                            indicatorColor = AquaElectric,
                            trackColor = AquaElectric.copy(alpha = 0.22f),
                            strokeWidth = 7.dp
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$currentIntake",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "/ $dailyGoal ml",
                                fontSize = 10.sp,
                                color = Color(0xFFA0B4C8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D283E))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$progressPercent%",
                                    fontSize = 10.sp,
                                    color = AquaElectric,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1. Quick Add +100 ml Sip Chip
                item(key = "quick_100") {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        label = { Text("+100 ml Sip", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        onClick = { addWaterAndSync(100) },
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color(0xFF152A3B),
                            contentColor = Color.White
                        )
                    )
                }

                // 2. Quick Add +250 ml Glass Chip
                item(key = "quick_250") {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        label = { Text("+250 ml Glass", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        onClick = { addWaterAndSync(250) },
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color(0xFF00496E),
                            contentColor = Color.White
                        )
                    )
                }

                // 3. Quick Add +500 ml Bottle Chip
                item(key = "quick_500") {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        label = { Text("+500 ml Bottle", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        onClick = { addWaterAndSync(500) },
                        colors = ChipDefaults.secondaryChipColors(
                            backgroundColor = Color(0xFF09365E),
                            contentColor = Color.White
                        )
                    )
                }

                // Controls Section Title
                item(key = "controls_header") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Controls & Goal",
                        fontSize = 11.sp,
                        color = Color(0xFFA0B4C8),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Toggle Reminders Chip
                item(key = "toggle_reminders") {
                    ToggleChip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        checked = isReminderEnabled,
                        onCheckedChange = { enabled ->
                            isReminderEnabled = enabled
                            repository.setReminderEnabled(enabled)
                            if (enabled) {
                                scheduler.scheduleNextReminder()
                            } else {
                                scheduler.cancelReminder()
                            }
                            sync.syncToPeer(currentIntake, dailyGoal, enabled, intervalMinutes)
                        },
                        label = { Text("Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        secondaryLabel = {
                            Text(
                                if (isReminderEnabled) "Every $formattedInterval" else "Disabled",
                                fontSize = 10.sp
                            )
                        },
                        toggleControl = {
                            Switch(
                                checked = isReminderEnabled,
                                onCheckedChange = null
                            )
                        },
                        colors = ToggleChipDefaults.toggleChipColors(
                            checkedStartBackgroundColor = Color(0xFF003D5C),
                            checkedEndBackgroundColor = Color(0xFF004F75),
                            uncheckedStartBackgroundColor = ContainerDark,
                            uncheckedEndBackgroundColor = ContainerDark
                        )
                    )
                }

                // Stepper Card: Timer Interval
                item(key = "stepper_interval") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(ContainerElevated)
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Timer Interval",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA0B4C8)
                            )
                            Text(
                                text = "Every $formattedInterval",
                                fontSize = 13.sp,
                                color = AquaElectric,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        val updated = repository.decreaseInterval()
                                        intervalMinutes = updated
                                        if (isReminderEnabled) scheduler.scheduleNextReminder()
                                        sync.syncToPeer(currentIntake, dailyGoal, isReminderEnabled, updated)
                                    },
                                    enabled = intervalMinutes > WaterRepository.MIN_INTERVAL,
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = Color(0xFF14202C),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("-15m", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Button(
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        val updated = repository.increaseInterval()
                                        intervalMinutes = updated
                                        if (isReminderEnabled) scheduler.scheduleNextReminder()
                                        sync.syncToPeer(currentIntake, dailyGoal, isReminderEnabled, updated)
                                    },
                                    enabled = intervalMinutes < WaterRepository.MAX_INTERVAL,
                                    colors = ButtonDefaults.primaryButtonColors(
                                        backgroundColor = AquaElectric,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("+15m", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Stepper Card: Daily Goal
                item(key = "stepper_goal") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(ContainerElevated)
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Daily Goal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA0B4C8)
                            )
                            Text(
                                text = "$dailyGoal ml",
                                fontSize = 13.sp,
                                color = AquaElectric,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        val updated = repository.decreaseGoal()
                                        dailyGoal = updated
                                        WaterComplicationService.requestComplicationUpdate(context)
                                        sync.syncToPeer(currentIntake, updated, isReminderEnabled, intervalMinutes)
                                    },
                                    enabled = dailyGoal > WaterRepository.MIN_GOAL,
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = Color(0xFF14202C),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("-250", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Button(
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        val updated = repository.increaseGoal()
                                        dailyGoal = updated
                                        WaterComplicationService.requestComplicationUpdate(context)
                                        sync.syncToPeer(currentIntake, updated, isReminderEnabled, intervalMinutes)
                                    },
                                    enabled = dailyGoal < WaterRepository.MAX_GOAL,
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = Color(0xFF0F3652),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("+250", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Reset Progress Button
                item(key = "reset_progress") {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        label = { Text("Reset Progress Today", textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        onClick = { showResetDialog = true },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF3B1515),
                            contentColor = Color(0xFFFF8A80)
                        )
                    )
                }
            }

            // Warning Overlay Dialog (Yes / No)
            if (showResetDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .clickable { showResetDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1F1414))
                            .clickable(enabled = false) {}
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠️ Reset Today?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFFF8A80)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reset today's water intake back to 0 ml?",
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color(0xFFA0B4C8)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    modifier = Modifier.width(54.dp),
                                    onClick = {
                                        repository.resetIntake()
                                        currentIntake = 0
                                        WaterComplicationService.requestComplicationUpdate(context)
                                        sync.syncToPeer(0, dailyGoal, isReminderEnabled, intervalMinutes)
                                        showResetDialog = false
                                    },
                                    colors = ButtonDefaults.primaryButtonColors(
                                        backgroundColor = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Yes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    modifier = Modifier.width(54.dp),
                                    onClick = { showResetDialog = false },
                                    colors = ButtonDefaults.secondaryButtonColors(
                                        backgroundColor = Color(0xFF263238),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("No", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
