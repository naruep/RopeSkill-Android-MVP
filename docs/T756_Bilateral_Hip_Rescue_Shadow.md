# T756 Bilateral Hip Rescue Shadow

## Decision

Add an offline-only `T756_BILATERAL_HIP_RESCUE_SHADOW` evaluation to Basic Bounce Video Diagnostic.

## Reason

The 150-jump video replay remains at 147 counts under production T738. Its missing cycles are predominantly `ANKLE_RISE_TOO_SMALL`, while no airborne interval remains open. T738 asymmetric rescue does not apply to those bilateral misses.

## Scope and safeguards

T756 uses `minimumIndividualAnkleRiseRatio = 0.006` and `strongHipRescueAnkleRiseRatio = 0.012`; it retains the existing hip-rise and feet-synchronization requirements. It runs only beside production T738 during offline video analysis and writes only the aggregate count to the diagnostic UI and CSV. It does not alter Training, live counting, History, video retention, or pose-data retention.

## Revisit condition

Consider a production candidate only if the replay reaches the manual ground truth and dedicated real-device control videos show no false counts, stalls, crashes, or History entries.

## T756 replay result and diagnostic follow-up

- Production T738 replay: `147/150` with Takeoff/Landing `147/147`, no open AIR interval,
  no suppressed Landing, and continued counting after jump 100.
- T756 shadow replay: `148/150`; the candidate recovered one count but did not reach the
  manual ground truth, so production promotion is not approved.
- The aggregate-only shadow result could not identify the recovered cycle or attribute the two
  remaining misses. The follow-up adds bounded shadow cycle/rejection timelines containing only
  timestamps, event types, diagnostics, Landing reasons, and AIR durations. It retains no image,
  video, landmark, or pose-coordinate data and remains offline Debug-only.
- Condo installation found a debug-signing mismatch with the Office APK. Debug builds now use
  package `com.ropeskill.app.diagnostic` and label `RopeSkill Diagnostic`, allowing side-by-side
  installation without uninstalling or changing data in the existing `com.ropeskill.app` app.
  Release keeps its original package and label.

## Follow-up verification

- `BasicBounceDetectorTest` 43/43, `BasicBounceVideoDiagnosticTest` 3/3, and
  `DebugApplicationIsolationTest` 1/1 passed.
- `assembleDebug` passed; APK inspection reported package `com.ropeskill.app.diagnostic`,
  version `0.1.0-diagnostic`, and label `RopeSkill Diagnostic`.
- Side-by-side installation succeeded on Samsung Galaxy S23 Ultra. The updated replay and CSV
  export remain required before attributing the recovered and remaining cycles.

## Shadow timeline and visual attribution

- The updated replay remained deterministic at production `147` and shadow `148`.
- Shadow recovered the production rejection at diagnostic elapsed `18.183s`: shadow Takeoff was
  accepted at `18.117s` and counted Landing at `18.249s` after `132ms` AIR.
- Diagnostic elapsed time starts at GO. With GO at video `2.000s`, the tail rejection timestamps
  must be shifted by `+2.000s` before visual comparison.
- Frame review of video `20260823_124136.mp4` found that the ankle-rejection cluster at video
  `72.785–73.709s` overlaps continuing two-foot Basic Bounce motion and remains the plausible
  genuine-miss window.
- The synchronization rejections at video `74.798s`, `76.349s`, and `76.943s` occur while the
  user stops the rope and steps toward the camera. They are post-stop controls, not missed Basic
  Bounce cycles, and the synchronization gate should remain unchanged.
- Next evidence should be a bounded Debug-only signed-margin trace for the ankle/hip gates in the
  genuine-miss window. Do not lower production thresholds from aggregate count alone.

## Signed-margin trace implementation

- Added a bounded `shadow_margin` CSV section for shadow rejected peaks. Positive values pass a
  gate and negative values show the exact shortfall.
- Columns cover standard smoothed ankle, left/right individual ankle, strong-rescue ankle,
  strong-rescue hip, and hip-to-ankle margins.
- Detector constants `0.045`, `0.100`, and `0.85` are shared from one internal source; values and
  production decisions are unchanged.
- Evidence contains only elapsed time, diagnostic reason, and derived signed ratios. It contains
  no image, video, landmark, or pose coordinates and remains bounded/offline Debug-only.
- Detector tests 43/43, diagnostic tests 3/3, package-isolation test 1/1, and `assembleDebug`
  passed. The updated side-by-side Diagnostic APK installed successfully on Samsung Galaxy S23
  Ultra. A new replay/CSV is required to select any further shadow threshold.

## Signed-margin result and passive proposal follow-up

- Replay CSV remained production/shadow `147/148`. Signed margins showed that the tail ankle
  cluster at elapsed `70.785–71.709s` was not near a takeoff gate: ankle motion was negative and
  rescue hip evidence was also below its gate. Lowering T756 further would risk accepting descent,
  standing, or approach motion and is not approved.
