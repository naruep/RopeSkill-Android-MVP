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

enum class T730TraceRegion {
    LEAD,
    WINDOW,
    BOUNDARY,
    TAIL,
}

enum class T730InvalidReason {
    FULL_BODY_LOSS,
    TRACKING_LOSS_DURING_CYCLE,
    RECALIBRATING,
    TIMESTAMP_MISSING,
    TIMESTAMP_BEFORE_MEASUREMENT,
    TIMESTAMP_REVERSED,
    TIMESTAMP_MISMATCH,
    CYCLE_SEQUENCE_GAP,
    CYCLE_SEQUENCE_COLLISION,
    OVERLAPPING_TAKEOFF,
    ORPHAN_ACCEPTED_LANDING,
    OUTCOME_EVENT_MISMATCH,
    AIRBORNE_INTERVAL_MISMATCH,
    MISSING_ACCEPTED_PEAK,
    TRACE_OVERFLOW,
}

enum class T730StopReason {
    POST_WINDOW_FULL_BODY_EXIT,
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
    val gateMargins: T730GateMargins? = null,
)

data class T730GateMargins(
    val standardAnkleRise: Float,
    val leftBilateralAnkleRise: Float,
    val rightBilateralAnkleRise: Float,
    val standardHipRise: Float,
    val rescueAnkleRise: Float,
    val rescueHipRise: Float,
    val hipToAnkleProduct: Float,
)

data class T730PeakTraceEvent(
    val eventId: Int,
    val region: T730TraceRegion,
    val outcome: TakeoffPeakOutcome,
    val emittedAtElapsedMillis: Long?,
    val spanStartedAtElapsedMillis: Long?,
    val peakAtElapsedMillis: Long?,
    val spanEndedAtElapsedMillis: Long?,
    val takeoffCycleSequence: Int?,
    val completionCycleSequence: Int?,
    val peakEvidence: TakeoffPeakEvidence,
    val rejectedAttribution: T730RejectedPeakAttribution?,
    val cycleSeparationEvidence: T730CycleSeparationEvidence?,
)

data class T730CycleSeparationEvidence(
    val observedAirborneMillis: Long,
    val reportedAirborneMillis: Long?,
    val airborneFrameSamples: Int,
    val rearmMillisBeforeTakeoff: Long?,
    val readyFrameSamplesBeforeTakeoff: Int,
    val takeoffIntervalMillis: Long?,
    val landingReason: LandingDetectionReason?,
    val countIntervalMillis: Long?,
)

data class T730CycleSeparationSummary(
    val acceptedCycleCount: Int,
    val medianObservedAirborneMillis: Long?,
    val maximumObservedAirborneMillis: Long?,
    val maximumObservedAirborneEventId: Int?,
    val medianRearmMillis: Long?,
    val maximumRearmMillis: Long?,
    val maximumRearmEventId: Int?,
    val medianTakeoffIntervalMillis: Long?,
    val maximumTakeoffIntervalMillis: Long?,
    val maximumTakeoffIntervalEventId: Int?,
    val longestTakeoffIntervalBreakdown: T730TakeoffIntervalBreakdown?,
)

data class T730TakeoffIntervalBreakdown(
    val eventId: Int,
    val previousEventId: Int,
    val takeoffIntervalMillis: Long,
    val previousObservedAirborneMillis: Long,
    val rearmMillisBeforeTakeoff: Long,
    val readyFrameSamplesBeforeTakeoff: Int,
    val residualMillis: Long,
)

data class T730WindowTraceSummary(
    val firstAcceptedEventId: Int?,
    val lastAcceptedEventId: Int?,
    val startedAtElapsedMillis: Long?,
    val endedAtElapsedMillis: Long?,
    val leadPeakCount: Int,
    val windowPeakCount: Int,
    val boundaryPeakCount: Int,
    val tailPeakCount: Int,
    val windowCountedPeakCount: Int,
    val windowRejectedPeakCount: Int,
    val windowSuppressedPeakCount: Int,
    val windowBlockingGateCounts: Map<T730BlockingGate, Int>,
)

data class T730RaCounterfactualSummary(
    val windowRaBlockedPeakCount: Int,
    val windowRaOnlyBlockedPeakCount: Int,
    val highestRaOnlyRecoveryFloor: Float?,
    val lowestRaOnlyRecoveryFloor: Float?,
)

