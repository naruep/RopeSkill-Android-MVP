# T-752 Speed Fixed-Reference Shadow Evidence V8

## Objective

Determine whether adaptive ground-baseline drift is the primary cause of missed Speed landings.
V8 runs a passive fixed-reference shadow state machine on the same valid foot samples as production.
It does not emit events or change the app counter.

## Evidence boundary

V7 recorded about `30 ±1` actual right-foot landings and `18` app steps. Production emitted raw
`L/R = 28/20`, with `RR = 2`. Right baseline shift reached `0.148`, right near-ground streaks
started `14` times and broke out of band `6` times. FPS was `25.0`, skipped frames were about zero,
and tracking loss was zero. A same-frame fixed-reference comparison is therefore required before
changing any threshold or baseline behavior.

## Diagnostic contract

At `GO`, V8 captures each production foot baseline as an immutable shadow reference. For every
subsequent valid frame, the Debug-only shadow uses the same average foot-ground Y, leg length and
unchanged thresholds as production while maintaining an independent phase and recovery streak:

- `FIXREF`: immutable left/right baseline captured at `GO`.
- `FXPH`: fixed-reference shadow phase for left/right.
- `FIXCORE`: current fixed-reference normalized rise ratio.
- `FXNG`: current/maximum recovery streak for left, then right.
- `FXAIR`: fixed-reference airborne transitions for left/right.
- `FXLAND`: total fixed-reference raw landings for left/right.
- `FXS/R`: strict/recovery landings for each foot.

Production `CORE`, `PHASE`, `REF/SHIFT`, near-ground evidence and raw `L/R/C` remain visible for
comparison. Shadow motion resets on tracking loss like production, and all V8 totals reset at `GO`.
Only bounded scalar diagnostics are retained in memory.

## Safety boundary

- Keep lift `0.08`, strict landing `0.03`, recovery `0.04 × 2 valid frames`, and simultaneous window
  `70 ms` unchanged.
- The shadow does not emit a `SpeedLandingEvent`, update production baseline/phase, or drive
  `SpeedStepDetector` or Counter.
- Evidence is exposed only when Debug diagnostics are enabled.
- `BasicBounceDetector.kt`, `SpeedStepDetector`, recorder, audio, Room schema, and History remain
  unchanged.

## Automated gates

1. Evidence-enabled and evidence-disabled classifiers emit identical production events.
2. A controlled adaptive-baseline displacement can produce a fixed-reference shadow landing while
   production output remains unchanged.
3. Shadow reference, phase, ratio, airborne transitions, strict/recovery and total landings report
   exact values.
4. `GO` captures the re-anchored baseline and clears prior shadow totals and motion state.
5. Release-disabled diagnostics expose no fixed-reference payload.
6. Windows `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` pass.

## Device protocol

Use Samsung Galaxy S23 Ultra, normal lighting, Speed 30, and `Record this workout = ON`. Stand still
through Countdown and start natural alternating steps after `GO`. Keep
`SPEED FIXED-REFERENCE SHADOW V8` and `FIXREF/FXPH/FIXCORE/FXNG/FXAIR/FXLAND/FXS/R` visible through
Result. Report actual right-foot landings, app `RIGHT STEPS`, fixed-reference rows, production
`PHASE/CORE/REF/SHIFT/NG/L/R/C/RR`, FPS, latency, skipped frames, Result/History/video status,
preview stuttering, and crash/freeze.

Do not tune thresholds or production baseline behavior until the V8 video has been inspected.
