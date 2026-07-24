package com.ropeskill.app

import kotlin.math.max

data class PosePerformanceSnapshot(
    val resultFps: Float = 0f,
    val averageLatencyMillis: Long = 0L,
    val maxLatencyMillis: Long = 0L,
    val submittedFrames: Long = 0L,
    val resultFrames: Long = 0L,
    val estimatedSkippedFrames: Long = 0L,
)

class PosePerformanceTracker {
    private var submittedFrames = 0L
    private var resultFrames = 0L
    private var totalLatencyMillis = 0L
    private var maxLatencyMillis = 0L
    private var windowStartedAtMillis = 0L
    private var windowResultFrames = 0L

    @Synchronized
    fun recordSubmittedFrame() {
        submittedFrames += 1L
    }

    @Synchronized
    fun recordResult(
        inputTimestampMillis: Long,
        completedAtMillis: Long,
    ): PosePerformanceSnapshot? {
        val latencyMillis = max(0L, completedAtMillis - inputTimestampMillis)
        resultFrames += 1L
        totalLatencyMillis += latencyMillis
        maxLatencyMillis = max(maxLatencyMillis, latencyMillis)

        if (windowStartedAtMillis == 0L) {
            windowStartedAtMillis = completedAtMillis
            windowResultFrames = resultFrames
            return null
        }

        val windowDurationMillis = completedAtMillis - windowStartedAtMillis
        if (windowDurationMillis < SNAPSHOT_INTERVAL_MILLIS) return null

        val resultFps =
            (resultFrames - windowResultFrames) * 1_000f / windowDurationMillis.toFloat()
        windowStartedAtMillis = completedAtMillis
        windowResultFrames = resultFrames

        return PosePerformanceSnapshot(
            resultFps = resultFps,
            averageLatencyMillis = totalLatencyMillis / resultFrames,
            maxLatencyMillis = maxLatencyMillis,
            submittedFrames = submittedFrames,
            resultFrames = resultFrames,
            estimatedSkippedFrames = max(
                0L,
                submittedFrames - resultFrames - IN_FLIGHT_FRAME_ALLOWANCE,
            ),
        )
    }

    private companion object {
        const val SNAPSHOT_INTERVAL_MILLIS = 1_000L
        const val IN_FLIGHT_FRAME_ALLOWANCE = 1L
    }
}
