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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
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
                            fontSize = 18.sp,
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
            PowerSportHeader(showMvp = false)

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = if (nickname.isBlank()) "READY TO TRAIN?" else "WELCOME BACK",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (nickname.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nickname,
                    color = colors.onBackground,
                    fontSize = 34.sp,
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
                fontSize = 14.sp,
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
private fun PowerSportHeader(showMvp: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "ROPE",
            color = colors.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = "SKILL",
            color = colors.primary,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showMvp) {
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
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 14.sp,
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = latestSession?.let {
                    "Last: ${it.jumpCount} jumps · ${formatCompactDuration(it.durationMillis)}"
                } ?: "No completed training yet",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp,
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
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colors.background,
        tonalElevation = 0.dp,
    ) {
        MainDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = 0.14f),
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        fontSize = 13.sp,
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
                if (
                    shouldShowTrainingCameraOverlays(cameraPermissionGranted) &&
                    shouldShowWorkoutMetrics(uiState)
                ) {
                    WorkoutMetricsOverlay(
                        jumpCount = uiState.jumpCount,
                        elapsedMillis = uiState.elapsedMillis,
                        showJumpCount = shouldShowJumpMetric(uiState),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
                    (
                        uiState.diagnosticTransitionCounts.isNotEmpty() ||
                            (
                                BuildConfig.DEBUG &&
                                    uiState.rejectedTakeoffEvidenceHistory.isNotEmpty()
                            )
                    )
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
                            if (
                                BuildConfig.DEBUG &&
                                uiState.rejectedTakeoffEvidenceHistory.isNotEmpty()
                            ) {
                                append("\nREJECTED TAKEOFF V5")
                                val rejectedEvidence =
                                    uiState.rejectedTakeoffEvidenceHistory
                                rejectedEvidence.forEachIndexed { index, evidence ->
                                    append(
                                        String.format(
                                            Locale.US,
                                            "\n%d A %.3f/%.3f H %.3f/%.3f" +
                                                "\n  R %.2f/%.2f S %s %s",
                                            index + 1,
                                            evidence.ankleRiseRatio,
                                            evidence.ankleRiseThreshold,
                                            evidence.hipRiseRatio,
                                            evidence.hipRiseThreshold,
                                            evidence.hipToAnkleRiseRatio,
                                            evidence.hipToAnkleRiseThreshold,
                                            if (evidence.feetSynchronized) "P" else "F",
                                            evidence.diagnostic.shortName(),
                                        ),
                                    )
                                }
                            }
                        },
                        color = PowerSportMuted,
                        fontSize = if (
                            BuildConfig.DEBUG &&
                            uiState.rejectedTakeoffEvidenceHistory.isNotEmpty()
                        ) {
                            8.sp
                        } else {
                            9.sp
                        },
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

internal fun shouldShowWorkoutMetrics(uiState: TrainingUiState): Boolean =
    uiState.hasWorkoutStarted

internal fun shouldShowJumpMetric(uiState: TrainingUiState): Boolean =
    uiState.hasWorkoutStarted &&
        !uiState.showGo &&
        uiState.status != WorkoutStatus.POSITIONING &&
        uiState.status != WorkoutStatus.COUNTDOWN &&
        uiState.status != WorkoutStatus.ARMED

@Composable
private fun WorkoutMetricsOverlay(
    jumpCount: Int,
    elapsedMillis: Long,
    showJumpCount: Boolean,
    modifier: Modifier = Modifier,
) {
    val textShadow = Shadow(
        color = Color.Black,
        offset = Offset(0f, 3f),
        blurRadius = 8f,
    )
    Box(
        modifier = modifier,
    ) {
        val elapsedTime = formatElapsedTime(elapsedMillis)
        Text(
            text = elapsedTime,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(shadow = textShadow),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 78.dp)
                .clearAndSetSemantics {
                    contentDescription = "Elapsed time $elapsedTime"
                },
        )
        if (showJumpCount) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = jumpCount.toString(),
                    color = Color.White,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = TextStyle(shadow = textShadow),
                )
                Text(
                    text = "JUMPS",
                    color = PowerSportOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    style = TextStyle(shadow = textShadow),
                )
            }
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
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(0f, 3f),
                blurRadius = 8f,
            ),
        ),
        modifier = modifier
            .fillMaxWidth(0.9f)
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
fun ResultScreen(
    uiState: TrainingUiState,
    onViewHistory: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.onBackground,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = "VIEW HISTORY",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
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
                        fontSize = 18.sp,
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PlainResultMetric(
                    label = "JUMPS",
                    value = uiState.jumpCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                PlainResultMetric(
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
private fun PlainResultMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = value,
            color = colors.onBackground,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 48.sp,
        )
    }
}

private fun BounceDiagnostic.shortName(): String = when (this) {
    BounceDiagnostic.ANKLE_RISE_TOO_SMALL -> "ANK"
    BounceDiagnostic.HIP_RISE_TOO_SMALL -> "HIP"
    BounceDiagnostic.FEET_NOT_SYNCHRONIZED -> "SYNC"
    else -> name
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
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    RopeSkillTheme {
        ResultScreen(
            uiState = TrainingUiState(jumpCount = 12, elapsedMillis = 34_000),
            onViewHistory = {},
            onDone = {},
        )
    }
}
