package com.ropeskill.app

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun BasicBounceVideoDiagnosticScreen(
    state: BasicBounceVideoDiagnosticUiState,
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
    DisposableEffect(Unit) { onDispose { videoView?.stopPlayback() } }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            TextButton(onClick = onBack, enabled = !state.isAnalyzing) { Text("BACK") }
            Text(
                text = "BASIC BOUNCE VIDEO DIAGNOSTIC",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "DEBUG ONLY · ON-DEVICE · READ-ONLY",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Select one screen recording. The app analyzes up to 120 seconds, " +
                    "does not copy or upload the video, and does not create a History entry.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { openVideo.launch(arrayOf("video/*")) },
                enabled = !state.isAnalyzing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.videoUri == null) "CHOOSE VIDEO" else "CHOOSE ANOTHER VIDEO") }

            if (state.videoUri != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.videoName, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
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
                                    setMediaController(
                                        MediaController(context).also { it.setAnchorView(this) },
                                    )
                                    setVideoURI(state.videoUri)
                                    setOnPreparedListener { seekTo(state.goTimestampMillis.toInt()) }
                                    videoView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Text(
                    text = "Playback ${formatBasicBounceVideoTime(playbackPositionMillis)} / " +
                        formatBasicBounceVideoTime(state.durationMillis),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
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
                ) { Text("USE CURRENT POSITION AS GO") }
                Text(
                    text = "GO ${formatBasicBounceVideoTime(state.goTimestampMillis)} · calibration uses the " +
                        "previous 2 seconds; analysis ends 120 seconds after GO.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(12.dp))
                if (state.isAnalyzing) {
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Processing frame ${state.processedFrames}")
                    OutlinedButton(onClick = onCancelAnalysis, modifier = Modifier.fillMaxWidth()) {
                        Text("CANCEL ANALYSIS")
                    }
                } else {
                    Button(
                        onClick = onAnalyze,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) { Text("ANALYZE BASIC BOUNCE", fontWeight = FontWeight.Black) }
                }
            }
            state.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.result?.let { result ->
                Spacer(Modifier.height(16.dp))
                Text(result.summaryLine(), fontWeight = FontWeight.Bold)
                Text(
                    "Shadow ${result.shadowProfileName}: ${result.shadowCountedJumps} jumps (not live counting)",
                    fontSize = 12.sp,
                )
                Text(result.rejectedTakeoffSummary(), fontSize = 12.sp)
                Text(formatT743LandingStateSnapshot(result.landingSnapshot), fontSize = 12.sp)
                Text(
                    text = "Offline diagnostic only. Compare this trace with the recorded live " +
                        "counter; it does not prove live-camera equivalence.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedButton(
                    onClick = { exportCsv.launch("basic-bounce-video-diagnostic.csv") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("EXPORT CSV") }
            }
        }
    }
}

private fun formatBasicBounceVideoTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}