data class T730AttributionSnapshot(
    val measurementActive: Boolean,
    val measurementSealed: Boolean,
    val measurementInvalid: Boolean,
    val stopReason: T730StopReason?,
    val stoppedAtElapsedMillis: Long?,
    val invalidReason: T730InvalidReason?,
    val invalidAtElapsedMillis: Long?,
    val completedPeakCount: Int,
    val countedPeakCount: Int,
    val rejectedPeakCount: Int,
    val suppressedPeakCount: Int,
    val blockingGateCounts: Map<T730BlockingGate, Int>,
    val retainedRejectedPeaks: List<T730RejectedPeakAttribution>,
    val traceEvents: List<T730PeakTraceEvent>,
    val window: T730WindowTraceSummary,
    val raCounterfactual: T730RaCounterfactualSummary,
    val cycleSeparation: T730CycleSeparationSummary,
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
    private val maxTraceEventHistory: Int = DEFAULT_TRACE_EVENT_HISTORY,
    private val maxRetainedRejectedPeakHistory: Int = DEFAULT_RETAINED_REJECTED_HISTORY,
) {
    private class PendingAcceptedTakeoff(
        val timestampMillis: Long,
        val cycleSequence: Int,
        var airborneFrameSamples: Int,
        val rearmMillisBeforeTakeoff: Long?,
        val readyFrameSamplesBeforeTakeoff: Int,
        val takeoffIntervalMillis: Long?,
    )

    private var measurementActive = false
    private var measurementSealed = false
    private var measurementInvalid = false
    private var stopReason: T730StopReason? = null
    private var stoppedAtElapsedMillis: Long? = null
    private var invalidReason: T730InvalidReason? = null
    private var invalidAtElapsedMillis: Long? = null
    private var completedPeakCount = 0
    private var countedPeakCount = 0
    private var rejectedPeakCount = 0
    private var suppressedPeakCount = 0
    private var overflowCount = 0
    private var blockingGateCounts = emptyMap<T730BlockingGate, Int>()
    private var retainedRejectedPeaks = emptyList<T730RejectedPeakAttribution>()
    private var traceEvents = emptyList<T730PeakTraceEvent>()
    private var lastRecordedPeakEvidence: TakeoffPeakEvidence? = null
    private var measurementStartedAtMillis: Long? = null
    private var lastRecordedAtMillis: Long? = null
    private var lastCycleSequence: Int? = null
    private var pendingAcceptedTakeoff: PendingAcceptedTakeoff? = null
    private var firstAcceptedEventId: Int? = null
    private var lastAcceptedEventId: Int? = null
    private var firstAcceptedTakeoffAtElapsedMillis: Long? = null
    private var lastAcceptedLandingAtElapsedMillis: Long? = null
    private var lastAcceptedTakeoffTimestampMillis: Long? = null
    private var lastAcceptedLandingTimestampMillis: Long? = null
    private var readyFrameSamplesSinceAcceptedLanding = 0

    init {
        require(maxTraceEventHistory > 0)
        require(maxRetainedRejectedPeakHistory > 0)
    }

    fun startMeasurement(timestampMillis: Long? = null): T730AttributionSnapshot? {
        if (!enabled) return null
        clear()
        measurementActive = true
        measurementStartedAtMillis = timestampMillis
        lastRecordedAtMillis = timestampMillis
        return snapshot()
    }

    fun record(
        result: BounceDetectionResult,
        timestampMillis: Long? = null,
    ): T730AttributionSnapshot? {
        if (!enabled || !measurementActive) return null
        validateTimestamp(timestampMillis)?.let { reason ->
            return invalidate(reason, timestampMillis)
        }

        val peak = result.takeoffPeakEvidence
        if (peak != null && peak === lastRecordedPeakEvidence) return null

        if (
            result.trackingStatus == BounceTrackingStatus.WAITING &&
            result.diagnostic == BounceDiagnostic.FULL_BODY_REQUIRED
        ) {
            if (pendingAcceptedTakeoff != null) {
                return invalidate(
                    T730InvalidReason.TRACKING_LOSS_DURING_CYCLE,
                    timestampMillis,
                )
            }
            val elapsed = elapsedSinceMeasurement(timestampMillis)
            val lastLanding = lastAcceptedLandingAtElapsedMillis
            if (
                elapsed != null &&
                lastLanding != null &&
                elapsed - lastLanding >= POST_WINDOW_EXIT_GRACE_MILLIS
            ) {
                return seal(
                    reason = T730StopReason.POST_WINDOW_FULL_BODY_EXIT,
                    timestampMillis = timestampMillis,
                )
            }
            return invalidate(T730InvalidReason.FULL_BODY_LOSS, timestampMillis)
        }
        if (
            result.trackingStatus == BounceTrackingStatus.CALIBRATING ||
            result.diagnostic == BounceDiagnostic.CALIBRATING
        ) {
            return invalidate(T730InvalidReason.RECALIBRATING, timestampMillis)
        }

        val cycleTrace = result.cycleTraceEvidence
        validateCycleTrace(cycleTrace, timestampMillis)?.let { reason ->
            return invalidate(reason, timestampMillis)
        }
        validateResultConsistency(result, cycleTrace)?.let { reason ->
            return invalidate(reason, timestampMillis)
        }
        val cycleEvent = if (measurementStartedAtMillis == null) {
            null
        } else {
            cycleTrace?.event
        }
        if (
            cycleEvent == null &&
            pendingAcceptedTakeoff != null &&
            result.trackingStatus == BounceTrackingStatus.AIRBORNE
        ) {
            requireNotNull(pendingAcceptedTakeoff).airborneFrameSamples += 1
        } else if (
            cycleEvent == null &&
            pendingAcceptedTakeoff == null &&
            lastAcceptedLandingTimestampMillis != null &&
            result.trackingStatus == BounceTrackingStatus.READY
        ) {
            readyFrameSamplesSinceAcceptedLanding += 1
        }
        when (cycleEvent) {
            CycleTraceEvent.TAKEOFF -> {
                if (pendingAcceptedTakeoff != null) {
                    return invalidate(
                        T730InvalidReason.OVERLAPPING_TAKEOFF,
                        timestampMillis,
                    )
                }
                pendingAcceptedTakeoff = PendingAcceptedTakeoff(
                    timestampMillis = requireNotNull(timestampMillis),
                    cycleSequence = requireNotNull(cycleTrace).sequence,
                    airborneFrameSamples = 1,
                    rearmMillisBeforeTakeoff =
                        lastAcceptedLandingTimestampMillis?.let { landing ->
                            requireNotNull(timestampMillis) - landing
                        },
                    readyFrameSamplesBeforeTakeoff =
                        readyFrameSamplesSinceAcceptedLanding,
                    takeoffIntervalMillis =
                        lastAcceptedTakeoffTimestampMillis?.let { takeoff ->
                            requireNotNull(timestampMillis) - takeoff
                        },
                )
                readyFrameSamplesSinceAcceptedLanding = 0
            }
            CycleTraceEvent.LANDING_COUNTED,
            CycleTraceEvent.LANDING_SUPPRESSED,
            -> {
                if (pendingAcceptedTakeoff == null) {
                    return invalidate(
                        T730InvalidReason.ORPHAN_ACCEPTED_LANDING,
                        timestampMillis,
                    )
                }
            }
            CycleTraceEvent.REJECTED_TAKEOFF,
            null,
            -> Unit
        }

        if (peak == null) {
            if (
                measurementStartedAtMillis != null &&
                (
                    result.countedJump ||
                    cycleTrace?.event == CycleTraceEvent.LANDING_COUNTED ||
                        cycleTrace?.event == CycleTraceEvent.LANDING_SUPPRESSED
                )
            ) {
                return invalidate(
                    T730InvalidReason.MISSING_ACCEPTED_PEAK,
                    timestampMillis,
                )
            }
            return null
        }
        lastRecordedPeakEvidence = peak
        if (traceEvents.size == maxTraceEventHistory) {
            overflowCount += 1
            return invalidate(T730InvalidReason.TRACE_OVERFLOW, timestampMillis)
        }

        val eventId = completedPeakCount + 1
        val rejectedAttribution = if (peak.outcome == TakeoffPeakOutcome.REJECTED) {
            attributeRejectedPeak(
                eventId = eventId,
                peak = peak,
                rejected = result.rejectedTakeoffEvidence,
            )
        } else {
            null
        }
        val traceEvent = createTraceEvent(
            eventId = eventId,
            peak = peak,
            rejectedAttribution = rejectedAttribution,
            hasRejectedEvidence = result.rejectedTakeoffEvidence != null,
            cycleTrace = cycleTrace,
            timestampMillis = timestampMillis,
        ) ?: return snapshot()

        completedPeakCount = eventId
        when (peak.outcome) {
            TakeoffPeakOutcome.COUNTED -> countedPeakCount += 1
            TakeoffPeakOutcome.SUPPRESSED -> suppressedPeakCount += 1
            TakeoffPeakOutcome.REJECTED -> {
                rejectedPeakCount += 1
                val attribution = requireNotNull(rejectedAttribution)
                attribution.blockingGates.forEach { gate ->
                    blockingGateCounts = blockingGateCounts +
                        (gate to (blockingGateCounts[gate] ?: 0) + 1)
                }
                retainedRejectedPeaks =
                    (retainedRejectedPeaks + attribution)
                        .takeLast(maxRetainedRejectedPeakHistory)
            }
        }
        traceEvents = traceEvents + traceEvent
        if (peak.outcome != TakeoffPeakOutcome.REJECTED) {
            if (firstAcceptedEventId == null) {
                firstAcceptedEventId = eventId
                firstAcceptedTakeoffAtElapsedMillis =
                    traceEvent.spanStartedAtElapsedMillis
            }
            lastAcceptedEventId = eventId
            lastAcceptedLandingAtElapsedMillis =
                traceEvent.spanEndedAtElapsedMillis
            if (measurementStartedAtMillis != null) {
                val acceptedTakeoff = requireNotNull(pendingAcceptedTakeoff)
                lastAcceptedTakeoffTimestampMillis =
                    acceptedTakeoff.timestampMillis
                lastAcceptedLandingTimestampMillis =
                    requireNotNull(timestampMillis)
                readyFrameSamplesSinceAcceptedLanding = 0
            }
            pendingAcceptedTakeoff = null
        }
        return snapshot()
    }

    fun reset() {
        clear()
    }

    private fun clear() {
        measurementActive = false
        measurementSealed = false
        measurementInvalid = false
        stopReason = null
        stoppedAtElapsedMillis = null
        invalidReason = null
        invalidAtElapsedMillis = null
        completedPeakCount = 0
        countedPeakCount = 0
        rejectedPeakCount = 0
        suppressedPeakCount = 0
        overflowCount = 0
        blockingGateCounts = emptyMap()
        retainedRejectedPeaks = emptyList()
        traceEvents = emptyList()
        lastRecordedPeakEvidence = null
        measurementStartedAtMillis = null
        lastRecordedAtMillis = null
        lastCycleSequence = null
        pendingAcceptedTakeoff = null
        firstAcceptedEventId = null
        lastAcceptedEventId = null
        firstAcceptedTakeoffAtElapsedMillis = null
        lastAcceptedLandingAtElapsedMillis = null
        lastAcceptedTakeoffTimestampMillis = null
        lastAcceptedLandingTimestampMillis = null
        readyFrameSamplesSinceAcceptedLanding = 0
    }

    private fun validateTimestamp(timestampMillis: Long?): T730InvalidReason? {
        val measurementStart = measurementStartedAtMillis ?: return null
        val timestamp = timestampMillis ?: return T730InvalidReason.TIMESTAMP_MISSING
        if (timestamp < measurementStart) {
            return T730InvalidReason.TIMESTAMP_BEFORE_MEASUREMENT
        }
        val previousTimestamp = lastRecordedAtMillis
        if (previousTimestamp != null && timestamp < previousTimestamp) {
            return T730InvalidReason.TIMESTAMP_REVERSED
        }
        lastRecordedAtMillis = timestamp
        return null
    }

    private fun validateCycleTrace(
        cycleTrace: CycleTraceEvidence?,
        timestampMillis: Long?,
    ): T730InvalidReason? {
        if (cycleTrace == null || measurementStartedAtMillis == null) return null
        val timestamp = timestampMillis ?: return T730InvalidReason.TIMESTAMP_MISSING
        if (cycleTrace.timestampMillis != timestamp) {
            return T730InvalidReason.TIMESTAMP_MISMATCH
        }
        val previousSequence = lastCycleSequence
        if (previousSequence != null) {
            if (cycleTrace.sequence <= previousSequence) {
                return T730InvalidReason.CYCLE_SEQUENCE_COLLISION
            }
            if (cycleTrace.sequence != previousSequence + 1) {
                return T730InvalidReason.CYCLE_SEQUENCE_GAP
            }
        }
        lastCycleSequence = cycleTrace.sequence
        return null
    }

    private fun validateResultConsistency(
        result: BounceDetectionResult,
        cycleTrace: CycleTraceEvidence?,
    ): T730InvalidReason? {
        if (measurementStartedAtMillis == null) return null
        return when (cycleTrace?.event) {
            CycleTraceEvent.TAKEOFF -> {
                if (
                    result.event != BounceEvent.TAKEOFF ||
                    result.countedJump
                ) {
                    T730InvalidReason.OUTCOME_EVENT_MISMATCH
                } else {
                    null
                }
            }
            CycleTraceEvent.LANDING_COUNTED -> {
                if (
                    result.event != BounceEvent.LANDING ||
                    !result.countedJump
                ) {
                    T730InvalidReason.OUTCOME_EVENT_MISMATCH
                } else {
                    null
                }
            }
            CycleTraceEvent.LANDING_SUPPRESSED -> {
                if (
                    result.event != BounceEvent.LANDING ||
                    result.countedJump
                ) {
                    T730InvalidReason.OUTCOME_EVENT_MISMATCH
                } else {
                    null
                }
            }
            CycleTraceEvent.REJECTED_TAKEOFF -> {
                if (
                    result.event != BounceEvent.NONE ||
                    result.countedJump
                ) {
                    T730InvalidReason.OUTCOME_EVENT_MISMATCH
                } else {
                    null
                }
            }
            null -> {
                if (result.countedJump) {
                    T730InvalidReason.MISSING_ACCEPTED_PEAK
                } else {
                    null
                }
            }
        }
    }

    private fun createTraceEvent(
        eventId: Int,
        peak: TakeoffPeakEvidence,
        rejectedAttribution: T730RejectedPeakAttribution?,
        hasRejectedEvidence: Boolean,
        cycleTrace: CycleTraceEvidence?,
        timestampMillis: Long?,
    ): T730PeakTraceEvent? {
        val emittedAtElapsedMillis = elapsedSinceMeasurement(timestampMillis)
        if (measurementStartedAtMillis != null && emittedAtElapsedMillis == null) {
            markInvalid(T730InvalidReason.TIMESTAMP_MISSING, timestampMillis)
            return null
        }

        return when (peak.outcome) {
            TakeoffPeakOutcome.REJECTED -> {
                if (
                    pendingAcceptedTakeoff != null ||
                    (
                        measurementStartedAtMillis != null &&
                            hasRejectedEvidence &&
                            cycleTrace == null
                    ) ||
                    (
                        cycleTrace != null &&
                            cycleTrace.event != CycleTraceEvent.REJECTED_TAKEOFF
                    )
                ) {
                    markInvalid(T730InvalidReason.OUTCOME_EVENT_MISMATCH, timestampMillis)
                    return null
                }
                val peakAtElapsedMillis = peak.nextFrameIntervalMillis?.let { afterPeak ->
                    emittedAtElapsedMillis?.minus(afterPeak)
                }
                T730PeakTraceEvent(
                    eventId = eventId,
                    region = T730TraceRegion.LEAD,
                    outcome = peak.outcome,
                    emittedAtElapsedMillis = emittedAtElapsedMillis,
                    spanStartedAtElapsedMillis =
                        peakAtElapsedMillis?.minus(peak.riseMillis),
                    peakAtElapsedMillis = peakAtElapsedMillis,
                    spanEndedAtElapsedMillis = emittedAtElapsedMillis,
                    takeoffCycleSequence = null,
                    completionCycleSequence = cycleTrace?.sequence,
                    peakEvidence = peak,
                    rejectedAttribution = rejectedAttribution,
                    cycleSeparationEvidence = null,
                )
            }
            TakeoffPeakOutcome.COUNTED,
            TakeoffPeakOutcome.SUPPRESSED,
            -> {
                if (measurementStartedAtMillis == null) {
                    return T730PeakTraceEvent(
                        eventId = eventId,
                        region = T730TraceRegion.WINDOW,
                        outcome = peak.outcome,
                        emittedAtElapsedMillis = null,
                        spanStartedAtElapsedMillis = null,
                        peakAtElapsedMillis = null,
                        spanEndedAtElapsedMillis = null,
                        takeoffCycleSequence = null,
                        completionCycleSequence = null,
                        peakEvidence = peak,
                        rejectedAttribution = null,
                        cycleSeparationEvidence = null,
                    )
                }
                val expectedCycleEvent = if (peak.outcome == TakeoffPeakOutcome.COUNTED) {
                    CycleTraceEvent.LANDING_COUNTED
                } else {
                    CycleTraceEvent.LANDING_SUPPRESSED
                }
                if (cycleTrace?.event != expectedCycleEvent) {
                    markInvalid(T730InvalidReason.OUTCOME_EVENT_MISMATCH, timestampMillis)
                    return null
                }
                val takeoff = pendingAcceptedTakeoff
                if (takeoff == null) {
                    markInvalid(T730InvalidReason.ORPHAN_ACCEPTED_LANDING, timestampMillis)
                    return null
                }
                val landingTimestamp = timestampMillis
                if (landingTimestamp == null) {
                    markInvalid(T730InvalidReason.TIMESTAMP_MISSING, timestampMillis)
                    return null
                }
                val measuredAirborneMillis =
                    (landingTimestamp - takeoff.timestampMillis).coerceAtLeast(0L)
                val reportedAirborneMillis = cycleTrace.airborneMillis
                if (reportedAirborneMillis != measuredAirborneMillis) {
                    markInvalid(
                        T730InvalidReason.AIRBORNE_INTERVAL_MISMATCH,
                        timestampMillis,
                    )
                    return null
                }
                T730PeakTraceEvent(
                    eventId = eventId,
                    region = T730TraceRegion.WINDOW,
                    outcome = peak.outcome,
                    emittedAtElapsedMillis = emittedAtElapsedMillis,
                    spanStartedAtElapsedMillis =
                        elapsedSinceMeasurement(takeoff.timestampMillis),
                    peakAtElapsedMillis = null,
                    spanEndedAtElapsedMillis = emittedAtElapsedMillis,
                    takeoffCycleSequence = takeoff.cycleSequence,
                    completionCycleSequence = cycleTrace.sequence,
                    peakEvidence = peak,
                    rejectedAttribution = null,
                    cycleSeparationEvidence = T730CycleSeparationEvidence(
                        observedAirborneMillis = measuredAirborneMillis,
                        reportedAirborneMillis = reportedAirborneMillis,
                        airborneFrameSamples = takeoff.airborneFrameSamples,
                        rearmMillisBeforeTakeoff =
                            takeoff.rearmMillisBeforeTakeoff,
                        readyFrameSamplesBeforeTakeoff =
                            takeoff.readyFrameSamplesBeforeTakeoff,
                        takeoffIntervalMillis = takeoff.takeoffIntervalMillis,
                        landingReason = cycleTrace.landingReason,
                        countIntervalMillis = cycleTrace.countIntervalMillis,
                    ),
                )
            }
        }
    }

    private fun elapsedSinceMeasurement(timestampMillis: Long?): Long? {
        val measurementStart = measurementStartedAtMillis ?: return null
        val timestamp = timestampMillis ?: return null
        return timestamp - measurementStart
    }

    private fun invalidate(
        reason: T730InvalidReason,
        timestampMillis: Long?,
    ): T730AttributionSnapshot {
        markInvalid(reason, timestampMillis)
        return snapshot()
    }

    private fun seal(
        reason: T730StopReason,
        timestampMillis: Long?,
    ): T730AttributionSnapshot {
        if (!measurementInvalid && !measurementSealed) {
            measurementActive = false
            measurementSealed = true
            stopReason = reason
            stoppedAtElapsedMillis = elapsedSinceMeasurement(timestampMillis)
        }
        return snapshot()
    }

    private fun markInvalid(
        reason: T730InvalidReason,
        timestampMillis: Long?,
    ) {
        if (measurementInvalid) return
        measurementActive = false
        measurementInvalid = true
        invalidReason = reason
        invalidAtElapsedMillis = elapsedSinceMeasurement(timestampMillis)
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
                gateMargins = null,
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
            gateMargins = T730GateMargins(
                standardAnkleRise =
                    rejected.ankleRiseRatio - rejected.ankleRiseThreshold,
                leftBilateralAnkleRise =
                    peak.rawLeftAnkleRiseRatio -
                        peak.individualAnkleRiseThreshold,
                rightBilateralAnkleRise =
                    peak.rawRightAnkleRiseRatio -
                        peak.individualAnkleRiseThreshold,
                standardHipRise =
                    rejected.hipRiseRatio - rejected.hipRiseThreshold,
                rescueAnkleRise =
                    rejected.ankleRiseRatio -
                        T729DetectorProfiles.BASELINE
                            .strongHipRescueAnkleRiseRatio,
                rescueHipRise =
                    rejected.hipRiseRatio -
                        STRONG_HIP_RESCUE_HIP_RISE_RATIO,
                hipToAnkleProduct =
                    peak.smoothedHipRiseRatio -
                        peak.rawAnkleRiseRatio *
                            rejected.hipToAnkleRiseThreshold,
            ),
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

    private fun snapshot(): T730AttributionSnapshot {
        val classifiedTraceEvents = traceEvents.map { event ->
            event.copy(region = classifyRegion(event))
        }
        val leadEvents = classifiedTraceEvents.filter {
            it.region == T730TraceRegion.LEAD
        }
        val windowEvents = classifiedTraceEvents.filter {
            it.region == T730TraceRegion.WINDOW
        }
        val boundaryEvents = classifiedTraceEvents.filter {
            it.region == T730TraceRegion.BOUNDARY
        }
        val tailEvents = classifiedTraceEvents.filter {
            it.region == T730TraceRegion.TAIL
        }
        val windowBlockingGateCounts = buildMap<T730BlockingGate, Int> {
            windowEvents.forEach { event ->
                event.rejectedAttribution?.blockingGates?.forEach { gate ->
                    put(gate, (get(gate) ?: 0) + 1)
                }
            }
        }
        val window = T730WindowTraceSummary(
            firstAcceptedEventId = firstAcceptedEventId,
            lastAcceptedEventId = lastAcceptedEventId,
            startedAtElapsedMillis = firstAcceptedTakeoffAtElapsedMillis,
            endedAtElapsedMillis = lastAcceptedLandingAtElapsedMillis,
            leadPeakCount = leadEvents.size,
            windowPeakCount = windowEvents.size,
            boundaryPeakCount = boundaryEvents.size,
            tailPeakCount = tailEvents.size,
            windowCountedPeakCount =
                windowEvents.count { it.outcome == TakeoffPeakOutcome.COUNTED },
            windowRejectedPeakCount =
                windowEvents.count { it.outcome == TakeoffPeakOutcome.REJECTED },
            windowSuppressedPeakCount =
                windowEvents.count { it.outcome == TakeoffPeakOutcome.SUPPRESSED },
            windowBlockingGateCounts = windowBlockingGateCounts,
        )
        val windowRaBlockedAttributions = windowEvents.mapNotNull {
            it.rejectedAttribution
        }.filter {
            T730BlockingGate.RESCUE_ANKLE_RISE in it.blockingGates
        }
        val windowRaOnlyAttributions = windowRaBlockedAttributions.filter {
            it.blockingGates == setOf(T730BlockingGate.RESCUE_ANKLE_RISE)
        }
        val raOnlyRecoveryFloors = windowRaOnlyAttributions.map {
            it.smoothedAnkleRiseRatio
        }
        val raCounterfactual = T730RaCounterfactualSummary(
            windowRaBlockedPeakCount = windowRaBlockedAttributions.size,
            windowRaOnlyBlockedPeakCount = windowRaOnlyAttributions.size,
            highestRaOnlyRecoveryFloor = raOnlyRecoveryFloors.maxOrNull(),
            lowestRaOnlyRecoveryFloor = raOnlyRecoveryFloors.minOrNull(),
        )
        val acceptedEvents = classifiedTraceEvents.filter {
            it.cycleSeparationEvidence != null
        }
        val maximumAirborneEvent = acceptedEvents.maxByOrNull {
            requireNotNull(it.cycleSeparationEvidence).observedAirborneMillis
        }
        val eventsWithRearm = acceptedEvents.filter {
            it.cycleSeparationEvidence?.rearmMillisBeforeTakeoff != null
        }
        val maximumRearmEvent = eventsWithRearm.maxByOrNull {
            requireNotNull(
                requireNotNull(it.cycleSeparationEvidence).rearmMillisBeforeTakeoff,
            )
        }
        val eventsWithTakeoffInterval = acceptedEvents.filter {
            it.cycleSeparationEvidence?.takeoffIntervalMillis != null
        }
        val maximumTakeoffIntervalEvent = eventsWithTakeoffInterval.maxByOrNull {
            requireNotNull(
                requireNotNull(it.cycleSeparationEvidence).takeoffIntervalMillis,
            )
        }
        val longestTakeoffIntervalBreakdown =
            maximumTakeoffIntervalEvent?.let { event ->
                val eventIndex = acceptedEvents.indexOf(event)
                val previousEvent = acceptedEvents.getOrNull(eventIndex - 1)
                val separation = event.cycleSeparationEvidence
                val previousSeparation = previousEvent?.cycleSeparationEvidence
                val interval = separation?.takeoffIntervalMillis
                val rearm = separation?.rearmMillisBeforeTakeoff
                val previousAirborne =
                    previousSeparation?.observedAirborneMillis
                if (
                    previousEvent == null ||
                    interval == null ||
                    rearm == null ||
                    previousAirborne == null
                ) {
                    null
                } else {
                    T730TakeoffIntervalBreakdown(
                        eventId = event.eventId,
                        previousEventId = previousEvent.eventId,
                        takeoffIntervalMillis = interval,
                        previousObservedAirborneMillis = previousAirborne,
                        rearmMillisBeforeTakeoff = rearm,
                        readyFrameSamplesBeforeTakeoff =
                            separation.readyFrameSamplesBeforeTakeoff,
                        residualMillis =
                            interval - previousAirborne - rearm,
                    )
                }
            }
        val cycleSeparation = T730CycleSeparationSummary(
            acceptedCycleCount = acceptedEvents.size,
            medianObservedAirborneMillis = acceptedEvents.map {
                requireNotNull(it.cycleSeparationEvidence).observedAirborneMillis
            }.medianMillis(),
            maximumObservedAirborneMillis =
                maximumAirborneEvent?.cycleSeparationEvidence
                    ?.observedAirborneMillis,
            maximumObservedAirborneEventId = maximumAirborneEvent?.eventId,
            medianRearmMillis = eventsWithRearm.map {
                requireNotNull(
                    requireNotNull(it.cycleSeparationEvidence)
                        .rearmMillisBeforeTakeoff,
                )
            }.medianMillis(),
            maximumRearmMillis =
                maximumRearmEvent?.cycleSeparationEvidence
                    ?.rearmMillisBeforeTakeoff,
            maximumRearmEventId = maximumRearmEvent?.eventId,
            medianTakeoffIntervalMillis = eventsWithTakeoffInterval.map {
                requireNotNull(
                    requireNotNull(it.cycleSeparationEvidence)
                        .takeoffIntervalMillis,
                )
            }.medianMillis(),
            maximumTakeoffIntervalMillis =
                maximumTakeoffIntervalEvent?.cycleSeparationEvidence
                    ?.takeoffIntervalMillis,
            maximumTakeoffIntervalEventId =
                maximumTakeoffIntervalEvent?.eventId,
            longestTakeoffIntervalBreakdown =
                longestTakeoffIntervalBreakdown,
        )
        return T730AttributionSnapshot(
            measurementActive = measurementActive,
            measurementSealed = measurementSealed,
            measurementInvalid = measurementInvalid,
            stopReason = stopReason,
            stoppedAtElapsedMillis = stoppedAtElapsedMillis,
            invalidReason = invalidReason,
            invalidAtElapsedMillis = invalidAtElapsedMillis,
            completedPeakCount = completedPeakCount,
            countedPeakCount = countedPeakCount,
            rejectedPeakCount = rejectedPeakCount,
            suppressedPeakCount = suppressedPeakCount,
            blockingGateCounts = blockingGateCounts,
            retainedRejectedPeaks = retainedRejectedPeaks,
            traceEvents = classifiedTraceEvents,
            window = window,
            raCounterfactual = raCounterfactual,
            cycleSeparation = cycleSeparation,
            overflowCount = overflowCount,
        )
    }

    private fun classifyRegion(event: T730PeakTraceEvent): T730TraceRegion {
        if (event.outcome != TakeoffPeakOutcome.REJECTED) {
            return T730TraceRegion.WINDOW
        }
        val windowStart = firstAcceptedTakeoffAtElapsedMillis
            ?: return T730TraceRegion.LEAD
        val windowEnd = lastAcceptedLandingAtElapsedMillis
            ?: return T730TraceRegion.BOUNDARY
        val eventStart = event.spanStartedAtElapsedMillis
            ?: return T730TraceRegion.BOUNDARY
        val eventEnd = event.spanEndedAtElapsedMillis
            ?: return T730TraceRegion.BOUNDARY
        return when {
            eventEnd < windowStart -> T730TraceRegion.LEAD
            eventStart > windowEnd -> T730TraceRegion.TAIL
            eventStart >= windowStart && eventEnd <= windowEnd ->
                T730TraceRegion.WINDOW
            else -> T730TraceRegion.BOUNDARY
        }
    }

    private companion object {
        const val DEFAULT_TRACE_EVENT_HISTORY = 512
        const val DEFAULT_RETAINED_REJECTED_HISTORY = 6
        const val POST_WINDOW_EXIT_GRACE_MILLIS = 2_000L
        const val STRONG_HIP_RESCUE_HIP_RISE_RATIO = 0.100f
    }
}

internal fun formatT730AttributionSnapshot(
    snapshot: T730AttributionSnapshot,
): String = buildString {
    append("T-732 TRACE V16 ")
    append(
        when {
            snapshot.measurementInvalid -> buildString {
                append("INVALID ")
                append(snapshot.invalidReason?.shortName() ?: "UNKNOWN")
                append("@")
                append(formatT730Elapsed(snapshot.invalidAtElapsedMillis))
            }
            snapshot.measurementSealed -> buildString {
                append("SEALED ")
                append(snapshot.stopReason?.shortName() ?: "UNKNOWN")
                append("@")
                append(formatT730Elapsed(snapshot.stoppedAtElapsedMillis))
            }
            snapshot.measurementActive &&
                snapshot.window.firstAcceptedEventId == null -> "WAIT-ANCHOR"
            snapshot.measurementActive -> "WINDOW"
            else -> "INACTIVE"
        },
    )
    append(
        String.format(
            Locale.US,
            "\nALL P%d C%d R%d S%d TR%d OV%d",
            snapshot.completedPeakCount,
            snapshot.countedPeakCount,
            snapshot.rejectedPeakCount,
            snapshot.suppressedPeakCount,
            snapshot.traceEvents.size,
            snapshot.overflowCount,
        ),
    )
    val cycle = snapshot.cycleSeparation
    append(
        String.format(
            Locale.US,
            "\nCYC N%d AIR M%s X%s GAP M%s X%s T2T M%s X%s",
            cycle.acceptedCycleCount,
            cycle.medianObservedAirborneMillis.formatT730Millis(),
            formatT730Maximum(
                cycle.maximumObservedAirborneMillis,
                cycle.maximumObservedAirborneEventId,
            ),
            cycle.medianRearmMillis.formatT730Millis(),
            formatT730Maximum(
                cycle.maximumRearmMillis,
                cycle.maximumRearmEventId,
            ),
            cycle.medianTakeoffIntervalMillis.formatT730Millis(),
            formatT730Maximum(
                cycle.maximumTakeoffIntervalMillis,
                cycle.maximumTakeoffIntervalEventId,
            ),
        ),
    )
    cycle.longestTakeoffIntervalBreakdown?.let { breakdown ->
        append(
            String.format(
                Locale.US,
                "\nLONG #%03d P#%03d T%d=A%d+G%d RF%d E%+d",
                breakdown.eventId,
                breakdown.previousEventId,
                breakdown.takeoffIntervalMillis,
                breakdown.previousObservedAirborneMillis,
                breakdown.rearmMillisBeforeTakeoff,
                breakdown.readyFrameSamplesBeforeTakeoff,
                breakdown.residualMillis,
            ),
        )
    }
    append(
        String.format(
            Locale.US,
            "\nSEG L%d W%d B%d T%d",
            snapshot.window.leadPeakCount,
            snapshot.window.windowPeakCount,
            snapshot.window.boundaryPeakCount,
            snapshot.window.tailPeakCount,
        ),
    )
    val window = snapshot.window
    if (window.firstAcceptedEventId == null) {
        append("\nWIN NONE P0 C0 R0 S0 U0")
    } else {
        append(
            String.format(
                Locale.US,
                "\nWIN %s..%s P%d C%d R%d S%d U%d",
                formatT730Elapsed(window.startedAtElapsedMillis),
                formatT730Elapsed(window.endedAtElapsedMillis),
                window.windowPeakCount,
                window.windowCountedPeakCount,
                window.windowRejectedPeakCount,
                window.windowSuppressedPeakCount,
                window.gateCount(T730BlockingGate.UNATTRIBUTED),
            ),
        )
    }
    append(
        String.format(
            Locale.US,
            "\nG SY%d BL%d BR%d Q%d SH%d RA%d RH%d",
            window.gateCount(T730BlockingGate.FEET_SYNCHRONIZED),
            window.gateCount(T730BlockingGate.LEFT_BILATERAL_ANKLE),
            window.gateCount(T730BlockingGate.RIGHT_BILATERAL_ANKLE),
            window.gateCount(T730BlockingGate.HIP_TO_ANKLE_RATIO),
            window.gateCount(T730BlockingGate.STANDARD_HIP_RISE),
            window.gateCount(T730BlockingGate.RESCUE_ANKLE_RISE),
            window.gateCount(T730BlockingGate.RESCUE_HIP_RISE),
        ),
    )
    val counterfactual = snapshot.raCounterfactual
    append(
        String.format(
            Locale.US,
            "\nCF RA-B%d ONLY%d ONE%s ALL%s",
            counterfactual.windowRaBlockedPeakCount,
            counterfactual.windowRaOnlyBlockedPeakCount,
            counterfactual.highestRaOnlyRecoveryFloor.formatT730Ratio(),
            counterfactual.lowestRaOnlyRecoveryFloor.formatT730Ratio(),
        ),
    )
    append("\nTH SA.0450 B.0100 SH.0600 RA.0200 RH.1000 Q.8500")
    snapshot.traceEvents.takeLast(T730_OVERLAY_TRACE_ROWS).forEach { event ->
        val peak = event.peakEvidence
        val attribution = event.rejectedAttribution
        append(
            String.format(
                Locale.US,
                "\n#%03d %s/%s @%s A%.4f L%.4f R%.4f H%.4f",
                event.eventId,
                event.region.shortName(),
                event.outcome.shortName(),
                event.formatSpan(),
                peak.smoothedAnkleRiseRatio,
                peak.rawLeftAnkleRiseRatio,
                peak.rawRightAnkleRiseRatio,
                peak.smoothedHipRiseRatio,
            ),
        )
        if (attribution != null) {
            append(
                String.format(
                    Locale.US,
                    " %s[%s] Q%s SY%s",
                    attribution.route.shortName(),
                    attribution.blockingGates.joinToString(",") { it.shortName() },
                    attribution.hipToAnkleRiseRatio?.let {
                        String.format(Locale.US, "%.4f", it)
                    } ?: "---",
                    when (attribution.gateEvaluation?.feetSynchronized) {
                        true -> "+"
                        false -> "-"
                        null -> "?"
                    },
                ),
            )
            attribution.gateMargins?.let { margins ->
                append(
                    String.format(
                        Locale.US,
                        " D SA%+.4f BL%+.4f BR%+.4f SH%+.4f " +
                            "RA%+.4f RH%+.4f Q%+.4f",
                        margins.standardAnkleRise,
                        margins.leftBilateralAnkleRise,
                        margins.rightBilateralAnkleRise,
                        margins.standardHipRise,
                        margins.rescueAnkleRise,
                        margins.rescueHipRise,
                        margins.hipToAnkleProduct,
                    ),
                )
            }
            if (T730BlockingGate.RESCUE_ANKLE_RISE in attribution.blockingGates) {
                val otherBlockers = attribution.blockingGates -
                    T730BlockingGate.RESCUE_ANKLE_RISE
                append(
                    String.format(
                        Locale.US,
                        " CF RA<=%.4f OTH[%s]",
                        attribution.smoothedAnkleRiseRatio,
                        if (otherBlockers.isEmpty()) {
                            "-"
                        } else {
                            otherBlockers.joinToString(",") { it.shortName() }
                        },
                    ),
                )
            }
        }
        event.cycleSeparationEvidence?.let { separation ->
            append(
                String.format(
                    Locale.US,
                    " CY A%d/F%d G%s/%d T%s LR%s CI%s",
                    separation.observedAirborneMillis,
                    separation.airborneFrameSamples,
                    separation.rearmMillisBeforeTakeoff.formatT730Millis(),
                    separation.readyFrameSamplesBeforeTakeoff,
                    separation.takeoffIntervalMillis.formatT730Millis(),
                    separation.landingReason.shortName(),
                    separation.countIntervalMillis.formatT730Millis(),
                ),
            )
        }
    }
}

private const val T730_OVERLAY_TRACE_ROWS = 6

private fun T730WindowTraceSummary.gateCount(gate: T730BlockingGate): Int =
    windowBlockingGateCounts[gate] ?: 0

private fun Float?.formatT730Ratio(): String =
    this?.let { String.format(Locale.US, "%.4f", it) } ?: "---"

private fun formatT730Elapsed(elapsedMillis: Long?): String =
    elapsedMillis?.let {
        String.format(Locale.US, "%+.3f", it / 1_000.0)
    } ?: "---"

private fun List<Long>.medianMillis(): Long? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[middle]
    } else {
        (sorted[middle - 1] + sorted[middle]) / 2L
    }
}

