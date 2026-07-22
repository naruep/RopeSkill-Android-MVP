package com.ropeskill.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ropeskill.app.ui.theme.RopeSkillTheme
import java.util.Locale

@Composable
fun HomeScreen(onStartTraining: () -> Unit) {
    ScreenContainer {
        Text(text = "RopeSkill", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Basic Bounce Training", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onStartTraining, modifier = Modifier.fillMaxWidth()) {
            Text("Start Training")
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
    ScreenContainer {
        Text(text = "Training", style = MaterialTheme.typography.headlineLarge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        ) {
            CameraPermissionContent(
                onPoseFrame = onPoseFrame,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(text = formatElapsedTime(uiState.elapsedMillis), style = MaterialTheme.typography.displayLarge)
        Text(text = "Jumps: ${uiState.jumpCount}", style = MaterialTheme.typography.headlineMedium)

        Button(
            onClick = onAddJump,
            enabled = uiState.status == WorkoutStatus.RUNNING,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Manual +1")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onStart,
                enabled = uiState.status == WorkoutStatus.IDLE || uiState.status == WorkoutStatus.PAUSED,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.status == WorkoutStatus.PAUSED) "Resume" else "Start")
            }
            OutlinedButton(
                onClick = onPause,
                enabled = uiState.status == WorkoutStatus.RUNNING,
                modifier = Modifier.weight(1f),
            ) {
                Text("Pause")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onFinish,
                enabled = uiState.status == WorkoutStatus.RUNNING || uiState.status == WorkoutStatus.PAUSED,
                modifier = Modifier.weight(1f),
            ) {
                Text("Finish")
            }
            OutlinedButton(
                onClick = onReset,
                enabled = uiState.status != WorkoutStatus.IDLE || uiState.jumpCount > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset")
            }
        }

        Text(text = "Status: ${uiState.status.displayName}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Tracking: ${uiState.trackingStatus.displayName}",
            style = MaterialTheme.typography.bodyMedium,
        )
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
