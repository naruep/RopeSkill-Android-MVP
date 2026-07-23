package com.ropeskill.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PowerSportBackground = Color(0xFF0D0F12)
val PowerSportSurface = Color(0xFF191C20)
val PowerSportSurfaceHigh = Color(0xFF22262C)
val PowerSportOrange = Color(0xFFFF6B1A)
val PowerSportGreen = Color(0xFF65D38E)
val PowerSportOnBackground = Color(0xFFF5F7FA)
val PowerSportMuted = Color(0xFFA9AFB8)
val PowerSportOutline = Color(0xFF30343A)

private val RopeSkillColors = darkColorScheme(
    primary = PowerSportOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF5C2100),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = PowerSportGreen,
    onSecondary = Color(0xFF00210D),
    background = PowerSportBackground,
    onBackground = PowerSportOnBackground,
    surface = PowerSportSurface,
    onSurface = PowerSportOnBackground,
    surfaceVariant = PowerSportSurfaceHigh,
    onSurfaceVariant = PowerSportMuted,
    outline = PowerSportOutline,
)

@Composable
fun RopeSkillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RopeSkillColors,
        content = content,
    )
}
