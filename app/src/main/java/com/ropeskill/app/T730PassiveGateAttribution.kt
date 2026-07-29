package com.ropeskill.app

import java.util.Locale

enum class T730PeakRoute {
    STANDARD,
    STRONG_HIP_RESCUE,
    UNKNOWN,
}

enum class T730BlockingGate {
    FEET_SYNCHRONIZED,
    LEFT_BILATERAL_ANKLE,
    RIGHT_BILATERAL_ANKLE,
    HIP_TO_ANKLE_RATIO,
    STANDARD_HIP_RISE,
    RESCUE_ANKLE_RISE,
    RESCUE_HIP_RISE,
    UNATTRIBUTED,
}

data class T730GateEvaluation(
    val feetSynchronized: Boolean,
    val standardAnkleRisePassed: Boolean,
    val leftBilateralAnklePassed: Boolean,
    val rightBilateralAnklePassed: Boolean,
    val standardHipRisePassed: Boolean,
    val rescueAnkleRisePassed: Boolean,
    val rescueHipRisePassed: Boolean,
    val hipToAnkleRatioPassed: Boolean,
    val standardPathPassed: Boolean,
    val rescuePathPassed: Boolean,
)

data class T730RejectedPeakAttribution(
    val eventId: Int,
    val route: T730PeakRoute,
    val blockingGates: Set<T730BlockingGate>,
    val smoothedAnkleRiseRatio: Float,
    val rawLeftAnkleRiseRatio: Float,
    val rawRightAnkleRiseRatio: Float,
    val smoothedHipRiseRatio: Float,
    val hipToAnkleRiseRatio: Float?,
    val gateEvaluation: T730GateEvaluation?,
    val evidenceOperandsExactlyMatched: Boolean,
)

data class T730AttributionSnapshot(
    val measurementActive: Boolean,
    val measurementInvalid: Boolean,
    val completedPeakCount: Int,
    val countedPeakCount: Int,
    val rejectedPeakCount: Int,
    val suppressedPeakCount: Int,
    val blockingGateCounts: Map<T730BlockingGate, Int>,
    val retainedRejectedPeaks: List<T730RejectedPeakAttribution>,
    val overflowCount: Int,
)

/**
 * Classifies debug evidence already emitted by the production detector.
 *
 * Exact operand equality is required before combining the two retained evidence records. Equality
 * does not prove shared frame provenance, so the output is a blocker classification, not a causal
 * attribution. This collector never feeds a value back into [BasicBounceDetector] and cannot
 * change workout state, Counter, Result, History, thresholds, or detector decisions.
 */
