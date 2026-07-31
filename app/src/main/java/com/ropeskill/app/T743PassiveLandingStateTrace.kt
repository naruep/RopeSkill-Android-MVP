package com.ropeskill.app

import java.util.Locale
import kotlin.math.max

enum class T743ProductionEvent {
    NONE,
    TAKEOFF,
    LANDING_COUNTED,
    LANDING_SUPPRESSED,
    RESET_OR_CALIBRATE,
}

enum class T743AirCloseReason {
    RETURNED_TO_BASELINE,
    COMPLETED_VERTICAL_CYCLE,
    BOTH,
    TIMED_OUT_AFTER_DESCENT,
    RESET_OR_CALIBRATE,
    EVIDENCE_GAP,
}

data class T743LandingFrame(
    val sequence: Long,
    val elapsedMillis: Long,
    val statusBefore: BounceTrackingStatus,
    val statusAfter: BounceTrackingStatus,
    val productionTakeoffCount: Int,
    val productionLandingCount: Int,
    val productionEvent: T743ProductionEvent,
    val evidence: LandingStateEvidence?,
)

data class T743AirInterval(
    val sequence: Int,
    val startedAtElapsedMillis: Long,
    val endedAtElapsedMillis: Long,
    val frameSamples: Int,
    val closeReason: T743AirCloseReason,
    val physicalPulsesWhileAirborne: Int,
)

data class T743AirPulse(
    val sequence: Int,
    val intervalSequence: Int?,
    val peakFrameSequence: Long,
    val elapsedMillis: Long,
    val ankleRiseRatio: Float,
    val hipRiseRatio: Float,
    val landingEvidenceAtPeak: LandingStateEvidence?,
)

data class T743LandingStateSnapshot(
    val measurementStarted: Boolean,
    val productionTakeoffCount: Int,
    val productionLandingCount: Int,
    val productionSuppressedLandingCount: Int,
    val airIntervalCount: Int,
    val openAirInterval: Boolean,
    val closedByBaselineCount: Int,
    val closedByVerticalCycleCount: Int,
    val closedByBothCount: Int,
    val closedByTimeoutRecoveryCount: Int,
    val resetOrCalibrateCount: Int,
    val evidenceGapCount: Int,
    val physicalPulsesWhileAirborne: Int,
    val recentFrames: List<T743LandingFrame>,
    val recentIntervals: List<T743AirInterval>,
    val recentAirPulses: List<T743AirPulse>,
    val processedFrames: Long,
)

