# T-737 V21 — Bounded Timeout Landing Recovery

สถานะ: Code prepared; Pure-Kotlin regression `99/99` ผ่าน; รอ Android build และ real-device confirmation

## หลักฐาน

T-735 Formal Repeat `Screen_Recording_20260730_172836.mp4` ได้ Actual/App `22/21`, `Q24 M22`, `T/L22/21`, `SUP0`. หลัง jump สุดท้ายสถานะค้าง `AIRBORNE` จนครบ timeout `1,500ms` แล้วเข้า `CALIBRATING` โดยไม่เพิ่ม Counter ทั้งที่ production ยอมรับ Takeoff ครบ 22.

## การเปลี่ยนแปลง

- คง Takeoff gates และ T-736 profile `bilateral 0.008 / rescue ankle 0.016 / rescue hip 0.100`
- ที่ airborne timeout ให้ Count เฉพาะ accepted Takeoff ที่เห็น ankle และ hip descent จาก peak ครบ Landing distances
- ถ้ายังค้างสูง, descent ไม่ครบ หรือ landmarks หาย ให้ reset/calibrate โดยไม่ Count เหมือนเดิม
- เพิ่ม landing reason `TIMED_OUT_AFTER_DESCENT` (`TD` ใน Debug UI, `T` ใน trace)
- ไม่เปลี่ยน cooldown, baseline adaptation, Result/History, Room หรือ auto-pause

Screen Recording ใช้ยืนยัน trace และ state transition แต่ไม่มี raw PoseFrame input เดิม จึงไม่สามารถ replay ผ่าน MediaPipe แบบ deterministic. Benchmark `22/22` เป็นเป้าหมาย device confirmation ไม่ใช่ผล replay ที่ยืนยันแล้ว

## Validation gates

1. Windows `testDebugUnitTest` และ `assembleDebug` ต้องผ่าน
2. Smoke 3 jumps ต้องได้ `3/3`, `T/L3/3`, หลังหยุด 0
3. Safety controls: heel raises 20, knee liftsซ้าย/ขวา 5/5 และ standing 15 วินาทีต้องได้ 0
4. Formal 22 jumps: เป้าหมาย `22/22`, ยอมรับขั้นต่ำ `21/22`; `T/L` ต้องสมดุล, `SUP0`, หลังหยุด 0
5. ถ้าเห็น `TD` ต้องตรงกับ accepted final Takeoff ที่ลงจาก peak แล้วเท่านั้น; false positive ใด ๆ ให้ rollback timeout recovery
