# T-743 — Passive Landing/State Trace

สถานะ: Implemented locally — static checks pass; Windows tests/build required

Implementation checkpoint: pending commit

## Implementation summary

- เพิ่ม read-only `LandingStateEvidence` ใน production result เฉพาะ AIR/Landing path เพื่อเผย exact operands ที่ decision เดิมคำนวณแล้ว
- เพิ่ม `T743PassiveLandingStateCollector` แบบ debug-only หลัง production decision
- เก็บ frame evidence สูงสุด 96 รายการ, AIR interval สูงสุด 24 รายการ และ AIR pulse สูงสุด 24 รายการ
- AIR pulse เก็บ sequence, interval, peak frame/time และ Landing operands ณ peak; pulse ที่ match production Takeoff จะไม่ถูกนับซ้ำ
- overlay `T-743 LANDING STATE V23` แสดง counts, close reasons, operand/component pass และ latest AIR pulse แบบตัวเลข/boolean เท่านั้น
- reset observer พร้อม Start, Pause/Auto-pause, Finish, Reset และ Countdown cancellation
- ปิด runtime T-735 collector เพื่อลด diagnostic overhead และป้องกัน overlay ซ้อน; production detector ไม่เปลี่ยน
- เพิ่ม tests สำหรับ observer on/off parity, exact operands, close reason, bounded memory, AIR pulse pairing, reset/disabled mode และ formatter

Gradle verification ใน workspace ถูกบล็อกก่อน compile เพราะไม่มี cached Gradle `9.3.0` และ network policy ไม่อนุญาต `services.gradle.org`; ต้องรัน `testDebugUnitTest` และ `assembleDebug` บน Windows ก่อนติดตั้ง APK หรือทดสอบโทรศัพท์

## เป้าหมาย

เติม evidence gap จาก T-742 สำหรับ missed cycles กลุ่ม `STATE_OR_LANDING_EVIDENCE` โดยบันทึก operand และ phase transition ของ Landing แบบ passive โดยไม่ขับ Counter, detector state, Result, History หรือ storage

T-742 ตรวจ 5 sessions รวม 110 physical cycles / 90 app counts และจัด 20 misses เป็น visible takeoff-gate rejection 10, AIR/state-or-landing evidence 9 และ proposal absent/uncertain 1. ไม่มี clothing-specific, performance หรือ stability pattern

## Scope

เพิ่ม debug-only observer ที่รับ `PoseFrame`, timestamp และผล production หลัง `BasicBounceDetector` ตัดสินแล้ว. Observer ต้องไม่คืน detection decision และห้ามแก้:

- `BasicBounceDetector` thresholds หรือ phase transitions
- takeoff/rescue gates
- Landing distances, timeout หรือ cooldown
- Counter, UI state machine, Result/History และ Room storage
- camera pipeline หรือ MediaPipe configuration

## Required passive evidence

ต่อ production Takeoff/AIR interval ให้เก็บแบบ bounded ring buffer:

1. sequence และ elapsed time
2. production phase/status ก่อนและหลัง frame
3. current production Takeoff/Landing count
4. ankle/hip delta เทียบ baseline และ limit ที่ production ใช้
5. `returnedToBaseline` พร้อม ankle/hip component pass
6. `descendedFromPeak` พร้อม ankle/hip component pass
7. `startedNextRise` พร้อม ankle/hip direction pass
8. `completedVerticalCycle`
9. airborne duration, `airborneTooLong`, `recoveredLandingAfterTimeout`
10. production event: none, Takeoff, Landing counted, Landing suppressed หรือ reset/calibrate
11. passive motion pulse sequence ที่เกิดขณะ production ยัง `AIRBORNE`

ข้อมูลบนหน้าจอต้องสรุปเฉพาะค่าตัวเลข/boolean และ sequence; ห้ามบันทึกภาพ, raw landmarks หรือข้อมูลระบุตัวบุคคลลงไฟล์หรือ Git

## Safety controls

- Pure-Kotlin tests ยืนยัน observer parity: เมื่อเปิด/ปิด diagnostic ต้องได้ production result เท่ากันทุก frame
- `BasicBounceDetector.kt` และ production constants ต้องไม่มี behavioral diff
- bounded memory และไม่มี per-frame unbounded list
- diagnostic reset พร้อม Session reset/Finish/Auto-pause
- Smoke 3 jumps ต้องได้ Counter 3, T/L 3/3, SUP0 และ trace ปิดครบ
- Standing, knee lifts ซ้าย/ขวา และ heel raises ต้องไม่เกิด false count
- performance, preview, Auto-pause, Result/History และ post-stop behavior ต้องไม่ถดถอย

## Device evidence plan

หลัง tests/build ผ่าน ให้รันตามลำดับ:

1. Smoke 3 jumps
2. Safety controls ชุดเดิม
3. Formal 22 jumps หนึ่งรอบ
4. หาก Formal ต่ำกว่า 21/22 ให้หยุดและส่งวิดีโอ; ไม่ทำ repeat
5. หาก Formal อย่างน้อย 21/22 และ controls ผ่าน จึงพิจารณา repeat หนึ่งรอบหลังพัก

## Acceptance

T-743 ผ่านเมื่อ:

- observer ไม่เปลี่ยน production output หรือ behavior
- ทุก production AIR interval มี close reason หรือ explicit evidence gap
- physical pulse ระหว่าง AIR จับคู่กับ Landing operands/phase ได้โดยไม่อาศัย aggregate label
- debug overlay อ่านได้และไม่ล้นจนข้อมูลหลักหาย
- tests/build และ device safety/stability controls ผ่าน

## Stop rules

- ห้ามปรับ threshold หรือ Landing logic ใน T-743
- ห้ามใช้ Screen Recording เป็น deterministic `PoseFrame` replay
- หาก diagnostic overhead ทำให้ FPS/LAT/SKIP หรือ preview ถดถอย ให้หยุดก่อน Formal
- หาก controls มี false count หรือ T/L/SUP ผิด ให้หยุดและ revert diagnostic checkpoint

## Result form

```text
# T-743 Passive Landing/State Trace

Project checkpoint:
Detector baseline: 752af1d
Observer parity tests:
Android tests/build:
BasicBounceDetector behavioral diff: None / Present

Run:
Actual/App:
T/L/SUP:
AIR intervals:
Closed by baseline:
Closed by vertical cycle:
Closed by timeout recovery:
Reset/calibrate:
Physical pulses while AIR:
Unresolved:
Controls:
Performance/stability:
Result/History:

Result: Pass / Fail / Blocked
```
