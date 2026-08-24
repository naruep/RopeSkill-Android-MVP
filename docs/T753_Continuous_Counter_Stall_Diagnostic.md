# T-753 Continuous Basic Bounce Counter-Stall Diagnostic

## Objective

Collect bounded, on-device Debug evidence when Basic Bounce stops increasing during a long
continuous session, especially after 100 jumps. This diagnostic does not change production
detection thresholds, state transitions, camera processing, stored sessions, or Release UI.

## Implementation

- Reuse the existing `T743PassiveLandingStateCollector`, enabled only by `BuildConfig.DEBUG`.
- Preserve its most recent bounded snapshot when the user presses `Pause`; previously the
  diagnostic collector was reset before its evidence could be inspected.
- The snapshot contains detector `TAKEOFF/LANDING` totals, whether an `AIRBORNE` interval is
  still open, bounded recent interval/frame evidence, timeout/reset totals, and qualified motion
  pulses seen while production remains `AIRBORNE`.
- No camera image, video, raw pose landmark, personal data, file, database row, or network data
  is retained by this diagnostic.

## Device procedure — Samsung Galaxy S23 Ultra

1. Install the Debug APK and select `Basic Bounce`.
2. Start normally, remain full-body in frame, and perform 120–150 continuous Basic Bounce jumps.
3. Count physical jumps independently in blocks of 25. Note the app count at 100 and at the
   first point it stops increasing.
4. If the counter stalls while jumps continue, do **not** resume immediately. Capture a screen
   recording or screenshot that includes the Debug panel and the visible app counter.
5. Press `Pause`. Capture a second screenshot of the preserved Debug panel, then press Start and
   resume only if desired.
6. Report the physical/app count, whether status changed to `Paused`, and the two images/video.

## Interpretation

- `I...*` with `T/L` unequal means an open `AIRBORNE` interval: inspect the last `B` and `D`
  operands and any `P#` pulse to determine why Landing did not close.
- `T/L` remains equal but the physical count rises: inspect rejected-takeoff/cycle evidence; the
  likely class is a Takeoff gate rejection rather than an airborne lock.
- `Paused` with `FULL_BODY_REQUIRED` indicates the separate tracking-loss auto-pause path.
- A normal timer/FPS with a stalled counter points to detector evidence, not the timer coroutine.

## Acceptance for this diagnostic

- Debug build and unit tests pass.
- The panel remains available after Pause and before Start/Resume resets the next measurement.
- Release behavior is unchanged because the collector and preserved snapshot are Debug-only.
