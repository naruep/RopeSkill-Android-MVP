package com.ropeskill.app

import java.util.Locale
import kotlin.math.max

enum class T735TakeoffRoute {
    STANDARD,
    RESCUE,
}

enum class T735BlockingGate {
    FEET_SYNCHRONIZED,
    LEFT_BILATERAL_ANKLE,
    RIGHT_BILATERAL_ANKLE,
    HIP_TO_ANKLE_RATIO,
    STANDARD_HIP_RISE,
    RESCUE_ANKLE_RISE,
    RESCUE_HIP_RISE,
}

data class T735GateAttribution(
    val route: T735TakeoffRoute,
    val blockingGates: Set<T735BlockingGate>,
    val smoothedAnkleRiseRatio: Float,
    val rawLeftAnkleRiseRatio: Float,
    val rawRightAnkleRiseRatio: Float,
    val smoothedHipRiseRatio: Float,
)

data class T735MotionPulse(
    val sequence: Int,
    val elapsedMillis: Long,
    val durationMillis: Long,
    val ankleRiseRatio: Float,
    val hipRiseRatio: Float,
    val qualified: Boolean,
    val matchedProductionTakeoff: Boolean,
    val peakTrackingStatus: BounceTrackingStatus,
    val gateAttribution: T735GateAttribution?,
)

data class T735TakeoffGateSnapshot(
    val measurementStarted: Boolean,
    val rawPulseCount: Int,
    val qualifiedPulseCount: Int,
    val matchedPulseCount: Int,
    val unmatchedPulseCount: Int,
    val unmatchedWhileAirborneCount: Int,
    val attributedUnmatchedCount: Int,
    val noProductionPeakCount: Int,
    val pendingEvidenceCount: Int,
    val blockingGateCounts: Map<T735BlockingGate, Int>,
    val productionTakeoffCount: Int,
    val productionLandingCount: Int,
    val interruptedPulseCount: Int,
    val latestPulses: List<T735MotionPulse>,
    val processedFrames: Long,
)

internal fun formatT735TakeoffGateSnapshot(
    snapshot: T735TakeoffGateSnapshot,
): String = buildString {
    append("T-736 TUNED GATE V20 ")
    append(if (snapshot.measurementStarted) "MATCHED" else "WAIT-GO")
    append(
        String.format(
            Locale.US,
            "\nRAW%d Q%d M%d U%d UA%d G%d NP%d P%d T/L%d/%d X%d F%d",
            snapshot.rawPulseCount,
            snapshot.qualifiedPulseCount,
            snapshot.matchedPulseCount,
            snapshot.unmatchedPulseCount,
            snapshot.unmatchedWhileAirborneCount,
            snapshot.attributedUnmatchedCount,
            snapshot.noProductionPeakCount,
            snapshot.pendingEvidenceCount,
            snapshot.productionTakeoffCount,
            snapshot.productionLandingCount,
            snapshot.interruptedPulseCount,
            snapshot.processedFrames,
        ),
    )
    append(
        String.format(
            Locale.US,
            "\nG SY%d BL%d BR%d Q%d SH%d RA%d RH%d",
            snapshot.gateCount(T735BlockingGate.FEET_SYNCHRONIZED),
            snapshot.gateCount(T735BlockingGate.LEFT_BILATERAL_ANKLE),
            snapshot.gateCount(T735BlockingGate.RIGHT_BILATERAL_ANKLE),
            snapshot.gateCount(T735BlockingGate.HIP_TO_ANKLE_RATIO),
            snapshot.gateCount(T735BlockingGate.STANDARD_HIP_RISE),
            snapshot.gateCount(T735BlockingGate.RESCUE_ANKLE_RISE),
            snapshot.gateCount(T735BlockingGate.RESCUE_HIP_RISE),
        ),
    )
    snapshot.latestPulses.forEach { pulse ->
        append(
            String.format(
                Locale.US,
                "\n#%02d +%.3f %s%s A%.3f H%.3f D%d %s%s",
                pulse.sequence,
                pulse.elapsedMillis / 1_000.0,
                if (pulse.qualified) "Q" else "r",
                if (pulse.matchedProductionTakeoff) "M" else "U",
                pulse.ankleRiseRatio,
                pulse.hipRiseRatio,
                pulse.durationMillis,
                pulse.peakTrackingStatus.shortT735Name(),
                pulse.gateAttribution?.formatT735Attribution() ?: (
                    if (pulse.qualified && !pulse.matchedProductionTakeoff) " NP" else ""
                ),
            ),
        )
    }
}

