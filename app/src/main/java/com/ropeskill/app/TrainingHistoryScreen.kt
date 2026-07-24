package com.ropeskill.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrainingHistoryScreen(
    sessions: List<TrainingSession>,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("←  BACK", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "TRAINING\nHISTORY.",
                color = colors.onBackground,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 42.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = "No completed sessions yet.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(items = sessions, key = TrainingSession::id) { session ->
                        TrainingHistoryRow(session = session)
                        HorizontalDivider(color = colors.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingHistoryRow(session: TrainingSession) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.exerciseType.replace('_', ' '),
                color = colors.onBackground,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatSessionDateTime(session.completedAtEpochMillis),
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${session.jumpCount} JUMPS",
                color = colors.primary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatElapsedTime(session.durationMillis),
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

internal fun formatSessionDateTime(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = SESSION_DATE_TIME_FORMATTER.format(
    Instant.ofEpochMilli(epochMillis).atZone(zoneId),
)

private val SESSION_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.US)
