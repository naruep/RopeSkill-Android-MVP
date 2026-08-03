# T-746 — Local Data Backup Boundary Fix

## Objective

ปิด Android cloud backup และ device-to-device transfer สำหรับข้อมูล RopeSkill เพื่อให้ Room History, DataStore profile/settings และ persisted music URI อยู่ภายในอุปกรณ์ตามขอบเขต MVP โดยไม่เปลี่ยน detector, Counter, Training lifecycle หรือโครงสร้างข้อมูล

## Implementation

- ตั้ง `android:allowBackup="false"`
- อ้าง `android:fullBackupContent="@xml/backup_rules"` สำหรับ Android 11 และต่ำกว่า
- อ้าง `android:dataExtractionRules="@xml/data_extraction_rules"` สำหรับ Android 12+
- exclude storage domains ทั้งหมดจาก cloud backup และ device transfer
- เพิ่ม `BackupBoundaryTest` ตรวจ Manifest และ rule coverage

การแก้นี้ไม่ลบข้อมูลเดิมในอุปกรณ์ การติดตั้ง APK แบบ upgrade (`adb install -r`) ต้องรักษา History เดิมไว้

## No-change guard

- ไม่แก้ `BasicBounceDetector`
- ไม่แก้ threshold, Landing, cooldown หรือ Counter
- ไม่แก้ Room schema หรือ destructive migration policy
- ไม่เพิ่ม permission, network, analytics หรือ cloud processing

## Windows verification

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

ทุกคำสั่งต้องจบด้วย `BUILD SUCCESSFUL`

## Merged Release manifest audit

หลัง `assembleRelease` ให้ตรวจ:

```powershell
$manifest = "app\build\intermediates\merged_manifests\release\processReleaseManifest\AndroidManifest.xml"
Select-String -Path $manifest -Pattern "allowBackup|fullBackupContent|dataExtractionRules"
```

Acceptance:

- `allowBackup="false"`
- `fullBackupContent="@xml/backup_rules"`
- `dataExtractionRules="@xml/data_extraction_rules"`

## Upgrade-install persistence smoke

1. ก่อนติดตั้ง ให้เปิด History และจดรายการที่มีอยู่หนึ่งรายการ
2. ติดตั้ง Debug APK แบบทับโดยไม่ถอนแอป:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = Join-Path $sdk "platform-tools\adb.exe"
$apk = "$PWD\app\build\outputs\apk\debug\app-debug.apk"

& $adb install --no-incremental -r $apk
```

3. เปิด RopeSkill และตรวจว่ารายการ History เดิมยังอยู่
4. เปิด Home, Settings และ Training จนถึง `GO`
5. ยืนนิ่ง 5 วินาทีแล้วกด Finish; ไม่ต้องทดสอบความแม่นยำด้วยการกระโดด

Acceptance:

- การติดตั้งขึ้น `Success`
- History เดิมยังอยู่หลัง upgrade install
- Home/Settings/Training เปิดได้
- ไม่มี crash/freeze หรือ preview stuttering

## Evidence form

```text
T-746 Local Data Backup Boundary Fix

HEAD:
testDebugUnitTest:
lintDebug:
assembleDebug:
assembleRelease:
Merged Release manifest:
Upgrade installation:
Previous History preserved:
Home/Settings/Training:
Crash/freeze:
Preview stuttering:
Result:
```

## Completion gate

T-746 และ KI-024 ปิดได้เมื่อ Windows verification, merged Release manifest audit และ upgrade-install persistence smoke ผ่าน จากนั้นจึงกลับไปสรุป T-744 release readiness โดย production signing/package ยังคงเป็น follow-up แยกต่างหาก

## Completion evidence — 2026-07-31

```text
HEAD: 7851c35
testDebugUnitTest: PASS
lintDebug: PASS
assembleDebug: PASS
assembleRelease: PASS
Merged Release manifest: PASS
  android:allowBackup="false"
  android:dataExtractionRules="@xml/data_extraction_rules"
  android:fullBackupContent="@xml/backup_rules"
Upgrade installation: PASS — adb install --no-incremental -r returned Success
Previous History preserved: PASS — 3 existing sessions retained
Home/Settings/Training: PASS
Finish/Result flow: PASS
Crash/freeze: No
Preview stuttering: No
Result: PASS
```

การปิด backup boundary ไม่ลบข้อมูลเดิมระหว่างการติดตั้งทับ และไม่พบ regression ในเส้นทางหลักบน Samsung Galaxy S23 Ultra จึงปิด T-746 และ KI-024 ได้ โดยไม่มีการเปลี่ยน detector, Counter หรือ Room schema
