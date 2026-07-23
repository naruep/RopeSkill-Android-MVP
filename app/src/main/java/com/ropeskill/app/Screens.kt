package com.ropeskill.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ropeskill.app.ui.theme.PowerSportBackground
import com.ropeskill.app.ui.theme.PowerSportMuted
import com.ropeskill.app.ui.theme.PowerSportOnBackground
import com.ropeskill.app.ui.theme.PowerSportOrange
import com.ropeskill.app.ui.theme.PowerSportOutline
import com.ropeskill.app.ui.theme.PowerSportSurface
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            PowerSportHeader()

            Spacer(modifier = Modifier.height(48.dp))

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
                text = "Build your rhythm, track every jump, and keep moving.",
                color = PowerSportMuted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

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
                    text = "START TRAINING  →",
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
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column {
                Text(
                    text = "TODAY'S WORKOUT",
                    color = PowerSportMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "Basic Bounce",
                    color = PowerSportOnBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "READY",
                color = PowerSportOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
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
                Text(
                    text = uiState.status.displayName.uppercase(Locale.US),
                    color = PowerSportOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
            ) {
                CameraPermissionContent(
                    onPoseFrame = onPoseFrame,
                    modifier = Modifier.fillMaxSize(),
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
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PowerSportMetric(
                    label = "JUMPS",
                    value = uiState.jumpCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                PowerSportMetric(
                    label = "TIME",
                    value = formatElapsedTime(uiState.elapsedMillis),
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onAddJump,
                enabled = uiState.status == WorkoutStatus.RUNNING,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowerSportSurface,
                    contentColor = PowerSportOnBackground,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text("MANUAL +1", fontWeight = FontWeight.Bold)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onStart,
                    enabled = uiState.status == WorkoutStatus.IDLE || uiState.status == WorkoutStatus.PAUSED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PowerSportOrange,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text(
                        text = if (uiState.status == WorkoutStatus.PAUSED) "RESUME" else "START",
                        fontWeight = FontWeight.Black,
                    )
                }
                OutlinedButton(
                    onClick = onPause,
                    enabled = uiState.status == WorkoutStatus.RUNNING,
                    border = BorderStroke(1.dp, PowerSportOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PowerSportOnBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text("PAUSE", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onFinish,
                    enabled = uiState.status == WorkoutStatus.RUNNING || uiState.status == WorkoutStatus.PAUSED,
                    border = BorderStroke(1.dp, PowerSportOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PowerSportOnBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("FINISH", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = uiState.status != WorkoutStatus.IDLE || uiState.jumpCount > 0,
                    border = BorderStroke(1.dp, PowerSportOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PowerSportMuted),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("RESET", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PowerSportMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PowerSportSurface),
        border = BorderStroke(1.dp, PowerSportOutline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = PowerSportMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = value,
                color = PowerSportOnBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp,
            )
        }
    }
}

@Composable
fun ResultScreen(uiState: TrainingUiState, onDone: () -> Unit) {
    ScreenContainer {
        Text(text = "Training Result", style = MaterialTheme.typography.headlineLarge)
        Text(text = formatElapsedTime(uiState.elapsedMillis), style = MaterialTheme.typography.displayLarge)
        Text(text = "Jumps: ${uiState.jumpCount}", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home")
        }
    }
}

@Composable
private fun ScreenContainer(content: @Composable () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            content()
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
