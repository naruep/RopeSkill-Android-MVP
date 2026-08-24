# T-754 Basic Bounce Video Diagnostic

## Purpose

Allow one recorded Basic Bounce session to be analyzed repeatedly on the Samsung Galaxy S23 Ultra,
so a tester does not need to repeat long jump rounds while investigating a counter stall.

## Boundary

- Debug build only; no Release UI or Training behavior changes.
- The selected video is read directly on-device. It is not copied, uploaded, retained, or added to
  History.
- The diagnostic uses the current Basic Bounce training thresholds in an offline replay and records
  only bounded counters, enums, and timing. CSV excludes images, videos, landmarks, and raw pose
  ratios.
- Offline results are diagnostic evidence only. Video decoding, sampling, and MediaPipe VIDEO mode
  can differ from the live CameraX stream, so one live confirmation remains required before a
  detector fix is accepted.

## Procedure

1. Make one screen recording of a Basic Bounce session that contains the counter stall.
2. In the Debug APK open `BASIC BOUNCE VIDEO DIAGNOSTIC` from Home.
3. Select the recording, seek to the first jump after at least 2 seconds standing still, and set
   that point as `GO`.
4. Analyze up to the following 120 seconds and export the CSV. This allows one long recording to
   cover a stall after 100 jumps without recalibrating in the middle of jumping.
5. Compare the video counter with `T/L`, the open-air marker `I...*`, diagnostics, and recent
   cycle/rejection rows. Send the video/screenshot and CSV for root-cause analysis.

## T-755 attribution extension

The offline result also reports every rejected Takeoff grouped by diagnostic reason and exports a
bounded timestamp-only timeline (up to 512 cycle/rejection events). It retains the latest 12
landing intervals, frames, and airborne motion pulses. This separates a bounded airborne re-arm
signature from an explicit Takeoff rejection without changing detector behavior.
