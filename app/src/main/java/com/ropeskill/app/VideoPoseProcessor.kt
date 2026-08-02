package com.ropeskill.app

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker

/** Synchronous MediaPipe processor used only by the Debug video test worker. */
class VideoPoseProcessor(context: Context) : AutoCloseable {
    private val poseLandmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET_PATH)
                    .build(),
            )
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build(),
    )

    fun process(bitmap: Bitmap, timestampMillis: Long): PoseFrame {
        val result = poseLandmarker.detectForVideo(
            BitmapImageBuilder(bitmap).build(),
            timestampMillis,
        )
        val landmarks = result.landmarks().firstOrNull().orEmpty().map { landmark ->
            NormalizedPoint(
                x = landmark.x(),
                y = landmark.y(),
                isVisible = landmark.visibility().orElse(0f) >= MIN_VISIBILITY,
            )
        }
        return PoseFrame(
            landmarks = landmarks,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            sourceTimestampMillis = timestampMillis,
        )
    }

    override fun close() {
        poseLandmarker.close()
    }

    private companion object {
        const val MODEL_ASSET_PATH = "pose_landmarker_lite.task"
        const val MIN_VISIBILITY = 0.5f
    }
}
