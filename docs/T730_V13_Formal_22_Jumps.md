# T-730 V13 Formal — 22 Jumps

สถานะ: Completed — Trace/Result/History/SEALED Pass; Counter Fail 19/22

## ผลปิดรอบ

- Smoke-R2 History prerequisite: Pass — `3 jumps / 00:22`
- Formal rerun ที่มีวิดีโอครบ: Actual/App `22/19` หรือ 86.4%; undercount 3
- `ALL P88 C19 R69 S0 TR88 OV0`
- `SEG L10 W19 B0 T59`; `WIN P19 C19 R0 S0 U0`; WINDOW gate totals ทุกตัว 0
- BASE `AIR/LAND 19/19 SUP0 RES17`; หลังหยุด Counter เพิ่ม 0
- FPS 29.7; LAT 29/60ms; IN/OUT 1399/1398 และ SKIP ประมาณ 0
- `SEALED POST-EXIT +32.403`; Result/History ตรงกันที่ `19 jumps / 00:33`
- ไม่พบ preview stuttering, crash หรือ freeze
- Accepted spans `#015 +5.252..+6.555` (1,303ms) และ `#019 +8.107..+8.850` (743ms) เป็น cycle-separation candidates จาก timing correlation ไม่ใช่ causal proof
- ขั้นถัดไปใช้ `docs/T730_V14_Cycle_Separation_Trace.md`; ห้ามเปลี่ยน detector/threshold/Counter จากผลนี้โดยตรง

## เป้าหมาย

หากเกิด undercount ให้เก็บและจัดประเภท retained blocker evidence ภายใน accepted-bookend `WINDOW` เพื่อศึกษาสาเหตุของ KI-020 โดยไม่เปลี่ยน `BasicBounceDetector`, threshold, Counter, Result หรือ History

Formal เป็น Basic Bounce ต่อเนื่องชุดเดียว:

```text
Counted opening bookend 1
+ target Basic Bounce 20
+ counted closing bookend 1
= Actual 22 jumps
```

Opening และ closing bookend รวมอยู่ใน `WIN C` แต่ไม่นำมาคิด accuracy ของ target 20 ครั้ง

## Checkpoint ก่อนเริ่ม

1. เปิด `VIEW HISTORY` จาก Smoke-R2 และยืนยันรายการล่าสุดเป็น `3 jumps / 00:22`
2. หาก History ไม่ตรง ให้หยุดและส่งภาพมาตรวจ ห้ามเริ่ม Formal
3. ใช้ Samsung Galaxy S23 Ultra, Music OFF, แสงปกติ และตำแหน่งกล้องเดิมที่เห็นเต็มตัว
4. APK ต้องแสดง `T-730 TRACE V13`; ไม่ต้อง Build หรือติดตั้งใหม่หากยังเป็น V13
5. เปิด Screen Recorder ก่อนเริ่ม Session และบันทึกต่อเนื่องถึงหน้า Result และ History เพราะ trace ไม่ถูก persist และ overlay แสดงเพียง 6 rows ล่าสุดจาก capacity 512
6. เตรียมพื้นที่เชือกให้ปลอดภัยและหยุดทันทีหากมีอาการเจ็บ เวียนศีรษะ หรือผิดปกติ

## ขั้นตอนทดสอบ

1. เริ่ม Basic Bounce Session ใหม่และยืนนิ่งเต็มเฟรมตลอด Positioning/Countdown
2. หลัง `GO` ให้เห็น `T-730 TRACE V13 WAIT-ANCHOR` และยืนนิ่งต่อประมาณ 5 วินาที
3. กระโดด opening bookend 1 ครั้งให้ชัดเจนและดูว่า Counter เพิ่ม `0 → 1`
4. กระโดด target Basic Bounce 20 ครั้งต่อเนื่องด้วยจังหวะปกติ
5. กระโดด closing bookend 1 ครั้งต่อเนื่องจาก target และดูว่า Counter เพิ่มอีก exactly 1
6. ห้ามหยุดนิ่งคั่นระหว่าง opening, target และ closing เพราะ standing jitter ระหว่าง accepted anchors จะอยู่ใน `WINDOW`
7. หลัง Landing ครั้งที่ 22 ให้ยืนนิ่งเต็มเฟรม 10 วินาทีเต็ม ตรวจว่า Counter ไม่เพิ่ม และให้วิดีโอจับ overlay ชัดเจน
8. ออกด้านข้างจนพ้นเฟรมและค้างนอกเฟรมประมาณ 2 วินาทีเพื่อจับ `SEALED POST-EXIT`. สถานะนี้อาจแสดงสั้น ๆ ก่อน auto-pause ล้าง overlay
9. กลับเข้ามา กด `Finish`, ตรวจ Result และเปิด `VIEW HISTORY`