internal fun formatT743LandingStateSnapshot(
    snapshot: T743LandingStateSnapshot,
): String = buildString {
    append("T-743 LANDING STATE V23 ")
    append(if (snapshot.measurementStarted) "PASSIVE" else "WAIT-GO")
    append(
        String.format(
            Locale.US,
            "\nI%d%s T/L%d/%d SUP%d PA%d U%d F%d",
            snapshot.airIntervalCount,
            if (snapshot.openAirInterval) "*" else "",
            snapshot.productionTakeoffCount,
            snapshot.productionLandingCount,
            snapshot.productionSuppressedLandingCount,
            snapshot.physicalPulsesWhileAirborne,
            snapshot.evidenceGapCount,
            snapshot.processedFrames,
        ),
    )
    append(
        String.format(
            Locale.US,
            "\nC B%d V%d X%d TO%d R%d",
            snapshot.closedByBaselineCount,
            snapshot.closedByVerticalCycleCount,
            snapshot.closedByBothCount,
            snapshot.closedByTimeoutRecoveryCount,
            snapshot.resetOrCalibrateCount,
        ),
    )
    snapshot.recentIntervals.lastOrNull()?.let { interval ->
        append(
            String.format(
                Locale.US,
                "\nI#%02d +%.3f-%.3f %s N%d P%d",
                interval.sequence,
                interval.startedAtElapsedMillis / 1_000.0,
                interval.endedAtElapsedMillis / 1_000.0,
                interval.closeReason.shortName(),
                interval.frameSamples,
                interval.physicalPulsesWhileAirborne,
            ),
        )
    }
    snapshot.recentFrames.lastOrNull()?.let { frame ->
        val evidence = frame.evidence
        append(
            String.format(
                Locale.US,
                "\n#%d +%.3f %s>%s %s",
                frame.sequence,
                frame.elapsedMillis / 1_000.0,
                frame.statusBefore.shortName(),
                frame.statusAfter.shortName(),
                frame.productionEvent.shortName(),
            ),
        )
        if (evidence != null) {
            append(
                String.format(
                    Locale.US,
                    " B A%.3f/%.3f:%d H%.3f/%.3f:%d",
                    evidence.ankleFromBaselineRatio,
                    evidence.ankleBaselineLimitRatio,
                    evidence.ankleReturnedToBaseline.bit(),
                    evidence.hipFromBaselineRatio,
                    evidence.hipBaselineLimitRatio,
                    evidence.hipReturnedToBaseline.bit(),
                ),
            )
            append(
                String.format(
                    Locale.US,
                    "\nD A%.3f/%.3f:%d H%.3f/%.3f:%d R%d%d C%d %dms",
                    evidence.ankleDescentFromPeakRatio,
                    evidence.ankleDescentLimitRatio,
                    evidence.ankleDescendedFromPeak.bit(),
                    evidence.hipDescentFromPeakRatio,
                    evidence.hipDescentLimitRatio,
                    evidence.hipDescendedFromPeak.bit(),
                    evidence.ankleStartedNextRise.bit(),
                    evidence.hipStartedNextRise.bit(),
                    evidence.completedVerticalCycle.bit(),
                    evidence.airborneMillis,
                ),
            )
        }
    }
    snapshot.recentAirPulses.lastOrNull()?.let { pulse ->
        append(
            String.format(
                Locale.US,
                "\nP#%02d I%s F%d +%.3f A%.3f H%.3f",
                pulse.sequence,
                pulse.intervalSequence?.toString() ?: "?",
                pulse.peakFrameSequence,
                pulse.elapsedMillis / 1_000.0,
                pulse.ankleRiseRatio,
                pulse.hipRiseRatio,
            ),
        )
        pulse.landingEvidenceAtPeak?.let { evidence ->
            append(
                String.format(
                    Locale.US,
                    "\nPB A%.3f:%d H%.3f:%d D%d%d R%d%d C%d",
                    evidence.ankleFromBaselineRatio,
                    evidence.ankleReturnedToBaseline.bit(),
                    evidence.hipFromBaselineRatio,
                    evidence.hipReturnedToBaseline.bit(),
                    evidence.ankleDescendedFromPeak.bit(),
                    evidence.hipDescendedFromPeak.bit(),
                    evidence.ankleStartedNextRise.bit(),
                    evidence.hipStartedNextRise.bit(),
                    evidence.completedVerticalCycle.bit(),
                ),
            )
        }
    }
}

/**
 * Debug-only passive Landing/state observer for T-743.
 *
 * It receives the already-produced detector result and the same PoseFrame. It never returns a
 * detector decision and cannot change Counter, detector state, Result, History, or storage.
 */
