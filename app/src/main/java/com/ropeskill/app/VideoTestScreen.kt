package com.ropeskill.app

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun VideoTestScreen(
    state: VideoTestUiState,
    onSelectVideo: (Uri) -> Unit,
    onSetGoTimestamp: (Long) -> Unit,
    onAnalyze: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onExportReport: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val openVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onSelectVideo) }
    val exportCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(onExportReport) }
    var playbackPositionMillis by remember { mutableLongStateOf(0L) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(videoView, state.videoUri) {
        while (state.videoUri != null) {
            playbackPositionMillis = videoView?.currentPosition?.toLong() ?: 0L
            delay(200L)
        }
    }
    DisposableEffect(Unit) {
        onDispose { videoView?.stopPlayback() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onBack, enabled = !state.isAnalyzing) {
                    Text("BACK")
                }
                Text(
                    text = "VIDEO TEST MODE",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = "DEBUG ONLY · ON-DEVICE ANALYSIS",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The selected source file is read directly. RopeSkill does not copy the " +
                    "video, record a new video, or create a History entry.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { openVideo.launch(arrayOf("video/*")) },
                enabled = !state.isAnalyzing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.videoUri == null) "CHOOSE VIDEO" else "CHOOSE ANOTHER VIDEO")
            }

            if (state.videoUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.videoName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                ) {
                    key(state.videoUri) {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    val controls = MediaController(context)
                                    controls.setAnchorView(this)
                                    setMediaController(controls)
                                    setVideoURI(state.videoUri)
                                    setOnPreparedListener { player ->
                                        player.isLooping = false
                                        seekTo(state.goTimestampMillis.toInt())
                                    }
                                    videoView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Playback ${formatVideoTime(playbackPositionMillis)} / " +
                        formatVideoTime(state.durationMillis),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = playbackPositionMillis.toFloat(),
                    onValueChange = { value ->
                        playbackPositionMillis = value.toLong()
                        videoView?.seekTo(value.toInt())
                    },
                    valueRange = 0f..state.durationMillis.coerceAtLeast(1L).toFloat(),
                    enabled = !state.isAnalyzing,
                )
                OutlinedButton(
                    onClick = { onSetGoTimestamp(playbackPositionMillis) },
                    enabled = !state.isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("USE CURRENT POSITION AS GO")
                }
                Text(
                    text = "GO ${formatVideoTime(state.goTimestampMillis)} · analyzes 2 seconds " +
                        "before GO and up to 30 seconds after GO",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (state.isAnalyzing) {
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Processing frame ${state.processedFrames} · keep this screen open",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    OutlinedButton(
                        onClick = onCancelAnalysis,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("CANCEL ANALYSIS")
                    }
                } else {
                    Button(
                        onClick = onAnalyze,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text("ANALYZE SPEED 30", fontWeight = FontWeight.Black)
                    }
                }
            }

            state.message?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.result?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ANALYSIS RESULT",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                ResultMetric("Production raw landings", result.productionRawLandings.toString())
                ResultMetric("Fixed-reference shadow landings", result.fixedReferenceRawLandings.toString())
                ResultMetric(
                    "Fixed-reference difference",
                    String.format(Locale.US, "%+d", result.fixedReferenceDifference),
                )
                ResultMetric("Counted right steps", result.countedRightSteps.toString())
                ResultMetric(
                    "Production L / R",
                    "${result.productionLeftLandings} / ${result.productionRightLandings}",
                )
                ResultMetric(
                    "Production recovery L / R",
                    "${result.productionLeftConservativeRearms} / " +
                        result.productionRightConservativeRearms,
                )
                ResultMetric(
                    "Fixed-reference L / R",
                    "${result.fixedReferenceLeftLandings} / ${result.fixedReferenceRightLandings}",
                )
                ResultMetric(
                    "Fixed strict L / R",
                    "${result.fixedReferenceLeftStrictLandings} / " +
                        result.fixedReferenceRightStrictLandings,
                )
                ResultMetric(
                    "Fixed recovery L / R",
                    "${result.fixedReferenceLeftConservativeRearms} / " +
                        result.fixedReferenceRightConservativeRearms,
                )
                ResultMetric(
                    "Valid / low-visibility frames",
                    "${result.framesWithValidPose} / ${result.lowVisibilityFrames}",
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        exportCsv.launch("RopeSkill-Video-Test-${result.goTimestampMillis}.csv")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("EXPORT CSV REPORT")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatVideoTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return String.format(
        Locale.US,
        "%02d:%02d.%03d",
        totalSeconds / 60L,
        totalSeconds % 60L,
        milliseconds.coerceAtLeast(0L) % 1_000L,
    )
}
