# T-750 — Production 16 KB Page-Size Compatibility Audit

## Objective

ยืนยันว่า Production APK/AAB ของ RopeSkill รองรับอุปกรณ์ Android ที่ใช้ memory page size 16 KB โดยตรวจทั้ง artifact และ runtime จริง และไม่เปลี่ยน `BasicBounceDetector`

## Baseline

- Repository: `naruep/RopeSkill-Android-MVP`
- Branch: `fix/adaptive-icon-safe-zone`
- Starting checkpoint: `568bba4`
- Package: `com.ropeskill.app`
- Version: `0.1.0` (`versionCode 1`)
- `compileSdk 36` / `targetSdk 36`
- AGP: `8.13.2`
- MediaPipe: `tasks-vision 0.10.35`
- App-owned native C/C++: ไม่มี
- Third-party native dependency: MediaPipe

## Acceptance gates

### A. Production artifact

- Production packaging, signature และ secret boundary ผ่านตาม T-747
- `bundletool dump config` แสดง `PAGE_ALIGNMENT_16K`
- `zipalign -c -P 16 -v 4` ผ่านกับ Production APK
- native `.so` ทุกไฟล์สำหรับ `arm64-v8a` และ `x86_64` มี ELF `LOAD` alignment ไม่น้อยกว่า `2**14`
- native `.so` ที่ตรวจมี `GNU_RELRO`

### B. 16 KB runtime

- Emulator ใช้ 16 KB system image และ `adb shell getconf PAGE_SIZE` คืนค่า `16384`
- ติดตั้ง Production-signed APK ได้โดยไม่ใช้ Android Studio Run
- package identity คือ `versionCode 1`, `versionName 0.1.0` และไม่พบ `DEBUGGABLE`
- ไม่พบ page-size compatibility warning
- Home, Training, Camera permission, Pause/Resume/Finish, Result และ History ผ่าน
- ไม่พบ native-linker error, crash หรือ freeze

Detector accuracy ไม่อยู่ในขอบเขต T-750 เพราะ Emulator ไม่ใช่สภาพทดสอบการกระโดดจริง

## Audit automation

ใช้ `scripts/Test-T750PageSizeCompatibility.ps1` บน Windows ที่มี Production signing configuration เดิม, Android SDK Build Tools 35.0.0+, Android NDK และ `bundletool`

รอบแรกตรวจและสร้าง Production artifacts:

```powershell
cd "C:\Users\narue\Desktop\RopeSkill-Android-MVP-work"

.\scripts\Test-T750PageSizeCompatibility.ps1 `
    -SigningProperties "C:\path\outside\repo\release-signing.properties"
```

หาก script หา `bundletool` ไม่พบ ให้ดาวน์โหลด `bundletool-all.jar` จาก official bundletool releases แล้วเพิ่ม:

```powershell
-BundletoolJar "$env:USERPROFILE\Downloads\bundletool-all.jar"
```

รอบที่สอง หลังสร้างและเปิด 16 KB Emulator:

```powershell
adb devices

.\scripts\Test-T750PageSizeCompatibility.ps1 `
    -SigningProperties "C:\path\outside\repo\release-signing.properties" `
    -BundletoolJar "$env:USERPROFILE\Downloads\bundletool-all.jar" `
    -SkipBuild `
    -EmulatorSerial "emulator-5556"
```

ห้ามส่ง keystore, signing properties หรือรหัสผ่านมาเป็นหลักฐาน ให้ส่งเฉพาะผล audit ที่ไม่มี secret และผล manual smoke

## Static assessment

- AGP `8.13.2` สูงกว่า minimum `8.5.1` ที่รองรับ 16 KB ZIP alignment
- MediaPipe release line ตั้งแต่ `0.10.26` ระบุ Android packages รองรับ 16 KB; RopeSkill ใช้ `0.10.35`
- Production APK ผ่าน `zipalign -c -P 16 -v 4`
- Production AAB รายงาน `PAGE_ALIGNMENT_16K`
- native `.so` ที่ตรวจผ่าน ELF `LOAD >= 2**14` และ `GNU_RELRO`

## Runtime and manual-smoke evidence

- ใช้ `16 KB Page Size Google APIs Intel x86_64 Atom System Image`, Android 15 / API 35
- `adb shell getconf PAGE_SIZE` คืนค่า `16384`
- automated runtime audit ติดตั้งและเปิด Production-signed APK สำเร็จ พร้อมผล `Automated T-750 checks passed on PAGE_SIZE=16384.`
- ผู้ใช้ยืนยัน manual smoke: Home, Start Training, Camera permission, Pause/Resume, Finish, Result และ History ผ่านทั้งหมด
- ไม่พบ 16 KB compatibility warning, native-linker error, crash หรือ freeze
- ไม่ได้ใช้ Emulator smoke ตัดสินความแม่นยำของ detector

## Current decision

```text
T-750: PASS
Static dependency assessment: COMPATIBLE BY VERSION
Production artifact audit: PASS
16 KB automated runtime audit: PASS
16 KB manual behavior smoke: PASS
KI-025: RESOLVED
Public release: HOLD — KI-026 and Play Console/store preparation remain
```

## Official references

- Android 16 KB page-size support and verification: https://developer.android.com/guide/practices/page-sizes
- bundletool releases: https://github.com/google/bundletool/releases
- MediaPipe releases: https://github.com/google-ai-edge/mediapipe/releases