internal class T730PassiveGateAttributionCollector(
    private val enabled: Boolean,
    private val maxRejectedPeakHistory: Int = DEFAULT_REJECTED_PEAK_HISTORY,
) {
    private var measurementActive = false
    private var measurementInvalid = false
    private var completedPeakCount = 0
    private var countedPeakCount = 0
    private var rejectedPeakCount = 0
    private var suppressedPeakCount = 0
    private var overflowCount = 0
    private var blockingGateCounts = emptyMap<T730BlockingGate, Int>()
    private var retainedRejectedPeaks = emptyList<T730RejectedPeakAttribution>()
    private var lastRecordedPeakEvidence: TakeoffPeakEvidence? = null
    private var measurementStartedAtMillis: Long? = null

    init {
        require(maxRejectedPeakHistory > 0)
    }

    fun startMeasurement(timestampMillis: Long? = null): T730AttributionSnapshot? {
        if (!enabled) return null
        clear()
        measurementActive = true
        measurementStartedAtMillis = timestampMillis
        return snapshot()
    }

    fun record(
        result: BounceDetectionResult,
        timestampMillis: Long? = null,
    ): T730AttributionSnapshot? {
        if (!enabled || !measurementActive) return null
        if (
            (
                result.trackingStatus == BounceTrackingStatus.WAITING &&
                    result.diagnostic == BounceDiagnostic.FULL_BODY_REQUIRED
            ) ||
            result.trackingStatus == BounceTrackingStatus.CALIBRATING ||
            result.diagnostic == BounceDiagnostic.CALIBRATING
        ) {
            measurementActive = false
            measurementInvalid = true
            return snapshot()
        }
        val peak = result.takeoffPeakEvidence ?: return null
        if (peak === lastRecordedPeakEvidence) return null
        lastRecordedPeakEvidence = peak
        if (peak.startedBeforeMeasurement(timestampMillis)) {
            measurementActive = false
            measurementInvalid = true
            return snapshot()
        }

        completedPeakCount += 1
        when (peak.outcome) {
            TakeoffPeakOutcome.COUNTED -> countedPeakCount += 1
            TakeoffPeakOutcome.SUPPRESSED -> suppressedPeakCount += 1
            TakeoffPeakOutcome.REJECTED -> {
                rejectedPeakCount += 1
                val attribution = attributeRejectedPeak(
                    eventId = completedPeakCount,
                    peak = peak,
                    rejected = result.rejectedTakeoffEvidence,
                )
                attribution.blockingGates.forEach { gate ->
                    blockingGateCounts = blockingGateCounts +
                        (gate to (blockingGateCounts[gate] ?: 0) + 1)
                }
                if (retainedRejectedPeaks.size == maxRejectedPeakHistory) {
                    overflowCount += 1
                }
                retainedRejectedPeaks =
                    (retainedRejectedPeaks + attribution).takeLast(maxRejectedPeakHistory)
            }
        }
        return snapshot()
    }

    fun reset() {
        clear()
    }

    private fun clear() {
        measurementActive = false
        measurementInvalid = false
        completedPeakCount = 0
        countedPeakCount = 0
        rejectedPeakCount = 0
        suppressedPeakCount = 0
        overflowCount = 0
        blockingGateCounts = emptyMap()
        retainedRejectedPeaks = emptyList()
        lastRecordedPeakEvidence = null
        measurementStartedAtMillis = null
    }

    private fun attributeRejectedPeak(
        eventId: Int,
        peak: TakeoffPeakEvidence,
        rejected: RejectedTakeoffEvidence?,
    ): T730RejectedPeakAttribution {
        val evidenceOperandsExactlyMatched = rejected != null &&
            peak.diagnostic == rejected.diagnostic &&
            peak.smoothedAnkleRiseRatio.toRawBits() ==
            rejected.ankleRiseRatio.toRawBits() &&
            peak.smoothedHipRiseRatio.toRawBits() ==
            rejected.hipRiseRatio.toRawBits()
        if (!evidenceOperandsExactlyMatched) {
            return T730RejectedPeakAttribution(
                eventId = eventId,
                route = T730PeakRoute.UNKNOWN,
                blockingGates = setOf(T730BlockingGate.UNATTRIBUTED),
                smoothedAnkleRiseRatio = peak.smoothedAnkleRiseRatio,
                rawLeftAnkleRiseRatio = peak.rawLeftAnkleRiseRatio,
                rawRightAnkleRiseRatio = peak.rawRightAnkleRiseRatio,
                smoothedHipRiseRatio = peak.smoothedHipRiseRatio,
                hipToAnkleRiseRatio = null,
                gateEvaluation = null,
                evidenceOperandsExactlyMatched = false,
            )
        }

        val standardAnkleRisePassed =
            rejected.ankleRiseRatio >= rejected.ankleRiseThreshold
        // Keep the production comparison direction; its reported division ratio is forced to zero
        // for non-positive ankle rise and cannot safely reconstruct this gate.
        val hipToAnkleRatioPassed =
            peak.smoothedHipRiseRatio >=
                peak.rawAnkleRiseRatio * rejected.hipToAnkleRiseThreshold
        val displayedHipToAnkleRatio =
            peak.rawAnkleRiseRatio
                .takeIf { it > 0f }
                ?.let { peak.smoothedHipRiseRatio / it }
        val evaluation = T730GateEvaluation(
            feetSynchronized = rejected.feetSynchronized,
            standardAnkleRisePassed = standardAnkleRisePassed,
            leftBilateralAnklePassed = peak.leftIndividualAnkleGatePassed,
            rightBilateralAnklePassed = peak.rightIndividualAnkleGatePassed,
            standardHipRisePassed =
                rejected.hipRiseRatio >= rejected.hipRiseThreshold,
            rescueAnkleRisePassed =
                rejected.ankleRiseRatio >=
                    T729DetectorProfiles.BASELINE.strongHipRescueAnkleRiseRatio,
            rescueHipRisePassed =
                rejected.hipRiseRatio >= STRONG_HIP_RESCUE_HIP_RISE_RATIO,
            hipToAnkleRatioPassed = hipToAnkleRatioPassed,
            standardPathPassed = false,
            rescuePathPassed = false,
        ).withDerivedPaths()
        val route = if (standardAnkleRisePassed) {
            T730PeakRoute.STANDARD
        } else {
            T730PeakRoute.STRONG_HIP_RESCUE
        }
        val blockers = buildSet {
            if (!evaluation.feetSynchronized) {
                add(T730BlockingGate.FEET_SYNCHRONIZED)
            }
            if (!evaluation.leftBilateralAnklePassed) {
                add(T730BlockingGate.LEFT_BILATERAL_ANKLE)
            }
            if (!evaluation.rightBilateralAnklePassed) {
                add(T730BlockingGate.RIGHT_BILATERAL_ANKLE)
            }
            if (!evaluation.hipToAnkleRatioPassed) {
                add(T730BlockingGate.HIP_TO_ANKLE_RATIO)
            }
            when (route) {
                T730PeakRoute.STANDARD -> {
                    if (!evaluation.standardHipRisePassed) {
                        add(T730BlockingGate.STANDARD_HIP_RISE)
                    }
                }
                T730PeakRoute.STRONG_HIP_RESCUE -> {
                    if (!evaluation.rescueAnkleRisePassed) {
                        add(T730BlockingGate.RESCUE_ANKLE_RISE)
                    }
                    if (!evaluation.rescueHipRisePassed) {
                        add(T730BlockingGate.RESCUE_HIP_RISE)
                    }
                }
                T730PeakRoute.UNKNOWN -> Unit
            }
        }.ifEmpty {
            setOf(T730BlockingGate.UNATTRIBUTED)
        }

        return T730RejectedPeakAttribution(
            eventId = eventId,
            route = route,
            blockingGates = blockers,
            smoothedAnkleRiseRatio = rejected.ankleRiseRatio,
            rawLeftAnkleRiseRatio = peak.rawLeftAnkleRiseRatio,
            rawRightAnkleRiseRatio = peak.rawRightAnkleRiseRatio,
            smoothedHipRiseRatio = rejected.hipRiseRatio,
            hipToAnkleRiseRatio = displayedHipToAnkleRatio,
            gateEvaluation = evaluation,
            evidenceOperandsExactlyMatched = true,
        )
    }

    private fun T730GateEvaluation.withDerivedPaths(): T730GateEvaluation =
        copy(
            standardPathPassed =
                feetSynchronized &&
                    standardAnkleRisePassed &&
                    leftBilateralAnklePassed &&
                    rightBilateralAnklePassed &&
                    standardHipRisePassed &&
                    hipToAnkleRatioPassed,
            rescuePathPassed =
                feetSynchronized &&
                    !standardAnkleRisePassed &&
                    leftBilateralAnklePassed &&
                    rightBilateralAnklePassed &&
                    rescueAnkleRisePassed &&
                    rescueHipRisePassed &&
                    hipToAnkleRatioPassed,
        )

    private fun TakeoffPeakEvidence.startedBeforeMeasurement(
        completedAtMillis: Long?,
    ): Boolean {
        if (outcome != TakeoffPeakOutcome.REJECTED) return false
        val windowStart = measurementStartedAtMillis ?: return false
        val completedAt = completedAtMillis ?: return false
        val afterPeakMillis = nextFrameIntervalMillis ?: return true
        val observationStartedAt =
            completedAt - afterPeakMillis - riseMillis
        return observationStartedAt < windowStart
    }

    private fun snapshot(): T730AttributionSnapshot =
        T730AttributionSnapshot(
            measurementActive = measurementActive,
            measurementInvalid = measurementInvalid,
            completedPeakCount = completedPeakCount,
            countedPeakCount = countedPeakCount,
            rejectedPeakCount = rejectedPeakCount,
            suppressedPeakCount = suppressedPeakCount,
            blockingGateCounts = blockingGateCounts,
            retainedRejectedPeaks = retainedRejectedPeaks,
            overflowCount = overflowCount,
        )

    private companion object {
        const val DEFAULT_REJECTED_PEAK_HISTORY = 6
        const val STRONG_HIP_RESCUE_HIP_RISE_RATIO = 0.100f
    }
}

