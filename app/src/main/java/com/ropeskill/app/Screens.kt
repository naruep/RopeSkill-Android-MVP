package com.ropeskill.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ropeskill.app.ui.theme.PowerSportBackground
import com.ropeskill.app.ui.theme.PowerSportGreen
import com.ropeskill.app.ui.theme.PowerSportMuted
import com.ropeskill.app.ui.theme.PowerSportOnBackground
import com.ropeskill.app.ui.theme.PowerSportOrange
import com.ropeskill.app.ui.theme.PowerSportOutline
import com.ropeskill.app.ui.theme.PowerSportSurface
import com.ropeskill.app.ui.theme.PowerSportSurfaceHigh
import com.ropeskill.app.ui.theme.RopeSkillTheme
import java.util.Locale

@Composable
fun HomeScreen(onStartTraining: () -> Unit) {
    Scaffold(
        containerColor = PowerSportBackground,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            PowerSportHeader()

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "BASIC BOUNCE",
                color = PowerSportOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = "TRAIN\nSTRONGER.",
                color = PowerSportOnBackground,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 50.sp,
            )
            Text(
                text = "Build your rhythm. See every jump. Keep improving.",
                color = PowerSportMuted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(modifier = Modifier.height(36.dp))

            WorkoutSummaryCard()

            Button(
                onClick = onStartTraining,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowerSportOrange,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text(
                    text = "START BASIC BOUNCE  →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }

            Text(
                text = "Camera data stays on this device",
                color = PowerSportMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun PowerSportHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(PowerSportOrange),
        ) {
            Text(
                text = "R",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "ROPESKILL",
            color = PowerSportOnBackground,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "MVP",
            color = PowerSportMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun WorkoutSummaryCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PowerSportSurface),
        border = BorderStroke(1.dp, PowerSportOutline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "TODAY'S WORKOUT",
                    color = PowerSportMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(label = "READY", color = PowerSportGreen)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Basic Bounce",
                color = PowerSportOnBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Camera setup  •  5-second countdown  •  Live counting",
                color = PowerSportMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun TrainingScreen(
    uiState: TrainingUiState,
    onAddJump: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onPoseFrame: (PoseFrame) -> Unit,
) {
    Scaffold(
        containerColor = PowerSportBackground,
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
                        color = PowerSportOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                    )
                    Text(
                        text = "TRAINING",
                        color = PowerSportOnBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(
                    label = uiState.status.displayName.uppercase(Locale.US),
                    color = statusColor(uiState.status),
                )
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
                Text(
                    text = "TRACKING  ${uiState.trackingStatus.displayName.uppercase(Locale.US)}",
                    color = PowerSportOnBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(
                    text = "DETECTOR  ${uiState.detectorDiagnostic.displayName.uppercase(Locale.US)}",
                    color = PowerSportMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
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
                            PowerSportOrange
                        } else {
                            PowerSportSurfaceHigh
                        },
                        contentColor = if (uiState.status == WorkoutStatus.PAUSED) {
                            Color.Black
                        } else {
                            PowerSportOnBackground
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
                    border = BorderStroke(1.dp, PowerSportOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PowerSportOnBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("FINISH", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onAddJump,
                    enabled = uiState.status == WorkoutStatus.RUNNING,
                    border = BorderStroke(1.dp, PowerSportOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PowerSportMuted),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("TEST +1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = uiState.status != WorkoutStatus.IDLE || uiState.jumpCount > 0,
                    border = BorderStroke(1.dp, PowerSportOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PowerSportMuted),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("RESET SESSION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
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
        uiState.status == WorkoutStatus.POSITIONING &&
            uiState.trackingStatus == BounceTrackingStatus.WAITING -> "STEP BACK"
        uiState.status == WorkoutStatus.POSITIONING -> "HOLD STILL"
        else -> null
    } ?: return

    Text(
        text = message,
        color = PowerSportOrange,
        fontSize = if (message.length <= 2) 72.sp else 44.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PowerSportSurface),
        border = BorderStroke(1.dp, PowerSportOutline),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text(
                text = label,
                color = PowerSportMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                color = PowerSportOnBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp,
            )
        }
    }
}

@Composable
fun ResultScreen(uiState: TrainingUiState, onDone: () -> Unit) {
    Scaffold(
        containerColor = PowerSportBackground,
        modifier = Modifier.fillMaxSize(),
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
                color = PowerSportOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = "STRONG\nFINISH.",
                color = PowerSportOnBackground,
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 48.sp,
            )
            Text(
                text = "Your Basic Bounce workout is complete.",
                color = PowerSportMuted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.height(36.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = PowerSportSurface),
                border = BorderStroke(1.dp, PowerSportOutline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "WORKOUT RESULT",
                        color = PowerSportMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowerSportOrange,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text(
                    text = "BACK TO HOME  →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }

            Text(
                text = "Workout summary stays on this device",
                color = PowerSportMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
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

private fun statusColor(status: WorkoutStatus): Color = when (status) {
    WorkoutStatus.RUNNING -> PowerSportGreen
    WorkoutStatus.PAUSED -> PowerSportMuted
    WorkoutStatus.FINISHED -> PowerSportGreen
    else -> PowerSportOrange
}

@Composable
private fun ResultMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = PowerSportOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = value,
            color = PowerSportOnBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 34.sp,
        )
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
    RopeSkillTheme { HomeScreen(onStartTraining = {}) }
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
