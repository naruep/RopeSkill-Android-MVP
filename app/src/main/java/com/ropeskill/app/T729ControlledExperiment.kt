package com.ropeskill.app

import java.util.Locale

data class T729ArmMetrics(
    val jumpCount: Int = 0,
    val takeoffCount: Int = 0,
    val landingCount: Int = 0,
    val cooldownSuppressedCount: Int = 0,
    val strongHipRescueCount: Int = 0,
    val readyForMeasurement: Boolean = false,
)

enum class T729MeasurementState {
    WAITING_FOR_ALL_READY,
    MATCHED,
    INVALID,
}

data class T729ExperimentSnapshot(
    val baseline: T729ArmMetrics,
    val bilateralOnly: T729ArmMetrics,
    val rescueOnly: T729ArmMetrics,
    val measurementState: T729MeasurementState,
    val processedFrames: Long,
    val averageProcessingMicros: Double,
    val maxProcessingMicros: Double,
)

internal fun formatT729ExperimentSnapshot(
    snapshot: T729ExperimentSnapshot,
): String = buildString {
    append("T-729 SHADOW V1 ")
    append(
        when (snapshot.measurementState) {
            T729MeasurementState.WAITING_FOR_ALL_READY -> "WAIT-READY"
            T729MeasurementState.MATCHED -> "MATCHED"
            T729MeasurementState.INVALID -> "INVALID-RESTART"
        },
    )
    appendT729Arm(
        label = "BASE",
        thresholds = T729DetectorProfiles.BASELINE,
        metrics = snapshot.baseline,
    )
    appendT729Arm(
        label = "BIL",
        thresholds = T729DetectorProfiles.BILATERAL_ONLY,
        metrics = snapshot.bilateralOnly,
        deltaFromBaseline =
            snapshot.bilateralOnly.jumpCount - snapshot.baseline.jumpCount,
    )
    appendT729Arm(
        label = "RES",
        thresholds = T729DetectorProfiles.RESCUE_ONLY,
        metrics = snapshot.rescueOnly,
        deltaFromBaseline =
            snapshot.rescueOnly.jumpCount - snapshot.baseline.jumpCount,
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

private fun StringBuilder.appendT729Arm(
    label: String,
    thresholds: BasicBounceDetectorThresholds,
    metrics: T729ArmMetrics,
    deltaFromBaseline: Int? = null,
) {
    append(
        String.format(
            Locale.US,
            "\n%s B%.4f R%.4f J%d A/L%d/%d S%d RES%d",
            label,
            thresholds.minimumIndividualAnkleRiseRatio,
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
 * Runs the production detector and two debug-only shadow profiles against the same pose frame.
 *
 * Only the returned baseline result may drive workout state, the visible counter, or sessions.
 * Shadow metrics stay in memory and exist only to select a candidate for a later active test.
 */
internal class T729ControlledExperimentRunner(
    private val shadowEnabled: Boolean,
    private val nanoTime: () -> Long = { System.nanoTime() },
) {
    private val baselineDetector = BasicBounceDetector(T729DetectorProfiles.BASELINE)
    private val bilateralOnlyDetector = if (shadowEnabled) {
        BasicBounceDetector(T729DetectorProfiles.BILATERAL_ONLY)
    } else {
        null
    }
    private val rescueOnlyDetector = if (shadowEnabled) {
        BasicBounceDetector(T729DetectorProfiles.RESCUE_ONLY)
    } else {
        null
    }

    private val baselineMetrics = ArmAccumulator()
    private val bilateralOnlyMetrics = ArmAccumulator()
    private val rescueOnlyMetrics = ArmAccumulator()

    private var baselineReadyForMeasurement = false
    private var bilateralOnlyReadyForMeasurement = false
    private var rescueOnlyReadyForMeasurement = false
    private var preMeasurementActivityObserved = false
    private var measurementRequested = false
    private var measurementActive = false
    private var measurementInvalid = false
    private var processedFrames = 0L
    private var totalProcessingNanos = 0L
    private var maxProcessingNanos = 0L
    private var publishedSnapshot: T729ExperimentSnapshot? = null

    fun process(frame: PoseFrame, timestampMillis: Long): BounceDetectionResult {
        publishedSnapshot = null
        val startedAtNanos = if (shadowEnabled && measurementActive) nanoTime() else 0L
        val baselineResult = baselineDetector.process(frame, timestampMillis)
        val bilateralOnlyResult = bilateralOnlyDetector?.process(frame, timestampMillis)
        val rescueOnlyResult = rescueOnlyDetector?.process(frame, timestampMillis)

        baselineReadyForMeasurement = baselineResult.isReadyForMatchedMeasurement()
        bilateralOnlyResult?.let {
            bilateralOnlyReadyForMeasurement = it.isReadyForMatchedMeasurement()
        }
        rescueOnlyResult?.let {
            rescueOnlyReadyForMeasurement = it.isReadyForMatchedMeasurement()
        }

        if (shadowEnabled && !measurementRequested) {
            preMeasurementActivityObserved =
                preMeasurementActivityObserved ||
                    baselineResult.hasPreMeasurementActivity() ||
                    requireNotNull(bilateralOnlyResult).hasPreMeasurementActivity() ||
                    requireNotNull(rescueOnlyResult).hasPreMeasurementActivity()
        }

        if (!shadowEnabled || !measurementRequested) {
            return baselineResult
        }

        val requiredBilateralResult = requireNotNull(bilateralOnlyResult)
        val requiredRescueResult = requireNotNull(rescueOnlyResult)
        if (!measurementActive) {
            baselineMetrics.reset(baselineResult.isReadyForMatchedMeasurement())
            bilateralOnlyMetrics.reset(
                requiredBilateralResult.isReadyForMatchedMeasurement(),
            )
            rescueOnlyMetrics.reset(
                requiredRescueResult.isReadyForMatchedMeasurement(),
            )
            val results = listOf(
                baselineResult,
                requiredBilateralResult,
                requiredRescueResult,
            )
            if (results.any(BounceDetectionResult::hasPreMeasurementActivity)) {
                measurementInvalid = true
            }
            if (
                !measurementInvalid &&
                results.all(BounceDetectionResult::isReadyForMatchedMeasurement)
            ) {
                measurementActive = true
            }
            publishedSnapshot = snapshot()
            return baselineResult
        }

        val baselineMetricsChanged = baselineMetrics.record(baselineResult)
        val bilateralMetricsChanged =
            bilateralOnlyMetrics.record(requiredBilateralResult)
        val rescueMetricsChanged =
            rescueOnlyMetrics.record(requiredRescueResult)
        val metricsChanged =
            baselineMetricsChanged || bilateralMetricsChanged || rescueMetricsChanged
        val processingNanos = (nanoTime() - startedAtNanos).coerceAtLeast(0L)
        processedFrames += 1
        totalProcessingNanos += processingNanos
        maxProcessingNanos = maxOf(maxProcessingNanos, processingNanos)
        val shouldPublish = metricsChanged ||
            processedFrames == 1L ||
            processedFrames % SNAPSHOT_INTERVAL_FRAMES == 0L

        if (shouldPublish) publishedSnapshot = snapshot()
        return baselineResult
    }

    fun takePublishedSnapshot(): T729ExperimentSnapshot? =
        publishedSnapshot.also { publishedSnapshot = null }

    fun startMeasurement(): T729ExperimentSnapshot? {
        if (!shadowEnabled) return null
        measurementRequested = true
        measurementInvalid = preMeasurementActivityObserved
        measurementActive =
            !measurementInvalid &&
                baselineReadyForMeasurement &&
                bilateralOnlyReadyForMeasurement &&
                rescueOnlyReadyForMeasurement
        processedFrames = 0L
        totalProcessingNanos = 0L
        maxProcessingNanos = 0L
        baselineMetrics.reset(baselineReadyForMeasurement)
        bilateralOnlyMetrics.reset(
            bilateralOnlyReadyForMeasurement,
        )
        rescueOnlyMetrics.reset(rescueOnlyReadyForMeasurement)
        return snapshot()
    }

    fun reset() {
        baselineDetector.reset()
        bilateralOnlyDetector?.reset()
        rescueOnlyDetector?.reset()
        baselineReadyForMeasurement = false
        bilateralOnlyReadyForMeasurement = false
        rescueOnlyReadyForMeasurement = false
        preMeasurementActivityObserved = false
        measurementRequested = false
        measurementActive = false
        measurementInvalid = false
        processedFrames = 0L
        totalProcessingNanos = 0L
        maxProcessingNanos = 0L
        publishedSnapshot = null
        baselineMetrics.reset(readyForMeasurement = false)
        bilateralOnlyMetrics.reset(readyForMeasurement = false)
        rescueOnlyMetrics.reset(readyForMeasurement = false)
    }

    private fun snapshot(): T729ExperimentSnapshot =
        T729ExperimentSnapshot(
            baseline = baselineMetrics.snapshot(),
            bilateralOnly = bilateralOnlyMetrics.snapshot(),
            rescueOnly = rescueOnlyMetrics.snapshot(),
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
                if (
                    result.trackingStatus == BounceTrackingStatus.READY &&
                    result.event == BounceEvent.NONE
                ) {
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

private fun BounceDetectionResult.isReadyForMatchedMeasurement(): Boolean =
    trackingStatus == BounceTrackingStatus.READY &&
        event == BounceEvent.NONE

private fun BounceDetectionResult.hasPreMeasurementActivity(): Boolean =
    event != BounceEvent.NONE ||
        countedJump ||
        trackingStatus == BounceTrackingStatus.AIRBORNE ||
        cooldownSuppressedEvidence != null