internal fun formatT730AttributionSnapshot(
    snapshot: T730AttributionSnapshot,
): String = buildString {
    append("T-730 GATE V12 ")
    append(
        when {
            snapshot.measurementInvalid -> "INVALID-RESTART"
            snapshot.measurementActive -> "ACTIVE"
            else -> "INACTIVE"
        },
    )
    append(
        String.format(
            Locale.US,
            "\nP%d C%d R%d S%d U%d OV%d",
            snapshot.completedPeakCount,
            snapshot.countedPeakCount,
            snapshot.rejectedPeakCount,
            snapshot.suppressedPeakCount,
            snapshot.blockingGateCounts[T730BlockingGate.UNATTRIBUTED] ?: 0,
            snapshot.overflowCount,
        ),
    )
    append(
        String.format(
            Locale.US,
            "\nG SY%d BL%d BR%d Q%d SH%d RA%d RH%d",
            snapshot.gateCount(T730BlockingGate.FEET_SYNCHRONIZED),
            snapshot.gateCount(T730BlockingGate.LEFT_BILATERAL_ANKLE),
            snapshot.gateCount(T730BlockingGate.RIGHT_BILATERAL_ANKLE),
            snapshot.gateCount(T730BlockingGate.HIP_TO_ANKLE_RATIO),
            snapshot.gateCount(T730BlockingGate.STANDARD_HIP_RISE),
            snapshot.gateCount(T730BlockingGate.RESCUE_ANKLE_RISE),
            snapshot.gateCount(T730BlockingGate.RESCUE_HIP_RISE),
        ),
    )
    append("\nTH SA.0450 B.0100 SH.0600 RA.0200 RH.1000 Q.8500")
    snapshot.retainedRejectedPeaks.forEach { evidence ->
        append(
            String.format(
                Locale.US,
                "\n#%02d %s[%s] A%.4f L%.4f R%.4f H%.4f Q%s SY%s",
                evidence.eventId,
                evidence.route.shortName(),
                evidence.blockingGates.joinToString(",") { it.shortName() },
                evidence.smoothedAnkleRiseRatio,
                evidence.rawLeftAnkleRiseRatio,
                evidence.rawRightAnkleRiseRatio,
                evidence.smoothedHipRiseRatio,
                evidence.hipToAnkleRiseRatio?.let {
                    String.format(Locale.US, "%.4f", it)
                } ?: "---",
                when (evidence.gateEvaluation?.feetSynchronized) {
                    true -> "+"
                    false -> "-"
                    null -> "?"
                },
            ),
        )
    }
}

private fun T730AttributionSnapshot.gateCount(gate: T730BlockingGate): Int =
    blockingGateCounts[gate] ?: 0

private fun T730PeakRoute.shortName(): String = when (this) {
    T730PeakRoute.STANDARD -> "STD"
    T730PeakRoute.STRONG_HIP_RESCUE -> "RES"
    T730PeakRoute.UNKNOWN -> "UNK"
}

private fun T730BlockingGate.shortName(): String = when (this) {
    T730BlockingGate.FEET_SYNCHRONIZED -> "SY"
    T730BlockingGate.LEFT_BILATERAL_ANKLE -> "BL"
    T730BlockingGate.RIGHT_BILATERAL_ANKLE -> "BR"
    T730BlockingGate.HIP_TO_ANKLE_RATIO -> "Q"
    T730BlockingGate.STANDARD_HIP_RISE -> "SH"
    T730BlockingGate.RESCUE_ANKLE_RISE -> "RA"
    T730BlockingGate.RESCUE_HIP_RISE -> "RH"
    T730BlockingGate.UNATTRIBUTED -> "UN"
}
