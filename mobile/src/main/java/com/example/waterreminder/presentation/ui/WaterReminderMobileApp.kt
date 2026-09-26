package com.example.waterreminder.presentation.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterreminder.data.WaterRepository
import com.example.waterreminder.presentation.theme.*
import com.example.waterreminder.reminder.NotificationHelper
import com.example.waterreminder.reminder.ReminderScheduler
import com.example.waterreminder.sync.WearableDataSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterReminderMobileApp() {
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

    // Listen for real-time state changes from paired Wear OS Smartwatch
    DisposableEffect(Unit) {
        val listener = sync.startListening { intake, goal, enabled, interval, nextAlarmTime ->
            currentIntake = intake
            dailyGoal = goal
            isReminderEnabled = enabled
            intervalMinutes = interval
        }
        onDispose { sync.stopListening(listener) }
    }

    val progress = remember(currentIntake, dailyGoal) {
        if (dailyGoal > 0) (currentIntake.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress_anim"
    )

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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PhoneBackgroundTop,
                        PhoneBackgroundMiddle,
                        PhoneBackgroundBottom
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF142436))
                                .clickable {
                                    devTapCount++
                                    if (devTapCount >= 3) {
                                        devTapCount = 0
                                        notificationHelper.showNotification()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Drink It! 💧",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AquaElectric
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Gauge Progress Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGlassBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = AquaElectric,
                                trackColor = AquaElectric.copy(alpha = 0.22f),
                                strokeWidth = 14.dp
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$currentIntake",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "/ $dailyGoal ml",
                                    fontSize = 16.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0D283E)
                                ) {
                                    Text(
                                        text = "$progressPercent% Achieved",
                                        fontSize = 13.sp,
                                        color = AquaElectric,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Add Water Section Title
                Text(
                    text = "Quick Add Water",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                )

                // Quick Add Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { addWaterAndSync(100) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF152A3B),
                            contentColor = Color.White
                        )
                    ) {
                        Text("+100 ml", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { addWaterAndSync(250) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00496E),
                            contentColor = Color.White
                        )
                    ) {
                        Text("+250 ml", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { addWaterAndSync(500) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF09365E),
                            contentColor = Color.White
                        )
                    ) {
                        Text("+500 ml", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Controls & Settings Title
                Text(
                    text = "Controls & Reminders",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                // Reminders Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevatedBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hydration Reminders",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isReminderEnabled) "Active • Every $formattedInterval" else "Disabled",
                                fontSize = 13.sp,
                                color = if (isReminderEnabled) AquaElectric else TextSecondary
                            )
                        }

                        Switch(
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
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AquaElectric
                            )
                        )
                    }
                }

                // Timer Interval Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevatedBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reminder Interval",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Every $formattedInterval",
                                fontSize = 13.sp,
                                color = AquaElectric,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val updated = repository.decreaseInterval()
                                    intervalMinutes = updated
                                    if (isReminderEnabled) scheduler.scheduleNextReminder()
                                    sync.syncToPeer(currentIntake, dailyGoal, isReminderEnabled, updated)
                                },
                                enabled = intervalMinutes > WaterRepository.MIN_INTERVAL,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-15m", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val updated = repository.increaseInterval()
                                    intervalMinutes = updated
                                    if (isReminderEnabled) scheduler.scheduleNextReminder()
                                    sync.syncToPeer(currentIntake, dailyGoal, isReminderEnabled, updated)
                                },
                                enabled = intervalMinutes < WaterRepository.MAX_INTERVAL,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AquaElectric,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("+15m", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Daily Goal Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevatedBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Goal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$dailyGoal ml",
                                fontSize = 13.sp,
                                color = AquaElectric,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val updated = repository.decreaseGoal()
                                    dailyGoal = updated
                                    sync.syncToPeer(currentIntake, updated, isReminderEnabled, intervalMinutes)
                                },
                                enabled = dailyGoal > WaterRepository.MIN_GOAL,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-250", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val updated = repository.increaseGoal()
                                    dailyGoal = updated
                                    sync.syncToPeer(currentIntake, updated, isReminderEnabled, intervalMinutes)
                                },
                                enabled = dailyGoal < WaterRepository.MAX_GOAL,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF0F3652),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("+250", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Button
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B1515),
                        contentColor = Color(0xFFFF8A80)
                    )
                ) {
                    Text("Reset Progress Today", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Reset Warning Overlay Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = {
                    Text(
                        text = "⚠️ Reset Today?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80)
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset today's water intake back to 0 ml?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            repository.resetIntake()
                            currentIntake = 0
                            sync.syncToPeer(0, dailyGoal, isReminderEnabled, intervalMinutes)
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Yes, Reset", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1F1414),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
