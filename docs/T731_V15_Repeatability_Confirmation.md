# T-731 V15 — Repeatability Confirmation

สถานะ: Round 1 Completed — Counter Fail / Attribution Complete

## ผล Round 1

- Implementation/document checkpoint: `2b6a3f4`
- Device: Samsung Galaxy S23 Ultra
- Music OFF; แสงปกติ
- วิดีโอ: `Screen_Recording_20260730_103141.mp4`
- Actual/App: 22/18 หรือ 81.8%; undercount 4
- หลังหยุด Counter เพิ่ม 0
- Result/History: `18 jumps / 00:35` ตรงกัน
- Formal window: `P22 C18 R4 S0 U0`; proposals ครบทั้ง 22 ครั้ง จึงไม่มี cycle-separation miss
- Gates: `BL1 BR2 RA4`; `RA` เกี่ยวข้องกับ rejected proposal ทั้ง 4 รายการ
- Signed margins:
  - `#029`: `BL -0.0083`, `BR -0.0128`, `RA -0.0211`
  - `#038`: `RA -0.0039`
  - `#043`: `BR -0.0075`, `RA -0.0151`
  - `#044`: `RA -0.0005`
- `LONG #045 P#042 T1546=A200+G1346 RF37 E+0` เกิดจาก accepted cycles คร่อม rejected proposals ไม่ใช่ proposal หาย
- FPS 30.1; LAT 27/59ms; IN/OUT 1400/1399; SKIP ประมาณ 0
- `SEALED POST-EXIT +33.966`; ไม่พบ preview stuttering, crash หรือ freeze

## ข้อสรุป

V15 ทำซ้ำ failure pattern ได้และจำกัด undercount รอบนี้ไว้ที่ gate rejection ทั้งหมด. `RA` เป็น blocker ร่วมทุกครั้ง แต่การลด RA อย่างเดียวไม่สามารถช่วย `#029` และ `#043` เพราะยังมี bilateral blocker. `#038` และ `#044` เป็น RA-only candidates ซึ่งต้องแยกจาก multi-gate rejection ก่อนกำหนด production experiment.

ยังไม่เปลี่ยน `BasicBounceDetector`, BASE `0.010/0.020`, Counter, Result/History หรือ storage. ขั้นถัดไปคือ T-732 V16 passive RA counterfactual trace เพื่อคำนวณ recovery floor จาก operand จริงโดยไม่ตั้งหรือใช้ threshold ใหม่ใน production.
