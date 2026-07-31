# T-748 — Production-Signed Clean-Device Smoke

## Objective

ยืนยันว่า Production-signed APK จาก T-747 ติดตั้งและทำงานบนสภาพแวดล้อมสะอาดได้ โดยไม่ถอน Debug app หรือทำให้ History เดิมบน Samsung Galaxy S23 Ultra สูญหาย

## Test boundary

- ใช้ Android Emulator แยกจากโทรศัพท์เครื่องหลัก
- ห้ามใช้ปุ่ม Run ของ Android Studio หลังติดตั้ง Production APK เพราะอาจติดตั้ง Debug build ทับ
- ตรวจ package identity ด้วย ADB ก่อนทดสอบ behavior
- ไม่ใช้ผลจาก Emulator ตัดสินความแม่นยำของ Basic Bounce detector
- ไม่เปลี่ยน `BasicBounceDetector`, Counter, History, Room schema หรือ production runtime

## Environment

```text
Device: Medium Phone Android Emulator
Android: Android 16 / API 36
ADB serial: emulator-5554
Package: com.ropeskill.app
versionCode: 1
versionName: 0.1.0
DEBUGGABLE: absent
```

## Test procedure

1. เปิด Production APK จากหน้า Apps ของ Emulator
2. ตรวจหน้า Home
3. กด `START TRAINING`
4. อนุญาต Camera permission
5. ตรวจว่าหน้า Training เปิดโดยไม่ crash หรือ freeze
6. ทดสอบ `Pause → Resume → Finish`
7. ตรวจหน้า Result และ History

## Result — 2026-07-31

- Production package identity: Pass
- Non-debuggable release boundary: Pass
- Home: Pass
- Training: Pass
- Camera permission: Pass
- Pause / Resume / Finish: Pass
- Result: Pass
- History: Pass
- Crash/freeze: ไม่พบ
- Samsung Galaxy S23 Ultra Debug app และ History เดิม: ไม่ถูกเปลี่ยนแปลง
- Detector accuracy: Not tested — อยู่นอกขอบเขต clean-device packaging smoke

```text
T-748: PASS
Production identity: PASS
Clean-device behavior smoke: PASS
Primary-device History protection: PASS
Public distribution: HOLD until secure upload-key backup is confirmed
```

## Follow-up

- ยืนยันว่า `ropeskill-upload.jks` และข้อมูลสำหรับ recovery ถูกสำรองในพื้นที่เข้ารหัสหรืออุปกรณ์ภายนอกที่เชื่อถือได้
- ห้าม commit, อัปโหลดสาธารณะ หรือส่ง keystore, signing properties และรหัสผ่าน
- เมื่อเลือก distribution channel ให้ทบทวน Play App Signing, release notes และ rollout policy ก่อนเผยแพร่
