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
- Windows `testDebugUnitTest`, `lintDebug`, `assembleDebug` และ `assembleRelease`: Pass
- Signed test APK สร้างจาก Release artifact และติดตั้งแบบ non-incremental: Pass
- `dumpsys package` หลังติดตั้งไม่มี `DEBUGGABLE` หรือ `TEST_ONLY`: Pass
- หลัง `GO` ยืนนิ่ง 10 วินาที: Counter `0`, diagnostic panels และ `TEST +1` ไม่ปรากฏ
- Basic Bounce Release smoke: Actual/App `10/10`, หลังหยุดเพิ่ม `0`, History `10 jumps / 00:22`
- ไม่พบ crash/freeze หรือ preview stuttering ที่มองเห็น

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

Pass — Windows verification และ real-device Release smoke ผ่านเมื่อ 31 July 2026
