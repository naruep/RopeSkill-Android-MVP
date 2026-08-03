# T-733 V17 — Matched RA Candidate Shadow

สถานะ: Evaluation complete — candidates ผ่าน controls แต่ไม่มี efficacy; ไม่ promote threshold

## เป้าหมาย

เปรียบเทียบผลของ rescue ankle floor ที่เป็นผู้สมัครบน pose frames เดียวกัน โดยไม่เปลี่ยน Counter หรือ production decision:

- `BASE`: bilateral `0.010`, RA `0.020`
- `RA16`: bilateral `0.010`, RA `0.016`
- `RA15`: bilateral `0.010`, RA `0.015`

ค่า `0.016` และ `0.015` มาจาก RA-only operands ของ T-731/T-732. ค่าเหล่านี้เป็น shadow candidates เพื่อวัด benefit และ false-positive risk ไม่ใช่ production thresholds.

## Isolation

- `BasicBounceDetector.kt` ไม่มี diff
- ทั้งสาม arms รับ `PoseFrame` และ timestamp เดียวกัน
- เฉพาะผล `BASE` ถูกคืนให้ `TrainingViewModel`
- `RA16/RA15` ไม่ขับ Counter, Timer, Result, History, Room หรือ workout state
- metrics อยู่ใน memory เฉพาะ Debug และถูกล้างเมื่อ Reset/Pause/Finish
- ปิด V16 collector ระหว่าง T-733 เพื่อลด overlay และ processing ที่ไม่เกี่ยวข้อง

## Overlay

```text
T-733 RA SHADOW V17 MATCHED
BASE R0.0200 J<n> A/L<n>/<n> S<n> RES<n>
RA16 R0.0160 J<n> A/L<n>/<n> S<n> RES<n> D+<n>
RA15 R0.0150 J<n> A/L<n>/<n> S<n> RES<n> D+<n>
PROC <avg>/<max>us F<frames>
```

`D` เป็นผลต่าง shadow jump count เทียบ BASE ใน measurement เดียวกัน. ค่านี้ไม่ถูกบันทึกเป็นผลการฝึก.

## Test order

1. Windows `testDebugUnitTest` และ `assembleDebug`
2. Smoke: Basic Bounce 3 ครั้ง; BASE/RA16/RA15 ต้องมี matched state และ Counter BASE ต้องตรง App
3. Formal benefit: Basic Bounce 22 ครั้ง, Music OFF, แสงปกติ
4. Safety controls แยก session เพื่ออ่าน delta ก่อนเดินเข้าหาโทรศัพท์:
   - heel raises 20 ครั้ง
   - left knee lifts 5 ครั้ง
   - right knee lifts 5 ครั้ง
   - standing still 15 วินาที

## Decision gate

- Candidate ใดเกิด false positive ใน safety control ให้ reject candidate นั้น
- Candidate ต้องเพิ่ม Basic Bounce count โดยไม่ทำให้ `AIR/LAND`, suppression, preview หรือ stability ถดถอย
- หาก `RA15` ยังไม่กู้ miss หรือ proposal miss ยังคงมีนัยสำคัญ ให้แยก cycle/proposal diagnosis ต่อ ห้ามลด RA เพิ่มโดยอัตโนมัติ
- ต้องได้รับความเห็นชอบโดยชัดแจ้งก่อนแก้ `BasicBounceDetector` หรือ production threshold

## Device results

- Windows `testDebugUnitTest` และ `assembleDebug`: Pass
- Smoke: Actual/App 3/3; BASE/RA16/RA15 = 3/3/3, `D+0`; Result/History 3/00:24
- Formal: Actual/App 22/21; BASE/RA16/RA15 = 21/21/21, `D+0`; Result/History 21/00:31
- Formal Repeat: Actual/App 22/21; BASE/RA16/RA15 = 21/21/21, `D+0`; Result/History 21/00:34
- ทั้งสอง Formal มี `A/L21/21`, `SUP0`, หลังหยุด 0 และ stability ผ่าน จึงชี้ไปที่ proposal/cycle miss ไม่ใช่ RA rejection
- Heel raises 20, left knee lifts 5, right knee lifts 5 และ standing still: BASE/RA16/RA15 = 0 ทุก arm

## Conclusion

`RA16/RA15` ไม่เพิ่ม false positive ใน controls ชุดนี้ แต่ไม่กู้ undercount ใน Formal สองรอบ จึงไม่มี efficacy gate สำหรับ active confirmation และห้ามลด production RA. ปิด shadows หลัง T-733 และส่งต่อ KI-020 ไป T-734 passive proposal/cycle-miss trace โดยคง BASE `0.010/0.020`.
