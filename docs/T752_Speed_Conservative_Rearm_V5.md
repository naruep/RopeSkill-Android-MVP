# T-752 Speed Conservative Phase Re-arm V5

## Objective

Reduce Speed 30 undercount caused by a foot remaining `AIRBORNE` when its exact classification rise
returns close to, but does not cross, the strict `0.03` landing ratio. This is a bounded pilot for the
Samsung Galaxy S23 Ultra, not a change to Basic Bounce.

## Evidence boundary

V4 device evidence recorded `33` actual right-foot landings and `19` app steps. Raw right landings
also ended at `19`, with `RR/OOS/TL = 0/0/0`. The longest right AIR episode was `4,275 ms`, and the
current latched episode reached `0.038`. These observations place the missing events before
`SpeedStepDetector`, inside the per-foot landing re-arm boundary.

## Pilot behavior

- Keep the normal lift ratio at `0.08`.
- Keep the strict landing ratio at `0.03`.
- Keep the simultaneous landing window at `70 ms`.
- While a foot is already `AIRBORNE`, allow conservative re-arm only when its exact average rise is
  at or below `0.04` for two consecutive valid frames.
- A one-frame visit to the recovery band must not emit a landing.
- Conservative re-arm updates that foot's local ground baseline to the confirmed near-ground sample.
- Tracking loss, an out-of-band frame, reset, or Pre-GO commit clears partial recovery evidence.
- The existing alternation, repeated-right, both-feet, unclear, and 30-second gates remain in
  `SpeedStepDetector` and are unchanged.

Debug builds show `SPEED RE-ARM V5` and `REARM L/R`. Release diagnostic visibility remains governed
by the existing Debug boundary.

## Automated acceptance gates

1. Existing strict left/right and simultaneous landing tests pass.
2. Two consecutive near-ground frames after takeoff produce one conservative landing.
3. One near-ground frame followed by an out-of-band frame produces no landing.
4. Near-ground frames while already grounded do not invent takeoff or landing.
5. A strict landing does not increment `REARM` evidence.
6. Conservative recovery of both feet emits `BOTH`, and the right-step counter does not count it.
7. `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` pass on Windows.

## Device protocol

Use Speed 30, normal lighting, the same phone position, and `Record this workout = ON`. Stand still
during Countdown and begin natural alternating steps only after `GO`. Report actual right-foot
landings, app `RIGHT STEPS`, `PHASE`, `REARM L/R`, `Landing L/R/C`, `RR/B/U`, `VIS/OOS/TL`, FPS,
latency, skipped frames, Result, History, video playback, preview stuttering, and crash/freeze.

Run safety controls after the first accuracy round: standing still for 15 seconds, 20 heel raises,
and five isolated knee lifts on each side. The app must add zero right steps in every control.

## Revisit conditions

Stop the pilot and inspect the video before further tuning if V5 creates a false right step in any
safety control, raises `BOTH` unexpectedly, causes a duplicate landing, still leaves a multi-second
AIR latch, or materially reduces FPS/stability. Do not change `BasicBounceDetector` as part of V5.
