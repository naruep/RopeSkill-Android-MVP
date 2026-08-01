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

`Pose frames -> landing classifier (phase 2) -> SpeedStepDetector -> Speed 30 score`

Keeping the classifier separate makes the alternation rule testable without camera timing or pose
thresholds. Phase 2 will add pose-to-landing classification and connect it to a new Speed menu. The
existing Basic Bounce route remains independent.

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

The phase 2 camera integration will add FPS, average/max inference latency, submitted/result/skipped
frames, visibility rejects, and inter-landing intervals. Debug diagnostics must not retain camera
images.

## UI plan

The Home or workout-selection screen will present two distinct modes:

1. `Basic Bounce` — current detector and existing behavior.
2. `Speed 30` — fixed 30-second timer and right-foot score.

The Training screen and Result/History records will display the selected workout mode. Room schema
changes are deferred until the UI/data integration phase is designed and its migration is tested.

## Test plan

Phase 1 unit tests cover first right, correct alternation, repeated foot, both feet, unclear landing,
tracking loss, reset, not-started input, out-of-order timestamps, and both edges of the 30-second
window.

Phase 2 real-device testing on Samsung Galaxy S23 Ultra will begin with standing still, left-only,
right-only, both-feet, slow alternation, and tracking-loss controls before any formal Speed 30 pilot.
No phone-test pass may be recorded until the user reports the observed results.