/**
 * Debug-only passive takeoff-gate trace for T-735.
 *
 * It observes the same PoseFrame and the already-produced BASE result. It never returns a detector
 * decision and cannot affect Counter, detector state, Result, History, or storage.
 */
internal class T735PassiveTakeoffGateCollector(
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
    private var attributedUnmatchedCount = 0
    private var noProductionPeakCount = 0
    private val blockingGateCounts = mutableMapOf<T735BlockingGate, Int>()
    private var productionTakeoffCount = 0
    private var productionLandingCount = 0
    private var interruptedPulseCount = 0
    private var processedFrames = 0L
    private val pulses = ArrayDeque<T735MotionPulse>()
    private val pendingPulses = ArrayDeque<CompletedPulse>()
    private val pendingPeakEvidence = ArrayDeque<TimestampedPeakEvidence>()

    fun startMeasurement(timestampMillis: Long): T735TakeoffGateSnapshot? {
        if (!enabled) return null
        reset()
        measurementStartedAtMillis = timestampMillis
        return snapshot()
    }

    fun record(
        frame: PoseFrame,
        result: BounceDetectionResult,
        timestampMillis: Long,
    ): T735TakeoffGateSnapshot? {
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
        result.takeoffPeakEvidence
            ?.takeIf { it.outcome == TakeoffPeakOutcome.REJECTED }
            ?.let { evidence ->
                pendingPeakEvidence.addLast(
                    TimestampedPeakEvidence(timestampMillis, evidence),
                )
            }

        val measurement = frame.measurement(timestampMillis) ?: run {
            if (activePulse != null) interruptedPulseCount += 1
            activePulse = null
            previousMeasurement = null
            matchOrExpirePending(timestampMillis)
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
                    completePulse(
                        pulse = pulse,
                        startedAtMillis = startedAtMillis,
                        completedAtMillis = timestampMillis,
                    )
                    activePulse = null
                    changed = true
                }
            }
        }
        changed = matchOrExpirePending(timestampMillis) || changed
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
        attributedUnmatchedCount = 0
        noProductionPeakCount = 0
        blockingGateCounts.clear()
        productionTakeoffCount = 0
        productionLandingCount = 0
        interruptedPulseCount = 0
        processedFrames = 0L
        pulses.clear()
        pendingPulses.clear()
        pendingPeakEvidence.clear()
    }

    private fun completePulse(
        pulse: ActivePulse,
        startedAtMillis: Long,
        completedAtMillis: Long,
    ) {
        val ankleRiseRatio =
            (pulse.startAnkleY - pulse.peakAnkleY) / pulse.legLength
        val hipRiseRatio =
            (pulse.startHipY - pulse.peakHipY) / pulse.legLength
        val qualified =
            ankleRiseRatio >= TRACE_MIN_ANKLE_RISE_RATIO &&
                hipRiseRatio >= TRACE_MIN_HIP_RISE_RATIO
        val completed = CompletedPulse(
            completedAtMillis = completedAtMillis,
            elapsedMillis = (pulse.peakAtMillis - startedAtMillis).coerceAtLeast(0L),
            durationMillis =
                (pulse.peakAtMillis - pulse.startedAtMillis).coerceAtLeast(0L),
            ankleRiseRatio = ankleRiseRatio,
            hipRiseRatio = hipRiseRatio,
            qualified = qualified,
            matchedProductionTakeoff = pulse.matchedProductionTakeoff,
            peakTrackingStatus = pulse.peakTrackingStatus,
        )
        if (qualified && !pulse.matchedProductionTakeoff) {
            pendingPulses.addLast(completed)
        } else {
            publishPulse(completed, gateAttribution = null)
        }
    }

    private fun matchOrExpirePending(timestampMillis: Long): Boolean {
        val publishedBefore = rawPulseCount
        while (pendingPulses.isNotEmpty()) {
            val pulse = pendingPulses.first()
            val match = pendingPeakEvidence
                .withIndex()
                .filter {
                    kotlin.math.abs(it.value.timestampMillis - pulse.completedAtMillis) <=
                        EVIDENCE_MATCH_WINDOW_MILLIS
                }
                .minByOrNull {
                    kotlin.math.abs(it.value.timestampMillis - pulse.completedAtMillis)
                }
            if (match != null) {
                repeat(match.index) { pendingPeakEvidence.removeFirst() }
                val evidence = pendingPeakEvidence.removeFirst().evidence
                pendingPulses.removeFirst()
                publishPulse(pulse, evidence.toT735GateAttribution())
                continue
            }
            if (timestampMillis - pulse.completedAtMillis <= EVIDENCE_MATCH_WINDOW_MILLIS) break
            pendingPulses.removeFirst()
            publishPulse(pulse, gateAttribution = null)
        }
        while (
            pendingPeakEvidence.isNotEmpty() &&
            timestampMillis - pendingPeakEvidence.first().timestampMillis >
            EVIDENCE_MATCH_WINDOW_MILLIS
        ) {
            pendingPeakEvidence.removeFirst()
        }
        return rawPulseCount != publishedBefore
    }

    private fun publishPulse(
        pulse: CompletedPulse,
        gateAttribution: T735GateAttribution?,
    ) {
        pulseSequence += 1
        rawPulseCount += 1
        if (pulse.qualified) {
            qualifiedPulseCount += 1
            if (pulse.matchedProductionTakeoff) {
                matchedPulseCount += 1
            } else {
                unmatchedPulseCount += 1
                if (pulse.peakTrackingStatus == BounceTrackingStatus.AIRBORNE) {
                    unmatchedWhileAirborneCount += 1
                }
                if (gateAttribution == null) {
                    noProductionPeakCount += 1
                } else {
                    attributedUnmatchedCount += 1
                    gateAttribution.blockingGates.forEach { gate ->
                        blockingGateCounts[gate] = (blockingGateCounts[gate] ?: 0) + 1
                    }
                }
            }
        }
        pulses.addLast(
            T735MotionPulse(
                sequence = pulseSequence,
                elapsedMillis = pulse.elapsedMillis,
                durationMillis = pulse.durationMillis,
                ankleRiseRatio = pulse.ankleRiseRatio,
                hipRiseRatio = pulse.hipRiseRatio,
                qualified = pulse.qualified,
                matchedProductionTakeoff = pulse.matchedProductionTakeoff,
                peakTrackingStatus = pulse.peakTrackingStatus,
                gateAttribution = gateAttribution,
            ),
        )
        while (pulses.size > MAX_RETAINED_PULSES) pulses.removeFirst()
    }

    private fun snapshotIfDue(force: Boolean): T735TakeoffGateSnapshot? =
        if (force || processedFrames == 1L || processedFrames % SNAPSHOT_INTERVAL_FRAMES == 0L) {
            snapshot()
        } else {
            null
        }

    private fun snapshot(): T735TakeoffGateSnapshot =
        T735TakeoffGateSnapshot(
            measurementStarted = measurementStartedAtMillis != null,
            rawPulseCount = rawPulseCount,
            qualifiedPulseCount = qualifiedPulseCount,
            matchedPulseCount = matchedPulseCount,
            unmatchedPulseCount = unmatchedPulseCount,
            unmatchedWhileAirborneCount = unmatchedWhileAirborneCount,
            attributedUnmatchedCount = attributedUnmatchedCount,
            noProductionPeakCount = noProductionPeakCount,
            pendingEvidenceCount = pendingPulses.size,
            blockingGateCounts = blockingGateCounts.toMap(),
            productionTakeoffCount = productionTakeoffCount,
            productionLandingCount = productionLandingCount,
            interruptedPulseCount = interruptedPulseCount,
            latestPulses = displayedPulses(),
            processedFrames = processedFrames,
        )

    private fun displayedPulses(): List<T735MotionPulse> {
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

    private data class CompletedPulse(
        val completedAtMillis: Long,
        val elapsedMillis: Long,
        val durationMillis: Long,
        val ankleRiseRatio: Float,
        val hipRiseRatio: Float,
        val qualified: Boolean,
        val matchedProductionTakeoff: Boolean,
        val peakTrackingStatus: BounceTrackingStatus,
    )

    private data class TimestampedPeakEvidence(
        val timestampMillis: Long,
        val evidence: TakeoffPeakEvidence,
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
        const val EVIDENCE_MATCH_WINDOW_MILLIS = 120L
    }
}

