# T-738 V22 — Bounded Asymmetric Ankle Rescue

สถานะ: Candidate prepared; รอ Windows build และ real-device verification

## หลักฐาน

T-737 Formal Repeat `Screen_Recording_20260730_181539.mp4`:

- Actual/App `22/21`
- `T/L21/21`, `UA0`, `X0`, `SUP0`
- Genuine miss `#23` ถูก `BR` ปฏิเสธก่อน Takeoff
- Operands: `P0.036 L0.069 R0.005 H0.145`

## Production change

คงเส้นทาง Standard และ Strong-Hip Rescue เดิมทั้งหมด แล้วเพิ่ม asymmetric rescue เมื่อผ่านทุกข้อ:

1. bilateral floor `0.008` ผ่านเพียงข้างเดียว
2. ข้อเท้าข้างเด่น rise อย่างน้อย `0.060`
3. ข้างอ่อนยัง rise อย่างน้อย `0.004`
4. smoothed ankle rise อย่างน้อย `0.016`
5. smoothed hip rise อย่างน้อย `0.120`
6. ผ่าน synchronization limit `0.080`
7. ผ่าน hip-to-ankle ratio `0.85`

ไม่เปลี่ยน Landing, timeout recovery, cooldown, baseline adaptation, Counter, Result, History หรือ Room

## Verification order

1. Windows `testDebugUnitTest`
2. Windows `assembleDebug`
3. Smoke — Basic Bounce 3 jumps
4. Safety Controls — standing 10 sec, left/right knee lifts 5/5, heel raises 10
5. Formal — Basic Bounce 22 jumps
6. Formal Repeat — Basic Bounce 22 jumps พร้อม Auto-pause และ History

Acceptance:

- Smoke `3/3`
- Controls ทุกประเภท `0`
- Formal และ Repeat `22/22`
- `T/L22/22`, `SUP0`, หลังหยุดเพิ่ม `0`
- Result/History ตรงกัน
- ไม่มี preview stutter, crash หรือ freeze

หาก control ใดเกิด Count หรือ Formal รอบใดต่ำกว่า `22/22` ให้หยุด ไม่ลด threshold เพิ่ม และวิเคราะห์ trace ใหม่
