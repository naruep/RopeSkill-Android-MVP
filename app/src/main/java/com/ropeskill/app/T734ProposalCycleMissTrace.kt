package com.ropeskill.app

import java.util.Locale
import kotlin.math.max

data class T734MotionPulse(
    val sequence: Int,
    val elapsedMillis: Long,
    val durationMillis: Long,
    val ankleRiseRatio: Float,
    val hipRiseRatio: Float,
    val qualified: Boolean,
    val matchedProductionTakeoff: Boolean,
    val peakTrackingStatus: BounceTrackingStatus,
)

data class T734ProposalCycleSnapshot(
    val measurementStarted: Boolean,
    val rawPulseCount: Int,
    val qualifiedPulseCount: Int,
    val matchedPulseCount: Int,
    val unmatchedPulseCount: Int,
    val unmatchedWhileAirborneCount: Int,
    val productionTakeoffCount: Int,
    val productionLandingCount: Int,
    val interruptedPulseCount: Int,
    val latestPulses: List<T734MotionPulse>,
    val processedFrames: Long,
)

internal fun formatT734ProposalCycleSnapshot(
    snapshot: T734ProposalCycleSnapshot,
): String = buildString {
    append("T-734 PULSE TRACE V18 ")
    append(if (snapshot.measurementStarted) "MATCHED" else "WAIT-GO")
    append(
        String.format(
            Locale.US,
            "\nRAW%d Q%d M%d U%d UA%d T/L%d/%d X%d F%d",
            snapshot.rawPulseCount,
            snapshot.qualifiedPulseCount,
            snapshot.matchedPulseCount,
            snapshot.unmatchedPulseCount,
            snapshot.unmatchedWhileAirborneCount,
            snapshot.productionTakeoffCount,
            snapshot.productionLandingCount,
            snapshot.interruptedPulseCount,
            snapshot.processedFrames,
        ),
    )
    snapshot.latestPulses.forEach { pulse ->
        append(
            String.format(
                Locale.US,
                "\n#%02d +%.3f %s%s A%.3f H%.3f D%d %s",
                pulse.sequence,
                pulse.elapsedMillis / 1_000.0,
                if (pulse.qualified) "Q" else "r",
                if (pulse.matchedProductionTakeoff) "M" else "U",
                pulse.ankleRiseRatio,
                pulse.hipRiseRatio,
                pulse.durationMillis,
                pulse.peakTrackingStatus.shortT734Name(),
            ),
        )
    }
}

/**
 * Debug-only passive motion trace for T-734.
 *
 * It observes the same PoseFrame and the already-produced BASE result. It never returns a detector
 * decision and cannot affect Counter, detector state, Result, History, or storage.
 */
