# T-730 V15 — Passive Rejection & Cycle-Miss Trace

สถานะ: Prepared — Pure-Kotlin regression 79/79 Pass; รอ Android build และ device smoke

## เป้าหมาย

แยกหลักฐาน V14 Formal ที่ Actual/App 22/19 ออกเป็นสองกลุ่มอย่างตรวจสอบได้:

1. rejected proposals 2 รายการ: แสดง signed margin ของ operand เทียบ threshold ที่ detector ใช้จริง
2. cycle-separation miss 1 ครั้ง: แยก longest takeoff-to-takeoff interval เป็น previous AIR + current re-arm GAP พร้อม READY frame samples

V15 เป็น external diagnostic collector เท่านั้น ไม่เปลี่ยน `BasicBounceDetector`, BASE thresholds `0.010/0.020`, Counter, Result/History, Room หรือ production behavior และไม่เก็บภาพหรือ landmark coordinates

## ค่าที่เพิ่มบน overlay

Rejected row:

```text
D SA±x BL±x BR±x SH±x RA±x RH±x Q±x
```

- `SA`: smoothed ankle rise − standard ankle threshold
- `BL/BR`: raw left/right ankle rise − bilateral threshold
- `SH`: smoothed hip rise − standard hip threshold
- `RA`: smoothed ankle rise − rescue ankle threshold
- `RH`: smoothed hip rise − rescue hip threshold
- `Q`: smoothed hip rise − raw average ankle rise × ratio threshold
- ค่าติดลบหมายถึง operand ต่ำกว่า threshold; `SY` ยังเป็น boolean เพราะ evidence ไม่มี numeric synchronization margin

Longest accepted-cycle interval:

```text
LONG #event P#previous T<t2t>=A<previous-air>+G<rearm-gap> RF<ready-frames> E<residual>
```

- `E0` ยืนยันว่า T2T ถูกอธิบายครบด้วย previous AIR + current GAP
- ค่านี้บอกตำแหน่งของ delay แต่ไม่ใช่ detector threshold หรือ causal proof โดยลำพัง

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
2. เปิด Screen Recorder ตั้งแต่ก่อนเริ่มจนถึง History
3. หลัง `GO` รอประมาณ 5 วินาที
4. กระโดด Basic Bounce ต่อเนื่อง 3 ครั้ง
5. ยืนนิ่งเต็มเฟรม 10 วินาที; Counter ต้องไม่เพิ่ม
6. ออกด้านข้างประมาณ 2 วินาทีเพื่อจับ `SEALED POST-EXIT`
7. กลับมา Finish และเปิด `VIEW HISTORY`

Smoke Pass เมื่อ App/Result/History เป็น 3/3, `CYC N3`, `LONG` แสดง decomposition ที่ `E0`, `AIR=LAND 3/3`, `SUP0`, trace equations และ `OV0` ผ่าน โดยไม่พบ performance/stability regression

## Formal — 22 jumps

ทำได้หลัง Smoke Pass เท่านั้น:

1. รอประมาณ 5 วินาทีหลัง `GO`
2. กระโดด opening bookend 1 + target 20 + closing bookend 1 ต่อเนื่อง รวม 22
3. ห้ามหยุดคั่นระหว่าง 22 ครั้ง
4. หลัง Final Landing ยืนนิ่งเต็มเฟรม 10 วินาที
5. ออกด้านข้างประมาณ 2 วินาทีเพื่อจับ `SEALED POST-EXIT`
6. กลับมา Finish และเปิด History

## แบบรายงาน

```text
T-730 V15 — SMOKE / FORMAL
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
LONG:
Rejected WINDOW rows including D margins:
BASE A/L/SUP/RES:

SEALED POST-EXIT:
FPS:
LAT:
IN/OUT/SKIP:
Preview stuttering:
Crash/freeze:
Video filename:
```

หาก Counter ขาด/เกิน, margin row ไม่ครบ, `LONG` ไม่ลงท้าย `E0`, trace invalid/overflow, Result/History ไม่ตรง หรือเกิด stability regression ให้เก็บวิดีโอและหยุดก่อนทดสอบซ้ำหรือเสนอ production fix
