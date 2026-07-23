# RopeSkill Codex Instructions

## Project goal

Build a native Android MVP that uses the camera and on-device pose estimation to help users practice and count Basic Bounce jump-rope movements.

## User and communication

- The project owner is a beginner in Android development.
- Communicate in clear Thai unless the user requests another language.
- Keep filenames, commands, code, errors, and official technical terms in English.
- Explain only the concepts needed for the next practical, testable task.
- Never claim that the app works on a real device until the user confirms the result.

## MVP development order

1. Create and run a basic Android sample app.
2. Learn the required Kotlin and Jetpack Compose concepts.
3. Build a counter and workout timer.
4. Add Home, Training, and Result navigation.
5. Add a CameraX live preview.
6. Add MediaPipe Pose Landmarker and an overlay.
7. Detect and count Basic Bounce.
8. Store training results locally.
9. Validate accuracy, performance, and stability on real devices.

Do not start backend, accounts, iOS, cloud processing, payments, or a complete scoring system before the Android MVP is validated.

## Technology constraints

- Kotlin and native Android
- Jetpack Compose and Material 3
- ViewModel, Kotlin Coroutines, and Flow
- CameraX
- MediaPipe Pose Landmarker
- Room or DataStore only when the storage requirements are known
- Gradle Kotlin DSL and Git

Verify changing Android, CameraX, MediaPipe, Kotlin, and Gradle recommendations against current official documentation before introducing versions or APIs.

## Code-change rules

- Inspect the existing project and relevant files before editing.
- Preserve unrelated changes and avoid premature abstractions or dependencies.
- Work in small increments that can be built and tested independently.
- State which files will change and why.
- Handle lifecycle, permissions, threading, error states, and cleanup explicitly.
- Never perform camera or AI processing on the Main Thread.
- Always close `ImageProxy` objects and release camera and MediaPipe resources.
- Handle rotation, mirroring, timestamps, and overlay coordinate conversion explicitly.
- Avoid unnecessary per-frame allocations.
- Do not log or retain camera images, pose data, personal data, or secrets.
- Run the most relevant build, test, or lint check after changes when the environment permits.

## Error diagnosis

When given an error, Logcat output, screenshot, or video:

1. Describe the observed symptom.
2. Find the first meaningful root-cause error.
3. List the most likely causes and how to verify them.
4. Apply or recommend the smallest appropriate fix.
5. Explain the verification procedure and expected result.

## Project records

Keep these files current when relevant:

- `docs/Requirements.md`
- `docs/Roadmap.md`
- `docs/Architecture_Decisions.md`
- `docs/Known_Issues.md`
- `docs/Test_Log.md`

For important technical decisions, record the decision, reason, affected areas, and conditions for revisiting it. Treat camera images, video, pose landmarks, and body-related data as sensitive and process them on-device by default.

## Current verified status

- Repository documentation is initialized.
- Android sample application source code is present.
- Primary real-device test target: Samsung Galaxy S23 Ultra.
- Android version and One UI version are not yet confirmed.
- Milestone 0 build and real-device run were confirmed by the user on 22 July 2026.
- Milestones 0, 2, 3, and 4 were confirmed on the Samsung Galaxy S23 Ultra on 22 July 2026.
- MediaPipe Pose Landmarker and overlay were confirmed on the Samsung Galaxy S23 Ultra on 22 July 2026.
- Ready detection, countdown, first-jump timer start, slow jumps และ medium jumps ผ่านการทดสอบบน Samsung Galaxy S23 Ultra เมื่อ 23 July 2026.
- Detector tuning รอบสองถดถอย: fast 1/10, slow 0/10, medium 0/10 และ knee-lift false positives 5 ครั้ง จึงคืน detector รอบแรกและรอทดสอบยืนยันบนอุปกรณ์จริง.
