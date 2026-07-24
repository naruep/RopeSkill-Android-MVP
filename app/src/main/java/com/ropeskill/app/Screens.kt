package com.ropeskill.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun HomeScreen(
    nickname: String = "",
    savedSessions: List<TrainingSession> = emptyList(),
    onStartTraining: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val summary = remember(savedSessions) {
        summarizeCurrentWeek(savedSessions)
    }
    val latestSession = savedSessions.firstOrNull()
    Scaffold(
        containerColor = colors.background,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.background(colors.background)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Button(
                        onClick = onStartTraining,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                    ) {
                        Text(
                            text = "START TRAINING",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }
                bottomBar()
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            PowerSportHeader(onOpenSettings = onOpenSettings)

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = if (nickname.isBlank()) "READY TO TRAIN?" else "WELCOME BACK",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (nickname.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nickname,
                    color = colors.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(modifier = Modifier.height(28.dp))

            WeeklySummary(summary = summary)

            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "CONTINUE TRAINING",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(12.dp))
            BasicBounceRow(
                latestSession = latestSession,
                onStartTraining = onStartTraining,
            )
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
            text = "ROPE",
            color = colors.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = "SKILL",
            color = colors.primary,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
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
private fun WeeklySummary(summary: WeeklyTrainingSummary) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        SummaryMetric(
            value = summary.jumpCount.toString(),
            label = "JUMPS\nTHIS WEEK",
        )
        SummaryMetric(
            value = formatCompactDuration(summary.durationMillis),
            label = "TIME\nTHIS WEEK",
        )
        SummaryMetric(
            value = summary.sessionCount.toString(),
            label = "SESSIONS\nTHIS WEEK",
        )
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp),
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BasicBounceRow(
    latestSession: TrainingSession?,
    onStartTraining: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onStartTraining)
            .padding(vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(42.dp)
                .background(colors.primary),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Basic Bounce",
                color = colors.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = latestSession?.let {
                    "Last: ${it.jumpCount} jumps · ${formatCompactDuration(it.durationMillis)}"
                } ?: "No completed training yet",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Text(
            text = "›",
            color = colors.onSurfaceVariant,
            fontSize = 24.sp,
        )
    }
    HorizontalDivider(color = colors.outline)
}

internal data class WeeklyTrainingSummary(
    val jumpCount: Int,
    val durationMillis: Long,
    val sessionCount: Int,
)

internal fun summarizeCurrentWeek(
    sessions: List<TrainingSession>,
    nowEpochMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): WeeklyTrainingSummary {
    val weekStart = Instant.ofEpochMilli(nowEpochMillis)
        .atZone(zoneId)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
    val currentSessions = sessions.filter {
        it.completedAtEpochMillis in weekStart..nowEpochMillis
    }
    return WeeklyTrainingSummary(
        jumpCount = currentSessions.sumOf { it.jumpCount },
        durationMillis = currentSessions.sumOf { it.durationMillis },
        sessionCount = currentSessions.size,
    )
}

private fun formatCompactDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
    return when {
        totalMinutes >= 60L -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        totalMinutes > 0L -> "${totalMinutes}m"
        else -> "${durationMillis.coerceAtLeast(0L) / 1_000L}s"
    }
}

internal enum class MainDestination(
    val route: String,
    val label: String,
    val iconRes: Int,
) {
    HOME("home", "Home", R.drawable.ic_home),
    HISTORY("history", "History", R.drawable.ic_history),
    SETTINGS("settings", "Settings", R.drawable.ic_settings),
}

@Composable
internal fun RopeSkillBottomBar(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        MainDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
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
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember {
        mutableStateOf(isCameraPermissionGranted(context))
    }
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
                    hasCameraPermission = cameraPermissionGranted,
                    onPoseFrame = onPoseFrame,
                    onPermissionResult = { cameraPermissionGranted = it },
                    modifier = Modifier.fillMaxSize(),
                )
                if (shouldShowTrainingCameraOverlays(cameraPermissionGranted)) {
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
                }
                if (
                    shouldShowTrainingCameraOverlays(cameraPermissionGranted) &&
                    uiState.countEvidenceHistory.isNotEmpty()
                ) {
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
                if (
                    shouldShowTrainingCameraOverlays(cameraPermissionGranted) &&
                    uiState.diagnosticTransitionCounts.isNotEmpty()
                ) {
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
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
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
                    border = BorderStroke(1.dp, colors.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
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

internal fun shouldShowTrainingCameraOverlays(cameraPermissionGranted: Boolean): Boolean =
    cameraPermissionGranted

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
                        text = "BACK TO HOME",
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
