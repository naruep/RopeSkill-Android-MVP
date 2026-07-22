package com.ropeskill.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.max

data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val isVisible: Boolean,
)

data class PoseFrame(
    val landmarks: List<NormalizedPoint>,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    companion object {
        val Empty = PoseFrame(emptyList(), 0, 0)
    }
}

@Composable
fun PoseOverlay(frame: PoseFrame, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (frame.imageWidth <= 0 || frame.imageHeight <= 0) return@Canvas

        // PreviewView uses FILL_CENTER, so apply the same scale and center crop.
        val scale = max(size.width / frame.imageWidth, size.height / frame.imageHeight)
        val offsetX = (size.width - frame.imageWidth * scale) / 2f
        val offsetY = (size.height - frame.imageHeight * scale) / 2f

        fun position(index: Int): Offset? {
            val point = frame.landmarks.getOrNull(index)?.takeIf { it.isVisible } ?: return null
            return Offset(
                x = offsetX + point.x * frame.imageWidth * scale,
                y = offsetY + point.y * frame.imageHeight * scale,
            )
        }

        POSE_CONNECTIONS.forEach { (startIndex, endIndex) ->
            val start = position(startIndex)
            val end = position(endIndex)
            if (start != null && end != null) {
                drawLine(
                    color = Color.Cyan,
                    start = start,
                    end = end,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
            }
        }
        frame.landmarks.indices.forEach { index ->
            position(index)?.let { center ->
                drawCircle(color = Color.Yellow, radius = 6f, center = center)
            }
        }
    }
}

private val POSE_CONNECTIONS = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 7,
    0 to 4, 4 to 5, 5 to 6, 6 to 8,
    9 to 10,
    11 to 12, 11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
    12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 27 to 29, 27 to 31, 29 to 31,
    24 to 26, 26 to 28, 28 to 30, 28 to 32, 30 to 32,
)
