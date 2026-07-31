# T-744 — MVP Release Readiness Audit

## Objective

ตรวจความพร้อมของ RopeSkill Android MVP หลังปิด T-743 โดยไม่เปลี่ยน detector behavior, thresholds, Counter, Training lifecycle หรือข้อมูล History

T-744 เป็น acceptance audit ไม่ใช่งาน detector tuning หากพบปัญหาให้บันทึกหลักฐานและแยกงานแก้ไขใหม่ก่อนเปลี่ยน source code

## Baseline

- Project checkpoint: `6e50b40`
- Accepted detector baseline: `752af1d`
- Device: Samsung Galaxy S23 Ultra
- T-743: Pass / Complete
- KI-022: Monitoring

## Scope

1. ตรวจ Git status และยืนยัน checkpoint
2. รัน automated verification บน Windows
3. ตรวจว่า release variant ไม่มี debug overlay หรือ passive diagnostic
4. ตรวจ permissions และ privacy boundary
5. รัน device release-readiness smoke
6. ตรวจ Training lifecycle และ local History
7. สรุป known limitations และตัดสิน MVP checkpoint

## No-change guard

ในช่วง audit:

- ห้ามเปลี่ยน `BasicBounceDetector`
- ห้ามเปลี่ยน threshold, Landing, cooldown หรือ Counter behavior
- ห้ามเพิ่ม permission, analytics, cloud upload หรือ remote processing
- ห้ามล้าง Training History
- หากพบ failure ให้หยุด บันทึกหลักฐาน และออกแบบงานแก้เฉพาะจุด

## A. Windows automated verification

รันจาก project root:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Acceptance:

- ทุกคำสั่งจบด้วย `BUILD SUCCESSFUL`
- ไม่มี unit-test failure
- ไม่มี lint error ที่ block release
- สร้าง Debug และ Release APK ได้

## B. Static release-boundary audit

ตรวจยืนยันว่า:

- T-743 และ diagnostic overlays ทำงานเฉพาะ `BuildConfig.DEBUG`
- Release path ไม่สร้างหรือแสดง passive trace payload
- ไม่บันทึก camera frame, pose landmark หรือข้อมูลร่างกายลงไฟล์/Log
- camera และ MediaPipe processing ยังทำงานบน background executor
- `ImageProxy` และ resources ถูกปิดตาม lifecycle
- manifest ขอเฉพาะ permission ที่จำเป็น
- ไม่มี secret, token หรือ credential ใน source
- Room ไม่มี destructive migration fallback สำหรับ Training History

## C. Device release-readiness smoke

ใช้ Debug build หลัง automated verification ผ่าน เพื่อให้ตรวจ diagnostic boundary และ behavior ได้:

1. เปิดแอปจาก cold start
2. ตรวจ Home, Settings และ History
3. ตั้ง Music OFF
4. เริ่ม Training และรอ Positioning → Countdown → GO
5. กระโดด Basic Bounce 3 ครั้ง
6. Pause → Resume และรอ GO ใหม่
7. กระโดดเพิ่ม 2 ครั้ง
8. หยุดนิ่ง 10 วินาที
9. กด Finish
10. ตรวจ Result = 5 jumps
11. เปิด History และยืนยัน session
12. ปิดและเปิดแอปใหม่ แล้วยืนยัน History ยังอยู่

Acceptance:

- Cold start, navigation และ camera preview ปกติ
- Counter รวม `5/5`
- หลังหยุด Counter ไม่เพิ่ม
- Pause/Resume กลับผ่าน Positioning และ Countdown
- Result/History ตรงกัน
- History คงอยู่หลังเปิดแอปใหม่
- ไม่มี crash, freeze, AIRBORNE freeze หรือ preview stuttering

## D. Release APK boundary check

ติดตั้ง Release APK เฉพาะเมื่อเครื่องมี signing configuration ที่ถูกต้องและ `assembleRelease` ผ่าน:

- แอปเปิดได้
- ไม่เห็น overlay ชื่อ `T-`, `V`, `TRACE`, `PERF`, gate counters หรือ diagnostic rows
- camera, Training, Result และ History ทำงานตาม production behavior
- ไม่มี debug-only menu หรือ raw evidence แสดงต่อผู้ใช้

หาก Release APK ยังไม่ได้ signing สำหรับติดตั้ง ให้บันทึกเป็น packaging follow-up; ห้ามสร้างหรือ commit keystore/รหัสผ่านลง repository

## Evidence form

```text
# T-744 MVP Release Readiness Audit

Project checkpoint:
Git status:
testDebugUnitTest:
lintDebug:
assembleDebug:
assembleRelease:
Release debug-overlay audit:
Permissions/privacy audit:

Device smoke Actual/App:
Pause/Resume:
Count increase after stopping:
Result/History:
History after restart:
Camera/preview:
Crash/freeze:

Release APK installed: Yes / No / Blocked
Release overlay absent: Pass / Fail / Not tested
Known limitations recorded:

Result: Pass / Fail / Blocked
```

## Stop rules

- Automated test, lint หรือ build ล้มเหลว: หยุดก่อน device smoke
- Counter ไม่ตรง, false count, T/L ผิดสมดุล หรือ stability ถดถอย: หยุดและเก็บหลักฐาน
- Release แสดง debug diagnostic: หยุด release acceptance
- พบ permission หรือ data retention เกินขอบเขต MVP: หยุดและเปิด privacy fix

## Completion decision

T-744 ผ่านเมื่อ automated verification, static release boundary และ device smoke ผ่าน พร้อมบันทึก limitations ที่เหลืออย่างชัดเจน หาก Release APK ติดตั้งไม่ได้เพราะยังไม่มี signing configuration ให้แยก packaging follow-up โดยไม่เก็บ keystore หรือ secret ใน Git

