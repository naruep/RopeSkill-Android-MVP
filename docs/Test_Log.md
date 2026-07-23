# RopeSkill Test Log

อัปเดตล่าสุด: 23 กรกฎาคม 2026

## สถานะปัจจุบัน

Milestone 0, Milestone 2, Milestone 3, Milestone 4 และ Pose overlay ใน Milestone 5 ผ่านการทดสอบบน Samsung Galaxy S23 Ultra แล้ว ผู้ใช้ยืนยันว่า Counter, Timer, Navigation, CameraX preview, permission flow, lifecycle และ pose landmarks ทำงานถูกต้อง

เส้นทาง Home → Training → Result → Home และหน้า Training/Result แบบ Power Sport ผ่านการทดสอบบนอุปกรณ์แล้ว Single Start ผ่าน แต่ detector รอบสองถดถอยเป็น fast 1/10, slow 0/10 และ medium 0/10 พร้อม knee-lift false positive 5 ครั้ง Countdown ยกเลิกได้ตลอดแต่ไวต่อการขยับเล็กน้อยเกินไป จึงคืน detector/Countdown behavior รอบแรกและเพิ่ม diagnostic overlay แบบไม่เก็บ pose data; รอทดสอบยืนยันบนอุปกรณ์ Workspace ของ Codex ไม่มี Android SDK และ Gradle Wrapper จึงยังไม่สามารถรัน `assembleDebug` ภายใน workspace ได้

## Test Environment

| รายการ | ค่า |
|---|---|
| Device model | Samsung Galaxy S23 Ultra (ยืนยันเป็นเครื่องทดสอบหลัก) |
| Android version | รอกรอก |
| CPU / RAM | รอกรอก |
| App version / commit | `0.1.0` / `a2bea75` สำหรับ Milestone 5 |
| Build type | `debug` |
| Camera | Front (build ถัดไปรอทดสอบ) |
| Lighting | รอกรอก |
| Distance from camera | รอกรอก |

## Build and Run Tests

| Test ID | วันที่ | ขั้นตอน | ผลที่คาดหวัง | ผลจริง | สถานะ |
|---|---|---|---|---|---|
| T-001 | 22 ก.ค. 2026 | Build `debug` ใน Android Studio | Build สำเร็จ | Android Studio แสดง `BUILD SUCCESSFUL` | Pass |
| T-002 | 22 ก.ค. 2026 | ติดตั้งและเปิดแอปบน Samsung Galaxy S23 Ultra | แอปเปิดโดยไม่ crash | แสดง `RopeSkill` และ `Milestone 0: Android sample app` | Pass |

## Feature Tests

