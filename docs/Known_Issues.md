# RopeSkill Known Issues

อัปเดตล่าสุด: 22 กรกฎาคม 2026

## สถานะ

ยังไม่มีปัญหาเปิดอยู่จากการ Build หรือการทดสอบที่ได้รับการยืนยัน Milestone 0–4 ผ่านบน Samsung Galaxy S23 Ultra แล้ว ส่วน MediaPipe Pose และ overlay ยังรอทดสอบ

## Issue Register

| ID | วันที่ | อาการ | Severity | สถานะ | Root cause | แนวทางแก้/ขั้นถัดไป |
|---|---|---|---|---|---|---|
| KI-001 | 2026-07-22 | ยังไม่ทราบว่าโปรเจกต์ Build และ Run ได้หรือไม่ | Blocker | Fixed | ทดสอบ sample app บนอุปกรณ์จริงแล้ว | ผู้ใช้ยืนยัน Build และ Run สำเร็จบน Samsung Galaxy S23 Ultra |

## Risks ที่ต้องเฝ้าระวัง

รายการเหล่านี้ยังไม่ใช่ปัญหาที่เกิดขึ้นจริง:

- Camera permission ถูกปฏิเสธหรือถูกถอนระหว่างใช้งาน
- `ImageProxy` ไม่ถูกปิด ทำให้ pipeline ค้าง
- Pose inference ช้ากว่าอัตราเฟรมและเกิด frame backlog
- Rotation หรือ mirroring ทำให้ overlay ไม่ตรง
- Landmark visibility ต่ำเมื่อแสงน้อยหรือร่างกายไม่ครบเฟรม
- Basic Bounce ถูกนับซ้ำหรือพลาดจาก threshold ที่ยังไม่ผ่านการปรับ
- โทรศัพท์ร้อนหรือแบตเตอรี่ลดเร็วใน Session ยาว
- Lifecycle interruption เช่น lock screen, app background หรือสายเรียกเข้า

## วิธีบันทึก Issue ใหม่

1. บันทึกอาการที่สังเกตเห็น ไม่สรุปสาเหตุก่อนมีหลักฐาน
2. เก็บ error แรกที่มีความหมายจาก Logcat
3. ระบุขั้นตอนทำซ้ำ รุ่นอุปกรณ์ และ app build
4. เปลี่ยน Root cause เมื่อยืนยันแล้วเท่านั้น
5. บันทึกผลทดสอบหลังแก้ใน `Test_Log.md`

## Issue Detail Template

```text
### KI-XXX — ชื่อสั้น
- Date:
- Device / Android version:
- App build / commit:
- Observed symptom:
- Reproduction steps:
- First meaningful error:
- Hypotheses:
- Confirmed root cause:
- Fix:
- Verification result:
- Status: Open | Investigating | Fixed | Deferred
```
