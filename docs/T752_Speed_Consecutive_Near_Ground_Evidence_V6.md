# T-752 Speed Consecutive Near-Ground Evidence V6

## Objective

Explain why the V5 `0.04` recovery band usually does not reach its two-consecutive-valid-frame
requirement. V6 is passive Debug evidence only; it does not tune or replace V5 behavior.

## Evidence boundary

V5 Accuracy Round 1 recorded about `35` actual right-foot landings and `17` app steps. Raw landings
were `L/R = 22/22`, conservative re-arms were `2/2`, repeated-right rejects were `5`, and maximum
AIR duration remained `L4,443/R6,766 ms`. History, video, preview, and stability passed.

## Diagnostic contract

For each foot while production phase is `AIRBORNE`, the Debug observer reads the exact
classification rise ratio before the existing V5 update and reports:

- `NG current/max`: current and maximum consecutive frames in `>0.03` and `<=0.04`.
- `NGS`: number of near-ground streak starts.
- `NGB`: partial streaks broken by a following frame above `0.04`.
- `NGX`: partial streaks completed by a strict landing at or below `0.03`.
- `NGL`: partial streaks cleared by tracking loss.

All cumulative V6 evidence resets at `GO`. The live current streak remains production motion state;
`commitPreGoCalibration()` resets that state immediately before resetting the evidence window.

## Safety boundary

- Keep lift `0.08`, strict landing `0.03`, recovery `0.04 × 2 valid frames`, and simultaneous window
  `70 ms` unchanged.
- The observer does not emit landings, update baselines, alter phases, or drive `SpeedStepDetector`.
- Observer counters are collected only when Debug evidence is enabled.
- `BasicBounceDetector.kt`, `SpeedStepDetector`, recorder, audio, Room schema, and History remain
  unchanged.

## Automated gates

1. Evidence-enabled and evidence-disabled classifiers emit identical production events.
2. One near-ground frame followed by a rise above `0.04` increments `NGB` only.
3. A partial streak followed by strict landing increments `NGX` only.
4. A partial streak followed by tracking loss increments `NGL` only.
5. A successful conservative re-arm records maximum streak `2` without a break reason.
6. Evidence-window reset clears cumulative V6 totals without recalibration or detector-state change.
7. Windows `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` pass.

## Device protocol

Use Samsung Galaxy S23 Ultra, normal lighting, Speed 30, and `Record this workout = ON`. Stand still
through Countdown and start natural alternating steps after `GO`. Keep the overlay visible through
Result. Report actual right-foot landings, app `RIGHT STEPS`, `NG`, `NGS`, `NGB`, `NGX`, `NGL`,
`REARM`, `L/R/C`, `RR/B/U`, `VIS/OOS/TL`, FPS, latency, skipped frames, Result/History/video status,
preview stuttering, and crash/freeze.

Do not run safety controls or change V5 thresholds until the accuracy video has been inspected.