private fun T735TakeoffGateSnapshot.gateCount(gate: T735BlockingGate): Int =
    blockingGateCounts[gate] ?: 0

private fun TakeoffPeakEvidence.toT735GateAttribution(): T735GateAttribution {
    val route = if (smoothedAnkleRiseRatio >= T735_STANDARD_ANKLE_RISE_RATIO) {
        T735TakeoffRoute.STANDARD
    } else {
        T735TakeoffRoute.RESCUE
    }
    val blockingGates = buildSet {
        if (diagnostic == BounceDiagnostic.FEET_NOT_SYNCHRONIZED) {
            add(T735BlockingGate.FEET_SYNCHRONIZED)
        }
        if (!leftIndividualAnkleGatePassed) {
            add(T735BlockingGate.LEFT_BILATERAL_ANKLE)
        }
        if (!rightIndividualAnkleGatePassed) {
            add(T735BlockingGate.RIGHT_BILATERAL_ANKLE)
        }
        if (smoothedHipRiseRatio < rawAnkleRiseRatio * T735_HIP_TO_ANKLE_RATIO) {
            add(T735BlockingGate.HIP_TO_ANKLE_RATIO)
        }
        when (route) {
            T735TakeoffRoute.STANDARD -> {
                if (smoothedHipRiseRatio < T735_STANDARD_HIP_RISE_RATIO) {
                    add(T735BlockingGate.STANDARD_HIP_RISE)
                }
            }
            T735TakeoffRoute.RESCUE -> {
                if (
                    smoothedAnkleRiseRatio <
                    T736DetectorProfiles.PRODUCTION.strongHipRescueAnkleRiseRatio
                ) {
                    add(T735BlockingGate.RESCUE_ANKLE_RISE)
                }
                if (smoothedHipRiseRatio < T735_RESCUE_HIP_RISE_RATIO) {
                    add(T735BlockingGate.RESCUE_HIP_RISE)
                }
            }
        }
    }
    return T735GateAttribution(
        route = route,
        blockingGates = blockingGates,
        smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
        rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
        rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
        smoothedHipRiseRatio = smoothedHipRiseRatio,
    )
}

