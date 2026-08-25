# T758 Takeoff Peak-Alignment Diagnostic

## Decision

Keep T757 shadow-only and add passive, offline evidence before considering another detector
candidate. The accepted production profile remains T738. T756 and T757 remain available only in
Basic Bounce Video Diagnostic.

## Evidence that triggered T758

- The original 150-jump replay was deterministic at production/T756/T757 `147/148/150`, with two
  T757 Landing re-arm rescues.
- A different 179-jump video produced `84/84/83`. T757 applied one rescue but finished one count
  below T756.
- A different 150-jump video produced `81/82/82`, with no T757 rescue.
- Across the two new videos, T756 rejected 165 Takeoff peaks. Exact signed margins showed a
  negative rescue-hip margin for 164/165 rows. The passive motion observer still found raw pulses
  close to manual ground truth (`179` and `153`).
- Raw pulse hip maxima and detector smoothed hip evidence were sampled at different times. The
  prior CSV did not export enough aligned operands to decide whether the gap was caused by temporal
  peak selection, smoothing, or genuinely insufficient detector evidence.

These results are an efficacy failure for T757 generalization. Skipped negative controls remain
Not tested; no production promotion is allowed.

## Implementation

`T735PassiveTakeoffGateCollector` now accepts the threshold profile of the detector it observes.
Training's disabled collector is explicitly paired with T738, while Basic Bounce Video Diagnostic
pairs the passive proposal trace with T756. This removes the former T736 rescue-threshold hardcode
from gate attribution without changing detector decisions.

For each bounded retained motion pulse, the CSV now exports:

- raw ankle and hip rise maxima plus pulse duration;
- hip-peak offset from the ankle proposal peak;
- detector-peak offset from the ankle proposal peak;
- detector raw and smoothed ankle/hip evidence;
- standard ankle, left/right individual ankle, rescue ankle, rescue hip, and hip-to-ankle signed
  margins calculated with the observed profile.

The trace retains at most 512 proposal rows in Basic Bounce Video Diagnostic. It stores no image,
video, landmark coordinate, or personal data. It cannot drive detector state, Counter, Training,
Result, History, Room, or Release behavior.

## Verification

- T758 focused regression: `68/68` passed.
  - `BasicBounceDetectorTest`: 44
  - `BasicBounceVideoDiagnosticTest`: 3
  - `T735PassiveTakeoffGateTraceTest`: 10
  - `T743PassiveLandingStateTraceTest`: 8
  - `DebugApplicationIsolationTest`: 3
- `lintDebug` and `lintT758`: Pass.
- `assembleDebug` and `assembleT758`: Pass.
- Side-by-side T758 APK SHA-256:
  `CFEDF815368088A0AD565B9A0D46FBC25BF7441DF7C4CF9F657B1736F11E1F6F`.
- Production T738, T756, T757, Counter, Training, Result, History and storage logic are unchanged.

## Device replay result

The 150-jump replay used the same duration `71.933s` and GO `2.000s` and retained strict
production/T756/T757 parity at `81/82/82`. The 179-jump replay used a different duration and GO
from its prior run, so strict parity for that video was not established.

For the 150-jump replay, 66 qualified unmatched pulses had gate attribution and all 66 were blocked
by rescue hip rise. Raw and smoothed detector hip evidence differed by only `0.00217` at the median,
so smoothing was not the main observed gap. Proposal and detector hip ratios used different local
references; therefore T758 did not justify a threshold change and led to T759 same-reference
evidence instead.

The approved side-by-side build uses application ID `com.ropeskill.app.diagnostic.t758`, version
`0.1.0-t758`, and label `RopeSkill T758`. It was installed and cold-launched successfully on the
Samsung Galaxy S23 Ultra on 25 August 2026. Production `com.ropeskill.app` and Condo Diagnostic
`com.ropeskill.app.diagnostic` remained installed; neither package nor its data was replaced.
