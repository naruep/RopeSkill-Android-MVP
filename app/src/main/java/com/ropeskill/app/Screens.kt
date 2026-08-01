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
import androidx.compose.ui.platform.LocalView
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
    onStartBasicBounce: () -> Unit,
    onStartSpeed30: () -> Unit,
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
                        onClick = onStartBasicBounce,
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
                            text = "START BASIC BOUNCE",
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
                onStartTraining = onStartBasicBounce,
            )
            Speed30Row(onStartTraining = onStartSpeed30)
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

@Composable
private fun Speed30Row(onStartTraining: () -> Unit) {
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
                .background(colors.secondary),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Speed 30",
                color = colors.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "30 seconds · counts valid right-foot landings",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        Text(text = "›", color = colors.onSurfaceVariant, fontSize = 24.sp)
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
    onToggleMusicMuted: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onPoseFrame: (PoseFrame) -> Unit,
    onPerformanceSnapshot: (PosePerformanceSnapshot) -> Unit = {},
) {
    KeepScreenAwakeWhileVisible()

    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember {
        mutableStateOf(isCameraPermissionGranted(context))
    }
    val colors = MaterialTheme.colorScheme
    val t730AttributionText = remember(uiState.t730AttributionSnapshot) {
        uiState.t730AttributionSnapshot?.let(::formatT730AttributionSnapshot)
    }
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
                        text = uiState.workoutMode.displayName.uppercase(Locale.US),
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
                        if (uiState.musicAvailable) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.musicMuted) {
                                            "Unmute music"
                                        } else {
                                            "Mute music"
                                        },
                                    )
                                },
                                enabled = uiState.musicPlaybackError == null,
                                onClick = {
                                    menuExpanded = false
                                    onToggleMusicMuted()
                                },
                            )
                        }
                        uiState.musicPlaybackError?.let { message ->
                            DropdownMenuItem(
                                text = { Text(message) },
                                enabled = false,
                                onClick = {},
                            )
                        }
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
                    onPerformanceSnapshot = onPerformanceSnapshot,
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
                        workoutMode = uiState.workoutMode,
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
                            value = if (uiState.workoutMode == WorkoutMode.SPEED_30) {
                                uiState.speedClassifierDiagnostic.name.replace('_', ' ')
                            } else {
                                uiState.detectorDiagnostic.displayName
                            },
                            color = PowerSportMuted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (
                    BuildConfig.DEBUG &&
                    cameraPermissionGranted &&
                    uiState.workoutMode == WorkoutMode.SPEED_30
                ) {
                    SpeedDiagnosticsOverlay(
                        uiState = uiState,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    )
                }
                if (
                    BuildConfig.DEBUG &&
                    shouldShowTrainingCameraOverlays(cameraPermissionGranted) &&
                    uiState.countEvidenceHistory.isNotEmpty()
                ) {
                    Text(
                        text = buildString {
                            append("COUNT HISTORY V8")
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
                                if (evidence.usedStrongHipRescue) {
                                    append(" RESCUE")
                                }
                                val foot = evidence.footContactEvidence
                                if (foot == null) {
                                    append("\n   FOOT N/A")
                                } else {
                                    append(
                                        String.format(
                                            Locale.US,
                                            "\n   HEEL %.3f/%.3f TOE %.3f/%.3f",
                                            foot.leftHeelRiseRatio,
                                            foot.rightHeelRiseRatio,
                                            foot.leftToeRiseRatio,
                                            foot.rightToeRiseRatio,
                                        ),
                                    )
                                }
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
                    shouldShowDebugDiagnosticPanel(
                        isDebugBuild = BuildConfig.DEBUG,
                        cameraPermissionGranted = cameraPermissionGranted,
                        uiState = uiState,
                    )
                ) {
                    Text(
                        text = buildString {
                            if (BuildConfig.DEBUG) {
                                uiState.t743LandingStateSnapshot?.let { snapshot ->
                                    append(formatT743LandingStateSnapshot(snapshot))
                                    append("\n")
                                }
                                uiState.t735TakeoffGateSnapshot?.let { snapshot ->
                                    append(formatT735TakeoffGateSnapshot(snapshot))
                                    append("\n")
                                }
                                uiState.t733RaCandidateSnapshot?.let { snapshot ->
                                    append(formatT733RaCandidateSnapshot(snapshot))
                                    append("\n")
                                }
                                t730AttributionText?.let { text ->
                                    append(text)
                                    append("\n")
                                }
                                if (
                                    t730AttributionText == null &&
                                    uiState.t743LandingStateSnapshot == null &&
                                    uiState.t735TakeoffGateSnapshot == null &&
                                    uiState.t733RaCandidateSnapshot == null
                                ) {
                                    uiState.t729ExperimentSnapshot?.let { snapshot ->
                                        append(formatT729ExperimentSnapshot(snapshot))
                                        append("\n")
                                    }
                                }
                            }
                            if (
                                t730AttributionText != null ||
                                uiState.t743LandingStateSnapshot != null ||
                                uiState.t735TakeoffGateSnapshot != null ||
                                uiState.t733RaCandidateSnapshot != null
                            ) {
                                append(
                                    String.format(
                                        Locale.US,
                                        "BASE A/L%d/%d SUP%d RES%d",
                                        uiState.diagnosticTransitionCounts[
                                            BounceDiagnostic.AIRBORNE
                                        ] ?: 0,
                                        uiState.diagnosticTransitionCounts[
                                            BounceDiagnostic.LANDED
                                        ] ?: 0,
                                        uiState.cooldownSuppressedCount,
                                        uiState.strongHipRescueCount,
                                    ),
                                )
                            } else {
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
                                append(
                                    String.format(
                                        Locale.US,
                                        "\nCOOLDOWN V7 SUP %d",
                                        uiState.cooldownSuppressedCount,
                                    ),
                                )
                                uiState.lastCooldownSuppressedEvidence?.let { evidence ->
                                    append(
                                        String.format(
                                            Locale.US,
                                            " LAST %d/%dms",
                                            evidence.intervalMillis,
                                            evidence.cooldownMillis,
                                        ),
                                    )
                                }
                                append(
                                    String.format(
                                        Locale.US,
                                        "\nSTRONG HIP RESCUE V8 %d",
                                        uiState.strongHipRescueCount,
                                    ),
                                )
                                if (
                                    BuildConfig.DEBUG &&
                                    uiState.cycleTraceHistory.isNotEmpty()
                                ) {
                                    append("\nCYCLE TRACE V9")
                                    uiState.cycleTraceHistory.forEach { evidence ->
                                        val ankleRise = evidence.ankleRiseRatio?.let {
                                            String.format(Locale.US, "%.3f", it)
                                        } ?: "---"
                                        val hipRise = evidence.hipRiseRatio?.let {
                                            String.format(Locale.US, "%.3f", it)
                                        } ?: "---"
                                        val interval =
                                            evidence.intervalMillis?.toString() ?: "---"
                                        append(
                                            String.format(
                                                Locale.US,
                                                "\n#%02d t%d d%s %s A%s H%s",
                                                evidence.sequence,
                                                evidence.elapsedMillis,
                                                interval,
                                                evidence.event.shortName(),
                                                ankleRise,
                                                hipRise,
                                            ),
                                        )
                                        evidence.landingReason?.let {
                                            append(" ${it.shortName()}")
                                        }
                                        evidence.countIntervalMillis?.let {
                                            append(" C$it")
                                        }
                                        evidence.airborneMillis?.let {
                                            append(" F$it")
                                        }
                                        if (
                                            evidence.event ==
                                            CycleTraceEvent.REJECTED_TAKEOFF
                                        ) {
                                            append(" ${evidence.diagnostic.shortName()}")
                                        }
                                        if (evidence.usedStrongHipRescue) {
                                            append(" RES")
                                        }
                                    }
                                }
                                if (
                                    BuildConfig.DEBUG &&
                                    uiState.takeoffPeakEvidenceHistory.isNotEmpty()
                                ) {
                                    append("\nTAKEOFF PEAK V11")
                                    uiState.takeoffPeakEvidenceHistory.forEach { evidence ->
                                        val peakInterval =
                                            evidence.peakFrameIntervalMillis
                                                ?.toString() ?: "---"
                                        val nextInterval =
                                            evidence.nextFrameIntervalMillis
                                                ?.toString() ?: "---"
                                        append(
                                            String.format(
                                                Locale.US,
                                                "\n%s A%.3f/%.3f H%.3f/%.3f" +
                                                    " F%d D%d P%s N%s",
                                                evidence.outcome.shortName(),
                                                evidence.smoothedAnkleRiseRatio,
                                                evidence.rawAnkleRiseRatio,
                                                evidence.smoothedHipRiseRatio,
                                                evidence.rawHipRiseRatio,
                                                evidence.riseFrameCount,
                                                evidence.riseMillis,
                                                peakInterval,
                                                nextInterval,
                                            ),
                                        )
                                        if (
                                            evidence.outcome ==
                                            TakeoffPeakOutcome.REJECTED
                                        ) {
                                            append(" ${evidence.diagnostic.shortName()}")
                                            append(
                                                String.format(
                                                    Locale.US,
                                                    "\n  BIL L%.3f %s R%.3f %s MIN%.3f",
                                                    evidence.rawLeftAnkleRiseRatio,
                                                    if (
                                                        evidence
                                                            .leftIndividualAnkleGatePassed
                                                    ) {
                                                        "PASS"
                                                    } else {
                                                        "FAIL"
                                                    },
                                                    evidence.rawRightAnkleRiseRatio,
                                                    if (
                                                        evidence
                                                            .rightIndividualAnkleGatePassed
                                                    ) {
                                                        "PASS"
                                                    } else {
                                                        "FAIL"
                                                    },
                                                    evidence.individualAnkleRiseThreshold,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        color = PowerSportMuted,
                        fontSize = if (
                            BuildConfig.DEBUG &&
                            (
                                uiState.t730AttributionSnapshot != null ||
                                uiState.t735TakeoffGateSnapshot != null ||
                                uiState.t733RaCandidateSnapshot != null ||
                                uiState.t729ExperimentSnapshot != null ||
                                uiState.cycleTraceHistory.isNotEmpty() ||
                                    uiState.takeoffPeakEvidenceHistory.isNotEmpty()
                            )
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
                        (
                            uiState.status == WorkoutStatus.RUNNING &&
                            uiState.workoutMode == WorkoutMode.BASIC_BOUNCE
                        ),
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
                        when {
                            uiState.status == WorkoutStatus.PAUSED -> "RESUME"
                            uiState.workoutMode == WorkoutMode.SPEED_30 &&
                                uiState.status == WorkoutStatus.RUNNING -> "30S ACTIVE"
                            else -> "PAUSE"
                        },
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
private fun KeepScreenAwakeWhileVisible() {
    val view = LocalView.current

    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true

        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
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

internal fun shouldShowDebugDiagnosticPanel(
    isDebugBuild: Boolean,
    cameraPermissionGranted: Boolean,
    uiState: TrainingUiState,
): Boolean =
    isDebugBuild &&
        uiState.workoutMode == WorkoutMode.BASIC_BOUNCE &&
        shouldShowTrainingCameraOverlays(cameraPermissionGranted) &&
        (
            uiState.diagnosticTransitionCounts.isNotEmpty() ||
                uiState.cooldownSuppressedCount > 0 ||
                uiState.strongHipRescueCount > 0 ||
                uiState.t735TakeoffGateSnapshot != null ||
                uiState.t743LandingStateSnapshot != null ||
                uiState.t733RaCandidateSnapshot != null ||
                uiState.t730AttributionSnapshot != null ||
                uiState.cycleTraceHistory.isNotEmpty() ||
                uiState.takeoffPeakEvidenceHistory.isNotEmpty()
        )

internal fun shouldShowWorkoutMetrics(uiState: TrainingUiState): Boolean =
    uiState.hasWorkoutStarted

internal fun shouldShowJumpMetric(uiState: TrainingUiState): Boolean =
    uiState.hasWorkoutStarted &&
        !uiState.showGo &&
        uiState.status != WorkoutStatus.POSITIONING &&
        uiState.status != WorkoutStatus.COUNTDOWN &&
        uiState.status != WorkoutStatus.ARMED

@Composable
private fun SpeedDiagnosticsOverlay(
    uiState: TrainingUiState,
    modifier: Modifier = Modifier,
) {
    val step = uiState.speedStepDiagnostics
    val classifier = uiState.speedClassifierDiagnostics
    val perf = uiState.speedPerformanceSnapshot
    Text(
        text = buildString {
            append("SPEED V1  ${uiState.speedClassifierDiagnostic.name}")
            append("\nL ${step?.leftLandingTimestampsMillis?.size ?: 0}")
            append("  R ${step?.rightLandingTimestampsMillis?.size ?: 0}")
            append("  C ${step?.countedRightTimestampsMillis?.size ?: 0}")
            append("  RR ${step?.repeatedRightRejects ?: 0}")
            append("\nB ${step?.bothFeetRejects ?: 0}")
            append("  U ${step?.unclearLandingRejects ?: 0}")
            append("  VIS ${classifier?.lowVisibilityFrames ?: 0}")
            append("  OOS ${classifier?.outOfOrderFrames ?: 0}")
            append("  TL ${step?.trackingLossEvents ?: 0}")
            append(
                String.format(
                    Locale.US,
                    "\nFPS %.1f  LAT %d/%dms  SKIP~ %d",
                    perf.resultFps,
                    perf.averageLatencyMillis,
                    perf.maxLatencyMillis,
                    perf.estimatedSkippedFrames,
                ),
            )
        },
        color = PowerSportMuted,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun WorkoutMetricsOverlay(
    jumpCount: Int,
    elapsedMillis: Long,
    workoutMode: WorkoutMode,
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
        val elapsedTime = formatWorkoutTime(elapsedMillis, workoutMode)
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
                    contentDescription = if (workoutMode == WorkoutMode.SPEED_30) {
                        "Remaining time $elapsedTime"
                    } else {
                        "Elapsed time $elapsedTime"
                    }
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
                    text = if (workoutMode == WorkoutMode.SPEED_30) "RIGHT STEPS" else "JUMPS",
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
                text = uiState.workoutMode.displayName.uppercase(Locale.US),
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
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
                    label = if (uiState.workoutMode == WorkoutMode.SPEED_30) {
                        "RIGHT STEPS"
                    } else {
                        "JUMPS"
                    },
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

private fun CycleTraceEvent.shortName(): String = when (this) {
    CycleTraceEvent.TAKEOFF -> "T"
    CycleTraceEvent.LANDING_COUNTED -> "LC"
    CycleTraceEvent.LANDING_SUPPRESSED -> "LS"
    CycleTraceEvent.REJECTED_TAKEOFF -> "R"
}

private fun TakeoffPeakOutcome.shortName(): String = when (this) {
    TakeoffPeakOutcome.COUNTED -> "C"
    TakeoffPeakOutcome.SUPPRESSED -> "S"
    TakeoffPeakOutcome.REJECTED -> "R"
}

private fun LandingDetectionReason.shortName(): String = when (this) {
    LandingDetectionReason.RETURNED_TO_BASELINE -> "B"
    LandingDetectionReason.COMPLETED_VERTICAL_CYCLE -> "C"
    LandingDetectionReason.BOTH -> "BC"
    LandingDetectionReason.TIMED_OUT_AFTER_DESCENT -> "TD"
}

fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

internal fun formatWorkoutTime(elapsedMillis: Long, mode: WorkoutMode): String {
    val displayMillis = if (mode == WorkoutMode.SPEED_30) {
        val remaining =
            (SpeedStepDetector.SPEED_30_DURATION_MILLIS - elapsedMillis).coerceAtLeast(0L)
        ((remaining + 999L) / 1_000L) * 1_000L
    } else {
        elapsedMillis
    }
    return formatElapsedTime(displayMillis)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    RopeSkillTheme {
        HomeScreen(
            nickname = "Jay",
            onStartBasicBounce = {},
            onStartSpeed30 = {},
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
