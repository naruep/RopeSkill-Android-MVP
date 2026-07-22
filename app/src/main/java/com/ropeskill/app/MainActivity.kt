package com.ropeskill.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ropeskill.app.ui.theme.RopeSkillTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RopeSkillTheme {
                RopeSkillApp()
            }
        }
    }
}

@Composable
fun RopeSkillApp(trainingViewModel: TrainingViewModel = viewModel()) {
    val uiState by trainingViewModel.uiState.collectAsStateWithLifecycle()

    LifecycleStartEffect(trainingViewModel) {
        onStopOrDispose {
            trainingViewModel.pauseWorkout()
        }
    }

    TrainingScreen(
        uiState = uiState,
        onAddJump = trainingViewModel::addJump,
        onStart = trainingViewModel::startWorkout,
        onPause = trainingViewModel::pauseWorkout,
        onFinish = trainingViewModel::finishWorkout,
        onReset = trainingViewModel::resetWorkout,
    )
}

@Composable
private fun TrainingScreen(
    uiState: TrainingUiState,
    onAddJump: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text(text = "RopeSkill", style = MaterialTheme.typography.headlineLarge)
            Text(text = formatElapsedTime(uiState.elapsedMillis), style = MaterialTheme.typography.displayLarge)
            Text(text = "Jumps: ${uiState.jumpCount}", style = MaterialTheme.typography.headlineMedium)

            Button(
                onClick = onAddJump,
                enabled = uiState.status == WorkoutStatus.RUNNING,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("+1 Jump")
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
        }
    }
}

private fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun TrainingScreenPreview() {
    RopeSkillTheme {
        TrainingScreen(
            uiState = TrainingUiState(jumpCount = 12, elapsedMillis = 34_000),
            onAddJump = {},
            onStart = {},
            onPause = {},
            onFinish = {},
            onReset = {},
        )
    }
}
