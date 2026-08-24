# T756 Bilateral Hip Rescue Shadow

## Decision

Add an offline-only `T756_BILATERAL_HIP_RESCUE_SHADOW` evaluation to Basic Bounce Video Diagnostic.

## Reason

The 150-jump video replay remains at 147 counts under production T738. Its missing cycles are predominantly `ANKLE_RISE_TOO_SMALL`, while no airborne interval remains open. T738 asymmetric rescue does not apply to those bilateral misses.

## Scope and safeguards

T756 uses `minimumIndividualAnkleRiseRatio = 0.006` and `strongHipRescueAnkleRiseRatio = 0.012`; it retains the existing hip-rise and feet-synchronization requirements. It runs only beside production T738 during offline video analysis and writes only the aggregate count to the diagnostic UI and CSV. It does not alter Training, live counting, History, video retention, or pose-data retention.

## Revisit condition

Consider a production candidate only if the replay reaches the manual ground truth and dedicated real-device control videos show no false counts, stalls, crashes, or History entries.
