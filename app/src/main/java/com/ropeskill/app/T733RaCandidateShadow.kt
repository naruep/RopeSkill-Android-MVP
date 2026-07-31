package com.ropeskill.app

import java.util.Locale

internal object T733RaCandidateProfiles {
    val BASELINE = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.010f,
        strongHipRescueAnkleRiseRatio = 0.020f,
    )
    val RA_016 = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.010f,
        strongHipRescueAnkleRiseRatio = 0.016f,
    )
    val RA_015 = BasicBounceDetectorThresholds(
        minimumIndividualAnkleRiseRatio = 0.010f,
        strongHipRescueAnkleRiseRatio = 0.015f,
    )
}

data class T733RaCandidateSnapshot(
    val baseline: T729ArmMetrics,
    val ra016: T729ArmMetrics,
    val ra015: T729ArmMetrics,
    val measurementState: T729MeasurementState,
    val processedFrames: Long,
    val averageProcessingMicros: Double,
    val maxProcessingMicros: Double,
)

internal fun formatT733RaCandidateSnapshot(
    snapshot: T733RaCandidateSnapshot,
): String = buildString {
    append("T-733 RA SHADOW V17 ")
    append(
        when (snapshot.measurementState) {
            T729MeasurementState.WAITING_FOR_ALL_READY -> "WAIT-READY"
            T729MeasurementState.MATCHED -> "MATCHED"
            T729MeasurementState.INVALID -> "INVALID-RESTART"
        },
    )
    appendArm("BASE", T733RaCandidateProfiles.BASELINE, snapshot.baseline)
    appendArm(
        "RA16",
        T733RaCandidateProfiles.RA_016,
        snapshot.ra016,
        snapshot.ra016.jumpCount - snapshot.baseline.jumpCount,
    )
    appendArm(
        "RA15",
        T733RaCandidateProfiles.RA_015,
        snapshot.ra015,
        snapshot.ra015.jumpCount - snapshot.baseline.jumpCount,
    )
    append(
        String.format(
            Locale.US,
            "\nPROC %.0f/%.0fus F%d",
            snapshot.averageProcessingMicros,
            snapshot.maxProcessingMicros,
            snapshot.processedFrames,
        ),
    )
}

private fun StringBuilder.appendArm(
    label: String,
    thresholds: BasicBounceDetectorThresholds,
    metrics: T729ArmMetrics,
    deltaFromBaseline: Int? = null,
) {
    append(
        String.format(
            Locale.US,
            "\n%s R%.4f J%d A/L%d/%d S%d RES%d",
            label,
            thresholds.strongHipRescueAnkleRiseRatio,
            metrics.jumpCount,
            metrics.takeoffCount,
            metrics.landingCount,
            metrics.cooldownSuppressedCount,
            metrics.strongHipRescueCount,
        ),
    )
    deltaFromBaseline?.let {
        append(String.format(Locale.US, " D%+d", it))
    }
    if (!metrics.readyForMeasurement) append(" WARM")
}

/**
 * Runs production BASE and two RA candidates against identical pose frames.
 *
 * Only the returned BASE result may affect workout state, Counter, Result, History, or storage.
 * Candidate metrics remain Debug-only and in memory.
 */
