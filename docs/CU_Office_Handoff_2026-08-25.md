# CU Office Handoff — 2026-08-25

## Start here

- Repository: `https://github.com/naruep/RopeSkill-Android-MVP.git`
- Handoff remote branch: `origin/codex/office-handoff-20260824`
- Continue from the GitHub repository, not OneDrive and not
  `D:\MyProjects\RopeSkill-Android-MVP-work` because that worktree contains separate Speed Step
  changes.
- Read `docs/T756_Bilateral_Hip_Rescue_Shadow.md` and the latest T-756/T-757 entries at the end of
  `docs/Test_Log.md` before editing.

## Confirmed result at Condo

- Source replay: `20260823_124136.mp4`, manual ground truth `150`.
- Production T738: `147`.
- T756 bilateral Takeoff shadow: `148`.
- T757 guarded Landing re-arm shadow: `150` with exactly two rescues.
- Rescue evidence:
  - elapsed `64.726s`, AIR `363ms`, ankle baseline margin `+0.00021`, hip baseline margin
    `+0.02646`, ankle descent margin `-0.01890`, hip descent margin `+0.13669`, joint next-rise.
  - elapsed `68.323s`, AIR `330ms`, ankle baseline margin `+0.00388`, hip baseline margin
    `+0.04634`, ankle descent margin `-0.03572`, hip descent margin `+0.09055`, joint next-rise.
- T757 did not rescue the tail/post-stop motion.
- Decision: replay target Pass; Production promotion is not approved yet.

## Candidate safeguards

`T757_LANDING_REARM_RESCUE_SHADOW` is offline Video Diagnostic only. It retains T756 Takeoff gates
and closes a candidate Landing only when all conditions pass:

- AIR time at least `300ms`.
- Standard baseline and completed vertical-cycle Landing have not already passed.
- ankle baseline ratio at most `0.047`.
- hip returned to baseline.
- hip descended from peak.
- ankle and hip both started the next rise.

Detector defaults keep this rescue disabled. `TrainingViewModel` has no T757 reference. Production
T738, Counter, Training, Result, History, Room and Release behavior are unchanged.

## Verified local gates

- Focused tests: `66/66`.
  - `BasicBounceDetectorTest` 44.
  - `BasicBounceVideoDiagnosticTest` 3.
  - `T735PassiveTakeoffGateTraceTest` 9.
  - `T743PassiveLandingStateTraceTest` 8.
  - `DebugApplicationIsolationTest` 2.
- `lintDebug`: Pass.
- `assembleDebug`: Pass.
- Diagnostic APK SHA-256:
  `958292C77519158844684BAC999955D6BB3C6D6E349AD7A20ECD75F4E4B7D670`.
- The earlier full Debug suite remained `256/257` because the pre-existing out-of-scope Speed Step
  test `PoseSpeedLandingClassifierTest.frameEvidence_reportsExactGeometryAndRejectedConservativeCandidate`
  fails repeatably. Do not mix that Speed work into this Basic Bounce checkpoint.

## First commands at CU Office

```powershell
git fetch origin
git switch codex/office-handoff-20260824
git pull --ff-only
git status --short --branch
```

Then run the focused regression before making changes:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.ropeskill.app.BasicBounceDetectorTest --tests com.ropeskill.app.BasicBounceVideoDiagnosticTest --tests com.ropeskill.app.T735PassiveTakeoffGateTraceTest --tests com.ropeskill.app.T743PassiveLandingStateTraceTest --tests com.ropeskill.app.DebugApplicationIsolationTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Do not run Gradle Sync merely to use an already-built APK. Sync is needed only if Android Studio
reports that project configuration changed or cannot resolve the imported project.

## Next validation order

1. Repeat the same 150-jump replay once. Expected T738/T756/T757 is `147/148/150`, T757 rescues
   exactly `2`, with no extra rescue after stopping.
2. Run offline negative-control videos against T757:
   - heel raises: 20 repetitions;
   - left knee lifts: 5;
   - right knee lifts: 5;
   - standing still: at least 15 seconds;
   - post-stop/walk toward camera: at least 10 seconds.
3. Require T757 false count/rescue `0` for every negative control and confirm no History entry,
   crash, freeze or stuck AIR state.
4. If controls pass, prepare a separate production-candidate proposal and real-device live CameraX
   protocol. Do not enable T757 in Training from replay evidence alone.
5. If any control fails, keep T757 shadow-only and inspect its exported `landing_rearm_rescue` row;
   do not relax the guards further.

## Repository safety

- Preserve unrelated changes and the generated untracked
  `gradle/gradle-daemon-jvm.properties` unless intentionally reviewed.
- Do not use `git add -A` or `git add .`.
- Do not merge Speed Step work into this Basic Bounce branch.
- Do not push a Production/Play release without explicit approval.
