# RopeSkill Known Issues

อัปเดตล่าสุด: 23 กรกฎาคม 2026

## สถานะ

Milestone 0–5 ผ่านส่วนหลักบน Samsung Galaxy S23 Ultra แล้ว Detector รอบแรกนับการกระโดดช้าและปานกลางได้ 10/10 แต่ fast jump ได้ 4/10 และ knee bend ถูกนับผิด 5 ครั้ง การแก้ไขรอบสองรอทดสอบจริง

## Issue Register

| ID | วันที่ | อาการ | Severity | สถานะ | Root cause | แนวทางแก้/ขั้นถัดไป |
|---|---|---|---|---|---|---|
| KI-001 | 2026-07-22 | ยังไม่ทราบว่าโปรเจกต์ Build และ Run ได้หรือไม่ | Blocker | Fixed | ทดสอบ sample app บนอุปกรณ์จริงแล้ว | ผู้ใช้ยืนยัน Build และ Run สำเร็จบน Samsung Galaxy S23 Ultra |
| KI-002 | 2026-07-23 | Detector รอบแรกนับ fast jump 4/10 และนับ knee bends ผิด 5 ครั้ง | High | Fix awaiting verification | Smoothing/cooldown อาจพลาดวงจรที่เร็ว และเงื่อนไขเดิมยังแยกการย่อเข่าจากการเคลื่อนขึ้นของร่างกายได้ไม่พอ | ลด smoothing lag/cooldown เพิ่ม vertical coherence และ horizontal-foot filter แล้วทดสอบ 3 ความเร็วใหม่ |
| KI-003 | 2026-07-23 | ผู้ใช้ต้องกด `START TRAINING` แล้วกด `START` ซ้ำในหน้า Training | Medium | Fix awaiting verification | Navigation และการเปิด Ready Detection เป็นคนละคำสั่งแต่ใช้คำว่า Start เหมือนกัน | ให้ปุ่มหน้า Home เปิด Ready Detection อัตโนมัติ นำ `START` button เริ่มต้นออก และคง `RESUME` หลัง Pause |
| KI-004 | 2026-07-23 | Countdown ยกเลิกได้ไม่ครบทุกช่วง 5–1 | Medium | Fix awaiting verification | ใช้ `trackingStatus == READY` ซึ่งยังคงเป็น READY ขณะเคลื่อนไหวบางรูปแบบ | เพิ่มสัญญาณ `isStable` จากการเคลื่อนของสะโพก/ข้อเท้าและตรวจทุก pose frame ระหว่าง Countdown |

## Risks ที่ต้องเฝ้าระวัง

รายการเหล่านี้ยังไม่ใช่ปัญหาที่เกิดขึ้นจริง:

- Camera permission ถูกปฏิเสธหรือถูกถอนระหว่างใช้งาน
- `ImageProxy` ไม่ถูกปิด ทำให้ pipeline ค้าง
- Pose inference ช้ากว่าอัตราเฟรมและเกิด frame backlog
- Rotation หรือ mirroring ของกล้องหน้าอาจทำให้ overlay ไม่ตรงจนกว่าจะผ่านการทดสอบจริง
- Landmark visibility ต่ำเมื่อแสงน้อยหรือร่างกายไม่ครบเฟรม
- Basic Bounce อาจยังถูกนับซ้ำหรือพลาดจาก threshold รอบใหม่ที่ยังไม่ผ่านการทดสอบจริง
- การเคลื่อนโทรศัพท์ระหว่าง Session อาจเลื่อน ankle baseline และทำให้เกิด false positive
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