internal class T733RaCandidateShadowRunner(
    private val shadowEnabled: Boolean,
    productionThresholds: BasicBounceDetectorThresholds = T733RaCandidateProfiles.BASELINE,
    landingStateEvidenceEnabled: Boolean = false,
    private val nanoTime: () -> Long = { System.nanoTime() },
) {
    private val baselineDetector = BasicBounceDetector(
        thresholds = productionThresholds,
        landingStateEvidenceEnabled = landingStateEvidenceEnabled,
    )
    private val ra016Detector = if (shadowEnabled) {
        BasicBounceDetector(T733RaCandidateProfiles.RA_016)
    } else {
        null
    }
    private val ra015Detector = if (shadowEnabled) {
        BasicBounceDetector(T733RaCandidateProfiles.RA_015)
    } else {
        null
    }

    private val baselineMetrics = ArmAccumulator()
    private val ra016Metrics = ArmAccumulator()
    private val ra015Metrics = ArmAccumulator()

    private var baselineReady = false
    private var ra016Ready = false
    private var ra015Ready = false
    private var preMeasurementActivityObserved = false
    private var measurementRequested = false
    private var measurementActive = false
    private var measurementInvalid = false
    private var processedFrames = 0L
    private var totalProcessingNanos = 0L
    private var maxProcessingNanos = 0L
    private var publishedSnapshot: T733RaCandidateSnapshot? = null

    fun process(frame: PoseFrame, timestampMillis: Long): BounceDetectionResult {
        publishedSnapshot = null
        val startedAtNanos = if (shadowEnabled && measurementActive) nanoTime() else 0L
        val baselineResult = baselineDetector.process(frame, timestampMillis)
        val ra016Result = ra016Detector?.process(frame, timestampMillis)
        val ra015Result = ra015Detector?.process(frame, timestampMillis)

        baselineReady = baselineResult.isReadyForT733Measurement()
        ra016Result?.let { ra016Ready = it.isReadyForT733Measurement() }
        ra015Result?.let { ra015Ready = it.isReadyForT733Measurement() }

        if (shadowEnabled && !measurementRequested) {
            preMeasurementActivityObserved =
                preMeasurementActivityObserved ||
                    baselineResult.hasT733PreMeasurementActivity() ||
                    requireNotNull(ra016Result).hasT733PreMeasurementActivity() ||
                    requireNotNull(ra015Result).hasT733PreMeasurementActivity()
        }

        if (!shadowEnabled || !measurementRequested) return baselineResult

        val requiredRa016Result = requireNotNull(ra016Result)
        val requiredRa015Result = requireNotNull(ra015Result)
        if (!measurementActive) {
            baselineMetrics.reset(baselineResult.isReadyForT733Measurement())
            ra016Metrics.reset(requiredRa016Result.isReadyForT733Measurement())
            ra015Metrics.reset(requiredRa015Result.isReadyForT733Measurement())
            if (
                listOf(baselineResult, requiredRa016Result, requiredRa015Result)
                    .any(BounceDetectionResult::hasT733PreMeasurementActivity)
            ) {
                measurementInvalid = true
            }
            if (
                !measurementInvalid &&
                listOf(baselineResult, requiredRa016Result, requiredRa015Result)
                    .all(BounceDetectionResult::isReadyForT733Measurement)
            ) {
                measurementActive = true
            }
            publishedSnapshot = snapshot()
            return baselineResult
        }

        val baselineMetricsChanged = baselineMetrics.record(baselineResult)
        val ra016MetricsChanged = ra016Metrics.record(requiredRa016Result)
        val ra015MetricsChanged = ra015Metrics.record(requiredRa015Result)
        val metricsChanged =
            baselineMetricsChanged || ra016MetricsChanged || ra015MetricsChanged
        val processingNanos = (nanoTime() - startedAtNanos).coerceAtLeast(0L)
        processedFrames += 1
        totalProcessingNanos += processingNanos
        maxProcessingNanos = maxOf(maxProcessingNanos, processingNanos)
        if (
            metricsChanged ||
            processedFrames == 1L ||
            processedFrames % SNAPSHOT_INTERVAL_FRAMES == 0L
        ) {
            publishedSnapshot = snapshot()
        }
        return baselineResult
    }

    fun takePublishedSnapshot(): T733RaCandidateSnapshot? =
        publishedSnapshot.also { publishedSnapshot = null }

    fun startMeasurement(): T733RaCandidateSnapshot? {
        if (!shadowEnabled) return null
        measurementRequested = true
        measurementInvalid = preMeasurementActivityObserved
        measurementActive =
            !measurementInvalid && baselineReady && ra016Ready && ra015Ready
        processedFrames = 0L
        totalProcessingNanos = 0L
        maxProcessingNanos = 0L
        baselineMetrics.reset(baselineReady)
        ra016Metrics.reset(ra016Ready)
        ra015Metrics.reset(ra015Ready)
        return snapshot()
    }

    fun reset() {
        baselineDetector.reset()
        ra016Detector?.reset()
        ra015Detector?.reset()
        baselineReady = false
        ra016Ready = false
        ra015Ready = false
        preMeasurementActivityObserved = false
        measurementRequested = false
        measurementActive = false
        measurementInvalid = false
        processedFrames = 0L
        totalProcessingNanos = 0L
        maxProcessingNanos = 0L
        publishedSnapshot = null
        baselineMetrics.reset(readyForMeasurement = false)
        ra016Metrics.reset(readyForMeasurement = false)
        ra015Metrics.reset(readyForMeasurement = false)
    }

    private fun snapshot(): T733RaCandidateSnapshot =
        T733RaCandidateSnapshot(
            baseline = baselineMetrics.snapshot(),
            ra016 = ra016Metrics.snapshot(),
            ra015 = ra015Metrics.snapshot(),
            measurementState = when {
                measurementInvalid -> T729MeasurementState.INVALID
                measurementActive -> T729MeasurementState.MATCHED
                else -> T729MeasurementState.WAITING_FOR_ALL_READY
            },
            processedFrames = processedFrames,
            averageProcessingMicros = if (processedFrames == 0L) {
                0.0
            } else {
                totalProcessingNanos.toDouble() / processedFrames / NANOS_PER_MICRO
            },
            maxProcessingMicros = maxProcessingNanos / NANOS_PER_MICRO,
        )

    private class ArmAccumulator {
        private var jumpCount = 0
        private var takeoffCount = 0
        private var landingCount = 0
        private var cooldownSuppressedCount = 0
        private var strongHipRescueCount = 0
        private var readyForMeasurement = false

        fun reset(readyForMeasurement: Boolean) {
            jumpCount = 0
            takeoffCount = 0
            landingCount = 0
            cooldownSuppressedCount = 0
            strongHipRescueCount = 0
            this.readyForMeasurement = readyForMeasurement
        }

        fun record(result: BounceDetectionResult): Boolean {
            if (!readyForMeasurement) {
                if (result.isReadyForT733Measurement()) {
                    readyForMeasurement = true
                    return true
                }
                return false
            }

            val hadMetricEvent =
                result.event != BounceEvent.NONE ||
                    result.countedJump ||
                    result.cooldownSuppressedEvidence != null
            if (result.event == BounceEvent.TAKEOFF) takeoffCount += 1
            if (result.event == BounceEvent.LANDING) landingCount += 1
            if (result.countedJump) {
                jumpCount += 1
                if (result.lastCountEvidence?.usedStrongHipRescue == true) {
                    strongHipRescueCount += 1
                }
            }
            if (result.cooldownSuppressedEvidence != null) {
                cooldownSuppressedCount += 1
            }
            return hadMetricEvent
        }

        fun snapshot(): T729ArmMetrics =
            T729ArmMetrics(
                jumpCount = jumpCount,
                takeoffCount = takeoffCount,
                landingCount = landingCount,
                cooldownSuppressedCount = cooldownSuppressedCount,
                strongHipRescueCount = strongHipRescueCount,
                readyForMeasurement = readyForMeasurement,
            )
    }

    private companion object {
        const val SNAPSHOT_INTERVAL_FRAMES = 30L
        const val NANOS_PER_MICRO = 1_000.0
    }
}

private fun BounceDetectionResult.isReadyForT733Measurement(): Boolean =
    trackingStatus == BounceTrackingStatus.READY &&
        event == BounceEvent.NONE

private fun BounceDetectionResult.hasT733PreMeasurementActivity(): Boolean =
    event != BounceEvent.NONE ||
        countedJump ||
        trackingStatus == BounceTrackingStatus.AIRBORNE ||
        cooldownSuppressedEvidence != null