internal class T743PassiveLandingStateCollector(
    private val enabled: Boolean,
    private val maxFrameHistory: Int = DEFAULT_MAX_FRAME_HISTORY,
    private val maxIntervalHistory: Int = DEFAULT_MAX_INTERVAL_HISTORY,
) {
    init {
        require(maxFrameHistory > 0)
        require(maxIntervalHistory > 0)
    }

    private var measurementStartedAtMillis: Long? = null
    private var previousStatus = BounceTrackingStatus.WAITING
    private var frameSequence = 0L
    private var intervalSequence = 0
    private var productionTakeoffCount = 0
    private var productionLandingCount = 0
    private var productionSuppressedLandingCount = 0
    private var closedByBaselineCount = 0
    private var closedByVerticalCycleCount = 0
    private var closedByBothCount = 0
    private var closedByTimeoutRecoveryCount = 0
    private var resetOrCalibrateCount = 0
    private var evidenceGapCount = 0
    private var physicalPulsesWhileAirborne = 0
    private var airPulseSequence = 0
    private var processedFrames = 0L
    private var openInterval: OpenInterval? = null
    private val frames = ArrayDeque<T743LandingFrame>()
    private val intervals = ArrayDeque<T743AirInterval>()
    private val airPulses = ArrayDeque<T743AirPulse>()
    private var previousMeasurement: Measurement? = null
    private var activePulse: ActivePulse? = null

    fun startMeasurement(timestampMillis: Long): T743LandingStateSnapshot? {
        if (!enabled) return null
        reset()
        measurementStartedAtMillis = timestampMillis
        return snapshot()
    }

    fun record(
        frame: PoseFrame,
        result: BounceDetectionResult,
        timestampMillis: Long,
    ): T743LandingStateSnapshot? {
        val startedAtMillis = measurementStartedAtMillis ?: return null
        if (!enabled) return null

        processedFrames += 1
        frameSequence += 1
        val statusBefore = previousStatus
        val elapsedMillis = (timestampMillis - startedAtMillis).coerceAtLeast(0L)
        var changed = false
        val productionEvent = when {
            result.event == BounceEvent.TAKEOFF -> {
                productionTakeoffCount += 1
                intervalSequence += 1
                openInterval?.let {
                    closeInterval(
                        interval = it,
                        endedAtElapsedMillis = elapsedMillis,
                        reason = T743AirCloseReason.EVIDENCE_GAP,
                    )
                }
                openInterval = OpenInterval(
                    sequence = intervalSequence,
                    startedAtElapsedMillis = elapsedMillis,
                )
                changed = true
                T743ProductionEvent.TAKEOFF
            }
            result.event == BounceEvent.LANDING && result.countedJump -> {
                productionLandingCount += 1
                changed = true
                T743ProductionEvent.LANDING_COUNTED
            }
            result.event == BounceEvent.LANDING -> {
                productionLandingCount += 1
                productionSuppressedLandingCount += 1
                changed = true
                T743ProductionEvent.LANDING_SUPPRESSED
            }
            statusBefore == BounceTrackingStatus.AIRBORNE &&
                result.trackingStatus != BounceTrackingStatus.AIRBORNE -> {
                changed = true
                T743ProductionEvent.RESET_OR_CALIBRATE
            }
            else -> T743ProductionEvent.NONE
        }

        openInterval?.frameSamples = openInterval?.frameSamples?.plus(1) ?: 0
        observeMotionPulse(
            frame = frame,
            result = result,
            timestampMillis = timestampMillis,
            elapsedMillis = elapsedMillis,
        )?.let { pulse ->
            airPulseSequence += 1
            physicalPulsesWhileAirborne += 1
            openInterval?.physicalPulsesWhileAirborne =
                openInterval?.physicalPulsesWhileAirborne?.plus(1) ?: 0
            airPulses.addLast(
                T743AirPulse(
                    sequence = airPulseSequence,
                    intervalSequence = openInterval?.sequence,
                    peakFrameSequence = pulse.peakFrameSequence,
                    elapsedMillis = pulse.peakElapsedMillis,
                    ankleRiseRatio = pulse.ankleRiseRatio,
                    hipRiseRatio = pulse.hipRiseRatio,
                    landingEvidenceAtPeak = pulse.landingEvidenceAtPeak,
                ),
            )
            while (airPulses.size > maxIntervalHistory) airPulses.removeFirst()
            changed = true
        }

        val landingFrame = T743LandingFrame(
            sequence = frameSequence,
            elapsedMillis = elapsedMillis,
            statusBefore = statusBefore,
            statusAfter = result.trackingStatus,
            productionTakeoffCount = productionTakeoffCount,
            productionLandingCount = productionLandingCount,
            productionEvent = productionEvent,
            evidence = result.landingStateEvidence,
        )
        if (
            result.landingStateEvidence != null ||
            productionEvent != T743ProductionEvent.NONE
        ) {
            frames.addLast(landingFrame)
            while (frames.size > maxFrameHistory) frames.removeFirst()
        }

        if (result.event == BounceEvent.LANDING) {
            closeInterval(
                interval = openInterval,
                endedAtElapsedMillis = elapsedMillis,
                reason = result.closeReason(),
            )
            openInterval = null
        } else if (
            productionEvent == T743ProductionEvent.RESET_OR_CALIBRATE &&
            openInterval != null
        ) {
            closeInterval(
                interval = openInterval,
                endedAtElapsedMillis = elapsedMillis,
                reason = T743AirCloseReason.RESET_OR_CALIBRATE,
            )
            openInterval = null
        }

        previousStatus = result.trackingStatus
        return snapshotIfDue(changed)
    }

    fun reset() {
        measurementStartedAtMillis = null
        previousStatus = BounceTrackingStatus.WAITING
        frameSequence = 0L
        intervalSequence = 0
        productionTakeoffCount = 0
        productionLandingCount = 0
        productionSuppressedLandingCount = 0
        closedByBaselineCount = 0
        closedByVerticalCycleCount = 0
        closedByBothCount = 0
        closedByTimeoutRecoveryCount = 0
        resetOrCalibrateCount = 0
        evidenceGapCount = 0
        physicalPulsesWhileAirborne = 0
        airPulseSequence = 0
        processedFrames = 0L
        openInterval = null
        frames.clear()
        intervals.clear()
        airPulses.clear()
        previousMeasurement = null
        activePulse = null
    }

    private fun closeInterval(
        interval: OpenInterval?,
        endedAtElapsedMillis: Long,
        reason: T743AirCloseReason,
    ) {
        if (interval == null) {
            evidenceGapCount += 1
            return
        }
        when (reason) {
            T743AirCloseReason.RETURNED_TO_BASELINE -> closedByBaselineCount += 1
            T743AirCloseReason.COMPLETED_VERTICAL_CYCLE ->
                closedByVerticalCycleCount += 1
            T743AirCloseReason.BOTH -> closedByBothCount += 1
            T743AirCloseReason.TIMED_OUT_AFTER_DESCENT ->
                closedByTimeoutRecoveryCount += 1
            T743AirCloseReason.RESET_OR_CALIBRATE -> resetOrCalibrateCount += 1
            T743AirCloseReason.EVIDENCE_GAP -> evidenceGapCount += 1
        }
        intervals.addLast(
            T743AirInterval(
                sequence = interval.sequence,
                startedAtElapsedMillis = interval.startedAtElapsedMillis,
                endedAtElapsedMillis = endedAtElapsedMillis,
                frameSamples = interval.frameSamples,
                closeReason = reason,
                physicalPulsesWhileAirborne = interval.physicalPulsesWhileAirborne,
            ),
        )
        while (intervals.size > maxIntervalHistory) intervals.removeFirst()
    }

    private fun observeMotionPulse(
        frame: PoseFrame,
        result: BounceDetectionResult,
        timestampMillis: Long,
        elapsedMillis: Long,
    ): CompletedAirPulse? {
        val measurement = frame.measurement(timestampMillis) ?: run {
            previousMeasurement = null
            activePulse = null
            return null
        }
        val previous = previousMeasurement
        previousMeasurement = measurement
        if (previous == null) return null

        val ankleDelta = previous.ankleY - measurement.ankleY
        val hipDelta = previous.hipY - measurement.hipY
        val movingUp = ankleDelta > DIRECTION_EPSILON && hipDelta > DIRECTION_EPSILON
        val movingDown = ankleDelta < -DIRECTION_EPSILON && hipDelta < -DIRECTION_EPSILON
        if (activePulse == null && movingUp) {
            activePulse = ActivePulse(
                startAnkleY = previous.ankleY,
                startHipY = previous.hipY,
                legLength = max(previous.legLength, MIN_LEG_LENGTH),
                peakAnkleY = measurement.ankleY,
                peakHipY = measurement.hipY,
                peakStatus = result.trackingStatus,
                matchedProductionTakeoff = result.event == BounceEvent.TAKEOFF,
                peakFrameSequence = frameSequence,
                peakElapsedMillis = elapsedMillis,
                landingEvidenceAtPeak = result.landingStateEvidence,
            )
            return null
        }
        val pulse = activePulse ?: return null
        if (result.event == BounceEvent.TAKEOFF) {
            pulse.matchedProductionTakeoff = true
        }
        if (measurement.ankleY < pulse.peakAnkleY) {
            pulse.peakAnkleY = measurement.ankleY
            pulse.peakStatus = result.trackingStatus
            pulse.peakFrameSequence = frameSequence
            pulse.peakElapsedMillis = elapsedMillis
            pulse.landingEvidenceAtPeak = result.landingStateEvidence
        }
        if (measurement.hipY < pulse.peakHipY) {
            pulse.peakHipY = measurement.hipY
        }
        if (!movingDown) return null

        activePulse = null
        val ankleRiseRatio = (pulse.startAnkleY - pulse.peakAnkleY) / pulse.legLength
        val hipRiseRatio = (pulse.startHipY - pulse.peakHipY) / pulse.legLength
        val qualifiedWhileAirborne = !pulse.matchedProductionTakeoff &&
            pulse.peakStatus == BounceTrackingStatus.AIRBORNE &&
            ankleRiseRatio >= TRACE_MIN_ANKLE_RISE_RATIO &&
            hipRiseRatio >= TRACE_MIN_HIP_RISE_RATIO
        return if (qualifiedWhileAirborne) {
            CompletedAirPulse(
                peakFrameSequence = pulse.peakFrameSequence,
                peakElapsedMillis = pulse.peakElapsedMillis,
                ankleRiseRatio = ankleRiseRatio,
                hipRiseRatio = hipRiseRatio,
                landingEvidenceAtPeak = pulse.landingEvidenceAtPeak,
            )
        } else {
            null
        }
    }

    private fun snapshotIfDue(changed: Boolean): T743LandingStateSnapshot? =
        if (
            changed ||
            processedFrames == 1L ||
            processedFrames % SNAPSHOT_INTERVAL_FRAMES == 0L
        ) {
            snapshot()
        } else {
            null
        }

    private fun snapshot(): T743LandingStateSnapshot =
        T743LandingStateSnapshot(
            measurementStarted = measurementStartedAtMillis != null,
            productionTakeoffCount = productionTakeoffCount,
            productionLandingCount = productionLandingCount,
            productionSuppressedLandingCount = productionSuppressedLandingCount,
            airIntervalCount = intervalSequence,
            openAirInterval = openInterval != null,
            closedByBaselineCount = closedByBaselineCount,
            closedByVerticalCycleCount = closedByVerticalCycleCount,
            closedByBothCount = closedByBothCount,
            closedByTimeoutRecoveryCount = closedByTimeoutRecoveryCount,
            resetOrCalibrateCount = resetOrCalibrateCount,
            evidenceGapCount = evidenceGapCount,
            physicalPulsesWhileAirborne = physicalPulsesWhileAirborne,
            recentFrames = frames.toList(),
            recentIntervals = intervals.toList(),
            recentAirPulses = airPulses.toList(),
            processedFrames = processedFrames,
        )

    private data class OpenInterval(
        val sequence: Int,
        val startedAtElapsedMillis: Long,
        var frameSamples: Int = 0,
        var physicalPulsesWhileAirborne: Int = 0,
    )

    private data class Measurement(
        val timestampMillis: Long,
        val ankleY: Float,
        val hipY: Float,
        val legLength: Float,
    )

    private data class ActivePulse(
        val startAnkleY: Float,
        val startHipY: Float,
        val legLength: Float,
        var peakAnkleY: Float,
        var peakHipY: Float,
        var peakStatus: BounceTrackingStatus,
        var matchedProductionTakeoff: Boolean,
        var peakFrameSequence: Long,
        var peakElapsedMillis: Long,
        var landingEvidenceAtPeak: LandingStateEvidence?,
    )

    private data class CompletedAirPulse(
        val peakFrameSequence: Long,
        val peakElapsedMillis: Long,
        val ankleRiseRatio: Float,
        val hipRiseRatio: Float,
        val landingEvidenceAtPeak: LandingStateEvidence?,
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
        return Measurement(timestampMillis, ankleY, hipY, legLength)
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
        const val DEFAULT_MAX_FRAME_HISTORY = 96
        const val DEFAULT_MAX_INTERVAL_HISTORY = 24
        const val SNAPSHOT_INTERVAL_FRAMES = 15L
    }
}