internal class T734ProposalCycleMissCollector(
    private val enabled: Boolean,
) {
    private var measurementStartedAtMillis: Long? = null
    private var previousMeasurement: Measurement? = null
    private var activePulse: ActivePulse? = null
    private var pulseSequence = 0
    private var rawPulseCount = 0
    private var qualifiedPulseCount = 0
    private var matchedPulseCount = 0
    private var unmatchedPulseCount = 0
    private var unmatchedWhileAirborneCount = 0
    private var productionTakeoffCount = 0
    private var productionLandingCount = 0
    private var interruptedPulseCount = 0
    private var processedFrames = 0L
    private val pulses = ArrayDeque<T734MotionPulse>()

    fun startMeasurement(timestampMillis: Long): T734ProposalCycleSnapshot? {
        if (!enabled) return null
        reset()
        measurementStartedAtMillis = timestampMillis
        return snapshot()
    }

    fun record(
        frame: PoseFrame,
        result: BounceDetectionResult,
        timestampMillis: Long,
    ): T734ProposalCycleSnapshot? {
        val startedAtMillis = measurementStartedAtMillis ?: return null
        if (!enabled) return null

        processedFrames += 1
        if (result.event == BounceEvent.TAKEOFF) {
            productionTakeoffCount += 1
            activePulse?.matchedProductionTakeoff = true
        }
        if (result.event == BounceEvent.LANDING) {
            productionLandingCount += 1
        }

        val measurement = frame.measurement(timestampMillis) ?: run {
            if (activePulse != null) interruptedPulseCount += 1
            activePulse = null
            previousMeasurement = null
            return snapshotIfDue(force = true)
        }
        val previous = previousMeasurement
        previousMeasurement = measurement
        if (previous == null) return snapshotIfDue(force = false)

        val ankleDelta = previous.ankleY - measurement.ankleY
        val hipDelta = previous.hipY - measurement.hipY
        val movingUp = ankleDelta > DIRECTION_EPSILON && hipDelta > DIRECTION_EPSILON
        val movingDown = ankleDelta < -DIRECTION_EPSILON && hipDelta < -DIRECTION_EPSILON
        var changed = result.event != BounceEvent.NONE

        if (activePulse == null && movingUp) {
            activePulse = ActivePulse(
                startedAtMillis = previous.timestampMillis,
                startAnkleY = previous.ankleY,
                startHipY = previous.hipY,
                legLength = max(previous.legLength, MIN_LEG_LENGTH),
                peakAnkleY = measurement.ankleY,
                peakHipY = measurement.hipY,
                peakAtMillis = timestampMillis,
                peakTrackingStatus = result.trackingStatus,
                matchedProductionTakeoff = result.event == BounceEvent.TAKEOFF,
            )
        } else {
            activePulse?.let { pulse ->
                if (measurement.ankleY < pulse.peakAnkleY) {
                    pulse.peakAnkleY = measurement.ankleY
                    pulse.peakAtMillis = timestampMillis
                    pulse.peakTrackingStatus = result.trackingStatus
                }
                if (measurement.hipY < pulse.peakHipY) {
                    pulse.peakHipY = measurement.hipY
                }
                if (result.event == BounceEvent.TAKEOFF) {
                    pulse.matchedProductionTakeoff = true
                }
                if (movingDown) {
                    completePulse(pulse, startedAtMillis)
                    activePulse = null
                    changed = true
                }
            }
        }
        return snapshotIfDue(force = changed)
    }

    fun reset() {
        measurementStartedAtMillis = null
        previousMeasurement = null
        activePulse = null
        pulseSequence = 0
        rawPulseCount = 0
        qualifiedPulseCount = 0
        matchedPulseCount = 0
        unmatchedPulseCount = 0
        unmatchedWhileAirborneCount = 0
        productionTakeoffCount = 0
        productionLandingCount = 0
        interruptedPulseCount = 0
        processedFrames = 0L
        pulses.clear()
    }

    private fun completePulse(
        pulse: ActivePulse,
        startedAtMillis: Long,
    ) {
        val ankleRiseRatio =
            (pulse.startAnkleY - pulse.peakAnkleY) / pulse.legLength
        val hipRiseRatio =
            (pulse.startHipY - pulse.peakHipY) / pulse.legLength
        val qualified =
            ankleRiseRatio >= TRACE_MIN_ANKLE_RISE_RATIO &&
                hipRiseRatio >= TRACE_MIN_HIP_RISE_RATIO
        pulseSequence += 1
        rawPulseCount += 1
        if (qualified) {
            qualifiedPulseCount += 1
            if (pulse.matchedProductionTakeoff) {
                matchedPulseCount += 1
            } else {
                unmatchedPulseCount += 1
                if (pulse.peakTrackingStatus == BounceTrackingStatus.AIRBORNE) {
                    unmatchedWhileAirborneCount += 1
                }
            }
        }
        pulses.addLast(
            T734MotionPulse(
                sequence = pulseSequence,
                elapsedMillis = (pulse.peakAtMillis - startedAtMillis).coerceAtLeast(0L),
                durationMillis =
                    (pulse.peakAtMillis - pulse.startedAtMillis).coerceAtLeast(0L),
                ankleRiseRatio = ankleRiseRatio,
                hipRiseRatio = hipRiseRatio,
                qualified = qualified,
                matchedProductionTakeoff = pulse.matchedProductionTakeoff,
                peakTrackingStatus = pulse.peakTrackingStatus,
            ),
        )
        while (pulses.size > MAX_RETAINED_PULSES) pulses.removeFirst()
    }

    private fun snapshotIfDue(force: Boolean): T734ProposalCycleSnapshot? =
        if (force || processedFrames == 1L || processedFrames % SNAPSHOT_INTERVAL_FRAMES == 0L) {
            snapshot()
        } else {
            null
        }

    private fun snapshot(): T734ProposalCycleSnapshot =
        T734ProposalCycleSnapshot(
            measurementStarted = measurementStartedAtMillis != null,
            rawPulseCount = rawPulseCount,
            qualifiedPulseCount = qualifiedPulseCount,
            matchedPulseCount = matchedPulseCount,
            unmatchedPulseCount = unmatchedPulseCount,
            unmatchedWhileAirborneCount = unmatchedWhileAirborneCount,
            productionTakeoffCount = productionTakeoffCount,
            productionLandingCount = productionLandingCount,
            interruptedPulseCount = interruptedPulseCount,
            latestPulses = displayedPulses(),
            processedFrames = processedFrames,
        )

    private fun displayedPulses(): List<T734MotionPulse> {
        val retainedUnmatched = pulses
            .filter { it.qualified && !it.matchedProductionTakeoff }
            .takeLast(MAX_DISPLAYED_UNMATCHED)
        val retainedSequences = retainedUnmatched.mapTo(mutableSetOf()) { it.sequence }
        val recent = pulses
            .filterNot { it.sequence in retainedSequences }
            .takeLast(DISPLAYED_PULSES - retainedUnmatched.size)
        return (retainedUnmatched + recent).sortedBy { it.sequence }
    }

    private data class ActivePulse(
        val startedAtMillis: Long,
        val startAnkleY: Float,
        val startHipY: Float,
        val legLength: Float,
        var peakAnkleY: Float,
        var peakHipY: Float,
        var peakAtMillis: Long,
        var peakTrackingStatus: BounceTrackingStatus,
        var matchedProductionTakeoff: Boolean,
    )

    private data class Measurement(
        val timestampMillis: Long,
        val ankleY: Float,
        val hipY: Float,
        val legLength: Float,
    )

    private fun PoseFrame.measurement(timestampMillis: Long): Measurement? {
        val leftHip = landmarks.getOrNull(LEFT_HIP)?.takeIf { it.isVisible } ?: return null
        val rightHip = landmarks.getOrNull(RIGHT_HIP)?.takeIf { it.isVisible } ?: return null
        val leftAnkle = landmarks.getOrNull(LEFT_ANKLE)?.takeIf { it.isVisible } ?: return null
        val rightAnkle = landmarks.getOrNull(RIGHT_ANKLE)?.takeIf { it.isVisible } ?: return null
        val hipY = (leftHip.y + rightHip.y) / 2f
        val ankleY = (leftAnkle.y + rightAnkle.y) / 2f
        val legLength = ankleY - hipY
        if (legLength < MIN_LEG_LENGTH) return null
        return Measurement(
            timestampMillis = timestampMillis,
            ankleY = ankleY,
            hipY = hipY,
            legLength = legLength,
        )
    }

    private companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val MIN_LEG_LENGTH = 0.1f
        const val DIRECTION_EPSILON = 0.00025f
        const val TRACE_MIN_ANKLE_RISE_RATIO = 0.006f
        const val TRACE_MIN_HIP_RISE_RATIO = 0.040f
        const val MAX_RETAINED_PULSES = 128
        const val DISPLAYED_PULSES = 6
        const val MAX_DISPLAYED_UNMATCHED = 3
        const val SNAPSHOT_INTERVAL_FRAMES = 30L
    }
}

private fun BounceTrackingStatus.shortT734Name(): String = when (this) {
    BounceTrackingStatus.WAITING -> "WAIT"
    BounceTrackingStatus.CALIBRATING -> "CAL"
    BounceTrackingStatus.READY -> "READY"
    BounceTrackingStatus.AIRBORNE -> "AIR"
}
