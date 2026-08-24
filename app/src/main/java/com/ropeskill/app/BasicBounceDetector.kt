package com.ropeskill.app

import kotlin.math.abs

enum class BounceTrackingStatus(val displayName: String) {
    WAITING("Stand fully in frame"),
    CALIBRATING("Hold still to calibrate"),
    READY("Ready to detect"),
    AIRBORNE("Jump detected"),
}

enum class BounceEvent {
    NONE,
    TAKEOFF,
    LANDING,
}

enum class CycleTraceEvent {
    TAKEOFF,
    LANDING_COUNTED,
    LANDING_SUPPRESSED,
    REJECTED_TAKEOFF,
}

enum class TakeoffPeakOutcome {
    COUNTED,
    SUPPRESSED,
    REJECTED,
}

enum class LandingDetectionReason {
    RETURNED_TO_BASELINE,
    COMPLETED_VERTICAL_CYCLE,
    BOTH,
    TIMED_OUT_AFTER_DESCENT,
}

enum class BounceDiagnostic(val displayName: String) {
    FULL_BODY_REQUIRED("Full body required"),
    CALIBRATING("Calibrating"),
    READY("Ready"),
    FEET_NOT_SYNCHRONIZED("Feet not synchronized"),
    ANKLE_RISE_TOO_SMALL("Ankle rise too small"),
    HIP_RISE_TOO_SMALL("Hip rise too small"),
    AIRBORNE("Airborne"),
    LANDED("Landed"),
}

data class BounceDetectionResult(
    val countedJump: Boolean,
    val trackingStatus: BounceTrackingStatus,
    val event: BounceEvent = BounceEvent.NONE,
    val diagnostic: BounceDiagnostic,
    val lastCountEvidence: CountEvidence? = null,
    val rejectedTakeoffEvidence: RejectedTakeoffEvidence? = null,
    val cooldownSuppressedEvidence: CooldownSuppressedEvidence? = null,
    val cycleTraceEvidence: CycleTraceEvidence? = null,
    val takeoffPeakEvidence: TakeoffPeakEvidence? = null,
    val landingStateEvidence: LandingStateEvidence? = null,
)

data class LandingStateEvidence(
    val ankleFromBaselineRatio: Float,
    val hipFromBaselineRatio: Float,
    val ankleBaselineLimitRatio: Float,
    val hipBaselineLimitRatio: Float,
    val ankleReturnedToBaseline: Boolean,
    val hipReturnedToBaseline: Boolean,
    val returnedToBaseline: Boolean,
    val ankleDescentFromPeakRatio: Float,
    val hipDescentFromPeakRatio: Float,
    val ankleDescentLimitRatio: Float,
    val hipDescentLimitRatio: Float,
    val ankleDescendedFromPeak: Boolean,
    val hipDescendedFromPeak: Boolean,
    val descendedFromPeak: Boolean,
    val ankleStartedNextRise: Boolean,
    val hipStartedNextRise: Boolean,
    val startedNextRise: Boolean,
    val completedVerticalCycle: Boolean,
    val airborneMillis: Long,
    val airborneTooLong: Boolean,
    val recoveredLandingAfterTimeout: Boolean,
    val landingRearmRescueApplied: Boolean = false,
)

data class CountEvidence(
    val leftAnkleRiseRatio: Float,
    val rightAnkleRiseRatio: Float,
    val hipRiseRatio: Float,
    val ankleDifferenceRatio: Float,
    val ankleDifference: Float,
    val ankleDifferenceLimit: Float,
    val feetSynchronized: Boolean,
    val airborneMillis: Long,
    val footContactEvidence: FootContactEvidence? = null,
    val usedStrongHipRescue: Boolean = false,
)

data class FootContactEvidence(
    val leftHeelRiseRatio: Float,
    val rightHeelRiseRatio: Float,
    val leftToeRiseRatio: Float,
    val rightToeRiseRatio: Float,
)

data class RejectedTakeoffEvidence(
    val ankleRiseRatio: Float,
    val hipRiseRatio: Float,
    val hipToAnkleRiseRatio: Float,
    val ankleRiseThreshold: Float,
    val hipRiseThreshold: Float,
    val hipToAnkleRiseThreshold: Float,
    val feetSynchronized: Boolean,
    val diagnostic: BounceDiagnostic,
    val footContactEvidence: FootContactEvidence? = null,
)

data class CooldownSuppressedEvidence(
    val intervalMillis: Long,
    val cooldownMillis: Long,
)

data class CycleTraceEvidence(
    val sequence: Int,
    val timestampMillis: Long,
    val elapsedMillis: Long,
    val intervalMillis: Long?,
    val event: CycleTraceEvent,
    val ankleRiseRatio: Float?,
    val hipRiseRatio: Float?,
    val diagnostic: BounceDiagnostic,
    val landingReason: LandingDetectionReason? = null,
    val countIntervalMillis: Long? = null,
    val airborneMillis: Long? = null,
    val usedStrongHipRescue: Boolean = false,
)

data class TakeoffPeakEvidence(
    val outcome: TakeoffPeakOutcome,
    val smoothedAnkleRiseRatio: Float,
    val rawAnkleRiseRatio: Float,
    val rawLeftAnkleRiseRatio: Float,
    val rawRightAnkleRiseRatio: Float,
    val individualAnkleRiseThreshold: Float,
    val leftIndividualAnkleGatePassed: Boolean,
    val rightIndividualAnkleGatePassed: Boolean,
    val smoothedHipRiseRatio: Float,
    val rawHipRiseRatio: Float,
    val riseFrameCount: Int,
    val riseMillis: Long,
    val peakFrameIntervalMillis: Long?,
    val nextFrameIntervalMillis: Long?,
    val diagnostic: BounceDiagnostic,
)

internal data class BasicBounceDetectorThresholds(
    val minimumIndividualAnkleRiseRatio: Float = 0.010f,
    val strongHipRescueAnkleRiseRatio: Float = 0.020f,
    val asymmetricAnkleRescueEnabled: Boolean = false,
    val asymmetricAnkleRescueStrongRiseRatio: Float = 0.060f,
    val asymmetricAnkleRescueWeakRiseRatio: Float = 0.004f,
    val asymmetricAnkleRescueHipRiseRatio: Float = 0.120f,
    val landingRearmRescueEnabled: Boolean = false,
    val landingRearmRescueAnkleBaselineRatio: Float = 0.047f,
    val landingRearmRescueMinimumAirborneMillis: Long = 300L,
) {
    init {
        require(minimumIndividualAnkleRiseRatio in 0f..1f)
        require(strongHipRescueAnkleRiseRatio in 0f..1f)
        require(asymmetricAnkleRescueStrongRiseRatio in 0f..1f)
        require(asymmetricAnkleRescueWeakRiseRatio in 0f..1f)
        require(asymmetricAnkleRescueHipRiseRatio in 0f..1f)
        require(landingRearmRescueAnkleBaselineRatio in 0f..1f)
        require(landingRearmRescueMinimumAirborneMillis >= 0L)
        require(
            asymmetricAnkleRescueWeakRiseRatio <=
                asymmetricAnkleRescueStrongRiseRatio,
        )
    }
}

internal object T729DetectorProfiles {
    val BASELINE = BasicBounceDetectorThresholds()
    val BILATERAL_ONLY = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.006f,
        strongHipRescueAnkleRiseRatio = 0.020f,
    )
    val RESCUE_ONLY = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.010f,
        strongHipRescueAnkleRiseRatio = 0.018f,
    )
}

internal object T736DetectorProfiles {
    val PRODUCTION = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.008f,
        strongHipRescueAnkleRiseRatio = 0.016f,
    )
}