private fun BounceDetectionResult.closeReason(): T743AirCloseReason =
    when (cycleTraceEvidence?.landingReason) {
        LandingDetectionReason.RETURNED_TO_BASELINE ->
            T743AirCloseReason.RETURNED_TO_BASELINE
        LandingDetectionReason.COMPLETED_VERTICAL_CYCLE ->
            T743AirCloseReason.COMPLETED_VERTICAL_CYCLE
        LandingDetectionReason.BOTH -> T743AirCloseReason.BOTH
        LandingDetectionReason.TIMED_OUT_AFTER_DESCENT ->
            T743AirCloseReason.TIMED_OUT_AFTER_DESCENT
        null -> T743AirCloseReason.EVIDENCE_GAP
    }

private fun BounceTrackingStatus.shortName(): String = when (this) {
    BounceTrackingStatus.WAITING -> "W"
    BounceTrackingStatus.CALIBRATING -> "C"
    BounceTrackingStatus.READY -> "R"
    BounceTrackingStatus.AIRBORNE -> "A"
}

private fun T743ProductionEvent.shortName(): String = when (this) {
    T743ProductionEvent.NONE -> "-"
    T743ProductionEvent.TAKEOFF -> "T"
    T743ProductionEvent.LANDING_COUNTED -> "L"
    T743ProductionEvent.LANDING_SUPPRESSED -> "S"
    T743ProductionEvent.RESET_OR_CALIBRATE -> "X"
}

private fun T743AirCloseReason.shortName(): String = when (this) {
    T743AirCloseReason.RETURNED_TO_BASELINE -> "BASE"
    T743AirCloseReason.COMPLETED_VERTICAL_CYCLE -> "CYCLE"
    T743AirCloseReason.BOTH -> "BOTH"
    T743AirCloseReason.TIMED_OUT_AFTER_DESCENT -> "TIME"
    T743AirCloseReason.RESET_OR_CALIBRATE -> "RESET"
    T743AirCloseReason.EVIDENCE_GAP -> "GAP"
}

private fun Boolean.bit(): Int = if (this) 1 else 0
