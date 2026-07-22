# RopeSkill Android MVP

RopeSkill is a native Android project that will use CameraX and on-device MediaPipe Pose Landmarker to help users practice and count Basic Bounce jump-rope movements.

The repository contains the Android MVP through the MediaPipe Pose Landmarker overlay increment.

## Current status

- Technology direction: Kotlin, Jetpack Compose, Material 3
- Primary test device: Samsung Galaxy S23 Ultra
- Current milestone: Milestone 5 — verify pose landmarks and overlay on the test device
- Real-device result: Milestones 0, 2, 3, and 4 confirmed on Samsung Galaxy S23 Ultra

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