internal object T738DetectorProfiles {
    val PRODUCTION = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.008f,
        strongHipRescueAnkleRiseRatio = 0.016f,
        asymmetricAnkleRescueEnabled = true,
    )
}

/**
 * Offline-only candidate for checking whether a bounded bilateral ankle gate would recover
 * video-diagnostic misses. It must not be used by Training until real-device controls pass.
 */
internal object T756DetectorProfiles {
    const val SHADOW_PROFILE_NAME = "T756_BILATERAL_HIP_RESCUE_SHADOW"

    val SHADOW_ONLY = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.006f,
        strongHipRescueAnkleRiseRatio = 0.012f,
    )
}

/** Offline-only Landing re-arm candidate; it must never be used by Training. */
internal object T757DetectorProfiles {
    const val SHADOW_PROFILE_NAME = "T757_LANDING_REARM_RESCUE_SHADOW"

    val SHADOW_ONLY = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.006f,
        strongHipRescueAnkleRiseRatio = 0.012f,
        landingRearmRescueEnabled = true,
        landingRearmRescueAnkleBaselineRatio = 0.047f,
        landingRearmRescueMinimumAirborneMillis = 300L,
    )
}

internal object BasicBounceTakeoffGateConstants {
    const val STANDARD_ANKLE_RISE_RATIO = 0.045f
    const val STRONG_HIP_RESCUE_HIP_RISE_RATIO = 0.100f
    const val MIN_HIP_TO_ANKLE_RISE_RATIO = 0.85f
}

/**
 * Detects a small two-foot bounce from normalized MediaPipe landmarks.
 *
 * This first MVP baseline intentionally uses a simple state machine. Thresholds must be tuned from
 * real-device accuracy tests before they are treated as final.
 */
