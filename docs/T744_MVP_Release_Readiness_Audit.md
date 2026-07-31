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
