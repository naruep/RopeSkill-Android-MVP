# T-745 — Release Diagnostic Boundary Fix

## Objective

ปิด diagnostic UI และการสะสม diagnostic payload ใน Release build หลัง T-744 static audit พบว่า count-evidence panel, `MEDIUM DIAGNOSTIC V4`, `COOLDOWN V7` และ rescue totals ยังอาจทำงานนอก `BuildConfig.DEBUG`

## No-change guard

- ไม่แก้ `BasicBounceDetector`
- ไม่แก้ threshold, Takeoff, Landing, cooldown, Counter หรือ History
- ไม่เพิ่ม permission, analytics, network หรือ storage
- ไม่สร้างหรือ commit keystore

## Implementation

- ครอบ count-evidence panel ด้วย `BuildConfig.DEBUG`
- รวมเงื่อนไข summary panel ไว้ใน `shouldShowDebugDiagnosticPanel(...)`
- บังคับให้ helper คืน `false` เมื่อ `isDebugBuild = false` แม้ state มี diagnostic payload
- หยุดสะสม counted evidence, diagnostic transition counts และ strong-hip rescue totals ใน Release
- คง jump count, tracking state และ production detector result เดิม
- เพิ่ม unit tests สำหรับ Release-hidden, permission-hidden, empty-state และ Debug-visible boundaries

## Verification

- `git diff --check`: Pass
- Source diff ไม่แตะ `BasicBounceDetector.kt`: Pass
- Workspace Gradle: Blocked ก่อนเริ่ม task เพราะไม่มี Gradle 9.3 distribution ใน cache และ network policy ไม่อนุญาต `services.gradle.org`

รันบน Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

## Acceptance

- ทั้ง 4 Gradle commands ผ่าน
- Debug build ยังแสดง diagnostic เมื่อมีข้อมูล
- Release build ไม่แสดง count evidence, `MEDIUM DIAGNOSTIC`, `COOLDOWN`, rescue, trace, gate หรือ T-series rows
- Counter, Pause/Resume, Result และ History ไม่ถดถอย

## Status

Implemented / Awaiting Windows verification
