# T-755 Restore Accepted T-738 Production Baseline Wiring

## Evidence

The offline 150-jump replay completed `147/150` with `T/L147/147`, `SUP0`, no open AIR interval,
and no timeout/reset. The final accepted Landing was at `+69.960s`; the following candidate pulses
were rejected as `ANKLE_RISE_TOO_SMALL` at `+70.785s`, `+71.049s`, and `+71.445s` before stopping.

Static inspection found that Training and the Basic Bounce offline diagnostic were still wired to
`T736DetectorProfiles.PRODUCTION`, while the accepted on-device baseline is
`T738DetectorProfiles.PRODUCTION`.

## Change

- Route Training and Basic Bounce Video Diagnostic to the accepted T-738 profile.
- Do not modify `BasicBounceDetector` logic, thresholds, Landing, cooldown, auto-pause, storage,
  Result, History, or camera processing.
- T-738 preserves bilateral, synchronization, hip-ratio, and strong-hip guards; it adds only the
  previously accepted bounded asymmetric rescue route.

## Verification required

1. Windows unit tests and Debug build.
2. Replay `20260823_124136.mp4`; compare the offline count and rejection timeline with the prior
   `147/150` baseline.
3. Samsung Galaxy S23 Ultra: Smoke 3/3, standing/knee-lift/heel-raise controls 0, then one
   150-jump continuous session. Do not claim the fix works on device until those results exist.
