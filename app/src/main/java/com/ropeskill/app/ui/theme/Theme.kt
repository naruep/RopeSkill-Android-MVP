package com.ropeskill.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

val PowerSportBackground = Color(0xFF0D0F12)
val PowerSportSurface = Color(0xFF191C20)
val PowerSportOrange = Color(0xFFFF6B1A)
val PowerSportOnBackground = Color(0xFFF5F7FA)
val PowerSportMuted = Color(0xFFA9AFB8)
val PowerSportOutline = Color(0xFF30343A)

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun RopeSkillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
