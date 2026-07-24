package com.ropeskill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosePerformanceTrackerTest {
    @Test
    fun snapshot_reportsFpsLatencyAndEstimatedSkippedFrames() {
        val tracker = PosePerformanceTracker()

        tracker.recordSubmittedFrame()
        assertNull(tracker.recordResult(inputTimestampMillis = 100L, completedAtMillis = 140L))

        repeat(3) { tracker.recordSubmittedFrame() }
        assertNull(tracker.recordResult(inputTimestampMillis = 500L, completedAtMillis = 550L))
        val snapshot = tracker.recordResult(
            inputTimestampMillis = 1_100L,
            completedAtMillis = 1_200L,
        )

        requireNotNull(snapshot)
        assertEquals(4L, snapshot.submittedFrames)
        assertEquals(3L, snapshot.resultFrames)
        assertEquals(0L, snapshot.estimatedSkippedFrames)
        assertEquals(63L, snapshot.averageLatencyMillis)
        assertEquals(100L, snapshot.maxLatencyMillis)
        assertEquals(1.89f, snapshot.resultFps, 0.01f)
    }

    @Test
    fun snapshot_allowsOneInFlightFrameBeforeReportingSkippedFrames() {
        val tracker = PosePerformanceTracker()

        repeat(4) { tracker.recordSubmittedFrame() }
        assertNull(tracker.recordResult(inputTimestampMillis = 100L, completedAtMillis = 120L))
        val snapshot = tracker.recordResult(
            inputTimestampMillis = 1_100L,
            completedAtMillis = 1_200L,
        )

        requireNotNull(snapshot)
        assertEquals(1L, snapshot.estimatedSkippedFrames)
    }
}