class BasicBounceDetector internal constructor(
    private val thresholds: BasicBounceDetectorThresholds = T738DetectorProfiles.PRODUCTION,
    private val landingStateEvidenceEnabled: Boolean = false,
) {
    private var phase = Phase.WAITING
    private var validCalibrationFrames = 0
    private var baselineAnkleY = 0f
    private var baselineAnkleDifference = 0f
    private var baselineHipY = 0f
    private var baselineFoot: FootMeasurement? = null
    private var validFootCalibrationFrames = 0
    private var smoothedAnkleY: Float? = null
    private var smoothedHipY: Float? = null
    private var lastCountedAtMillis = Long.MIN_VALUE
    private var missingFrameCount = 0
    private var lastTrackingStatus = BounceTrackingStatus.WAITING
    private var pendingTakeoffEvidence: TakeoffEvidence? = null
    private var airbornePeakAnkleY: Float? = null
    private var airbornePeakHipY: Float? = null
    private var airborneLowestAnkleY: Float? = null
    private var airborneLowestHipY: Float? = null
    private var airborneLowestFoot: FootMeasurement? = null
    private var previousAirborneAnkleY: Float? = null
    private var previousAirborneHipY: Float? = null
    private var lastCountEvidence: CountEvidence? = null
    private var previousRejectedObservationAnkleY: Float? = null
    private var previousRejectedObservationHipY: Float? = null
    private var rejectedObservationIsRising = false
    private var bestRejectedTakeoffAnkleRiseRatio: Float? = null
    private var bestRejectedTakeoffHipRiseRatio = 0f
    private var bestRejectedTakeoffHipToAnkleRiseRatio = 0f
    private var bestRejectedTakeoffFeetSynchronized = false
    private var bestRejectedTakeoffDiagnostic = BounceDiagnostic.READY
    private var bestRejectedTakeoffFootContactEvidence: FootContactEvidence? = null
    private var cycleTraceSequence = 0
    private var cycleTraceStartedAtMillis: Long? = null
    private var lastCycleTraceAtMillis: Long? = null
    private var hasPreviousTakeoffPeakFrame = false
    private var previousTakeoffPeakAnkleY = 0f
    private var previousTakeoffPeakHipY = 0f
    private var previousTakeoffPeakTimestampMillis = 0L
    private var takeoffPeakObservationActive = false
    private var takeoffPeakObservationStartedAtMillis = 0L
    private var takeoffPeakRiseFrameCount = 0
    private var takeoffPeakTimestampMillis = 0L
    private var hasTakeoffPeakFrameInterval = false
    private var takeoffPeakFrameIntervalMillis = 0L
    private var takeoffPeakSmoothedAnkleY = 0f
    private var takeoffPeakSmoothedAnkleRiseRatio = 0f
    private var takeoffPeakRawAnkleRiseRatio = 0f
    private var takeoffPeakRawLeftAnkleRiseRatio = 0f
    private var takeoffPeakRawRightAnkleRiseRatio = 0f
    private var takeoffPeakLeftIndividualAnkleGatePassed = false
    private var takeoffPeakRightIndividualAnkleGatePassed = false
    private var takeoffPeakSmoothedHipRiseRatio = 0f
    private var takeoffPeakRawHipRiseRatio = 0f
    private var takeoffPeakDiagnostic = BounceDiagnostic.READY
    private var takeoffPeakAccepted = false
    private var pendingAcceptedTakeoffPeak: CompletedTakeoffPeak? = null
    private var takeoffPeakLockedUntilLanding = false

    fun process(frame: PoseFrame, timestampMillis: Long): BounceDetectionResult {
        val measurement = measurement(frame) ?: run {
            missingFrameCount += 1
            if (missingFrameCount > MAX_MISSING_FRAME_COUNT) {
                resetTracking()
                return BounceDetectionResult(
                    countedJump = false,
                    trackingStatus = BounceTrackingStatus.WAITING,
                    diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
                )
            }
            return BounceDetectionResult(
                countedJump = false,
                trackingStatus = lastTrackingStatus,
                diagnostic = BounceDiagnostic.FULL_BODY_REQUIRED,
            )
        }
        missingFrameCount = 0

        val ankleY = smoothAnkle(measurement.ankleY)
        val hipY = smoothHip(measurement.hipY)
        val takeoffDistance =
            measurement.legLength * BasicBounceTakeoffGateConstants.STANDARD_ANKLE_RISE_RATIO
        val hipTakeoffDistance = measurement.legLength * HIP_TAKEOFF_LEG_RATIO
        val landingDistance = measurement.legLength * LANDING_LEG_RATIO

        return when (phase) {
            Phase.WAITING, Phase.CALIBRATING -> calibrate(
                ankleY = ankleY,
                hipY = hipY,
                ankleDifference = measurement.leftAnkleY - measurement.rightAnkleY,
                legLength = measurement.legLength,
                foot = measurement.foot,
            )
            Phase.GROUNDED -> {
                baselineAnkleY += BASELINE_ADAPTATION * (ankleY - baselineAnkleY)
                baselineHipY += BASELINE_ADAPTATION * (hipY - baselineHipY)
                adaptFootBaseline(measurement.foot)
                val ankleDifference = abs(
                    (measurement.leftAnkleY - measurement.rightAnkleY) -
                        baselineAnkleDifference,
                )
                val ankleDifferenceLimit =
                    measurement.legLength * MAX_ANKLE_HEIGHT_DIFFERENCE_RATIO
                val bothFeetRiseTogether = ankleDifference <= ankleDifferenceLimit
                val anklesRise = baselineAnkleY - ankleY >= takeoffDistance
                val baselineLeftAnkleY =
                    baselineAnkleY + baselineAnkleDifference / 2f
                val baselineRightAnkleY =
                    baselineAnkleY - baselineAnkleDifference / 2f
                val leftAnkleRise = baselineLeftAnkleY - measurement.leftAnkleY
                val rightAnkleRise = baselineRightAnkleY - measurement.rightAnkleY
                val individualAnkleRiseDistance =
                    measurement.legLength * thresholds.minimumIndividualAnkleRiseRatio
                val leftIndividualAnkleGatePassed =
                    leftAnkleRise >= individualAnkleRiseDistance
                val rightIndividualAnkleGatePassed =
                    rightAnkleRise >= individualAnkleRiseDistance
                val bothAnklesRise =
                    leftIndividualAnkleGatePassed &&
                        rightIndividualAnkleGatePassed
                val rawLeftAnkleRiseRatio = leftAnkleRise / measurement.legLength
                val rawRightAnkleRiseRatio = rightAnkleRise / measurement.legLength
                val hipsRise = baselineHipY - hipY >= hipTakeoffDistance
                val averageAnkleRise =
                    baselineAnkleY - (measurement.leftAnkleY + measurement.rightAnkleY) / 2f
                val hipRise = baselineHipY - hipY
                val smoothedAnkleRiseRatio =
                    (baselineAnkleY - ankleY) / measurement.legLength
                val hipRiseRatio = hipRise / measurement.legLength
                val rawAnkleRiseRatio = averageAnkleRise / measurement.legLength
                val rawHipRiseRatio =
                    (baselineHipY - measurement.hipY) / measurement.legLength
                val hipsRiseWithAnkles =
                    hipRise >=
                        averageAnkleRise * BasicBounceTakeoffGateConstants.MIN_HIP_TO_ANKLE_RISE_RATIO
                val standardTakeoff =
                    anklesRise &&
                        bothAnklesRise &&
                        hipsRise &&
                        hipsRiseWithAnkles
                val strongHipRescue =
                    !anklesRise &&
                        bothAnklesRise &&
                        smoothedAnkleRiseRatio >=
                        thresholds.strongHipRescueAnkleRiseRatio &&
                        hipRiseRatio >=
                        BasicBounceTakeoffGateConstants.STRONG_HIP_RESCUE_HIP_RISE_RATIO &&
                        hipsRiseWithAnkles
                val strongerRawAnkleRiseRatio =
                    maxOf(rawLeftAnkleRiseRatio, rawRightAnkleRiseRatio)
                val weakerRawAnkleRiseRatio =
                    minOf(rawLeftAnkleRiseRatio, rawRightAnkleRiseRatio)
                val asymmetricAnkleRescue =
                    thresholds.asymmetricAnkleRescueEnabled &&
                        !anklesRise &&
                        (leftIndividualAnkleGatePassed xor
                            rightIndividualAnkleGatePassed) &&
                        smoothedAnkleRiseRatio >=
                        thresholds.strongHipRescueAnkleRiseRatio &&
                        strongerRawAnkleRiseRatio >=
                        thresholds.asymmetricAnkleRescueStrongRiseRatio &&
                        weakerRawAnkleRiseRatio >=
                        thresholds.asymmetricAnkleRescueWeakRiseRatio &&
                        hipRiseRatio >=
                        thresholds.asymmetricAnkleRescueHipRiseRatio &&
                        hipsRiseWithAnkles
                val usedHipQualifiedRescue =
                    strongHipRescue || asymmetricAnkleRescue
                val takeoffDiagnostic = when {
                    !bothFeetRiseTogether -> BounceDiagnostic.FEET_NOT_SYNCHRONIZED
                    !anklesRise || !bothAnklesRise -> BounceDiagnostic.ANKLE_RISE_TOO_SMALL
                    !hipsRise || !hipsRiseWithAnkles ->
                        BounceDiagnostic.HIP_RISE_TOO_SMALL
                    else -> BounceDiagnostic.READY
                }
                val completedTakeoffPeak = observeTakeoffPeak(
                    timestampMillis = timestampMillis,
                    smoothedAnkleY = ankleY,
                    smoothedHipY = hipY,
                    smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
                    rawAnkleRiseRatio = rawAnkleRiseRatio,
                    rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                    rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                    leftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed,
                    rightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed,
                    smoothedHipRiseRatio = hipRiseRatio,
                    rawHipRiseRatio = rawHipRiseRatio,
                    diagnostic = takeoffDiagnostic,
                )
                if (
                    bothFeetRiseTogether &&
                    (standardTakeoff || usedHipQualifiedRescue)
                ) {
                    resetRejectedTakeoffObservation()
                    markTakeoffPeakAccepted(
                        timestampMillis = timestampMillis,
                        smoothedAnkleY = ankleY,
                        smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
                        rawAnkleRiseRatio = rawAnkleRiseRatio,
                        rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                        rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                        leftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed,
                        rightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed,
                        smoothedHipRiseRatio = hipRiseRatio,
                        rawHipRiseRatio = rawHipRiseRatio,
                        diagnostic = takeoffDiagnostic,
                    )
                    phase = Phase.AIRBORNE
                    airbornePeakAnkleY = ankleY
                    airbornePeakHipY = hipY
                    airborneLowestAnkleY = ankleY
                    airborneLowestHipY = hipY
                    airborneLowestFoot = measurement.foot
                    previousAirborneAnkleY = ankleY
                    previousAirborneHipY = hipY
                    val takeoffEvidence = TakeoffEvidence(
                        leftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                        rightAnkleRiseRatio = rawRightAnkleRiseRatio,
                        ankleRiseRatio = smoothedAnkleRiseRatio,
                        hipRiseRatio = hipRiseRatio,
                        ankleDifferenceRatio = ankleDifference / measurement.legLength,
                        ankleDifference = ankleDifference,
                        ankleDifferenceLimit = ankleDifferenceLimit,
                        feetSynchronized = bothFeetRiseTogether,
                        takeoffTimestampMillis = timestampMillis,
                        footContactEvidence = footContactEvidence(
                            foot = measurement.foot,
                            legLength = measurement.legLength,
                        ),
                        usedStrongHipRescue = usedHipQualifiedRescue,
                    )
                    pendingTakeoffEvidence = takeoffEvidence
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.AIRBORNE,
                        event = BounceEvent.TAKEOFF,
                        diagnostic = BounceDiagnostic.AIRBORNE,
                        cycleTraceEvidence = cycleTrace(
                            timestampMillis = timestampMillis,
                            event = CycleTraceEvent.TAKEOFF,
                            ankleRiseRatio = takeoffEvidence.ankleRiseRatio,
                            hipRiseRatio = takeoffEvidence.hipRiseRatio,
                            diagnostic = BounceDiagnostic.AIRBORNE,
                            usedStrongHipRescue = takeoffEvidence.usedStrongHipRescue,
                        ),
                    )
                } else {
                    val rejectedTakeoffEvidence = observeRejectedTakeoff(
                        ankleY = ankleY,
                        hipY = hipY,
                        ankleRiseRatio =
                            smoothedAnkleRiseRatio,
                        hipRiseRatio = hipRiseRatio,
                        hipToAnkleRiseRatio = if (averageAnkleRise > 0f) {
                            hipRise / averageAnkleRise
                        } else {
                            0f
                        },
                        feetSynchronized = bothFeetRiseTogether,
                        diagnostic = takeoffDiagnostic,
                        footContactEvidence = footContactEvidence(
                            foot = measurement.foot,
                            legLength = measurement.legLength,
                        ),
                    )
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.READY,
                        diagnostic = takeoffDiagnostic,
                        rejectedTakeoffEvidence = rejectedTakeoffEvidence,
                        cycleTraceEvidence = rejectedTakeoffEvidence?.let { evidence ->
                            cycleTrace(
                                timestampMillis = timestampMillis,
                                event = CycleTraceEvent.REJECTED_TAKEOFF,
                                ankleRiseRatio = evidence.ankleRiseRatio,
                                hipRiseRatio = evidence.hipRiseRatio,
                                diagnostic = evidence.diagnostic,
                            )
                        },
                        takeoffPeakEvidence = completedTakeoffPeak
                            ?.takeUnless { it.accepted }
                            ?.toEvidence(
                                outcome = TakeoffPeakOutcome.REJECTED,
                                individualAnkleRiseThreshold =
                                    thresholds.minimumIndividualAnkleRiseRatio,
                            ),
                    )
                }
            }
            Phase.AIRBORNE -> {
                val baselineLeftAnkleY =
                    baselineAnkleY + baselineAnkleDifference / 2f
                val baselineRightAnkleY =
                    baselineAnkleY - baselineAnkleDifference / 2f
                val rawLeftAnkleRiseRatio =
                    (baselineLeftAnkleY - measurement.leftAnkleY) /
                        measurement.legLength
                val rawRightAnkleRiseRatio =
                    (baselineRightAnkleY - measurement.rightAnkleY) /
                        measurement.legLength
                observeTakeoffPeak(
                    timestampMillis = timestampMillis,
                    smoothedAnkleY = ankleY,
                    smoothedHipY = hipY,
                    smoothedAnkleRiseRatio =
                        (baselineAnkleY - ankleY) / measurement.legLength,
                    rawAnkleRiseRatio =
                        (baselineAnkleY - measurement.ankleY) / measurement.legLength,
                    rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                    rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                    leftIndividualAnkleGatePassed =
                        rawLeftAnkleRiseRatio >=
                            thresholds.minimumIndividualAnkleRiseRatio,
                    rightIndividualAnkleGatePassed =
                        rawRightAnkleRiseRatio >=
                            thresholds.minimumIndividualAnkleRiseRatio,
                    smoothedHipRiseRatio =
                        (baselineHipY - hipY) / measurement.legLength,
                    rawHipRiseRatio =
                        (baselineHipY - measurement.hipY) / measurement.legLength,
                    diagnostic = BounceDiagnostic.AIRBORNE,
                )?.takeIf { it.accepted }?.let {
                    pendingAcceptedTakeoffPeak = it
                }
                airbornePeakAnkleY = minOf(airbornePeakAnkleY ?: ankleY, ankleY)
                airbornePeakHipY = minOf(airbornePeakHipY ?: hipY, hipY)
                airborneLowestAnkleY =
                    maxOf(airborneLowestAnkleY ?: ankleY, ankleY)
                airborneLowestHipY = maxOf(airborneLowestHipY ?: hipY, hipY)
                measurement.foot?.let { foot ->
                    airborneLowestFoot = airborneLowestFoot?.lowerWith(foot) ?: foot
                }
                val returnedToBaseline =
                    abs(baselineAnkleY - ankleY) <= landingDistance &&
                        abs(baselineHipY - hipY) <= hipTakeoffDistance
                val ankleFromBaselineRatio =
                    abs(baselineAnkleY - ankleY) / measurement.legLength
                val hipFromBaselineRatio =
                    abs(baselineHipY - hipY) / measurement.legLength
                val ankleReturnedToBaseline =
                    abs(baselineAnkleY - ankleY) <= landingDistance
                val hipReturnedToBaseline =
                    abs(baselineHipY - hipY) <= hipTakeoffDistance
                val ankleDescentFromPeak =
                    ankleY - (airbornePeakAnkleY ?: ankleY)
                val hipDescentFromPeak =
                    hipY - (airbornePeakHipY ?: hipY)
                val ankleDescendedFromPeak =
                    ankleDescentFromPeak >= landingDistance
                val hipDescendedFromPeak =
                    hipDescentFromPeak >= hipTakeoffDistance
                val descendedFromPeak =
                    ankleY - (airbornePeakAnkleY ?: ankleY) >= landingDistance &&
                        hipY - (airbornePeakHipY ?: hipY) >= hipTakeoffDistance
                val ankleStartedNextRise =
                    ankleY < (previousAirborneAnkleY ?: ankleY)
                val hipStartedNextRise =
                    hipY < (previousAirborneHipY ?: hipY)
                val startedNextRise =
                    ankleY < (previousAirborneAnkleY ?: ankleY) &&
                        hipY < (previousAirborneHipY ?: hipY)
                val completedVerticalCycle = descendedFromPeak && startedNextRise
                val takeoffTimestampMillis = pendingTakeoffEvidence?.takeoffTimestampMillis
                val airborneMillis = takeoffTimestampMillis?.let {
                    (timestampMillis - it).coerceAtLeast(0L)
                } ?: 0L
                val landingRearmRescueApplied =
                    thresholds.landingRearmRescueEnabled &&
                        !returnedToBaseline &&
                        !completedVerticalCycle &&
                        airborneMillis >= thresholds.landingRearmRescueMinimumAirborneMillis &&
                        ankleFromBaselineRatio <=
                        thresholds.landingRearmRescueAnkleBaselineRatio &&
                        hipReturnedToBaseline &&
                        hipDescendedFromPeak &&
                        startedNextRise
                val hasLanded =
                    returnedToBaseline || completedVerticalCycle || landingRearmRescueApplied
                val airborneTooLong = !hasLanded &&
                    takeoffTimestampMillis != null &&
                    timestampMillis - takeoffTimestampMillis >= MAX_AIRBORNE_DURATION_MILLIS
                val recoveredLandingAfterTimeout =
                    airborneTooLong && descendedFromPeak
                val shouldFinalizeLanding =
                    hasLanded || recoveredLandingAfterTimeout
                val landingStateEvidence = if (landingStateEvidenceEnabled) {
                    LandingStateEvidence(
                        ankleFromBaselineRatio =
                            ankleFromBaselineRatio,
                        hipFromBaselineRatio = hipFromBaselineRatio,
                        ankleBaselineLimitRatio = LANDING_LEG_RATIO,
                        hipBaselineLimitRatio = HIP_TAKEOFF_LEG_RATIO,
                        ankleReturnedToBaseline = ankleReturnedToBaseline,
                        hipReturnedToBaseline = hipReturnedToBaseline,
                        returnedToBaseline = returnedToBaseline,
                        ankleDescentFromPeakRatio =
                            ankleDescentFromPeak / measurement.legLength,
                        hipDescentFromPeakRatio =
                            hipDescentFromPeak / measurement.legLength,
                        ankleDescentLimitRatio = LANDING_LEG_RATIO,
                        hipDescentLimitRatio = HIP_TAKEOFF_LEG_RATIO,
                        ankleDescendedFromPeak = ankleDescendedFromPeak,
                        hipDescendedFromPeak = hipDescendedFromPeak,
                        descendedFromPeak = descendedFromPeak,
                        ankleStartedNextRise = ankleStartedNextRise,
                        hipStartedNextRise = hipStartedNextRise,
                        startedNextRise = startedNextRise,
                        completedVerticalCycle = completedVerticalCycle,
                        airborneMillis = airborneMillis,
                        airborneTooLong = airborneTooLong,
                        recoveredLandingAfterTimeout = recoveredLandingAfterTimeout,
                        landingRearmRescueApplied = landingRearmRescueApplied,
                    )
                } else {
                    null
                }
                if (airborneTooLong && !recoveredLandingAfterTimeout) {
                    resetTracking()
                    calibrate(
                        ankleY = measurement.ankleY,
                        hipY = measurement.hipY,
                        ankleDifference = measurement.leftAnkleY - measurement.rightAnkleY,
                        legLength = measurement.legLength,
                        foot = measurement.foot,
                    ).copy(landingStateEvidence = landingStateEvidence)
                } else if (!shouldFinalizeLanding) {
                    previousAirborneAnkleY = ankleY
                    previousAirborneHipY = hipY
                    BounceDetectionResult(
                        countedJump = false,
                        trackingStatus = BounceTrackingStatus.AIRBORNE,
                        diagnostic = BounceDiagnostic.AIRBORNE,
                        landingStateEvidence = landingStateEvidence,
                    )
                } else {
                    phase = Phase.GROUNDED
                    val landingReason = when {
                        recoveredLandingAfterTimeout ->
                            LandingDetectionReason.TIMED_OUT_AFTER_DESCENT
                        landingRearmRescueApplied ->
                            LandingDetectionReason.RETURNED_TO_BASELINE
                        returnedToBaseline && completedVerticalCycle ->
                            LandingDetectionReason.BOTH
                        returnedToBaseline ->
                            LandingDetectionReason.RETURNED_TO_BASELINE
                        else ->
                            LandingDetectionReason.COMPLETED_VERTICAL_CYCLE
                    }
                    if (completedVerticalCycle || recoveredLandingAfterTimeout) {
                        baselineAnkleY = airborneLowestAnkleY ?: ankleY
                        baselineHipY = airborneLowestHipY ?: hipY
                        baselineAnkleDifference =
                            measurement.leftAnkleY - measurement.rightAnkleY
                        airborneLowestFoot?.let { baselineFoot = it }
                    }
                    val countIntervalMillis = if (lastCountedAtMillis == Long.MIN_VALUE) {
                        null
                    } else {
                        (timestampMillis - lastCountedAtMillis).coerceAtLeast(0L)
                    }
                    val outsideCooldown = countIntervalMillis == null ||
                        countIntervalMillis >= COUNT_COOLDOWN_MILLIS
                    val completedTakeoffEvidence = pendingTakeoffEvidence
                    val completedTakeoffPeak =
                        pendingAcceptedTakeoffPeak ?: completeTakeoffPeakObservation()
                    val takeoffPeakEvidence = completedTakeoffPeak?.toEvidence(
                        outcome = if (outsideCooldown) {
                            TakeoffPeakOutcome.COUNTED
                        } else {
                            TakeoffPeakOutcome.SUPPRESSED
                        },
                        individualAnkleRiseThreshold =
                            thresholds.minimumIndividualAnkleRiseRatio,
                    )
                    if (outsideCooldown) {
                        lastCountedAtMillis = timestampMillis
                        completedTakeoffEvidence?.let { takeoff ->
                            lastCountEvidence = CountEvidence(
                                leftAnkleRiseRatio = takeoff.leftAnkleRiseRatio,
                                rightAnkleRiseRatio = takeoff.rightAnkleRiseRatio,
                                hipRiseRatio = takeoff.hipRiseRatio,
                                ankleDifferenceRatio = takeoff.ankleDifferenceRatio,
                                ankleDifference = takeoff.ankleDifference,
                                ankleDifferenceLimit = takeoff.ankleDifferenceLimit,
                                feetSynchronized = takeoff.feetSynchronized,
                                airborneMillis =
                                    (timestampMillis - takeoff.takeoffTimestampMillis)
                                        .coerceAtLeast(0L),
                                footContactEvidence = takeoff.footContactEvidence,
                                usedStrongHipRescue = takeoff.usedStrongHipRescue,
                            )
                        }
                    }
                    val cycleTraceEvidence = cycleTrace(
                        timestampMillis = timestampMillis,
                        event = if (outsideCooldown) {
                            CycleTraceEvent.LANDING_COUNTED
                        } else {
                            CycleTraceEvent.LANDING_SUPPRESSED
                        },
                        ankleRiseRatio = completedTakeoffEvidence?.ankleRiseRatio,
                        hipRiseRatio = completedTakeoffEvidence?.hipRiseRatio,
                        diagnostic = BounceDiagnostic.LANDED,
                        landingReason = landingReason,
                        countIntervalMillis = countIntervalMillis,
                        airborneMillis = completedTakeoffEvidence?.let { takeoff ->
                            (timestampMillis - takeoff.takeoffTimestampMillis).coerceAtLeast(0L)
                        },
                        usedStrongHipRescue =
                            completedTakeoffEvidence?.usedStrongHipRescue == true,
                    )
                    pendingTakeoffEvidence = null
                    airbornePeakAnkleY = null
                    airbornePeakHipY = null
                    airborneLowestAnkleY = null
                    airborneLowestHipY = null
                    airborneLowestFoot = null
                    previousAirborneAnkleY = null
                    previousAirborneHipY = null
                    resetTakeoffPeakAfterLanding()
                    BounceDetectionResult(
                        countedJump = outsideCooldown,
                        trackingStatus = BounceTrackingStatus.READY,
                        event = BounceEvent.LANDING,
                        diagnostic = BounceDiagnostic.LANDED,
                        lastCountEvidence = lastCountEvidence,
                        cooldownSuppressedEvidence = countIntervalMillis
                            ?.takeIf { !outsideCooldown }
                            ?.let {
                                CooldownSuppressedEvidence(
                                    intervalMillis = it,
                                    cooldownMillis = COUNT_COOLDOWN_MILLIS,
                                )
                            },
                        cycleTraceEvidence = cycleTraceEvidence,
                        takeoffPeakEvidence = takeoffPeakEvidence,
                        landingStateEvidence = landingStateEvidence,
                    )
                }
            }
        }.also { lastTrackingStatus = it.trackingStatus }
    }

    fun reset() {
        lastCountedAtMillis = Long.MIN_VALUE
        lastCountEvidence = null
        cycleTraceSequence = 0
        cycleTraceStartedAtMillis = null
        lastCycleTraceAtMillis = null
        resetTracking()
    }

    private fun calibrate(
        ankleY: Float,
        hipY: Float,
        ankleDifference: Float,
        legLength: Float,
        foot: FootMeasurement?,
    ): BounceDetectionResult {
        phase = Phase.CALIBRATING
        val movedTooMuch = validCalibrationFrames > 0 &&
            (abs(ankleY - baselineAnkleY) > legLength * CALIBRATION_MOTION_RATIO ||
                abs(hipY - baselineHipY) > legLength * CALIBRATION_MOTION_RATIO)
        if (movedTooMuch) {
            validCalibrationFrames = 0
            validFootCalibrationFrames = 0
            baselineFoot = null
        }

        baselineAnkleY = if (validCalibrationFrames == 0) {
            ankleY
        } else {
            baselineAnkleY + (ankleY - baselineAnkleY) / (validCalibrationFrames + 1)
        }
        baselineHipY = if (validCalibrationFrames == 0) {
            hipY
        } else {
            baselineHipY + (hipY - baselineHipY) / (validCalibrationFrames + 1)
        }
        baselineAnkleDifference = if (validCalibrationFrames == 0) {
            ankleDifference
        } else {
            baselineAnkleDifference +
                (ankleDifference - baselineAnkleDifference) / (validCalibrationFrames + 1)
        }
        foot?.let(::calibrateFootBaseline)
        validCalibrationFrames += 1
        if (validCalibrationFrames >= CALIBRATION_FRAME_COUNT) {
            phase = Phase.GROUNDED
            return BounceDetectionResult(
                countedJump = false,
                trackingStatus = BounceTrackingStatus.READY,
                diagnostic = BounceDiagnostic.READY,
            )
        }
        return BounceDetectionResult(
            countedJump = false,
            trackingStatus = BounceTrackingStatus.CALIBRATING,
            diagnostic = BounceDiagnostic.CALIBRATING,
        )
    }

    private fun measurement(frame: PoseFrame): Measurement? {
        val leftHip = frame.visiblePoint(LEFT_HIP) ?: return null
        val rightHip = frame.visiblePoint(RIGHT_HIP) ?: return null
        val leftAnkle = frame.visiblePoint(LEFT_ANKLE) ?: return null
        val rightAnkle = frame.visiblePoint(RIGHT_ANKLE) ?: return null
        val foot = footMeasurement(frame)

        val hipY = (leftHip.y + rightHip.y) / 2f
        val ankleY = (leftAnkle.y + rightAnkle.y) / 2f
        val legLength = ankleY - hipY
        if (legLength < MIN_NORMALIZED_LEG_LENGTH) return null
        return Measurement(
            ankleY = ankleY,
            hipY = hipY,
            leftAnkleY = leftAnkle.y,
            rightAnkleY = rightAnkle.y,
            legLength = legLength,
            foot = foot,
        )
    }

    private fun footMeasurement(frame: PoseFrame): FootMeasurement? {
        val leftHeel = frame.visiblePoint(LEFT_HEEL) ?: return null
        val rightHeel = frame.visiblePoint(RIGHT_HEEL) ?: return null
        val leftToe = frame.visiblePoint(LEFT_FOOT_INDEX) ?: return null
        val rightToe = frame.visiblePoint(RIGHT_FOOT_INDEX) ?: return null
        return FootMeasurement(
            leftHeelY = leftHeel.y,
            rightHeelY = rightHeel.y,
            leftToeY = leftToe.y,
            rightToeY = rightToe.y,
        )
    }

    private fun calibrateFootBaseline(foot: FootMeasurement) {
        val sampleCount = validFootCalibrationFrames
        baselineFoot = baselineFoot?.averageWith(foot, sampleCount) ?: foot
        validFootCalibrationFrames += 1
    }

    private fun adaptFootBaseline(foot: FootMeasurement?) {
        if (foot == null) return
        baselineFoot = baselineFoot?.adaptToward(foot) ?: foot
    }

    private fun footContactEvidence(
        foot: FootMeasurement?,
        legLength: Float,
    ): FootContactEvidence? {
        val baseline = baselineFoot ?: return null
        val current = foot ?: return null
        return FootContactEvidence(
            leftHeelRiseRatio = (baseline.leftHeelY - current.leftHeelY) / legLength,
            rightHeelRiseRatio = (baseline.rightHeelY - current.rightHeelY) / legLength,
            leftToeRiseRatio = (baseline.leftToeY - current.leftToeY) / legLength,
            rightToeRiseRatio = (baseline.rightToeY - current.rightToeY) / legLength,
        )
    }

    private fun smoothAnkle(value: Float): Float {
        val previous = smoothedAnkleY
        return if (previous == null) {
            value
        } else {
            previous + SMOOTHING_ALPHA * (value - previous)
        }.also { smoothedAnkleY = it }
    }

    private fun smoothHip(value: Float): Float {
        val previous = smoothedHipY
        return if (previous == null) {
            value
        } else {
            previous + SMOOTHING_ALPHA * (value - previous)
        }.also { smoothedHipY = it }
    }

    private fun observeRejectedTakeoff(
        ankleY: Float,
        hipY: Float,
        ankleRiseRatio: Float,
        hipRiseRatio: Float,
        hipToAnkleRiseRatio: Float,
        feetSynchronized: Boolean,
        diagnostic: BounceDiagnostic,
        footContactEvidence: FootContactEvidence?,
    ): RejectedTakeoffEvidence? {
        val previousAnkleY = previousRejectedObservationAnkleY
        val previousHipY = previousRejectedObservationHipY
        val movingUp = previousAnkleY != null &&
            previousHipY != null &&
            ankleY < previousAnkleY &&
            hipY < previousHipY
        val movingDown = previousAnkleY != null &&
            previousHipY != null &&
            ankleY > previousAnkleY &&
            hipY > previousHipY

        if (movingUp) rejectedObservationIsRising = true
        val currentBestAnkleRiseRatio = bestRejectedTakeoffAnkleRiseRatio
        if (
            rejectedObservationIsRising &&
            (
                currentBestAnkleRiseRatio == null ||
                    ankleRiseRatio >
                    currentBestAnkleRiseRatio
            )
        ) {
            bestRejectedTakeoffAnkleRiseRatio = ankleRiseRatio
            bestRejectedTakeoffHipRiseRatio = hipRiseRatio
            bestRejectedTakeoffHipToAnkleRiseRatio = hipToAnkleRiseRatio
            bestRejectedTakeoffFeetSynchronized = feetSynchronized
            bestRejectedTakeoffDiagnostic = diagnostic
            bestRejectedTakeoffFootContactEvidence = footContactEvidence
        }

        val completedEvidence = if (
            rejectedObservationIsRising &&
            movingDown &&
            bestRejectedTakeoffAnkleRiseRatio != null
        ) {
            RejectedTakeoffEvidence(
                ankleRiseRatio = requireNotNull(bestRejectedTakeoffAnkleRiseRatio),
                hipRiseRatio = bestRejectedTakeoffHipRiseRatio,
                hipToAnkleRiseRatio = bestRejectedTakeoffHipToAnkleRiseRatio,
                ankleRiseThreshold = BasicBounceTakeoffGateConstants.STANDARD_ANKLE_RISE_RATIO,
                hipRiseThreshold = HIP_TAKEOFF_LEG_RATIO,
                hipToAnkleRiseThreshold = BasicBounceTakeoffGateConstants.MIN_HIP_TO_ANKLE_RISE_RATIO,
                feetSynchronized = bestRejectedTakeoffFeetSynchronized,
                diagnostic = bestRejectedTakeoffDiagnostic,
                footContactEvidence = bestRejectedTakeoffFootContactEvidence,
            )
        } else {
            null
        }
        if (completedEvidence != null) {
            rejectedObservationIsRising = false
            bestRejectedTakeoffAnkleRiseRatio = null
            bestRejectedTakeoffFootContactEvidence = null
        }
        previousRejectedObservationAnkleY = ankleY
        previousRejectedObservationHipY = hipY
        return completedEvidence
    }

    private fun resetRejectedTakeoffObservation() {
        previousRejectedObservationAnkleY = null
        previousRejectedObservationHipY = null
        rejectedObservationIsRising = false
        bestRejectedTakeoffAnkleRiseRatio = null
        bestRejectedTakeoffHipRiseRatio = 0f
        bestRejectedTakeoffHipToAnkleRiseRatio = 0f
        bestRejectedTakeoffFeetSynchronized = false
        bestRejectedTakeoffDiagnostic = BounceDiagnostic.READY
        bestRejectedTakeoffFootContactEvidence = null
    }

    private fun observeTakeoffPeak(
        timestampMillis: Long,
        smoothedAnkleY: Float,
        smoothedHipY: Float,
        smoothedAnkleRiseRatio: Float,
        rawAnkleRiseRatio: Float,
        rawLeftAnkleRiseRatio: Float,
        rawRightAnkleRiseRatio: Float,
        leftIndividualAnkleGatePassed: Boolean,
        rightIndividualAnkleGatePassed: Boolean,
        smoothedHipRiseRatio: Float,
        rawHipRiseRatio: Float,
        diagnostic: BounceDiagnostic,
    ): CompletedTakeoffPeak? {
        val hadPreviousFrame = hasPreviousTakeoffPeakFrame
        val previousAnkleY = previousTakeoffPeakAnkleY
        val previousHipY = previousTakeoffPeakHipY
        val previousTimestampMillis = previousTakeoffPeakTimestampMillis
        hasPreviousTakeoffPeakFrame = true
        previousTakeoffPeakAnkleY = smoothedAnkleY
        previousTakeoffPeakHipY = smoothedHipY
        previousTakeoffPeakTimestampMillis = timestampMillis
        if (takeoffPeakLockedUntilLanding || !hadPreviousFrame) {
            return null
        }

        val movingUp =
            smoothedAnkleY < previousAnkleY &&
                smoothedHipY < previousHipY
        val movingDown =
            smoothedAnkleY > previousAnkleY &&
                smoothedHipY > previousHipY

        if (!takeoffPeakObservationActive && movingUp) {
            takeoffPeakObservationActive = true
            takeoffPeakObservationStartedAtMillis = previousTimestampMillis
            takeoffPeakRiseFrameCount = 2
            recordTakeoffPeak(
                timestampMillis = timestampMillis,
                frameIntervalMillis =
                    (timestampMillis - previousTimestampMillis).coerceAtLeast(0L),
                hasFrameInterval = true,
                smoothedAnkleY = smoothedAnkleY,
                smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
                rawAnkleRiseRatio = rawAnkleRiseRatio,
                rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                leftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed,
                rightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed,
                smoothedHipRiseRatio = smoothedHipRiseRatio,
                rawHipRiseRatio = rawHipRiseRatio,
                diagnostic = diagnostic,
            )
        } else if (takeoffPeakObservationActive) {
            if (movingUp) takeoffPeakRiseFrameCount += 1
            if (smoothedAnkleY < takeoffPeakSmoothedAnkleY) {
                recordTakeoffPeak(
                    timestampMillis = timestampMillis,
                    frameIntervalMillis =
                        (timestampMillis - previousTimestampMillis).coerceAtLeast(0L),
                    hasFrameInterval = true,
                    smoothedAnkleY = smoothedAnkleY,
                    smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
                    rawAnkleRiseRatio = rawAnkleRiseRatio,
                    rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                    rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                    leftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed,
                    rightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed,
                    smoothedHipRiseRatio = smoothedHipRiseRatio,
                    rawHipRiseRatio = rawHipRiseRatio,
                    diagnostic = diagnostic,
                )
            }
        }

        if (!takeoffPeakObservationActive || !movingDown) return null
        return completeTakeoffPeakObservation(
            nextFrameIntervalMillis =
                (timestampMillis - takeoffPeakTimestampMillis).coerceAtLeast(0L),
        )?.also {
            takeoffPeakObservationActive = false
            if (it.accepted) takeoffPeakLockedUntilLanding = true
        }
    }

    private fun markTakeoffPeakAccepted(
        timestampMillis: Long,
        smoothedAnkleY: Float,
        smoothedAnkleRiseRatio: Float,
        rawAnkleRiseRatio: Float,
        rawLeftAnkleRiseRatio: Float,
        rawRightAnkleRiseRatio: Float,
        leftIndividualAnkleGatePassed: Boolean,
        rightIndividualAnkleGatePassed: Boolean,
        smoothedHipRiseRatio: Float,
        rawHipRiseRatio: Float,
        diagnostic: BounceDiagnostic,
    ) {
        if (!takeoffPeakObservationActive) {
            takeoffPeakObservationActive = true
            takeoffPeakObservationStartedAtMillis = timestampMillis
            takeoffPeakRiseFrameCount = 1
            recordTakeoffPeak(
                timestampMillis = timestampMillis,
                frameIntervalMillis = 0L,
                hasFrameInterval = false,
                smoothedAnkleY = smoothedAnkleY,
                smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
                rawAnkleRiseRatio = rawAnkleRiseRatio,
                rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                leftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed,
                rightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed,
                smoothedHipRiseRatio = smoothedHipRiseRatio,
                rawHipRiseRatio = rawHipRiseRatio,
                diagnostic = diagnostic,
            )
        }
        takeoffPeakAccepted = true
    }

    private fun recordTakeoffPeak(
        timestampMillis: Long,
        frameIntervalMillis: Long,
        hasFrameInterval: Boolean,
        smoothedAnkleY: Float,
        smoothedAnkleRiseRatio: Float,
        rawAnkleRiseRatio: Float,
        rawLeftAnkleRiseRatio: Float,
        rawRightAnkleRiseRatio: Float,
        leftIndividualAnkleGatePassed: Boolean,
        rightIndividualAnkleGatePassed: Boolean,
        smoothedHipRiseRatio: Float,
        rawHipRiseRatio: Float,
        diagnostic: BounceDiagnostic,
    ) {
        takeoffPeakTimestampMillis = timestampMillis
        takeoffPeakFrameIntervalMillis = frameIntervalMillis
        hasTakeoffPeakFrameInterval = hasFrameInterval
        takeoffPeakSmoothedAnkleY = smoothedAnkleY
        takeoffPeakSmoothedAnkleRiseRatio = smoothedAnkleRiseRatio
        takeoffPeakRawAnkleRiseRatio = rawAnkleRiseRatio
        takeoffPeakRawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio
        takeoffPeakRawRightAnkleRiseRatio = rawRightAnkleRiseRatio
        takeoffPeakLeftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed
        takeoffPeakRightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed
        takeoffPeakSmoothedHipRiseRatio = smoothedHipRiseRatio
        takeoffPeakRawHipRiseRatio = rawHipRiseRatio
        takeoffPeakDiagnostic = diagnostic
    }

    private fun completeTakeoffPeakObservation(
        nextFrameIntervalMillis: Long? = null,
    ): CompletedTakeoffPeak? {
        if (!takeoffPeakObservationActive) return null
        return CompletedTakeoffPeak(
            smoothedAnkleRiseRatio = takeoffPeakSmoothedAnkleRiseRatio,
            rawAnkleRiseRatio = takeoffPeakRawAnkleRiseRatio,
            rawLeftAnkleRiseRatio = takeoffPeakRawLeftAnkleRiseRatio,
            rawRightAnkleRiseRatio = takeoffPeakRawRightAnkleRiseRatio,
            leftIndividualAnkleGatePassed =
                takeoffPeakLeftIndividualAnkleGatePassed,
            rightIndividualAnkleGatePassed =
                takeoffPeakRightIndividualAnkleGatePassed,
            smoothedHipRiseRatio = takeoffPeakSmoothedHipRiseRatio,
            rawHipRiseRatio = takeoffPeakRawHipRiseRatio,
            riseFrameCount = takeoffPeakRiseFrameCount,
            riseMillis =
                (takeoffPeakTimestampMillis - takeoffPeakObservationStartedAtMillis)
                    .coerceAtLeast(0L),
            peakFrameIntervalMillis = if (hasTakeoffPeakFrameInterval) {
                takeoffPeakFrameIntervalMillis
            } else {
                null
            },
            nextFrameIntervalMillis = nextFrameIntervalMillis,
            diagnostic = takeoffPeakDiagnostic,
            accepted = takeoffPeakAccepted,
        )
    }

    private fun resetTakeoffPeakAfterLanding() {
        takeoffPeakObservationActive = false
        takeoffPeakAccepted = false
        pendingAcceptedTakeoffPeak = null
        takeoffPeakLockedUntilLanding = false
    }

    private fun resetTakeoffPeakObservation() {
        hasPreviousTakeoffPeakFrame = false
        takeoffPeakObservationActive = false
        takeoffPeakAccepted = false
        pendingAcceptedTakeoffPeak = null
        takeoffPeakLockedUntilLanding = false
    }

    private fun cycleTrace(
        timestampMillis: Long,
        event: CycleTraceEvent,
        ankleRiseRatio: Float?,
        hipRiseRatio: Float?,
        diagnostic: BounceDiagnostic,
        landingReason: LandingDetectionReason? = null,
        countIntervalMillis: Long? = null,
        airborneMillis: Long? = null,
        usedStrongHipRescue: Boolean = false,
    ): CycleTraceEvidence {
        val startedAtMillis = cycleTraceStartedAtMillis ?: timestampMillis.also {
            cycleTraceStartedAtMillis = it
        }
        val intervalMillis = lastCycleTraceAtMillis?.let {
            (timestampMillis - it).coerceAtLeast(0L)
        }
        cycleTraceSequence += 1
        lastCycleTraceAtMillis = timestampMillis
        return CycleTraceEvidence(
            sequence = cycleTraceSequence,
            timestampMillis = timestampMillis,
            elapsedMillis = (timestampMillis - startedAtMillis).coerceAtLeast(0L),
            intervalMillis = intervalMillis,
            event = event,
            ankleRiseRatio = ankleRiseRatio,
            hipRiseRatio = hipRiseRatio,
            diagnostic = diagnostic,
            landingReason = landingReason,
            countIntervalMillis = countIntervalMillis,
            airborneMillis = airborneMillis,
            usedStrongHipRescue = usedStrongHipRescue,
        )
    }

    private fun resetTracking() {
        phase = Phase.WAITING
        validCalibrationFrames = 0
        baselineAnkleY = 0f
        baselineAnkleDifference = 0f
        baselineHipY = 0f
        baselineFoot = null
        validFootCalibrationFrames = 0
        smoothedAnkleY = null
        smoothedHipY = null
        missingFrameCount = 0
        lastTrackingStatus = BounceTrackingStatus.WAITING
        pendingTakeoffEvidence = null
        airbornePeakAnkleY = null
        airbornePeakHipY = null
        airborneLowestAnkleY = null
        airborneLowestHipY = null
        airborneLowestFoot = null
        previousAirborneAnkleY = null
        previousAirborneHipY = null
        resetRejectedTakeoffObservation()
        resetTakeoffPeakObservation()
    }

    private fun PoseFrame.visiblePoint(index: Int): NormalizedPoint? =
        landmarks.getOrNull(index)?.takeIf { it.isVisible }

    private data class Measurement(
        val ankleY: Float,
        val hipY: Float,
        val leftAnkleY: Float,
        val rightAnkleY: Float,
        val legLength: Float,
        val foot: FootMeasurement?,
    )

    private data class FootMeasurement(
        val leftHeelY: Float,
        val rightHeelY: Float,
        val leftToeY: Float,
        val rightToeY: Float,
    ) {
        fun averageWith(sample: FootMeasurement, existingSampleCount: Int): FootMeasurement {
            val divisor = existingSampleCount + 1f
            return FootMeasurement(
                leftHeelY = leftHeelY + (sample.leftHeelY - leftHeelY) / divisor,
                rightHeelY = rightHeelY + (sample.rightHeelY - rightHeelY) / divisor,
                leftToeY = leftToeY + (sample.leftToeY - leftToeY) / divisor,
                rightToeY = rightToeY + (sample.rightToeY - rightToeY) / divisor,
            )
        }

        fun adaptToward(sample: FootMeasurement): FootMeasurement =
            FootMeasurement(
                leftHeelY = leftHeelY +
                    BASELINE_ADAPTATION * (sample.leftHeelY - leftHeelY),
                rightHeelY = rightHeelY +
                    BASELINE_ADAPTATION * (sample.rightHeelY - rightHeelY),
                leftToeY = leftToeY +
                    BASELINE_ADAPTATION * (sample.leftToeY - leftToeY),
                rightToeY = rightToeY +
                    BASELINE_ADAPTATION * (sample.rightToeY - rightToeY),
            )

        fun lowerWith(sample: FootMeasurement): FootMeasurement =
            FootMeasurement(
                leftHeelY = maxOf(leftHeelY, sample.leftHeelY),
                rightHeelY = maxOf(rightHeelY, sample.rightHeelY),
                leftToeY = maxOf(leftToeY, sample.leftToeY),
                rightToeY = maxOf(rightToeY, sample.rightToeY),
            )
    }

    private data class TakeoffEvidence(
        val leftAnkleRiseRatio: Float,
        val rightAnkleRiseRatio: Float,
        val ankleRiseRatio: Float,
        val hipRiseRatio: Float,
        val ankleDifferenceRatio: Float,
        val ankleDifference: Float,
        val ankleDifferenceLimit: Float,
        val feetSynchronized: Boolean,
        val takeoffTimestampMillis: Long,
        val footContactEvidence: FootContactEvidence?,
        val usedStrongHipRescue: Boolean,
    )

    private data class CompletedTakeoffPeak(
        val smoothedAnkleRiseRatio: Float,
        val rawAnkleRiseRatio: Float,
        val rawLeftAnkleRiseRatio: Float,
        val rawRightAnkleRiseRatio: Float,
        val leftIndividualAnkleGatePassed: Boolean,
        val rightIndividualAnkleGatePassed: Boolean,
        val smoothedHipRiseRatio: Float,
        val rawHipRiseRatio: Float,
        val riseFrameCount: Int,
        val riseMillis: Long,
        val peakFrameIntervalMillis: Long?,
        val nextFrameIntervalMillis: Long?,
        val diagnostic: BounceDiagnostic,
        val accepted: Boolean,
    ) {
        fun toEvidence(
            outcome: TakeoffPeakOutcome,
            individualAnkleRiseThreshold: Float,
        ): TakeoffPeakEvidence =
            TakeoffPeakEvidence(
                outcome = outcome,
                smoothedAnkleRiseRatio = smoothedAnkleRiseRatio,
                rawAnkleRiseRatio = rawAnkleRiseRatio,
                rawLeftAnkleRiseRatio = rawLeftAnkleRiseRatio,
                rawRightAnkleRiseRatio = rawRightAnkleRiseRatio,
                individualAnkleRiseThreshold = individualAnkleRiseThreshold,
                leftIndividualAnkleGatePassed = leftIndividualAnkleGatePassed,
                rightIndividualAnkleGatePassed = rightIndividualAnkleGatePassed,
                smoothedHipRiseRatio = smoothedHipRiseRatio,
                rawHipRiseRatio = rawHipRiseRatio,
                riseFrameCount = riseFrameCount,
                riseMillis = riseMillis,
                peakFrameIntervalMillis = peakFrameIntervalMillis,
                nextFrameIntervalMillis = nextFrameIntervalMillis,
                diagnostic = diagnostic,
            )
    }

    private enum class Phase { WAITING, CALIBRATING, GROUNDED, AIRBORNE }

    private companion object {
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
        const val LEFT_ANKLE = 27
        const val RIGHT_ANKLE = 28
        const val LEFT_HEEL = 29
        const val RIGHT_HEEL = 30
        const val LEFT_FOOT_INDEX = 31
        const val RIGHT_FOOT_INDEX = 32
        const val CALIBRATION_FRAME_COUNT = 45
        const val MIN_NORMALIZED_LEG_LENGTH = 0.12f
        const val HIP_TAKEOFF_LEG_RATIO = 0.060f
        const val LANDING_LEG_RATIO = 0.04f
        const val MAX_ANKLE_HEIGHT_DIFFERENCE_RATIO = 0.08f
        const val CALIBRATION_MOTION_RATIO = 0.025f
        const val SMOOTHING_ALPHA = 0.60f
        const val BASELINE_ADAPTATION = 0.02f
        const val COUNT_COOLDOWN_MILLIS = 250L
        const val MAX_AIRBORNE_DURATION_MILLIS = 1_500L
        const val MAX_MISSING_FRAME_COUNT = 5
    }
}