- The three synchronization rejections remain post-stop controls. Therefore the two-count gap
  from manual ground truth cannot be attributed to the retained rejected peaks.
- Add a bounded offline Debug-only passive proposal trace around T756 shadow output. It reuses the
  T735 upward/downward pulse observer to report raw, qualified, matched, unmatched, AIR-state,
  gate-attributed, no-peak, pending, and interrupted totals plus a coordinate-free pulse timeline.
- The proposal observer reads the same `PoseFrame` and the already-produced T756 shadow result
  after the detector decision. It cannot drive detector state, Counter, Training, Result, History,
  storage, or Release behavior. Retention is bounded to 512 pulse rows and contains no image,
  video, landmark coordinate, or personal data.
- Focused Basic Bounce/diagnostic/isolation tests passed `56/56`; `lintDebug` and `assembleDebug`
  passed. The full Debug suite remains `256/257` because the pre-existing Speed Step test
  `PoseSpeedLandingClassifierTest.frameEvidence_reportsExactGeometryAndRejectedConservativeCandidate`
  fails repeatably outside this Basic Bounce scope; no Speed source or test was changed.

## Passive proposal replay and long-AIR evidence

- Replay remained production/shadow `147/148`. T756 proposal trace reported `Q151 M148 U3`,
  including `UA2`, one gate-attributed READY pulse, `NP2`, and `P0`.
- The two AIR-state pulses reconcile the manual ground truth exactly: shadow `148` plus two
  physical pulses while AIR is `150`. Shadow Takeoff-to-Takeoff median was `462ms`, with only two
  large gaps: `924ms` from elapsed `63.459–64.383s` and `891ms` from `67.089–67.980s`.
- Corresponding AIR intervals were the only two above `400ms`: `660ms` ending at `64.119s` and
  `594ms` ending at `67.683s`, versus median `198ms`. This attributes the remaining deficit to
  cycle merge/Landing re-arm persistence, not a Takeoff threshold miss.
- Added a T756 shadow `landingStateEvidence` observer to the offline diagnostic. It retains at most
  4,096 evidence/event frames and exports only intervals lasting at least `400ms` or containing a
  physical AIR pulse. CSV rows contain baseline/descent operands, limits, signed margins,
  next-rise/completed-cycle flags, and bounded AIR-pulse evidence; no landmarks or coordinates.
- Focused detector/diagnostic/T735/T743/isolation regression passed `64/64`; `lintDebug` and
  `assembleDebug` passed. Production detector decisions, thresholds, Counter, Training, Result,
  History, storage, and Release behavior remain unchanged. Updated device replay/CSV is pending.

## T757 landing re-arm rescue shadow

- Long-AIR signed margins identified guarded re-arm frames at elapsed `64.713s` and `68.310s`.
  AIR time was `363/330ms`; hip baseline and hip descent passed, both ankle/hip next-rise flags
  were true, while ankle baseline missed the standard `0.040` limit by `0.00679/0.00366`.
- Added offline-only `T757_LANDING_REARM_RESCUE_SHADOW` beside production T738 and T756. It retains
  T756 Takeoff thresholds and can close Landing only when AIR is at least `300ms`, standard Landing
  and completed-cycle rules have not passed, ankle baseline ratio is at most `0.047`, hip baseline
  and hip descent pass, and ankle plus hip start the next rise together.
- The detector default keeps this rescue disabled. T757 is instantiated only by
  `BasicBounceVideoDiagnostic`; `TrainingViewModel` has no T757 reference. The candidate exports
  its count, rescue total, elapsed/AIR time, signed baseline/descent margins, and next-rise flags.
- Focused detector/diagnostic/T735/T743/isolation regression passed `66/66`; `lintDebug` and
  `assembleDebug` passed. Device replay, heel raise, left/right knee lift, standing, post-stop,
  deterministic-repeat, Result/History isolation and stability controls are required before any
  production proposal.

## T757 replay result

- Replay `20260823_124136.mp4` remained deterministic at production `147` and T756 `148`.
- T757 reached manual ground truth `150/150` with exactly two guarded Landing re-arm rescues and
  no additional tail/post-stop rescue.
- Rescue 1 occurred at GO-relative elapsed `64.726s`, AIR `363ms`, with ankle baseline margin
  `+0.00021`, hip baseline `+0.02646`, ankle descent `-0.01890`, hip descent `+0.13669`, and joint
  next-rise true.
- Rescue 2 occurred at GO-relative elapsed `68.323s`, AIR `330ms`, with ankle baseline margin
  `+0.00388`, hip baseline `+0.04634`, ankle descent `-0.03572`, hip descent `+0.09055`, and joint
  next-rise true.
- This is positive replay evidence, not production approval. Deterministic repeat and heel raise,
  left/right knee lift, standing, post-stop, Result/History isolation, performance and stability
  controls remain required. Production T738 and live Training remain unchanged.