private fun T735GateAttribution.formatT735Attribution(): String =
    String.format(
        Locale.US,
        " %s[%s] P%.3f L%.3f R%.3f H%.3f",
        if (route == T735TakeoffRoute.STANDARD) "STD" else "RES",
        if (blockingGates.isEmpty()) "?" else {
            blockingGates.joinToString("+") { it.shortT735Name() }
        },
        smoothedAnkleRiseRatio,
        rawLeftAnkleRiseRatio,
        rawRightAnkleRiseRatio,
        smoothedHipRiseRatio,
    )

private fun T735BlockingGate.shortT735Name(): String = when (this) {
    T735BlockingGate.FEET_SYNCHRONIZED -> "SY"
    T735BlockingGate.LEFT_BILATERAL_ANKLE -> "BL"
    T735BlockingGate.RIGHT_BILATERAL_ANKLE -> "BR"
    T735BlockingGate.HIP_TO_ANKLE_RATIO -> "Q"
    T735BlockingGate.STANDARD_HIP_RISE -> "SH"
    T735BlockingGate.RESCUE_ANKLE_RISE -> "RA"
    T735BlockingGate.RESCUE_HIP_RISE -> "RH"
}

private fun BounceTrackingStatus.shortT735Name(): String = when (this) {
    BounceTrackingStatus.WAITING -> "WAIT"
    BounceTrackingStatus.CALIBRATING -> "CAL"
    BounceTrackingStatus.READY -> "READY"
    BounceTrackingStatus.AIRBORNE -> "AIR"
}

private const val T735_STANDARD_ANKLE_RISE_RATIO = 0.045f
private const val T735_STANDARD_HIP_RISE_RATIO = 0.060f
private const val T735_RESCUE_HIP_RISE_RATIO = 0.100f
private const val T735_HIP_TO_ANKLE_RATIO = 0.85f
