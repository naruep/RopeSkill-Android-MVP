# RopeSkill Known Issues

อัปเดตล่าสุด: 24 กรกฎาคม 2026

## สถานะ

Milestone 0–5 ผ่านส่วนหลักบน Samsung Galaxy S23 Ultra แล้ว Detector รอบสองถดถอยเป็น fast 1/10, slow 0/10 และ medium 0/10 พร้อม knee-lift false positive 5 ครั้ง จึงคืน detector รอบแรกและรอทดสอบยืนยัน

## Issue Register

| ID | วันที่ | อาการ | Severity | สถานะ | Root cause | แนวทางแก้/ขั้นถัดไป |
|---|---|---|---|---|---|---|
| KI-001 | 2026-07-22 | ยังไม่ทราบว่าโปรเจกต์ Build และ Run ได้หรือไม่ | Blocker | Fixed | ทดสอบ sample app บนอุปกรณ์จริงแล้ว | ผู้ใช้ยืนยัน Build และ Run สำเร็จบน Samsung Galaxy S23 Ultra |
| KI-002 | 2026-07-23 | Detector รอบสองนับ fast 1/10, slow/medium 0/10 และ knee lift ผิด 5 ครั้ง | High | Fix awaiting verification | `verticalMotionDifference ≤ 3.5%` ปฏิเสธ Basic Bounce จริงมากเกินไป; ตัวกรองยังไม่แยก knee lift ได้ | คืน detector รอบแรกที่ slow/medium เคยได้ 10/10 และเพิ่ม diagnostic overlay ก่อนปรับ Fast/Knee lift ทีละเงื่อนไข |
| KI-003 | 2026-07-23 | Detector ที่กู้คืนยังนับ fast 4/10, medium 8/10 และนับ knee lift ซ้าย 3/5 ขวา 5/5 | High | Fix awaiting verification | หลักฐาน V3 พบ `hipRise / averageAnkleRise` ของ Basic Bounce 9 ตัวอย่างอยู่ที่ 1.45–2.42 แต่ knee lift false positive 3 ตัวอย่างอยู่ที่ 0.39–0.79 | เพิ่มตัวกรอง Takeoff ขั้นต่ำ 1.10 พร้อม unit tests และทดสอบ Slow/Medium/Fast กับ knee lift ซ้ำบนอุปกรณ์จริง |
| KI-003 | 2026-07-23 | ผู้ใช้ต้องกด `START TRAINING` แล้วกด `START` ซ้ำในหน้า Training | Medium | Fixed | Navigation และการเปิด Ready Detection เป็นคนละคำสั่งแต่ใช้คำว่า Start เหมือนกัน | ผู้ใช้ยืนยัน Single Start และไม่มีปุ่ม Start ซ้ำผ่านบนอุปกรณ์ |
| KI-004 | 2026-07-23 | Countdown รอบสองยกเลิกจากการขยับเล็กน้อยและเริ่มใหม่บ่อยเกินไป | Medium | Fix awaiting verification | ค่า stability 2–2.5% ไวต่อ landmark jitter และ movement ปกติ | คืน behavior รอบแรกตามผลตอบรับผู้ใช้; การยกเลิกการเคลื่อนไหวบางแบบยังเป็นข้อจำกัดที่ต้องออกแบบใหม่จากข้อมูลจริง |
| KI-005 | 2026-07-23 | `LAST COUNT` ของ knee lift ขวาแสดง ΔR 0.110 ทั้งที่ source กำหนด synchronization ratio limit 0.080 | High | Resolved | ค่า V1 ที่อ่านได้ไม่ตรงกับหลักฐาน Takeoff ที่ใช้ตัดสิน; V2 snapshot ตัวแปรเดียวกับเงื่อนไขโดยตรง | ผู้ใช้ยืนยัน V2 แสดง DIFF ≤ LIMIT และ SYNC PASS ครบทั้ง Basic Bounce และ knee lift; ใช้ V2/V3 เป็นหลักฐานรอบถัดไป |
| KI-006 | 2026-07-23 | กด `RESET` ในหน้า Training แล้ว Timer, Counter, Tracking และปุ่มควบคุมหยุดเหมือนแอปค้าง | High | Fix awaiting verification | `resetWorkout()` เปลี่ยนสถานะเป็น `IDLE` แต่หน้า Training ไม่มีปุ่มเริ่มสำหรับสถานะนี้ และ Pose frames จะไม่ถูกประมวลผลขณะ `IDLE` | ให้ปุ่ม `RESET` ล้าง Session แล้วเรียก `startWorkout()` ต่อทันทีเพื่อกลับสู่ `POSITIONING`; รอ Build และทดสอบบนอุปกรณ์จริง |
| KI-007 | 2026-07-23 | Countdown จบแล้วแสดง `START` แต่ Timer และ `GO!` ไม่เริ่มจนกว่าจะกระโดด | Medium | Fix awaiting verification | สถานะ `ARMED` เรียก `beginRunning()` เฉพาะเมื่อ detector พบ `TAKEOFF` | เรียก `beginRunning()` ทันทีเมื่อ Countdown จบ เพื่อให้ `GO!`, Timer และ detector เริ่มพร้อมกัน; รอทดสอบบนอุปกรณ์จริง |
| KI-008 | 2026-07-23 | Medium Basic Bounce ตรวจพบเพียง 6/10 หลังเพิ่ม hip/ankle filter | High | Resolved | ผลรอบเดียวแปรผัน; V4 ยืนยันว่า Takeoff ที่เข้า AIR ลง LAND ครบ และการทดสอบซ้ำไม่พบการถดถอยต่อเนื่อง | Medium เพิ่ม 3 รอบได้ 10/10, 9/10, 10/10 รวม 29/30; knee lift ซ้าย/ขวา 0/5 false positives จึงตรึง detector ปัจจุบันเป็น baseline |
| KI-009 | 2026-07-24 | หลัง Pause/Resume และระหว่างกระโดดเชือกต่อเนื่อง Counter พลาดหลายครั้งพร้อม diagnostic ค้าง `AIRBORNE` | High | Fix awaiting verification | Timeout ช่วยให้ไม่ค้างถาวรแต่ไม่แก้ cadence จริง; วิดีโอเป้าหมายประมาณ 125–140 jumps/min แสดงว่ารอบใหม่เริ่มก่อน ankle landmark กลับเข้า landing band ของ baseline เก่า | คง thresholds และ timeout เดิม แต่ยอมรับ Landing เมื่อเห็นวงจรขึ้น→ลงครบระยะและเริ่มขึ้นรอบถัดไป พร้อมใช้ตำแหน่งต่ำสุดของ cycle เป็น baseline รอบต่อไป; รอ unit test และ T-705 บนอุปกรณ์จริง |
| KI-010 | 2026-07-24 | กระโดดเชือกจริงที่ประมาณ 133 jumps/min นับได้ 11/20 และรอบยืนยัน V5 ได้ 16/20 แม้ไม่มี false count หลังหยุด | High | Fix awaiting verification | รอบ V5 แสดง `AIR 16 / LAND 16`; rejected cycles ทั้ง 3 มี ankle/hip ผ่าน, synchronization ผ่าน แต่ hip-to-ankle ratio `0.78`, `0.90`, `0.94` ต่ำกว่า threshold `1.10` จึงถูกปฏิเสธก่อน Takeoff | ลดเฉพาะ `MIN_HIP_TO_ANKLE_RISE_RATIO` จาก `1.10` เป็น `0.85` เพื่อรับรอบ `0.90–0.94` แต่ยังปฏิเสธ `0.78`; รอ T-707 ตรวจ accuracy และ false positives บนอุปกรณ์จริง |

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
