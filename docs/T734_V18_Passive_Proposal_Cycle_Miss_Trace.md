# T-734 V18 — Passive Proposal/Cycle-Miss Pulse Trace

สถานะ: Complete — Windows tests/build, Smoke และ Formal repeatability จบแล้ว

## Results

- Windows `testDebugUnitTest`, `assembleDebug`, push และ `installDebug`: Pass
- Smoke: Actual/App `3/3`; `RAW59 Q6 M3 U3 UA0 T/L3/3 X0`; unmatched ทั้ง 3 อยู่ช่วง `READY` ก่อน/นอก jump set; หลังหยุด 0; Result/History `3 / 00:23`; Auto-pause และ stability ผ่าน
- Formal: Actual/App `22/21`; clean pre-exit `RAW53 Q22 M21 U1 UA0 T/L21/21 X0`; unmatched `#36 +18.211 QU A0.059 H0.371 D232 READY`; หลังหยุด 0; Result/History `21 / 00:34`; Auto-pause และ stability ผ่าน
- Formal Repeat: Actual/App `22/21`; clean pre-exit `RAW39 Q22 M21 U1 UA0 T/L21/21 X0`; unmatched `#39 +17.819 QU A0.075 H0.368 D199 READY`; หลังหยุด 0; Result/History `21 / 00:32`; Auto-pause และ stability ผ่าน

ทั้งสอง Formal เห็น physical pulse ครบ `Q22` แต่ production Takeoff เพียง `M21`; miss เป็น `QU ... READY` และ `UA0` ซ้ำ จึงปิดสมมติฐาน AIRBORNE cycle lock สำหรับสองรอบนี้ และส่งต่อ T-735 เพื่อจับคู่ `QU` กับ production peak/gate evidence.

## Objective

แยก undercount ที่ T-733 พบซ้ำ `22/21` ว่าเกิดจาก:

1. มี kinematic motion pulse แต่ production BASE ไม่สร้าง Takeoff ขณะยัง `GROUNDED`; หรือ
2. เกิด motion pulse รอบใหม่ขณะ production BASE ยัง `AIRBORNE` จาก cycle ก่อน จึงรวม physical jumps มากกว่าหนึ่งครั้งไว้ใน cycle เดียว; หรือ
3. Pose result ไม่แสดง pulse ที่ผ่าน evidence floor ทำให้ต้องตรวจ sampling/landmark quality ต่อ

## Isolation

- ปิด T-733 RA shadows; APK ใช้ production BASE `0.010/0.020` เพียง detector เดียว
- `BasicBounceDetector.kt`, thresholds, Takeoff/Landing state machine และ cooldown ไม่มี diff
- Collector อ่าน PoseFrame และ `BounceDetectionResult` หลัง BASE ตัดสินใจแล้ว
- Collector ไม่คืน decision, count หรือ state ใดเข้า detector, `TrainingViewModel`, Counter, Result, History หรือ Room
- เก็บเฉพาะ normalized ratios, elapsed time, enum และ counters แบบ bounded in-memory ของ Debug session; ไม่เก็บภาพ วิดีโอ หรือ landmark coordinates

## Evidence floor and overlay

Trace ใช้ broad evidence floor `ankle ≥ 0.006` และ `hip ≥ 0.040` เพื่อคัด pulse สำหรับ video correlation. ค่านี้ไม่ใช่ detector threshold และจำนวน `Q` ไม่ใช่ App jump count.

```text
T-734 PULSE TRACE V18 MATCHED
RAW<n> Q<n> M<n> U<n> UA<n> T/L<n>/<n> X<n> F<n>
#<id> +<sec> <Q|r><M|U> A<ratio> H<ratio> D<ms> <READY|AIR>
BASE A/L<n>/<n> SUP<n> RES<n>
```

- `RAW`: upward/downward pulses ทั้งหมดที่ trace ปิดได้
- `Q`: pulses ที่ผ่าน broad evidence floor
- `M`: qualified pulse ที่ overlap production Takeoff
- `U`: qualified pulse ที่ไม่มี production Takeoff
- `UA`: unmatched qualified pulse ซึ่ง peak เกิดขณะ production BASE รายงาน `AIRBORNE`
- `T/L`: production Takeoff/Landing events
- `X`: pulse ที่ถูกตัดเพราะ full-body landmarks หาย
- `QM/QU`: qualified matched/unmatched; `rM/rU` คือ raw pulse ต่ำกว่า evidence floor
- overlay รักษา unmatched qualified ล่าสุดสูงสุด 3 รายการและเติมด้วย recent rows รวมไม่เกิน 6 รายการ

## Test order

1. Windows: `testDebugUnitTest` และ `assembleDebug`
2. Smoke: Music OFF, แสงปกติ, Basic Bounce 3 ครั้ง, ยืนนิ่ง 10 วินาที
3. Formal: Basic Bounce 22 ครั้งต่อเนื่อง, ยืนนิ่ง 10 วินาที, บันทึก final overlay ก่อนออกจากเฟรม
4. ตรวจ Result/History, FPS/LAT/IN/OUT/SKIP, preview และ crash/freeze

## Interpretation

- `Actual 22`, `Q22`, `M21`, `U1`, `UA1`, `T/L21/21`: หลักฐานสนับสนุน cycle merge/next pulse ระหว่าง BASE ยัง AIRBORNE; ต้อง video-correlate row `QU ... AIR`
- `Actual 22`, `Q22`, `M21`, `U1`, `UA0`: มี qualified pulse ที่ BASE ไม่สร้าง Takeoff ขณะไม่ AIRBORNE; ตรวจ gate/proposal timing รอบ row `QU`
- `Actual 22`, `Q21`, `M21`, `U0`: trace ไม่เห็น pulse ที่ผ่าน evidence floorสำหรับครั้งที่ขาด; ยังสรุป cycle miss ไม่ได้และต้องตรวจ raw pulse, sampling หรือ landmark quality
- `Q > Actual` หรือ standing สร้าง `U`: evidence floor/trace มี jitter contamination; ห้ามใช้ Q เป็นจำนวน jump หรือเสนอ production change

ผล T-734 เป็น diagnostic evidence เท่านั้น. ต้องวิเคราะห์ device video ก่อนเสนอ experiment ที่แตะ detector และต้องได้รับความเห็นชอบโดยชัดแจ้งก่อนแก้ `BasicBounceDetector` หรือ production state/threshold.
