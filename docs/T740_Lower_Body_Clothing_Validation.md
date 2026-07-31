# T-740 — Lower-Body Clothing Environmental Validation

สถานะ: Prepared — รอทดสอบบน Samsung Galaxy S23 Ultra

## เป้าหมาย

ตรวจว่า accepted detector baseline `752af1d` ยังนับ Basic Bounce ได้ตามเกณฑ์เมื่อเปลี่ยนเฉพาะเสื้อผ้าส่วนล่าง โดยไม่เปลี่ยน `BasicBounceDetector`, thresholds, camera pipeline หรือ production behavior

T-721–T-724 ครอบคลุมแสงสลัวแล้ว จึงคงแสงปกติใน T-740 และไม่รวมการเปลี่ยนพื้นหลัง ระยะกล้อง ตำแหน่งโทรศัพท์ หรือเพลงในรอบเดียวกัน

## ตัวแปร

- รอบ A — Reference: ใช้กางเกงแบบเดียวกับ T-738 หรือชุดปกติที่ใช้ทดสอบ detector ล่าสุด
- รอบ B — Changed clothing: เปลี่ยนเฉพาะกางเกงเป็นสีหรือความสว่างที่ต่างจากรอบ A อย่างเห็นได้ชัด
- เสื้อ รองเท้า แสง พื้นหลัง โทรศัพท์ ขาตั้ง ระยะกล้อง มุมกล้อง และตำแหน่งยืนต้องเหมือนเดิม
- ใช้เสื้อผ้าที่กระชับพอให้กระโดดได้ปลอดภัย; หลีกเลี่ยงชายผ้าหลวมที่อาจเกี่ยวเชือก

หากไม่สามารถทำรอบ A ด้วยชุดเดิมของ T-738 ได้ ให้ใช้ชุดปัจจุบันเป็น Reference และบันทึกลักษณะกางเกงทั้ง A/B แบบสั้น ๆ โดยไม่ต้องแนบภาพนิ่งของผู้ทดสอบ

## Preconditions

1. Branch `fix/adaptive-icon-safe-zone`
2. Project checkpoint `523e0a2`
3. Detector baseline `752af1d`
4. Samsung Galaxy S23 Ultra, front camera และตำแหน่งกล้องเดิม
5. Music OFF และแสงปกติคงที่
6. เห็นร่างกายเต็มตัวและแสดง `DISTANCE GOOD`/พร้อมเข้าสู่ `GO`
7. เปิด Screen Recorder ก่อนเริ่มแต่ละ Session และบันทึกจนถึง Result/History
8. พื้นที่กระโดดปลอดภัย; หยุดทันทีเมื่อเจ็บ เวียนศีรษะ เชือกเกี่ยว หรือสภาพแวดล้อมไม่ปลอดภัย

## Test order

### Round A — Reference

1. ใช้กางเกง Reference และคงสภาพแวดล้อมเดิม
2. เริ่ม Session, รอ `GO` แล้วกระโดด Basic Bounce 22 ครั้งด้วยจังหวะสม่ำเสมอ
3. หลังครั้งที่ 22 ให้ยืนนิ่งเต็มเฟรม 10 วินาที
4. บันทึก App Counter, `T/L`, `SUP`, Count increase after stopping, PERF และ stability
5. Finish และตรวจ Result/History
6. พักจนหายเหนื่อยก่อน Round B

### Round B — Changed clothing

1. เปลี่ยนเฉพาะกางเกง; ห้ามขยับโทรศัพท์ ขาตั้ง หรือเปลี่ยนแสง/พื้นหลัง
2. ทำขั้นตอนเดียวกับ Round A จำนวน 22 ครั้ง
3. ยืนนิ่ง 10 วินาที แล้วบันทึกหลักฐานชุดเดียวกัน
4. Finish และตรวจ Result/History

## Acceptance

แต่ละรอบต้องผ่านทุกข้อ:

- Actual/App อย่างน้อย `21/22` หรือ 95.5%; เป้าหมายคือ `22/22`
- `T/L` สมดุลกับ Count ที่รับ
- `SUP0`
- Count increase after stopping `0`
- Result/History ตรงกับ App Counter
- ไม่มี `AIRBORNE` ค้าง, preview stutter, crash หรือ freeze
- FPS/LAT/SKIP ไม่มี regression ที่เห็นได้ชัดเมื่อเทียบระหว่าง A และ B

Round B ผ่าน environmental validation เมื่อผ่านเกณฑ์ข้างต้นและไม่ด้อยกว่า Round A มากกว่า 1 Count

## Stop rules

- หาก Round A ต่ำกว่า `21/22` ให้หยุดก่อน Round B เพราะ control baseline ไม่เสถียร
- หาก Count เพิ่มหลังหยุด, `T/L` ไม่สมดุล, `SUP>0`, `AIRBORNE` ค้าง หรือเกิด crash/freeze ให้หยุดและส่งวิดีโอรอบนั้น
- หาก Round B ต่ำกว่า `21/22` ห้ามปรับ threshold หรือแก้ detector จากผลรอบเดียว; ให้บันทึกเป็น Fail และวิเคราะห์วิดีโอก่อน
- หากมีการเปลี่ยนแสง ตำแหน่งกล้อง พื้นหลัง หรือระยะยืนระหว่างรอบ ให้ระบุ Protocol deviation และไม่สรุปผลเรื่องเสื้อผ้า

## Result form

```text
# T-740 Lower-Body Clothing Validation

Project checkpoint: 523e0a2
Detector baseline: 752af1d
Device: Samsung Galaxy S23 Ultra
Music OFF: Yes
Lighting: Normal / unchanged
Camera/background/distance unchanged: Yes / No

Round A clothing:
Actual jumps:
App jumps:
T/L:
SUP:
Count increase after stopping:
FPS:
LAT:
IN/OUT/SKIP:
Result/History:
AIRBORNE freeze:
Preview stuttering:
Crash/freeze:
Video filename:

Round B clothing:
Actual jumps:
App jumps:
T/L:
SUP:
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
Result: Pass / Fail / Inconclusive
```
