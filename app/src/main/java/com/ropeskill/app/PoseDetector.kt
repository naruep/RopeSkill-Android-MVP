package com.ropeskill.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseDetector(
    context: Context,
    private val onResult: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit,
) : AutoCloseable {
    private val poseLandmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET_PATH)
                    .build(),
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener(::handleResult)
            .setErrorListener { onError("Pose detection is unavailable.") }
            .build(),
    )

    fun detect(imageProxy: ImageProxy) {
        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888,
            )
            bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
            imageProxy.close()

            val rotatedBitmap = if (rotationDegrees == 0) {
                bitmap
            } else {
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    Matrix().apply { postRotate(rotationDegrees.toFloat()) },
                    true,
                ).also { bitmap.recycle() }
            }

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            poseLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (_: RuntimeException) {
            imageProxy.close()
            onError("Pose detection is unavailable.")
        }
    }

    private fun handleResult(result: PoseLandmarkerResult, inputImage: com.google.mediapipe.framework.image.MPImage) {
        val landmarks = result.landmarks().firstOrNull().orEmpty().map { landmark ->
            NormalizedPoint(
                x = landmark.x(),
                y = landmark.y(),
                isVisible = landmark.visibility().orElse(0f) >= MIN_VISIBILITY,
            )
        }
        onResult(
            PoseFrame(
                landmarks = landmarks,
                imageWidth = inputImage.width,
                imageHeight = inputImage.height,
            ),
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
