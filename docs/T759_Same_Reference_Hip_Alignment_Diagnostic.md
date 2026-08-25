# T759 Same-Reference Hip Alignment Diagnostic

## Decision

Keep production T738 unchanged and retain T756/T757 as offline shadows. T759 adds only bounded
evidence needed to compare proposal and detector hip rise with a common detector-derived reference.
No threshold or detector decision changes are approved.

## Evidence that triggered T759

- The 179-jump replay produced production/T756/T757 `88/89/89` for a different analysis window,
  so it could not verify strict parity with the prior `84/84/83` run.
- The 150-jump replay used the same `71.933s` video duration and GO `2.000s`; it retained strict
  parity at `81/82/82`.
- In that 150-jump replay, 66 qualified unmatched pulses had gate attribution and every row was
  blocked by rescue hip rise. Detector raw/smoothed hip medians were `0.07842/0.07945`, only
  `0.00217` apart, while the proposal local-pulse hip median was `0.21362`.
- The proposal and detector values used different starting references, so their magnitude gap was
  not sufficient evidence for a threshold change.

## Implementation

For a rejected detector peak, T759 derives the detector peak timestamp from evidence-emission time
minus `nextFrameIntervalMillis`. It finds the matching retained raw measurement and reconstructs a
normalized detector baseline from the existing raw detector hip-rise ratio. That same derived
baseline is then applied at both the proposal ankle peak and proposal hip peak.

CSV proposal rows add:

- `detector_evidence_emission_delta_ms` beside the corrected detector peak delta;
- `same_reference_hip_at_proposal_ankle_peak_ratio`;
- `same_reference_hip_at_proposal_hip_peak_ratio`;
- each same-reference value's signed delta from detector raw hip evidence.

Only derived ratios and timestamps are exported. Raw landmark coordinates, images, and video data
are not retained or exported. Measurement retention is bounded to 32 frames. The collector remains
passive and cannot drive detector state, Counter, Training, Result, History, Room, or Release.

## Isolation and acceptance

- Production T738 and T756/T757 detector thresholds and decisions must remain unchanged.
- Same-video count parity must remain production/T756/T757 `81/82/82` for
  `20260824_150_1.mp4`, duration `71.933s`, GO `2.000s`.
- T759 is diagnostic evidence only. It cannot justify lowering the rescue hip threshold without
  event alignment and the skipped negative controls.
- Side-by-side package: `com.ropeskill.app.diagnostic.t759`, version `0.1.0-t759`, label
  `RopeSkill T759`.

## Verification

- Focused T759 regression passed `70/70`:
  - `BasicBounceDetectorTest`: 44
  - `BasicBounceVideoDiagnosticTest`: 3
  - `T735PassiveTakeoffGateTraceTest`: 11
  - `T743PassiveLandingStateTraceTest`: 8
  - `DebugApplicationIsolationTest`: 4
- `lintT759` and `assembleT759`: Pass.
- APK identity inspection confirmed package `com.ropeskill.app.diagnostic.t759`, version
  `0.1.0-t759`, and label `RopeSkill T759`.
- APK SHA-256: `587AA93A632E50BC1B45552F70864A14BF968903A85E9A663218A6F0287F9EDC`.
- Side-by-side install and cold launch succeeded on Samsung Galaxy S23 Ultra. Production, Condo
  Diagnostic, and T758 packages remained installed.
- Device replay and exported T759 CSV remain pending.
