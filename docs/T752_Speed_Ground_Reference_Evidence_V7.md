# T-752 Speed Ground-Reference Evidence V7

## Objective

Explain why genuine foot contacts frequently remain above the V5 recovery ratio `0.04`. V7 is
passive Debug evidence only and does not tune classifier or counter behavior.

## Evidence boundary

V6 recorded about `27 ±1` actual right-foot landings and `7` app steps. Raw landings were
`L/R = 14/12`, repeated-right rejects were `5`, and conservative re-arms were `3/4`. Both feet
reached a two-frame near-ground streak, all right streaks completed through strict landing or
re-arm, and there were no right out-of-band or tracking-loss breaks. The missing events therefore
occur before the near-ground observer: genuine contacts usually do not enter `<=0.04`.

## Diagnostic contract

Before each unchanged production foot update, the Debug observer records per foot:

- `REF`: the baseline at `GO` and the current production baseline Y.
- `SHIFT`: current signed and maximum absolute baseline shift, normalized by current leg length.
- `SAMPLE`: current average foot-ground Y and leg length.
- `LOWREF`: production baseline and foot-ground Y at the closest airborne approach.
- `LOWGAP`: raw baseline-to-ground gap, leg length, and exact normalized rise ratio at that approach.

The closest approach is the lowest classification rise ratio observed while the production phase
is `AIRBORNE`. V7 evidence resets at `GO`. It stores only bounded scalar diagnostics in memory and
does not retain landmarks, images, or video.

## Safety boundary

- Keep lift `0.08`, strict landing `0.03`, recovery `0.04 × 2 valid frames`, and simultaneous window
  `70 ms` unchanged.
- The observer does not emit landings, update a baseline, alter a phase, or drive
  `SpeedStepDetector`.
- Evidence is exposed only when Debug diagnostics are enabled.
- `BasicBounceDetector.kt`, `SpeedStepDetector`, recorder, audio, Room schema, and History remain
  unchanged.

## Automated gates

1. Evidence-enabled and evidence-disabled classifiers emit identical production events.
2. Exact pre-update baseline, ground Y, leg length, raw gap, and normalized gap are reported.
3. Grounded baseline adaptation is observable without being changed by the observer.
4. The `GO` boundary captures the re-anchored baseline and clears prior closest-approach evidence.
5. Release-disabled diagnostics expose no ground-reference payload.
6. Windows `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` pass.

## Device protocol

Use Samsung Galaxy S23 Ultra, normal lighting, Speed 30, and `Record this workout = ON`. Stand still
through Countdown and start natural alternating steps after `GO`. Keep `SPEED GROUND-REFERENCE V7`
and the `REF/SHIFT/SAMPLE/LOWREF/LOWGAP` rows visible through Result. Report actual right-foot
landings, app `RIGHT STEPS`, all V7 rows, `PHASE`, `AIRMS/AIRMIN`, `REARM`, `NG`, `L/R/C`,
`RR/B/U`, `VIS/OOS/TL`, FPS, latency, skipped frames, Result/History/video status, preview
stuttering, and crash/freeze.

Do not tune thresholds or run safety controls until the V7 accuracy video has been inspected.