private fun Long?.formatT730Millis(): String = this?.toString() ?: "---"

private fun formatT730Maximum(
    millis: Long?,
    eventId: Int?,
): String = if (millis == null || eventId == null) {
    "---"
} else {
    String.format(Locale.US, "%d#%03d", millis, eventId)
}

private fun T730PeakTraceEvent.formatSpan(): String {
    val start = formatT730Elapsed(spanStartedAtElapsedMillis)
    val peak = peakAtElapsedMillis?.let(::formatT730Elapsed)
    val end = formatT730Elapsed(spanEndedAtElapsedMillis)
    return if (peak == null) "$start..$end" else "$start>$peak>$end"
}

private fun T730TraceRegion.shortName(): String = when (this) {
    T730TraceRegion.LEAD -> "L"
    T730TraceRegion.WINDOW -> "W"
    T730TraceRegion.BOUNDARY -> "B"
    T730TraceRegion.TAIL -> "T"
}

private fun TakeoffPeakOutcome.shortName(): String = when (this) {
    TakeoffPeakOutcome.COUNTED -> "C"
    TakeoffPeakOutcome.REJECTED -> "R"
    TakeoffPeakOutcome.SUPPRESSED -> "S"
}

private fun T730InvalidReason.shortName(): String = when (this) {
    T730InvalidReason.FULL_BODY_LOSS -> "FULL-BODY"
    T730InvalidReason.TRACKING_LOSS_DURING_CYCLE -> "TRACK-CYCLE"
    T730InvalidReason.RECALIBRATING -> "RECAL"
    T730InvalidReason.TIMESTAMP_MISSING -> "TIME-MISSING"
    T730InvalidReason.TIMESTAMP_BEFORE_MEASUREMENT -> "TIME-BEFORE"
    T730InvalidReason.TIMESTAMP_REVERSED -> "TIME-REVERSED"
    T730InvalidReason.TIMESTAMP_MISMATCH -> "TIME-MISMATCH"
    T730InvalidReason.CYCLE_SEQUENCE_GAP -> "SEQ-GAP"
    T730InvalidReason.CYCLE_SEQUENCE_COLLISION -> "SEQ-COLLISION"
    T730InvalidReason.OVERLAPPING_TAKEOFF -> "TAKEOFF-OVERLAP"
    T730InvalidReason.ORPHAN_ACCEPTED_LANDING -> "LAND-ORPHAN"
    T730InvalidReason.OUTCOME_EVENT_MISMATCH -> "OUTCOME-MISMATCH"
    T730InvalidReason.AIRBORNE_INTERVAL_MISMATCH -> "AIR-TIME-MISMATCH"
    T730InvalidReason.MISSING_ACCEPTED_PEAK -> "PEAK-MISSING"
    T730InvalidReason.TRACE_OVERFLOW -> "TRACE-OVERFLOW"
}

private fun T730StopReason.shortName(): String = when (this) {
    T730StopReason.POST_WINDOW_FULL_BODY_EXIT -> "POST-EXIT"
}

private fun LandingDetectionReason?.shortName(): String = when (this) {
    LandingDetectionReason.RETURNED_TO_BASELINE -> "R"
    LandingDetectionReason.COMPLETED_VERTICAL_CYCLE -> "C"
    LandingDetectionReason.BOTH -> "B"
    LandingDetectionReason.TIMED_OUT_AFTER_DESCENT -> "T"
    null -> "?"
}

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
