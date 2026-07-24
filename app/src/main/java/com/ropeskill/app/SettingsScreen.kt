package com.ropeskill.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ropeskill.app.ui.theme.RopeSkillTheme

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onCountdownChange: (Int) -> Unit,
    onMeasurementUnitsChange: (MeasurementUnits) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onResetSettings: () -> Unit,
) {
    var nicknameDialogVisible by remember { mutableStateOf(false) }
    var resetDialogVisible by remember { mutableStateOf(false) }
    var privacyDialogVisible by remember { mutableStateOf(false) }
    var aboutDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(onBack = onBack)
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SettingsSection(title = "PROFILE") {
                SettingsRow(
                    title = "Nickname",
                    subtitle = settings.nickname.ifBlank { "Not set" },
                    onClick = { nicknameDialogVisible = true },
                )
                SettingsRow(
                    title = "Personal profile",
                    subtitle = "Height, weight and BMI · Coming later",
                    enabled = false,
                )
            }

            SettingsSection(title = "TRAINING") {
                SettingsSwitchRow(
                    title = "Sound cues",
                    subtitle = "Play workout status sounds",
                    checked = settings.soundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                )
                SettingsSwitchRow(
                    title = "Vibration",
                    subtitle = "Use haptic feedback for key actions",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChange,
                )
                CountdownSettingRow(
                    selectedSeconds = settings.countdownSeconds,
                    onSelected = onCountdownChange,
                )
                UnitsSettingRow(
                    selectedUnits = settings.measurementUnits,
                    onSelected = onMeasurementUnitsChange,
                )
            }

            SettingsSection(title = "APPEARANCE") {
                ThemeSettingRow(
                    selectedTheme = settings.appTheme,
                    onSelected = onAppThemeChange,
                )
            }

            SettingsSection(title = "DATA & PRIVACY") {
                SettingsRow(
                    title = "Privacy & on-device processing",
                    subtitle = "How RopeSkill handles camera and pose data",
                    onClick = { privacyDialogVisible = true },
                )
            }

            SettingsSection(title = "SUPPORT") {
                SettingsRow(
                    title = "About RopeSkill",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    onClick = { aboutDialogVisible = true },
                )
            }

            OutlinedButton(
                onClick = { resetDialogVisible = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("RESET SETTINGS", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (nicknameDialogVisible) {
        NicknameDialog(
            currentNickname = settings.nickname,
            onDismiss = { nicknameDialogVisible = false },
            onSave = {
                nicknameDialogVisible = false
                onNicknameChange(it)
            },
        )
    }

    if (resetDialogVisible) {
        ConfirmationDialog(
            title = "Reset settings?",
            message = "Nickname and training preferences will return to their defaults. Training history is not affected.",
            confirmLabel = "RESET",
            onDismiss = { resetDialogVisible = false },
            onConfirm = {
                resetDialogVisible = false
                onResetSettings()
            },
        )
    }

    if (privacyDialogVisible) {
        InformationDialog(
            title = "Privacy",
            message = "Camera frames and pose landmarks are processed on this device. RopeSkill does not save or upload camera images, video, or pose landmarks by default.",
            onDismiss = { privacyDialogVisible = false },
        )
    }

    if (aboutDialogVisible) {
        InformationDialog(
            title = "About RopeSkill",
            message = "RopeSkill ${BuildConfig.VERSION_NAME}\n\nAn Android MVP for Basic Bounce jump-rope training using on-device pose estimation.",
            onDismiss = { aboutDialogVisible = false },
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Text(
                text = "‹",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = "SETTINGS",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            if (enabled && onClick != null) {
                Text(text = "›", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
            }
        }
        SettingsDivider()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
        SettingsDivider()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 16.dp),
    )
}

@Composable
private fun CountdownSettingRow(
    selectedSeconds: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingsRow(
            title = "Countdown",
            subtitle = "$selectedSeconds seconds",
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            listOf(3, 5, 10).forEach { seconds ->
                DropdownMenuItem(
                    text = { Text("$seconds seconds") },
                    onClick = {
                        expanded = false
                        onSelected(seconds)
                    },
                )
            }
        }
    }
}

@Composable
private fun UnitsSettingRow(
    selectedUnits: MeasurementUnits,
    onSelected: (MeasurementUnits) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedUnits) {
        MeasurementUnits.METRIC -> "Metric · kg, cm"
        MeasurementUnits.IMPERIAL -> "Imperial · lb, ft"
    }
    Box {
        SettingsRow(
            title = "Measurement units",
            subtitle = selectedLabel,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Metric · kg, cm") },
                onClick = {
                    expanded = false
                    onSelected(MeasurementUnits.METRIC)
                },
            )
            DropdownMenuItem(
                text = { Text("Imperial · lb, ft") },
                onClick = {
                    expanded = false
                    onSelected(MeasurementUnits.IMPERIAL)
                },
            )
        }
    }
}

@Composable
private fun ThemeSettingRow(
    selectedTheme: AppTheme,
    onSelected: (AppTheme) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedTheme) {
        AppTheme.SYSTEM -> "System default"
        AppTheme.DARK -> "Dark"
        AppTheme.LIGHT -> "Light"
    }
    Box {
        SettingsRow(
            title = "Theme",
            subtitle = selectedLabel,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (theme) {
                                AppTheme.SYSTEM -> "System default"
                                AppTheme.DARK -> "Dark"
                                AppTheme.LIGHT -> "Light"
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(theme)
                    },
                )
            }
        }
    }
}

@Composable
private fun NicknameDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var nickname by remember(currentNickname) { mutableStateOf(currentNickname) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nickname") },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(30) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                supportingText = { Text("${nickname.length}/30") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(nickname) }) {
                Text(
                    "SAVE",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
    )
}

@Composable
private fun InformationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CLOSE",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RopeSkillTheme {
        SettingsScreen(
            settings = UserSettings(nickname = "Jay"),
            onBack = {},
            onNicknameChange = {},
            onSoundEnabledChange = {},
            onVibrationEnabledChange = {},
            onCountdownChange = {},
            onMeasurementUnitsChange = {},
            onAppThemeChange = {},
            onResetSettings = {},
        )
    }
}
