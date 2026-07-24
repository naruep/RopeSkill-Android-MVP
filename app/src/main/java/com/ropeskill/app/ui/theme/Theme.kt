package com.ropeskill.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.ropeskill.app.AppTheme

val PowerSportBackground = Color(0xFF0D0F12)
val PowerSportSurface = Color(0xFF191C20)
val PowerSportSurfaceHigh = Color(0xFF22262C)
val PowerSportOrange = Color(0xFFFF6B1A)
val PowerSportGreen = Color(0xFF65D38E)
val PowerSportOnBackground = Color(0xFFF5F7FA)
val PowerSportMuted = Color(0xFFA9AFB8)
val PowerSportOutline = Color(0xFF30343A)

private val RopeSkillDarkColors = darkColorScheme(
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

private val RopeSkillLightColors = lightColorScheme(
    primary = Color(0xFFC24100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF341000),
    secondary = Color(0xFF006D3A),
    onSecondary = Color.White,
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF181A1D),
    surface = Color.White,
    onSurface = Color(0xFF181A1D),
    surfaceVariant = Color(0xFFE9EAEC),
    onSurfaceVariant = Color(0xFF5E636B),
    outline = Color(0xFFC7C9CD),
)

@Composable
fun RopeSkillTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (useDarkTheme) RopeSkillDarkColors else RopeSkillLightColors,
        content = content,
    )
}
