# RopeSkill Android MVP

RopeSkill is a native Android project that will use CameraX and on-device MediaPipe Pose Landmarker to help users practice and count Basic Bounce jump-rope movements.

The repository now contains the first Android sample project. It must still be synced, built, and run in Android Studio before Milestone 0 is considered complete.

## Current status

- Technology direction: Kotlin, Jetpack Compose, Material 3
- Primary test device: Samsung Galaxy S23 Ultra
- Current milestone: Milestone 0 — sync, build, and run the Android sample project on the test device
- Real-device result: not yet confirmed

## Project documents

- [Project Brief](docs/RopeSkill_Project_Brief.md)
- [Requirements](docs/Requirements.md)
- [Roadmap](docs/Roadmap.md)
- [Architecture Decisions](docs/Architecture_Decisions.md)
- [Known Issues](docs/Known_Issues.md)
- [Test Log](docs/Test_Log.md)
- [UI Wireframes](docs/UI_Wireframes.md)

## Privacy baseline

Camera frames and pose-related data are sensitive. The MVP processes them on the device by default and must not upload or retain them unless a later feature explicitly requires it and the design is approved.
