# T-752 Speed 30 Requirements and Detector Design

## Scope

Speed 30 is a separate workout mode from Basic Bounce. It counts valid right-foot landings during
the 30-second interval. It does not modify `BasicBounceDetector`, recognize freestyle skills, detect
double unders, or claim to verify that the rope passed under the athlete's feet.

## Counting contract

- The valid interval is `GO <= landing timestamp < GO + 30,000 ms`.
- The first clear right-foot landing may count.
- A subsequent right-foot landing counts only after a clear left-foot landing.
- Repeated right landings, both-feet landings, unclear landings, and tracking loss do not unlock the
  next right-foot count.
- A left landing unlocks the next right landing but never increments the displayed score.
- Events with timestamps older than the last processed event are rejected.
- Skipped camera frames never create compensating landing events.
- MediaPipe anatomical left/right is authoritative; mirrored preview position is not.

Reference: <https://rules.ijru.sport/judging-manual/speed/counting/>

## Architecture

T-752 phase 1 implements the deterministic event-to-count core:

`PoseFrame(source timestamp) -> PoseSpeedLandingClassifier -> SpeedStepDetector -> Speed 30 score`

Keeping the classifier separate makes the alternation rule testable without camera timing or pose
thresholds. Phase 2 adds the classifier and connects it to a new Speed menu. The existing Basic
Bounce detector remains unchanged and is selected only for `BASIC_BOUNCE` mode.

## Phase 2 pilot implementation

- `PoseFrame.sourceTimestampMillis` carries the MediaPipe input/result timestamp. It has a default
  value so existing Basic Bounce tests and constructors remain compatible.
- The classifier uses anatomical MediaPipe indices 23/24, 27/28, 29/30, and 31/32. Preview
  mirroring never swaps the detector's left/right meaning.
- Six initial valid frames calibrate a conservative per-foot ground baseline.
- Separate lift and landing thresholds provide hysteresis. The current ratios (`0.08` and `0.03`
  of hip-to-foot length) are hypotheses, not accepted production thresholds.
- Opposite-foot landings within 70 ms are classified as `BOTH`; a single landing is delayed until
  that safety window expires.
- Visibility loss while a foot is airborne or a landing is pending emits `UNCLEAR`, clears only
  transient contact state, and never unlocks the right-foot alternation gate.
- Speed uses the same monotonic `uptimeMillis` clock as MediaPipe timestamps. The UI timer remains
  based on `elapsedRealtime` and automatically finishes at 30 seconds.
- Debug-only diagnostics show landing totals/rejects, visibility/out-of-order/tracking-loss totals,
  FPS, average/max latency, and estimated skipped frames. No image or landmark list is retained.

## States

- `RIGHT_ELIGIBLE`: the next clear right landing may count.
- `NEED_LEFT`: a right landing has counted; only a clear left landing unlocks the next count.

Tracking loss preserves the current alternation state. Motion/contact state in the phase 2 landing
classifier may be reset after tracking loss, but the count gate must not be reset.

## Landing classifications

- `LEFT`: clear left-only landing.
- `RIGHT`: clear right-only landing.
- `BOTH`: both feet land together or within the simultaneous-landing window.
- `UNCLEAR`: visibility or timing cannot identify a safe landing side.
- `NONE`: no landing event.

When confidence is insufficient, the classifier must prefer `UNCLEAR` over guessing a side.

## Diagnostics

The event core records:

- left and right landing timestamps;
- counted-right timestamps;
- repeated-left and repeated-right rejects;
- both-feet and unclear-landing rejects;
- out-of-window and out-of-order timestamp rejects;
- tracking-loss count and accumulated duration;
- alternation state before and after each returned result.

The phase 2 camera integration adds FPS, average/max inference latency, submitted/result/skipped
frames, visibility rejects, and timestamped left/right/counted landings. Debug diagnostics do not
retain camera images.

## UI plan

The Home or workout-selection screen will present two distinct modes:

1. `Basic Bounce` — current detector and existing behavior.
2. `Speed 30` — fixed 30-second timer and right-foot score.

The Training and Result screens display the selected workout mode. Speed results are deliberately
not written to the Basic Bounce History table in this pilot. Room schema changes remain deferred
until Speed field validation and a migration design are approved.

## Test plan

Phase 1 unit tests cover first right, correct alternation, repeated foot, both feet, unclear landing,
tracking loss, reset, not-started input, out-of-order timestamps, and both edges of the 30-second
window.

Phase 2 real-device testing on Samsung Galaxy S23 Ultra will begin with standing still, left-only,
right-only, both-feet, slow alternation, and tracking-loss controls before any formal Speed 30 pilot.
No phone-test pass may be recorded until the user reports the observed results.

Before device installation, Windows must pass `testDebugUnitTest` and `assembleDebug`. The current
Codex workspace could not download the Gradle 9.3.0 distribution because external Gradle network
access was blocked; this is a verification limitation, not a recorded test pass.
