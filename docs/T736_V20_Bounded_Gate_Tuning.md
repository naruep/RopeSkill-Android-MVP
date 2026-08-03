# T-736 V20 — Bounded Production Gate Tuning

สถานะ: Candidate prepared; Pure-Kotlin regression 97/97 ผ่าน; รอ Windows build และ real-device confirmation

## หลักฐานที่ใช้

- T-735 Formal `Screen_Recording_20260730_123337.mp4`: Actual/App `22/19`
- Misses ขณะ production อยู่ `READY`: `RES[RA]` สองครั้งที่แสดง `P0.017` และ `P0.019`; `RES[BR]` หนึ่งครั้งที่แสดง `R0.010` แต่ค่าจริงต่ำกว่า floor เดิมเล็กน้อย
- T-735 Smoke/diagnostic เดิมมี low-hip pulses `H0.042` และ `H0.071`; จึงห้ามลด rescue hip floor
- T-733 controls: RA `0.016/0.015` ไม่เพิ่ม count ใน heel raises 20, knee lifts ซ้าย/ขวา 5/5 และ standing
- T-729 bilateral `0.006` ไม่เพิ่ม count ใน matched continuous run; ใช้เป็น safety lower bound ไม่ใช่ production target

Screen Recording ใช้ยืนยัน ground truth, timing และ overlay evidence เดิม แต่ไม่ถือเป็น landmark replay ผ่าน MediaPipe ใหม่ เพราะไฟล์ไม่มี PoseFrame inputs เดิมทุกเฟรม

## การเปลี่ยนแปลง

| Gate | เดิม | T-736 | เหตุผล |
|---|---:|---:|---|
| Individual ankle rise | `0.010` | `0.008` | ครอบคลุม rejected side ที่แสดง `0.010` หลังปัดเศษ โดยยังสูงกว่าค่า experimental safety floor `0.006` |
| Strong-hip rescue ankle | `0.020` | `0.016` | ครอบคลุม Formal operands `0.017/0.019` และอยู่ใน candidate ที่ผ่าน controls |
| Strong-hip rescue hip | `0.100` | `0.100` | คงไว้เพื่อปฏิเสธ low-hip preparation/heel-like motion |
| Standard ankle / hip | `0.045/0.060` | ไม่เปลี่ยน | ไม่มีหลักฐานว่าเป็น blocker หลัก |
| Hip-to-ankle ratio | `0.85` | ไม่เปลี่ยน | ไม่มีหลักฐานให้ผ่อน |

Landing, cooldown, baseline adaptation, synchronization, Counter, Result/History, Room, auto-pause และ Music ไม่มีการเปลี่ยน

## Offline validation

- Pure-Kotlin regression `97/97 Pass`
- T-736 boundary tests รับ rescue `0.016–0.019` และ individual ankle ใกล้ recorded `0.010`
- Synthetic left/right knee lift, heel raise และ standing controls ยังคง 0
- Historical T-729/T-730/T-733 baselines ถูกระบุ explicit ที่ `0.010/0.020` เพื่อป้องกันการปะปนกับ production candidate
- Debug overlay เปลี่ยนเป็น `T-736 TUNED GATE V20` และประเมิน RA ด้วย production `0.016`

## Gate ก่อนใช้งานจริง

1. Windows: `testDebugUnitTest` และ `assembleDebug` ต้องผ่าน
2. Smoke 3 jumps: App `3/3`, หลังหยุด 0, `T/L3/3`, `P0`, ไม่มี crash/freeze/stutter
3. Safety controls: heel raises 20, knee lift ซ้าย/ขวา 5/5 และ standing 15 วินาทีต้องได้ 0
4. Formal 22 jumps ใช้ยืนยันเพียงหนึ่งรอบหลัง Smoke/controls ผ่าน; เป้าหมายอย่างน้อย `21/22`

หาก control ใดเกิด false positive ให้ rollback candidate ทั้งคู่และวิเคราะห์ blocker interaction ก่อน ไม่ลด RH หรือ gate อื่นเพิ่ม
