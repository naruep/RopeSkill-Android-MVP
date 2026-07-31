# T-741 — Same-Condition Cadence-Controlled Repeatability

สถานะ: Complete — Fail / Stop rule applied

## เป้าหมาย

ตรวจว่าความแม่นยำที่แปรผันใน T-740 เกิดซ้ำหรือไม่เมื่อควบคุมเสื้อผ้า สภาพแวดล้อม ตำแหน่ง และจังหวะให้คงที่ โดยไม่เปลี่ยน `BasicBounceDetector`, thresholds, camera pipeline หรือ production behavior

T-740 ได้ same-clothing results `21/22`, `15/22`, `18/22` และ changed-clothing `18/22`; clothing attribution จึง Inconclusive ขณะที่ safety/stability ผ่านทุก session

## Preconditions

1. Branch `fix/adaptive-icon-safe-zone`
2. Detector baseline `752af1d`
3. Samsung Galaxy S23 Ultra และ front camera
4. Music OFF
5. ใช้กางเกง รองเท้า เสื้อ แสง พื้นหลัง โทรศัพท์ ขาตั้ง ระยะกล้อง มุมกล้อง และตำแหน่งยืนเดียวกันทั้งสองรอบ
6. เห็นร่างกายเต็มตัวและระบบพร้อมเข้าสู่ `GO`
7. พักจากการทดสอบก่อนหน้าและไม่มีอาการเหนื่อย เจ็บ หรือเวียนศีรษะ
8. เปิด Screen Recorder ตั้งแต่ก่อนเริ่ม Session จนเห็น Result และ History

## Cadence control

- ใช้จังหวะ Basic Bounce ที่สม่ำเสมอและเป็นธรรมชาติแบบเดียวกันตลอดรอบ
- ก่อนเริ่มให้นึกจังหวะคงที่ 3 ครั้ง แล้วเริ่มกระโดดเมื่อพร้อม
- ห้ามเร่งหรือชะลอเพื่อชดเชยเมื่อเห็นว่า Counter พลาด
- ห้ามเปลี่ยนท่ากระโดด ความกว้างเท้า หรือตำแหน่งยืนระหว่างรอบ
- ไม่ใช้ Training Music หรือ external metronome เพื่อหลีกเลี่ยงการเพิ่มตัวแปรใหม่

## Test procedure

### Run 1

1. เริ่ม Session และรอ `GO`
2. กระโดด Basic Bounce 22 ครั้งด้วยจังหวะสม่ำเสมอ
3. หลังครั้งที่ 22 ให้ยืนนิ่งเต็มเฟรม 10 วินาที
4. บันทึก App Counter, `T/L`, `SUP`, rejected reasons, FPS/LAT/IN/OUT/SKIP และอาการผิดปกติ
5. Finish และตรวจ Result/History

หาก Run 1 ต่ำกว่า `21/22`, Count เพิ่มหลังหยุด, `T/L` ไม่สมดุล, `SUP>0` หรือมี safety/stability anomaly ให้หยุดและส่งหลักฐานโดยไม่ทำ Run 2

### Run 2

1. พักอย่างน้อย 3 นาทีและเริ่มเมื่อหายเหนื่อย
2. ตรวจว่าอุปกรณ์ เสื้อผ้า แสง พื้นหลัง ระยะและตำแหน่งไม่เปลี่ยน
3. ทำขั้นตอนเดียวกับ Run 1

## Acceptance

T-741 ผ่านเมื่อทั้งสองรอบ:

- Actual/App อย่างน้อย `21/22`
- ผลต่างระหว่างรอบไม่เกิน 1 Count
- `T/L` สมดุลกับ Count ที่รับ
- `SUP0`
- Count increase after stopping `0`
- Result/History ตรงกับ Counter
- FPS/LAT/SKIP ไม่มี regression ที่เห็นได้ชัด
- ไม่มี AIRBORNE freeze, preview stutter, crash หรือ freeze

หากรอบใดต่ำกว่าเกณฑ์ ให้บันทึก KI-022 เป็น Confirmed repeatability variance และวิเคราะห์ missed-cycle evidence ก่อนออกแบบ diagnostic หรือเสนอ detector change

## ผลการทดสอบ

### Run 1 — 31 กรกฎาคม 2026

- Project checkpoint: `ec23e15`
- Detector baseline: `752af1d`
- Device: Samsung Galaxy S23 Ultra
- Music OFF: Yes
- Clothing/environment unchanged: Yes
- Actual/App: `22/18` (81.8%)
- Missed: 4
- `T/L`: `18/18`
- `SUP`: `0`
- Primary visible rejected reasons: `ANKLE RISE TOO SMALL`, `FEET NOT SYNCHRONIZED`
- Count increase after stopping: `0`
- FPS: `30.2`
- LAT: `27/48ms`
- IN/OUT/SKIP: `1226/1226/~0`
- Result/History: ตรงกันที่ `18 jumps / 00:29`
- AIRBORNE freeze: No
- Preview stuttering: No visible abnormality
- Crash/freeze: No
- Video: `Screen_Recording_20260731_092805.mp4`
- Result: Fail — accuracy ต่ำกว่า `21/22`

### Stop decision

Run 2 ไม่ได้ดำเนินการตาม protocol stop rule เพราะ Run 1 ได้ `18/22`. ผลนี้ทำให้ KI-022 เปลี่ยนจาก Investigating เป็น Confirmed repeatability variance แต่ยังไม่ยืนยันว่า rejection label ใดเป็น root cause ของ missed cycle แต่ละรอบ เนื่องจาก Screen Recording ไม่มี raw `PoseFrame` และ exact gate operands ครบทุกเฟรม

T-741 ปิดเป็น `Fail / Stopped by protocol`. Safety/stability, Count หลังหยุด และ Result/History ผ่าน แต่ accuracy ไม่ผ่าน ห้ามปรับ `BasicBounceDetector` หรือ thresholds จาก aggregate labels เพียงอย่างเดียว; ให้ทำ T-742 existing-video missed-cycle audit ก่อน

## Result form

```text
# T-741 Same-Condition Repeatability

Project checkpoint:
Detector baseline: 752af1d
Device: Samsung Galaxy S23 Ultra
Music OFF: Yes
Clothing/environment unchanged: Yes / No

Run:
Actual jumps: 22
App jumps:
T/L:
SUP:
Primary rejected reasons:
Count increase after stopping:
FPS:
LAT:
IN/OUT/SKIP:
Result/History:
AIRBORNE freeze:
Preview stuttering:
Crash/freeze:
Video filename:

Protocol deviation:
Result: Pass / Fail / Stop
```