Auto-pause หลัง `SEALED POST-EXIT` เป็นพฤติกรรมที่ยอมรับได้. Auto-pause ระหว่างกระโดดหรือก่อน seal ทำให้รอบใช้ไม่ได้

## หยุดรอบและเก็บวิดีโอไว้ หาก

- History prerequisite ไม่ใช่ 3 jumps / 00:22
- opening bookend ไม่เพิ่ม Counter จาก 0 เป็น 1
- closing bookend ไม่เพิ่ม Counter exactly 1
- จำนวนจริงไม่ครบ 22 หรือลำดับ opening/target/closing ไม่แน่ใจ
- สะดุดเชือก
- Header เป็น `INVALID`
- auto-pause ระหว่าง measurement หรือก่อน seal
- Counter เพิ่มระหว่างยืนนิ่ง 10 วินาที
- Final App JUMPS มากกว่า Actual 22 ซึ่งเป็น overcount/false-positive deviation
- `OV > 0`, `WIN U > 0`, `SUP > 0` หรือ `AIR ≠ LAND`
- Result/History ไม่ตรงกับ App Counter
- crash, freeze หรือ preview กระตุกผิดปกติ

อย่าลบวิดีโอของรอบที่หยุด เพราะยังอาจช่วยวิเคราะห์ instrumentation ได้. หากจับ `SEALED POST-EXIT` ไม่ทัน ให้รายงานเป็น protocol deviation; ไม่ใช่ detector failure โดยอัตโนมัติ แต่หลักฐานอาจต้อง review หรือ retest

## สมการที่ต้องตรวจ

```text
ALL P = C + R + S
ALL P = TR                 เมื่อ OV0
SEG L + W + B + T = ALL P
WIN P = C + R + S
WIN U <= WIN R
App JUMPS = ALL C = WIN C
```

เมื่อ opening และ closing bookend ถูกนับครบและ Final App JUMPS ไม่เกิน Actual 22:

```text
Target detected = App JUMPS - 2
Target misses = 22 - App JUMPS
Target accuracy = (App JUMPS - 2) / 20
```

`App 21` เท่ากับ target 19/20 หรือ 95%. `App < 22` ไม่ได้ทำให้ trace invalid โดยอัตโนมัติ เพราะเป้าหมายของ T-730 คือเก็บหลักฐาน undercount

หาก Final App JUMPS มากกว่า 22 ให้รายงานเป็น overcount/false-positive deviation และห้ามใช้สูตร target undercount ข้างต้น

ห้ามตั้งเงื่อนไขว่า `WIN P = 22`: completed peak event ไม่ตรงกับ physical jump แบบหนึ่งต่อหนึ่ง. `LEAD`, `BOUNDARY` และ `TAIL` มากกว่า 0 ได้ และไม่ใช่ Fail อัตโนมัติ. ใช้เฉพาะ rejected events ใน `WINDOW` สำหรับ primary gate attribution. `WIN R` กับ gate totals เป็น blocker classification ไม่ใช่ causal proof. `RES` คือ Strong-Hip Rescue count และมากกว่า 0 ได้

## แบบรายงาน

```text
T-730 V13 FORMAL — 22 JUMPS
Implementation commit:
Music OFF: Yes
Lighting: Normal

History prerequisite 3 jumps / 00:22: Pass / Fail
Actual opening / target / closing: 1 / 20 / 1
Opening bookend counted 0→1: Yes / No
App before closing:
App after closing:
Final App JUMPS:

Header before exit:
Header on exit:
Standing after final Landing:
SEALED POST-EXIT captured: Yes / No

ALL P/C/R/S/TR/OV:
SEG L/W/B/T:
WIN time/P/C/R/S/U:
G SY/BL/BR/Q/SH/RA/RH:
Last 6 trace rows:
BASE A/L/SUP/RES:

Count increase after stopping:
FPS:
LAT:
IN/OUT/SKIP:
Result:
History:

Rope trip: Yes / No
Auto-pause before seal: Yes / No
Auto-pause after seal: Yes / No
Preview stuttering: Yes / No
Crash/freeze: Yes / No
Video filename:
```

FPS/LAT/SKIP ใช้เป็น performance monitoring ตาม baseline เดิม ไม่ใช่ V13-specific numeric hard gate. เกณฑ์อ้างอิงเดิมคือ FPS ประมาณ 28–30, average/peak LAT ไม่เกินประมาณ 40/150ms และ skip rate ไม่เกินประมาณ 0.10%; ต้องพิจารณาร่วมกับ preview และ stability จากวิดีโอ
