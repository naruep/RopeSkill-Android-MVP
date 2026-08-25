# T757 Live Diagnostic

## Purpose

Evaluate the existing T757 Landing re-arm candidate with real-time CameraX and on-device
MediaPipe. This is a diagnostic build, not a Production candidate or release approval.

## Isolation

- Package: `com.ropeskill.app.diagnostic.t757live`
- Version: `0.1.0-t757-live`
- Label: `RopeSkill T757 Live`
- `BuildConfig.T757_LIVE_ENABLED` defaults to `false` and is `true` only in this build.
- Production, Release, and ordinary Diagnostic retain T738.
- App data and History are isolated by application ID.

## Detector routing

Real-time Basic Bounce Training uses `T757DetectorProfiles.SHADOW_ONLY`, including T756 takeoff
gates and the guarded Landing re-arm rescue. Threshold values are unchanged. Speed 30 is unchanged.

## Current evidence boundary

T757 reached `150/150` in the original replay and deterministic repeat but did not generalize in
two other videos (`83/179` and `82/150`). Negative controls were skipped. Therefore real-time
results must be treated as exploratory evidence and cannot promote T757 to Production.

## First bounded real-time check

1. Open **RopeSkill T757 Live**, not RopeSkill or RopeSkill Diagnostic.
2. Select Basic Bounce and complete positioning/countdown.
3. Perform a short manually counted round at a comfortable cadence.
4. Stop moving for at least five seconds and confirm the counter does not increase.
5. Finish the workout and confirm Result equals the displayed counter. History is expected only
   inside the isolated T757 Live app.
6. Report actual jumps, app count, any post-stop false counts, and whether preview/tracking froze.

Stop the test if counting continues while standing, the app freezes, tracking remains AIRBORNE, or
the phone becomes unusually hot.
