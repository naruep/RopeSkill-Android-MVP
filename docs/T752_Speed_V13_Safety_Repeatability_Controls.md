# T-752 V13 Safety and Repeatability Controls

## Objective

ตรวจว่า V13 minimum-transition guard ป้องกัน false right steps ใน negative controls โดยไม่ตัด
valid right steps ในการสลับขาช้าและเร็ว และให้ผลซ้ำเดิมเมื่อวิเคราะห์ไฟล์วิดีโอเดียวกัน

V13 ยังคงเป็น Video Test diagnostic-only. การทดสอบนี้ไม่อนุมัติให้เปลี่ยน production detector
โดยอัตโนมัติ และต้องไม่แก้ `BasicBounceDetector.kt`, `SpeedStepDetector`, classifier thresholds,
Training, Room หรือ History

## Preconditions

- Commit: `d2ff4a0`
- Windows 4 gates: Pass
- Device: Samsung Galaxy S23 Ultra
- ใช้ `Video Test Mode`
- ตั้ง `GO = 2,000 ms` หรือกำหนด GO ให้ครอบคลุมเฉพาะช่วงทดสอบ แล้วบันทึกค่าจริงในผล
- กรอก manual ground truth ก่อนกดวิเคราะห์
- Export CSV ทุก run
- Video Test ต้องไม่สร้าง History

## Test order and hard gates

| Run | Video | Manual ground truth | V13 hard gate |
|---|---|---:|---:|
| C1 | Standing still | 0 | 0 |
| C2 | Both-feet hopping | 0 | 0 |
| C3 | Slow valid alternation | นับ right-foot landings จากวิดีโอ | ตรง ground truth |
| C4 | Fast valid alternation | นับ right-foot landings จากวิดีโอ | ตรง ground truth |
| D1 | Reference repeat 1 | 55 | 55 |
| D2 | Reference repeat 2 | 55 | 55 |

## Current result — 2026-08-02

| Run | Video | Ground truth / V13 | History | Stability | Status |
|---|---|---:|---|---|---|
| C1 | `StanceStill.mp4` | `0 / 0` | Not created | No crash/freeze | Pass |
| C2 | `Hopping.mp4` | `0 / 0` | Not created | No crash/freeze | Pass |
| C3 | Slow valid alternation | Pending | Pending | Pending | Next |
| C4 | Fast valid alternation | Pending | Pending | Pending | Pending |
| D1/D2 | `SpeedDemo1.mp4` repeats | Pending | Pending | Pending | Pending |

### Related evidence — C4 fixed-GO diagnostic pilot

ชื่อ `C4 fixed-GO diagnostic pilot` หมายถึงการทดลอง classifier/reference path บน
`SpeedDemo1.mp4` และ **ไม่ใช่** run `C4 Fast valid alternation` ในตารางด้านบน ดังนั้นสถานะ
C4 fast-alternation ยังคง `Pending`

ผล diagnostic: ground truth `55`, production `24`, fixed-GO alternation `53`, fixed right/left
landings `55/52` และ V13 minimum-gap `55`. Tracking valid `970/970`, low visibility `0`;
fixed reference ตรวจ right events ต่อถึง `+29.845s` แต่ production event สุดท้ายอยู่ที่
`+14.137s`. fixed-GO alternation ปฏิเสธ right ที่ `+15.193s` และ `+15.721s` เป็น
`REPEATED_RIGHT` หลัง fixed reference พลาด left สองจุด

หลักฐานนี้สนับสนุนให้ทำ C3/C4 safety controls ต่อ แต่ยังไม่อนุมัติ production replacement

C1 มี production/V10 false count `1` ที่ `27.654s` แต่ V13 ปฏิเสธเป็น
`UNCONFIRMED_ALTERNATION`. C2 มี V12 false count `1` จาก `LEFT→RIGHT` ที่ timestamp เดียวกัน
(`0ms`) แต่ V13 ปฏิเสธ right decisions ทั้งหมด. ผลทั้งสองยังเป็น diagnostic evidence เท่านั้น
และไม่อนุมัติ production replacement

ให้หยุดชุดทดสอบทันทีเมื่อ C1 หรือ C2 ได้ V13 มากกว่า 0 เพราะเป็น safety-control failure
และยังไม่ควรเสนอ threshold หรือ promotion จนกว่าจะตรวจ CSV/event timestamps ก่อน

## Recording instructions

### C1 — Standing still

1. ยืนเต็มตัวในเฟรมตลอดช่วงทดสอบ
2. ไม่กระโดด ไม่ยกเท้าสลับ และไม่เดินออกจากตำแหน่ง
3. ตั้ง ground truth `0` แล้ววิเคราะห์และ export CSV

### C2 — Both-feet hopping

1. กระโดดด้วยเท้าทั้งสองขึ้นและลงพร้อมกันอย่างต่อเนื่อง
2. ห้ามสลับเท้า เพราะ run นี้ตรวจว่าการกระโดดแบบ Basic Bounce ไม่ถูกนับเป็น Speed
3. ตั้ง ground truth `0` แล้ววิเคราะห์และ export CSV

### C3/C4 — Valid alternation

1. กระโดดสลับซ้าย–ขวาต่อเนื่อง โดยให้เท้าขวาลงพื้นเป็นจังหวะที่ต้องนับ
2. C3 ใช้จังหวะชัดและช้ากว่า reference; C4 ใช้จังหวะเร็วที่ยังควบคุมได้
3. ตรวจวิดีโอและนับ anatomical right-foot landings ด้วยตนเองก่อนวิเคราะห์
4. กรอกยอดนั้นเป็น ground truth แล้ว export CSV

### D1/D2 — Deterministic repeat

1. ใช้ไฟล์ `SpeedDemo1.mp4` เดิมและค่า GO เดิมทั้งสองรอบ
2. ห้าม trim, transcode หรือแก้ไฟล์ระหว่าง run
3. ผล V13, accepted/rejected totals, evidence totals และ decision rows ต้องเหมือนกัน

## Result form

```text
T-752 V13 Safety/Repeatability Controls
Commit: d2ff4a0
Windows 4 gates: Pass

C1 Standing:
Ground truth / V13:
History created: Yes/No
Crash/freeze: Yes/No
CSV filename:

C2 Both-feet:
Ground truth / V13:
History created: Yes/No
Crash/freeze: Yes/No
CSV filename:

C3 Slow alternation:
Ground truth / V13:
History created: Yes/No
Crash/freeze: Yes/No
CSV filename:

C4 Fast alternation:
Ground truth / V13:
History created: Yes/No
Crash/freeze: Yes/No
CSV filename:

D1/D2 Reference:
V13 count run 1 / run 2:
Accepted/rejected identical: Yes/No
Evidence totals identical: Yes/No
Decision rows identical: Yes/No
CSV filenames:
```

## Promotion gate

พิจารณา production candidate ได้เมื่อ C1–C4 และ D1–D2 ผ่านทั้งหมด, Video Test ไม่สร้าง
History, ไม่พบ crash/freeze และ CSV ไม่พบ timestamp ซ้ำหรือย้อนลำดับ หากข้อใดไม่ผ่าน ให้คง
V13 เป็น diagnostic-only และวิเคราะห์ evidence ก่อนเปลี่ยนกฎ
