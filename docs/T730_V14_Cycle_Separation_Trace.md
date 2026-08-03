# T-730 V14 — Passive Cycle-Separation Trace

สถานะ: Completed — Instrumentation/Smoke Pass, Formal Counter Fail

## เป้าหมาย

ตรวจว่า undercount แบบ `22 → 19` สัมพันธ์กับ accepted cycle ที่อยู่ `AIRBORNE` นาน, Landing/re-arm ช้า หรือปิด cycle ด้วย next-rise path หรือไม่ โดยใช้ external diagnostic collector เท่านั้น

V14 ไม่เปลี่ยน `BasicBounceDetector`, thresholds `0.010/0.020`, Counter, Result, History, Room หรือ production behavior และไม่เก็บภาพ/landmark coordinates

## ค่าที่เพิ่มบน overlay

```text
CYC N<count> AIR M<median> X<max>#<event>
GAP M<median> X<max>#<event>
T2T M<median> X<max>#<event>

CY A<airborne-ms>/F<airborne-samples>
G<rearm-ms>/<ready-samples>
T<takeoff-to-takeoff-ms>
LR<R|C|B|?>
CI<count-interval-ms>
```

- `AIR`: Takeoff ถึง Landing
- `GAP`: Landing ก่อนหน้าถึง Takeoff ปัจจุบัน
- `T2T`: Takeoff ก่อนหน้าถึง Takeoff ปัจจุบัน
- `LR R/C/B`: Returned-to-Baseline / Completed-Vertical-Cycle / Both
- ค่า maximum เป็น candidate สำหรับเทียบวิดีโอ ไม่ใช่ causal proof หรือ threshold ใหม่

## Build checkpoint บนเครื่องผู้ใช้

```powershell
Set-Location 'C:\Users\narue\Desktop\RopeSkill-Android-MVP-work'
git status --short --branch
git log --oneline --decorate -5
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

ต้องผ่านทั้งสองคำสั่งและ `BasicBounceDetector.kt` ต้องไม่มี diff ก่อนติดตั้ง APK

## Smoke — 3 jumps

1. Samsung Galaxy S23 Ultra, Music OFF, แสงปกติ และเห็นเต็มตัว
2. เปิด Screen Recorder ตลอด Session ถึง Result และ History
3. หลัง `GO` รอประมาณ 5 วินาที
4. กระโดด Basic Bounce 3 ครั้งต่อเนื่องโดยไม่หยุดคั่น
5. ยืนนิ่งเต็มเฟรม 10 วินาที; Counter ต้องไม่เพิ่ม
6. ออกด้านข้างและอยู่นอกเฟรมประมาณ 2 วินาทีเพื่อจับ `SEALED POST-EXIT`
7. กลับมา Finish และเปิด `VIEW HISTORY`

Smoke Pass เมื่อ App/Result/History เป็น 3/3 ตรงกัน, `CYC N3`, `AIR=LAND 3/3`, `SUP0`, trace equations ผ่าน, `OV0`, Counter หลังหยุดเพิ่ม 0 และไม่พบ performance/stability regression

## Formal — 22 jumps

ทำได้หลัง Smoke Pass เท่านั้น:

1. รอประมาณ 5 วินาทีหลัง `GO`
2. กระโดด opening bookend 1 + target 20 + closing bookend 1 ต่อเนื่อง รวม 22
3. ห้ามยืนหยุดคั่นระหว่าง 22 ครั้ง
4. หลัง Final Landing ยืนนิ่งเต็มเฟรม 10 วินาที
5. ออกด้านข้างประมาณ 2 วินาทีเพื่อจับ `SEALED POST-EXIT`
6. กลับมา Finish และเปิด History

## แบบรายงาน

```text
T-730 V14 — SMOKE / FORMAL
Implementation commit:
Actual/App:
Count increase after stopping:
Result:
History:

ALL P/C/R/S/TR/OV:
SEG L/W/B/T:
WIN P/C/R/S/U:
WINDOW gate totals:
CYC N AIR M/X#:
GAP M/X#:
T2T M/X#:
Last 6 rows including CY:
BASE A/L/SUP/RES:

SEALED POST-EXIT:
FPS:
LAT:
IN/OUT/SKIP:
Preview stuttering:
Crash/freeze:
Video filename:
```

หาก Counter ขาดหรือเกิน, trace invalid/overflow, Result/History ไม่ตรง, auto-pause ก่อน seal หรือวิดีโอไม่ยืนยันจำนวนจริง ให้เก็บวิดีโอและหยุดวิเคราะห์ก่อนทดสอบซ้ำหรือเสนอ detector change

## ผลยืนยันบนอุปกรณ์จริง

- Implementation commit `73f3608`
- Windows `testDebugUnitTest` และ `assembleDebug`: Pass
- Smoke: Actual/App 3/3, `CYC N3`, `T2T M498 X499`, หลังหยุดเพิ่ม 0, Result/History Pass, `SEALED POST-EXIT`, stability Pass
- Formal: Actual/App 22/19 (86.4%), หลังหยุดเพิ่ม 0, Result/History 19 jumps / 00:28, `SEALED POST-EXIT`, stability Pass
- Formal window: `P21 C19 R2`; มี 1 physical cycle ที่ไม่สร้าง proposal และ 2 rejected proposals (`RA2`, โดยหนึ่งรายการมี `BR1`)
- Longest cycle evidence: `T2T X1027#038`, `GAP X862#038`; longest AIR `404#024`

V14 จึงผ่านเป้าหมาย instrumentation และจำกัดสาเหตุ undercount 3 ครั้งเป็น 1 cycle-separation miss + 2 gate rejections แต่ยังไม่แสดง numeric margin ของ rejected gates หรือ exact phase decomposition ของ longest T2T จึงดำเนินต่อด้วย V15 passive trace โดยไม่เปลี่ยน production logic
