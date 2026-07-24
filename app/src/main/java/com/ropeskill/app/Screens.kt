package com.ropeskill.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ropeskill.app.ui.theme.PowerSportMuted
import com.ropeskill.app.ui.theme.PowerSportOnBackground
import com.ropeskill.app.ui.theme.PowerSportOrange
import com.ropeskill.app.ui.theme.RopeSkillTheme
import java.util.Locale

@Composable
fun HomeScreen(
    nickname: String = "",
    onStartTraining: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val needsLogoBackdrop = colors.background.luminance() > 0.5f
    Scaffold(
        containerColor = colors.background,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Button(
                    onClick = onStartTraining,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                ) {
                    Text(
                        text = "START TRAINING  →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            PowerSportHeader(onOpenSettings = onOpenSettings)

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(152.dp),
                ) {
                    if (needsLogoBackdrop) {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF071426)),
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.ropeskill_jump_rope_logo),
                        contentDescription = "RopeSkill jump rope logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(152.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "TRAIN\nSTRONGER.",
                color = colors.onBackground,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 50.sp,
            )
            if (nickname.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "WELCOME BACK, ${nickname.uppercase(Locale.getDefault())}",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
            }
            Spacer(modifier = Modifier.height(30.dp))

            WorkoutSummaryCard(onOpenHistory = onOpenHistory)
        }
    }
}

