# T-735 V19 — Passive Production Takeoff Gate Trace

สถานะ: Diagnostic complete

## Objective

T-734 Formal สองรอบได้ `Q22 M21 U1 UA0` และพบ `QU ... READY` ซ้ำ. T-735 จะแยกว่า unmatched pulse:

1. จับคู่กับ production rejected peak และถูกปฏิเสธที่ gate ใด; หรือ
2. ไม่มี production completed peak evidence (`NP`) ภายในช่วงจับคู่

## Isolation

- Production BASE คง thresholds `0.010/0.020` และเป็นตัวเดียวที่ขับ Counter
- `BasicBounceDetector.kt`, Takeoff/Landing, baseline, cooldown, Result/History, Room และ auto-pause ไม่มี diff
- Collector อ่าน PoseFrame และ `BounceDetectionResult` หลัง BASE ตัดสินใจ
- ไม่มี collector output feed back เข้า detector หรือ UI state ที่ใช้ควบคุม workout
- Debug-only, bounded in-memory; ไม่เก็บภาพ วิดีโอ หรือ landmark coordinates

## Evidence matching and overlay

Production peak ใช้ smoothed landmarks ส่วน passive pulse ใช้ raw joint motion จึงอาจปิดคนละเฟรม. T-735 จับคู่ evidence ที่ใกล้ที่สุดภายใน ±120ms. Pulse ที่ยังรอจะแสดงใน `P`; เมื่อหมดหน้าต่างโดยไม่มี rejected peak จึงเปลี่ยนเป็น `NP`.

```text
T-735 TAKEOFF GATE V19 MATCHED
RAW<n> Q<n> M<n> U<n> UA<n> G<n> NP<n> P<n> T/L<n>/<n> X<n> F<n>
G SY<n> BL<n> BR<n> Q<n> SH<n> RA<n> RH<n>
#<id> +<sec> QU A<raw> H<raw> D<ms> READY <STD|RES>[<gates>] P<a> L<a> R<a> H<h>
BASE A/L<n>/<n> SUP<n> RES<n>
```

- `G`: unmatched qualified pulses ที่จับคู่ production rejected peak ได้
- `NP`: unmatched qualified pulses ที่ไม่มี production rejected peak ภายใน ±120ms
- `P`: pulses ที่ยังรอ evidence; clean snapshot สำหรับตัดสินต้องเป็น `P0`
- `SY/BL/BR/Q/SH/RA/RH`: blocker totals; หนึ่ง peakอาจมีหลาย blocker
- `STD/RES`: production standard/rescue route ตามค่า peak
- ค่า `P/L/R/H` ใน row คือ production smoothed ankle, raw left/right ankle และ smoothed hip ratios

## Test order

1. Windows: `testDebugUnitTest` และ `assembleDebug`
2. Smoke: Music OFF, แสงปกติ, Basic Bounce 3 ครั้ง, ยืนนิ่งเต็มเฟรม 10 วินาที
3. ตรวจ `3/3`, `M3`, `T/L3/3`, `P0`, หลังหยุด 0, Result/History และ performance
4. ออกจากเฟรมให้ Auto-pause แล้วกลับมากด Finish
5. เมื่อ Smoke ผ่านจึงรัน Formal 22 jumps ด้วย protocol เดียวกับ T-734 และบันทึก clean pre-exit snapshot

## Interpretation

- `Q22 M21 U1 G1 NP0 P0`: production สร้าง rejected peak; ใช้ route/blockers ใน `QU` row เป็น evidence หลัก
- `Q22 M21 U1 G0 NP1 P0`: pulse ผ่าน passive qualification แต่ production ไม่ publish completed rejected peak ภายในหน้าต่าง; วิเคราะห์ proposal observation/timing ต่อ
- `P>0`: snapshot ยังไม่ final; ยืนนิ่งต่อจนเป็น `P0`
- `Q > Actual`, standing สร้าง U หรือ evidence จับคู่ผิดจังหวะ: instrumentation invalid สำหรับ causal interpretation

ผล T-735 เป็น diagnostics เท่านั้น. ห้ามเปลี่ยน `BasicBounceDetector` หรือ production thresholds จนกว่าจะวิเคราะห์ Formal repeatability และได้รับอนุมัติชัดเจน.

## Device results

- Formal `Screen_Recording_20260730_123337.mp4`: Actual/App `22/19`; `Q24 M19 U5 UA0 G4 NP1 P0 T/L19/19`; genuine blockers `RA ×2`, `BR ×1`
- Formal Repeat `Screen_Recording_20260730_172836.mp4`: Actual/App `22/21`; clean trace `RAW60 Q24 M22 U2 UA1 G1 NP1 P0 T/L22/21 X0 F965`; gate totals `BR1 RA1 RH1`; `BASE A/L22/21 SUP0 RES13`
- Formal Repeat มี rejected preparation pulse `#12 ... READY RES[BR+RA+RH]` และ unmatched pulse `#26 ... AIR NP` แต่ production Takeoff ครบ `M22`. หลัง jump สุดท้าย detector อยู่ `AIRBORNE`, timeout แล้วเข้า `CALIBRATING`; Result/History `21/00:35`
- สรุป: Formal รอบแรกแสดง Takeoff gate misses; Formal Repeat แสดง accepted Takeoff ที่ไม่จบ Landing หนึ่งวงจร. ส่งต่อ bounded gate tuning ไป T-736 และ bounded timeout landing recovery ไป T-737