| Test ID | Feature | Scenario | Expected | Actual | Status |
|---|---|---|---|---|---|
| T-101 | Counter | กดเพิ่ม Counter ระหว่าง Running | ค่าเพิ่มหนึ่งครั้งต่อการกด | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-102 | Timer | Start, Pause, Resume, Finish, Reset และ background | เวลาและสถานะเปลี่ยนถูกต้อง | ผู้ใช้ยืนยันว่าทุกขั้นตอนทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-201 | Navigation | Home → Training → Result → Home รวม Back และเริ่ม Session ใหม่ | ไปแต่ละหน้าถูกต้อง, Timer หยุดเมื่อออก และ Session ใหม่เริ่มจากศูนย์ | ผู้ใช้ยืนยันว่าทุกขั้นตอนทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-202 | Power Sport Home UI | เปิดแอป ตรวจ layout และกด Start Training | หน้า Home แสดงครบโดยไม่ล้น และปุ่มเปิดหน้า Training ได้ | ผู้ใช้ยืนยันว่าเข้าสู่หน้า Training ได้ตามเดิม; ยังไม่ได้ยืนยันรายละเอียด layout ทุกจุด | Partial Pass |
| T-203 | Power Sport Training UI | เปิด Training ตรวจกล้อง, Counter, Timer และปุ่มควบคุม | องค์ประกอบครบ ไม่ล้นจอ และการทำงานเดิมไม่เปลี่ยน | ผู้ใช้ยืนยันว่า layout, Camera preview, Pose overlay, Counter, Timer และปุ่มควบคุมผ่านทั้งหมด | Pass |
| T-204 | Power Sport Result UI | กด Finish ตรวจผล Session และกลับหน้า Home | แสดง Jumps และ Time ถูกต้อง, layout ไม่ล้น และปุ่มกลับ Home ทำงาน | ผู้ใช้ยืนยันว่า Result layout, Jumps, Time และ Back to Home ผ่านทั้งหมด | Pass |
| T-301 | Camera permission | Allow | แสดง preview | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-302 | Camera permission | Deny และกลับเข้า Training | แสดงคำอธิบาย/ทางเลือกโดยไม่ crash | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-303 | Camera lifecycle | ออกจาก Training แล้วเข้าใหม่ | กล้องถูกปล่อยและเปิดใหม่ได้ | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-401 | Pose | ยืนให้เห็นร่างกาย, เคลื่อนไหว, หมุนจอ, ออกจากเฟรมและกลับเข้า, ทดสอบ lifecycle | landmarks ติดตามร่างกายและแอปทำงานต่อเนื่อง | ผู้ใช้ยืนยันว่าทุกขั้นตอนทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-402 | Front camera | เปิด Training และยกแขนซ้าย/ขวา | preview แสดงแบบกระจกและ overlay ตรงกับร่างกายด้านเดียวกัน | รอกรอก | Not Run |
| T-501 | Basic Bounce baseline | กระโดด 10 ครั้ง จำนวน 3 รอบ | นับใกล้เคียงจำนวนจริง | นับ 1/10 ทั้ง 3 รอบในแสงปกติ; พลาดการกระโดดต่อเนื่อง | Fail |
| T-502 | Basic Bounce false positive baseline | ยืน, ย่อเข่า, ยกแขน และเดินเล็กน้อยโดยไม่กระโดด | Counter ไม่เพิ่ม | Standing 0, knee bends 1, arm movements 0, small steps 1; การเดินไปกด Pause ถูกนับ 1 | Fail |
| T-503 | Ready position | กด Start แล้วยืนเต็มตัวและนิ่ง | แสดง HOLD STILL แล้วเริ่ม countdown อัตโนมัติ | ผู้ใช้ยืนยัน Ready detection และ countdown 5–1 ทำงาน | Pass |
| T-504 | Countdown cancellation | ขยับ/ออกจากเฟรมระหว่าง 5–1 | ยกเลิกและกลับ HOLD STILL โดย Counter/Timer ยังเป็นศูนย์ | ยกเลิกได้บางช่วง แต่หลังผ่านเลข 5 การขยับบางแบบไม่ยกเลิก | Partial Pass |
| T-505 | First-jump start | รอข้อความ START แล้วกระโดด | Timer เริ่มที่ Takeoff และ Landing แรกนับเป็น 1 | ผู้ใช้ยืนยันทั้ง Timer start และ first jump | Pass |
| T-506 | Tuned Basic Bounce | กระโดด 10 ครั้งที่ fast, slow และ medium | อย่างน้อย 8/10 ต่อรอบ | Fast 4/10, slow 10/10, medium 10/10 | Fail |
| T-507 | Tuned false positives | ยืน, ย่อเข่า, ยกแขน และเดินเล็กน้อย | ไม่เกิน 1 ครั้งต่อกิจกรรม | Standing 0, knee bends 5, arm movements 0, small steps 1 | Fail |
| T-508 | Single-action start | กด `START TRAINING` ที่ Home หนึ่งครั้ง | หน้า Training เข้าสู่ HOLD STILL โดยไม่มีปุ่ม START ซ้ำ | Single Start และไม่มีปุ่ม Start ซ้ำผ่าน; Resume ยังไม่ได้ระบุผลชัดเจน | Partial Pass |
| T-509 | Detector tuning round 2 | ทดสอบ fast/slow/medium และกิจกรรม false positive ซ้ำ | Fast ≥8/10, slow/medium ≥9/10, false positive ≤1 ต่อกิจกรรม | Fast 1/10, slow 0/10, medium 0/10; knee lift 5, รายการอื่น 0 | Fail |
| T-510 | Restored detector with diagnostics | ทดสอบ fast/slow/medium, knee lift และสังเกตข้อความ DETECTOR | คืน slow/medium ≥9/10 และข้อความแสดงเหตุผลก่อน Takeoff โดยไม่เก็บ pose data | รอทดสอบ build รอบสาม | Not Run |
| T-601 | Storage | จบ Session และเปิดแอปใหม่ | ผลยังอยู่ | รอกรอก | Not Run |

## Jump Detection Accuracy Template

| Run | Ground truth jumps | Detected jumps | Missed | False positives | Accuracy note |
|---|---:|---:|---:|---:|---|
| 1 |  |  |  |  |  |
| 2 |  |  |  |  |  |
| 3 |  |  |  |  |  |

สูตรที่ใช้รายงานเบื้องต้น:

```text
Count Error (%) = abs(Detected - Ground Truth) / Ground Truth × 100
```

สูตรนี้ยังไม่เพียงพอสำหรับแยก missed jumps กับ false positives จึงต้องบันทึกทั้งสองค่าด้วยเมื่อวิเคราะห์วิดีโอหรือมีผู้ช่วยนับ

## Performance Template

| Scenario | Preview FPS | Inference FPS | Median latency | CPU | Memory | Battery/Heat | Notes |
|---|---:|---:|---:|---:|---:|---|---|
| Idle preview |  |  |  |  |  |  |  |
| Pose active 1 min |  |  |  |  |  |  |  |
| Training 10 min |  |  |  |  |  |  |  |

## Test Session Template

```text
### Test Session YYYY-MM-DD-NN
- Tester:
- Device / Android:
- App build / commit:
- Environment:
- Preconditions:
- Steps:
- Expected:
- Actual:
- Logs/screenshots:
- Privacy note: ห้ามแนบภาพบุคคลโดยไม่ได้รับความยินยอม
- Result: Pass | Fail | Blocked
- Related issue:
```