@Composable
private fun PowerSportHeader(onOpenSettings: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "ROPESKILL",
            color = colors.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onOpenSettings != null) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Text(
                text = "MVP",
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun WorkoutSummaryCard(onOpenHistory: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
    ) {
        Text(
            text = "Basic Bounce",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(
            onClick = onOpenHistory,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "TRAINING HISTORY  →",
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
fun TrainingScreen(
    uiState: TrainingUiState,
    settings: UserSettings,
    onAddJump: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onPoseFrame: (PoseFrame) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    WorkoutCues(uiState = uiState, settings = settings)

    Scaffold(
        containerColor = colors.background,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = "BASIC BOUNCE",
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                    )
                    Text(
                        text = "TRAINING",
                        color = colors.onBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(
                    label = uiState.status.displayName.uppercase(Locale.US),
                    color = statusColor(uiState.status),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true },
                ) {
                    Text(
                        text = "⋮",
                        color = colors.onBackground,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reset session") },
                            enabled = uiState.status != WorkoutStatus.IDLE || uiState.jumpCount > 0,
                            onClick = {
                                menuExpanded = false
                                showResetConfirmation = true
                            },
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CompactMetric(
                    label = "JUMPS",
                    value = uiState.jumpCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                CompactMetric(
                    label = "TIME",
                    value = formatElapsedTime(uiState.elapsedMillis),
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF171A1E), Color.Black),
                        ),
                    ),
            ) {
                CameraPermissionContent(
                    onPoseFrame = onPoseFrame,
                    modifier = Modifier.fillMaxSize(),
                )
                TrainingStartOverlay(
                    uiState = uiState,
                    modifier = Modifier.align(Alignment.Center),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    CameraStatusLabel(
                        label = "TRACKING",
                        value = uiState.trackingStatus.displayName,
                        color = PowerSportOnBackground,
                        modifier = Modifier.weight(1f),
                    )
                    CameraStatusLabel(
                        label = "DETECTOR",
                        value = uiState.detectorDiagnostic.displayName,
                        color = PowerSportMuted,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (uiState.countEvidenceHistory.isNotEmpty()) {
                    Text(
                        text = buildString {
                            append("COUNT HISTORY V3")
                            uiState.countEvidenceHistory.forEachIndexed { index, evidence ->
                                append(
                                    String.format(
                                        Locale.US,
                                        "\n%d  L %.3f R %.3f H %.3f ΔR %.3f %dms" +
                                            " D %.4f/%.4f %s",
                                        index + 1,
                                        evidence.leftAnkleRiseRatio,
                                        evidence.rightAnkleRiseRatio,
                                        evidence.hipRiseRatio,
                                        evidence.ankleDifferenceRatio,
                                        evidence.airborneMillis,
                                        evidence.ankleDifference,
                                        evidence.ankleDifferenceLimit,
                                        if (evidence.feetSynchronized) "PASS" else "FAIL",
                                    ),
                                )
                            }
                        },
                        color = PowerSportMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                if (uiState.diagnosticTransitionCounts.isNotEmpty()) {
                    Text(
                        text = buildString {
                            append("MEDIUM DIAGNOSTIC V4")
                            append(
                                String.format(
                                    Locale.US,
                                    "\nANK %d  HIP %d  SYNC %d  AIR %d  LAND %d",
                                    uiState.diagnosticTransitionCounts[
                                        BounceDiagnostic.ANKLE_RISE_TOO_SMALL
                                    ] ?: 0,
                                    uiState.diagnosticTransitionCounts[
                                        BounceDiagnostic.HIP_RISE_TOO_SMALL
                                    ] ?: 0,
                                    uiState.diagnosticTransitionCounts[
                                        BounceDiagnostic.FEET_NOT_SYNCHRONIZED
                                    ] ?: 0,
                                    uiState.diagnosticTransitionCounts[
                                        BounceDiagnostic.AIRBORNE
                                    ] ?: 0,
                                    uiState.diagnosticTransitionCounts[
                                        BounceDiagnostic.LANDED
                                    ] ?: 0,
                                ),
                            )
                        },
                        color = PowerSportMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = if (uiState.status == WorkoutStatus.PAUSED) onStart else onPause,
                    enabled = uiState.status == WorkoutStatus.PAUSED ||
                        uiState.status == WorkoutStatus.POSITIONING ||
                        uiState.status == WorkoutStatus.COUNTDOWN ||
                        uiState.status == WorkoutStatus.ARMED ||
                        uiState.status == WorkoutStatus.RUNNING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.status == WorkoutStatus.PAUSED) {
                            colors.primary
                        } else {
                            colors.surfaceVariant
                        },
                        contentColor = if (uiState.status == WorkoutStatus.PAUSED) {
                            colors.onPrimary
                        } else {
                            colors.onSurfaceVariant
                        },
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(52.dp),
                ) {
                    Text(
                        if (uiState.status == WorkoutStatus.PAUSED) "RESUME" else "PAUSE",
                        fontWeight = FontWeight.Black,
                    )
                }
                OutlinedButton(
                    onClick = onFinish,
                    enabled = uiState.status != WorkoutStatus.IDLE &&
                        uiState.status != WorkoutStatus.FINISHED,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("FINISH", fontWeight = FontWeight.Bold)
                }
            }

            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = onAddJump,
                    enabled = uiState.status == WorkoutStatus.RUNNING,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.onSurfaceVariant,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("TEST +1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset session?") },
            text = { Text("Your current jumps and time will return to zero.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onReset()
                    },
                ) {
                    Text(
                        "RESET",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("CANCEL")
                }
            },
        )
    }
}

@Composable
private fun WorkoutCues(
    uiState: TrainingUiState,
    settings: UserSettings,
) {
    val context = LocalContext.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }

    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(uiState.countdownSeconds, uiState.showGo) {
        val countdownTick = uiState.status == WorkoutStatus.COUNTDOWN &&
            uiState.countdownSeconds != null
        val goCue = uiState.showGo
        if (!countdownTick && !goCue) return@LaunchedEffect

        if (settings.soundEnabled) {
            toneGenerator.startTone(
                if (goCue) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP,
                if (goCue) 220 else 100,
            )
        }
        if (settings.vibrationEnabled) {
            context.getSystemService(Vibrator::class.java)?.vibrate(
                VibrationEffect.createOneShot(
                    if (goCue) 100L else 45L,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                ),
            )
        }
    }
}

@Composable
private fun TrainingStartOverlay(
    uiState: TrainingUiState,
    modifier: Modifier = Modifier,
) {
    val message = when {
        uiState.showGo -> "GO!"
        uiState.status == WorkoutStatus.COUNTDOWN -> uiState.countdownSeconds?.toString()
        uiState.status == WorkoutStatus.ARMED -> "START"
        uiState.status == WorkoutStatus.POSITIONING -> when (uiState.positioningGuidance) {
            PositioningGuidance.STEP_BACK -> "STEP BACK"
            PositioningGuidance.MOVE_CLOSER -> "MOVE CLOSER"
            PositioningGuidance.FULL_BODY_REQUIRED -> "SHOW FULL BODY"
            PositioningGuidance.DISTANCE_GOOD -> "DISTANCE GOOD\nHOLD STILL"
        }
        else -> null
    } ?: return

    Text(
        text = message,
        color = PowerSportOrange,
        fontSize = if (message.length <= 2) 72.sp else 34.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        lineHeight = if (message.length <= 2) 72.sp else 36.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

@Composable
private fun CameraStatusLabel(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$label\n${value.uppercase(Locale.US)}",
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        lineHeight = 11.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 5.dp),
    )
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text(
                text = label,
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                color = colors.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp,
            )
        }
    }
}

@Composable
fun ResultScreen(uiState: TrainingUiState, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = "BACK TO HOME  →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            PowerSportHeader()

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "SESSION COMPLETE",
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = "STRONG\nFINISH.",
                color = colors.onBackground,
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 48.sp,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ResultMetric(
                    label = "JUMPS",
                    value = uiState.jumpCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                ResultMetric(
                    label = "TIME",
                    value = formatElapsedTime(uiState.elapsedMillis),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun statusColor(status: WorkoutStatus): Color = when (status) {
    WorkoutStatus.RUNNING -> MaterialTheme.colorScheme.secondary
    WorkoutStatus.COUNTDOWN -> MaterialTheme.colorScheme.secondary
    WorkoutStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    WorkoutStatus.FINISHED -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun ResultMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(132.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        ) {
        Text(
            text = label,
            color = colors.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = value,
            color = colors.onSurface,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 46.sp,
        )
        }
    }
}

fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    RopeSkillTheme {
        HomeScreen(
            nickname = "Jay",
            onStartTraining = {},
            onOpenSettings = {},
            onOpenHistory = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    RopeSkillTheme {
        ResultScreen(
            uiState = TrainingUiState(jumpCount = 12, elapsedMillis = 34_000),
            onDone = {},
        )
    }
}
