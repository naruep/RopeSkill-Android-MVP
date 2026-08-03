# T-752 Speed Landing Evidence V2

## Trigger

The first Samsung Galaxy S23 Ultra Speed 30 smoke completed the full integration flow but produced
`0` right steps for `32` actual right-foot landings. Tracking remained ready at about 30 FPS with
no skipped-frame, visibility, out-of-order, crash, or Basic Bounce regression signal. The landing
classifier emitted no left, right, both, or unclear event.

Reference video `RV-01` is 32.0 seconds long and contains an approximately 29.4-second active
sequence. Manual frame/audio review found 109 alternating landings: anatomical right 55 and left
54. The first and last landing are right. This manual count is evidence for comparison, not an
automated offline MediaPipe test.

## Diagnostic hypothesis

The unchanged pilot classifier uses the average normalized Y position of ankle, heel, and foot
index and requires an average rise of `0.08 × hip-to-foot length` before changing from `GROUNDED`
to `AIRBORNE`. In a speed step, the ankle may rise while the toe remains pointed down, causing the
average to stay below the lift threshold.

V2 must measure this hypothesis before any threshold or production counting change.

## Implementation boundary

- Keep `liftRatio = 0.08`, `landingRatio = 0.03`, the 70 ms simultaneous window, alternation gate,
  timer, History behavior, and `BasicBounceDetector.kt` unchanged.
- Collect evidence only when `BuildConfig.DEBUG` is true.
- Retain only the latest values and bounded maximum values; never retain frames, images, landmark
  lists, or an unbounded time series.
- Reset maxima at `GO` without resetting classifier calibration or foot state.
- Show anatomical left/right phase plus current/maximum rise ratios for average, ankle, heel, and
  toe, together with existing landing and performance counters.
- Keep the complete panel hidden in Release builds and outside Speed 30 mode.

## Device test

Run one Speed 30 round on the Samsung Galaxy S23 Ultra and record the full screen. Report:

```text
T-752 Speed Landing Evidence V2
Actual right landings:
App RIGHT STEPS:
Final PHASE L/R:
MAX AVG L/R:
MAX ANK L/R:
MAX HEEL L/R:
MAX TOE L/R:
Landing totals L/R/C/B/U:
VIS/OOS/TL:
FPS/LAT/SKIP:
Crash/freeze:
Basic Bounce regression:
```

Interpretation is deferred until the values are visible. In particular, do not lower a threshold
only because the counter remains zero.
