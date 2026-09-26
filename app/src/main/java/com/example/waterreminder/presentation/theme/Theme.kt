package com.example.waterreminder.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// Material You Wear OS Color Tokens
val AquaElectric = Color(0xFF38D6FF)
val AquaPrimaryVariant = Color(0xFF00A3FF)
val AquaSecondary = Color(0xFF8AD7FF)
val SurfaceDark = Color(0xFF070B0E)
val ContainerDark = Color(0xFF141C24)
val ContainerElevated = Color(0xFF1F2A36)
val ContainerChipPill = Color(0xFF1A2A3A)

val WearWaterColors = Colors(
    primary = AquaElectric,
    primaryVariant = AquaPrimaryVariant,
    secondary = AquaSecondary,
    background = SurfaceDark,
    surface = ContainerDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun WaterReminderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearWaterColors,
        content = content
    )
}
