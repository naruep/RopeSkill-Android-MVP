# T-747 — Production Release Signing and Packaging

## Objective

สร้าง Release APK และ Android App Bundle (AAB) ที่เซ็นด้วย upload key สำหรับ RopeSkill โดยเก็บ keystore และรหัสผ่านไว้นอก Git repository พร้อมตรวจ signature, certificate และ SHA-256 ก่อนพิจารณาเผยแพร่

## Security boundary

- ห้าม commit `.jks`, `.keystore`, signing properties หรือ password
- signing properties และ keystore จริงต้องอยู่นอก repository
- Gradle อ่าน path ของ properties ผ่าน `ROPESKILL_SIGNING_PROPERTIES`
- build ปกติที่ไม่มี environment variable ยังคงสร้าง unsigned Release ได้
- task `packageProductionRelease` ต้องหยุดหาก signing configuration ไม่มีหรือไม่ครบ
- ห้ามส่งภาพที่เห็น password, private key หรือเนื้อหา properties จริง

## Configuration

ตัวอย่างโครงสร้าง properties อยู่ที่ `config/release-signing.properties.example` และมีเพียง placeholder ให้คัดลอกไปไว้ในโฟลเดอร์ส่วนตัวนอก repository เช่น:

```text
C:\Users\<USER>\RopeSkill-Secrets\release-signing.properties
C:\Users\<USER>\RopeSkill-Secrets\ropeskill-upload.jks
```

ค่าที่ต้องมี:

```properties
storeFile=C:\\Users\\<USER>\\RopeSkill-Secrets\\ropeskill-upload.jks
storePassword=<SECRET>
keyAlias=ropeskill-upload
keyPassword=<SECRET>
```

ใช้รหัสผ่านเดียวกันสำหรับ keystore และ key alias เพื่อลดปัญหาความเข้ากันได้ของเครื่องมือ Android ตามคำแนะนำใน Android signing workflow

## Windows packaging

รันจาก repository:

```powershell
.\scripts\Build-T747ProductionRelease.ps1 `
    -SigningProperties "$env:USERPROFILE\RopeSkill-Secrets\release-signing.properties"
```

Script จะ:

1. ยืนยันว่า properties อยู่นอก repository
2. ยืนยันว่า Git ไม่ track keystore/signing properties
3. รัน tests, Release lint และสร้าง signed APK/AAB
4. ตรวจ APK ด้วย `apksigner`
5. ตรวจ AAB ด้วย `jarsigner` และอ่าน certificate ด้วย `keytool`
6. ตรวจ package/version, `allowBackup=false` และไม่มี `debuggable/testOnly`
7. แสดงขนาดไฟล์และ SHA-256 โดยไม่แสดง secret

Artifacts:

```text
app\build\outputs\apk\release\app-release.apk
app\build\outputs\bundle\release\app-release.aab
```

## Acceptance

- `packageProductionRelease`: `BUILD SUCCESSFUL`
- APK signature verification: Pass
- AAB signature verification: `jar verified`
- certificate fingerprint ถูกบันทึกโดยไม่เปิดเผย private key
- APK/AAB SHA-256 ถูกบันทึก
- `git status` สะอาดหลัง build
- ไม่มี secret หรือ binary signing key ถูก track
- signed artifact ผ่าน final static manifest/package audit
- ก่อน device install ต้องวางแผนเรื่อง certificate mismatch กับ Debug app เพื่อไม่ให้ History เดิมสูญหาย

Initial release identity:

```text
applicationId: com.ropeskill.app
versionCode: 1
versionName: 0.1.0
```

## Device safety

Production certificate ไม่ตรงกับ Debug certificate ที่ติดตั้งอยู่ จึงไม่สามารถติดตั้งทับ Debug app เดิมได้ Android จะปฏิเสธ update ที่ลายเซ็นต่างกัน การถอน Debug app จะลบ local History เพราะ T-746 ปิด backup แล้ว ดังนั้นห้ามถอนแอปจาก Samsung Galaxy S23 Ultra จนกว่าผู้ใช้จะอนุมัติการสูญเสียข้อมูล หรือมีอุปกรณ์/emulator แยกสำหรับ final signed-build smoke

## Final verification — 2026-07-31

Windows production packaging verification ผ่านจาก implementation commit `464bd6d`:

- `testDebugUnitTest`, `lintDebug`, `assembleDebug` และ `assembleRelease`: `BUILD SUCCESSFUL`
- สร้าง upload keystore ภายนอก repository ด้วย alias `ropeskill-upload`, `PrivateKeyEntry`, RSA 4096-bit และอายุถึง 16 ธันวาคม 2053
- `packageProductionRelease`: `BUILD SUCCESSFUL`
- APK signature verification: Pass
- AAB signature verification: `jar verified`
- Package identity: `com.ropeskill.app`, `versionCode 1`, `versionName 0.1.0`
- Release certificate SHA-256: `BE:82:53:33:68:B8:B3:62:77:C9:E4:2E:B4:9F:7C:52:A3:29:CB:80:EA:34:55:50:26:06:9C:5A:40:8C:A5:47`
- APK: `66,130,954` bytes; SHA-256 `24870A237E9777BC98E0A6F94B72A698811067E0506E3C11A331275675730538`
- AAB: `39,572,109` bytes; SHA-256 `DE0852A3AE4DFBFB9146BF71B95790863F8FF9F61101C10EB8E2B0A2684D362D`
- `git status` สะอาด และ `git ls-files` ไม่พบ `.jks`, `.keystore`, `.p12`, `.pfx` หรือ `release-signing.properties`

Signed-build device smoke ยังไม่รันบน Samsung Galaxy S23 Ultra เครื่องหลัก เพราะ production certificate ต่างจาก Debug certificate และการถอน Debug app จะลบ History เดิม ให้ใช้ clean emulator หรืออุปกรณ์ทดสอบแยกก่อนเผยแพร่ต่อสาธารณะ

```text
T-747: PASS
Production signing/package: PASS
Secret boundary: PASS
Primary-device install: NOT RUN — protected existing local History
Public distribution: HOLD until clean-device signed-build smoke and secure key backup are confirmed
```

## Official references

- Android app signing: https://developer.android.com/studio/publish/app-signing
- APK signature verification: https://developer.android.com/tools/apksigner
- Android App Bundle testing: https://developer.android.com/tools/bundletool
