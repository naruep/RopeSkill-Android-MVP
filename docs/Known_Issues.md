# RopeSkill Known Issues

อัปเดตล่าสุด: 23 กรกฎาคม 2026

## สถานะ

Milestone 0–5 ผ่านส่วนหลักบน Samsung Galaxy S23 Ultra แล้ว Detector รอบสองถดถอยเป็น fast 1/10, slow 0/10 และ medium 0/10 พร้อม knee-lift false positive 5 ครั้ง จึงคืน detector รอบแรกและรอทดสอบยืนยัน

## Issue Register

| ID | วันที่ | อาการ | Severity | สถานะ | Root cause | แนวทางแก้/ขั้นถัดไป |
|---|---|---|---|---|---|---|
| KI-001 | 2026-07-22 | ยังไม่ทราบว่าโปรเจกต์ Build และ Run ได้หรือไม่ | Blocker | Fixed | ทดสอบ sample app บนอุปกรณ์จริงแล้ว | ผู้ใช้ยืนยัน Build และ Run สำเร็จบน Samsung Galaxy S23 Ultra |
| KI-002 | 2026-07-23 | Detector รอบสองนับ fast 1/10, slow/medium 0/10 และ knee lift ผิด 5 ครั้ง | High | Fix awaiting verification | `verticalMotionDifference ≤ 3.5%` ปฏิเสธ Basic Bounce จริงมากเกินไป; ตัวกรองยังไม่แยก knee lift ได้ | คืน detector รอบแรกที่ slow/medium เคยได้ 10/10 และเพิ่ม diagnostic overlay ก่อนปรับ Fast/Knee lift ทีละเงื่อนไข |
| KI-003 | 2026-07-23 | Detector ที่กู้คืนยังนับ fast 4/10, medium 8/10 และนับ knee lift ซ้าย 3/5 ขวา 5/5 | High | Investigating | Diagnostic แบบ transient ถูก `CALIBRATING` หลัง Landing เขียนทับ จึงยังไม่ทราบค่าของ Takeoff ที่ทำให้เกิด false positive | เพิ่ม `LAST COUNT` แบบค้างค่า L/R/H/Δ และ airborne time โดยไม่เปลี่ยน threshold แล้วเก็บผล Basic Bounce เทียบ knee lift บนอุปกรณ์จริง |
| KI-003 | 2026-07-23 | ผู้ใช้ต้องกด `START TRAINING` แล้วกด `START` ซ้ำในหน้า Training | Medium | Fixed | Navigation และการเปิด Ready Detection เป็นคนละคำสั่งแต่ใช้คำว่า Start เหมือนกัน | ผู้ใช้ยืนยัน Single Start และไม่มีปุ่ม Start ซ้ำผ่านบนอุปกรณ์ |
| KI-004 | 2026-07-23 | Countdown รอบสองยกเลิกจากการขยับเล็กน้อยและเริ่มใหม่บ่อยเกินไป | Medium | Fix awaiting verification | ค่า stability 2–2.5% ไวต่อ landmark jitter และ movement ปกติ | คืน behavior รอบแรกตามผลตอบรับผู้ใช้; การยกเลิกการเคลื่อนไหวบางแบบยังเป็นข้อจำกัดที่ต้องออกแบบใหม่จากข้อมูลจริง |
| KI-005 | 2026-07-23 | `LAST COUNT` ของ knee lift ขวาแสดง ΔR 0.110 ทั้งที่ source กำหนด synchronization ratio limit 0.080 | High | Investigating | ยังไม่ยืนยันว่าเกิดจาก APK revision, การอ่านค่าจากภาพ หรือ snapshot ไม่ตรงกับค่าตัดสินจริง | เพิ่ม `LAST COUNT V2` ให้แสดง DIFF, LIMIT และผล SYNC ที่ snapshot จาก Takeoff เดียวกัน โดยไม่เปลี่ยน detector |

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