## Final audit checkpoint — 2026-07-31

### Confirmed passes

- Windows `testDebugUnitTest`, `lintDebug`, `assembleDebug` และ `assembleRelease` ผ่าน
- Debug device smoke, Pause/Resume, post-stop, Result/History และ persistence ผ่าน
- T-745 ปิด Release diagnostic boundary; Release smoke ได้ Actual/App `10/10` และไม่พบ diagnostic UI
- Manifest ขอ runtime permission เฉพาะ `CAMERA`; `VIBRATE` เป็น normal permission และถูกใช้กับ training feedback
- ไม่ประกาศ `INTERNET` หรือ storage permission และไม่พบ analytics, cloud upload หรือ remote processing
- Camera/MediaPipe ทำงานบนอุปกรณ์; ไม่พบเส้นทางบันทึกภาพ วิดีโอ หรือ pose landmarks
- Room schema อยู่ที่ version 1 และไม่มี `fallbackToDestructiveMigration`; schema version 2 ต้องมี explicit migration และ migration test
- ไม่พบ keystore, signing secret, token หรือ credential ที่ track ใน repository

### Release blocker

`AndroidManifest.xml` ตั้ง `android:allowBackup="true"` โดยไม่มี `dataExtractionRules` หรือ `fullBackupContent` exclusions. Android Auto Backup/device transfer จึงอาจนำ `ropeskill.db`, DataStore ที่มี nickname/settings/music URI และข้อมูลแอปอื่นออกจากอุปกรณ์ แม้ UI จะสื่อว่าข้อมูลทำงานแบบ local/on-device.

เปิด `KI-024` และเสนอ `T-746 — Local Data Backup Boundary Fix`. ตาม no-change guard ของ T-744 รอบ audit นี้ยังไม่แก้ Manifest หรือ production behavior.

### Known limitations

- Detector ยังมี repeatability variance ภายใต้บางเงื่อนไข (`KI-022` Monitoring); ให้เปิดงานใหม่เมื่อ field accuracy ต่ำกว่า 95% หรือ safety/stability guard ถดถอย
- รองรับเฉพาะ Basic Bounce, Samsung Galaxy S23 Ultra เป็นอุปกรณ์ acceptance หลัก และยังไม่มี compatibility matrix หลายรุ่น/หลาย Android version
- ข้อมูล History ที่ผู้ใช้ลบไม่สามารถกู้คืนในแอป และ schema version 2 ในอนาคตต้องมี explicit migration
- ไฟล์เพลงเป็น URI ที่ผู้ใช้เลือกผ่าน Storage Access Framework; แอปไม่คัดลอกไฟล์และ access อาจหายเมื่อไฟล์/provider เปลี่ยน
- APK ที่ใช้ Release smoke เซ็นด้วย debug certificate เพื่อรักษา History และใช้ทดสอบเท่านั้น; ยังต้องจัดทำ production signing/release packaging นอก repository ก่อนเผยแพร่

### Decision

```text
Automated verification: PASS
Debug device smoke and History persistence: PASS
Release diagnostic boundary: PASS
Release basic behavior: PASS
Permissions declaration: PASS
On-device camera/pose boundary: PASS
History migration policy: PASS for schema v1
Local-data backup boundary: FAIL — KI-024
Production signing/package: FOLLOW-UP REQUIRED
T-744 overall: BLOCKED
MVP public release readiness: NOT YET
```

หลัง T-746 ต้องรัน static manifest audit, automated verification และ upgrade-install persistence smoke ซ้ำ จากนั้นกลับมาปิด T-744; ไม่ต้องปรับ detector หรือกระโดด accuracy test เพิ่ม เว้นแต่ behavior เปลี่ยนโดยไม่คาดคิด

## Final completion — 2026-07-31

T-746 ผ่าน Windows verification, Release packaged-manifest audit และ upgrade-install persistence smoke บน Samsung Galaxy S23 Ultra:

- Release manifest มี `allowBackup="false"`, `dataExtractionRules` และ `fullBackupContent` ครบ
- การติดตั้งทับด้วย `adb install --no-incremental -r` สำเร็จ
- History เดิม 3 sessions ยังคงอยู่
- Home, Settings, Training, Finish และ Result ทำงานตามปกติ
- ไม่พบ crash, freeze หรือ preview stuttering

จึงปิด `KI-024` และนำ local-data backup boundary กลับเข้าผล audit เป็น Pass

```text
Automated verification: PASS
Debug device smoke and History persistence: PASS
Release diagnostic boundary: PASS
Release basic behavior: PASS
Permissions declaration: PASS
On-device camera/pose boundary: PASS
History migration policy: PASS for schema v1
Local-data backup boundary: PASS — T-746 / KI-024 Resolved
Known limitations: RECORDED
Production signing/package: PASS — T-747
Production-signed clean-device smoke: PASS — T-748
T-744 overall: PASS
MVP audit checkpoint: ACCEPTED
MVP public release readiness: HOLD — secure upload-key backup confirmation pending
```

T-744 ผ่านตาม completion rule เพราะ technical, privacy และ device acceptance gates ครบแล้ว, T-747 ปิด production signing/package โดยเก็บ credentials นอก repository และ T-748 ยืนยัน production-signed build บน clean Android 16 / API 36 emulator โดยไม่ถอน Debug app จากเครื่องหลัก. ยังห้ามเผยแพร่ต่อสาธารณะจนกว่าจะยืนยัน secure upload-key backup
