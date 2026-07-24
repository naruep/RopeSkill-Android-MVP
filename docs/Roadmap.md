# RopeSkill Android MVP Roadmap

อัปเดตล่าสุด: 23 กรกฎาคม 2026

หลักการ: ทำทีละ Milestone และเริ่มขั้นถัดไปเมื่อขั้นก่อนหน้า Build และทดสอบผ่านแล้ว

## Milestone 0 — ตรวจสภาพโปรเจกต์

- [x] ตรวจโครงสร้างไฟล์ Android project
- [x] ตรวจ `build.gradle.kts`, `settings.gradle.kts` และ version catalog
- [x] Build แอปโดยไม่มีการแก้โค้ด
- [x] Run แอปตัวอย่างบนโทรศัพท์ Samsung Galaxy S23 Ultra
- [x] บันทึกผลใน `Test_Log.md`

Checkpoint: แอปตัวอย่าง Build สำเร็จ และผู้ใช้ยืนยันผลการ Run

## Milestone 1 — Kotlin และ Compose ที่จำเป็น

- [ ] เข้าใจ `val`, `var`, function และ data class
- [ ] เข้าใจ Composable, state และ recomposition ในระดับที่ใช้กับโปรเจกต์
- [ ] แก้ข้อความหรือ UI เล็กน้อยแล้ว Run ได้

Checkpoint: ผู้ใช้แก้ UI ง่าย ๆ และเห็นผลบนอุปกรณ์

## Milestone 2 — Counter และ Workout Timer

- [x] สร้าง Counter และทดสอบบนอุปกรณ์
- [x] สร้างปุ่ม Start, Pause และ Finish และทดสอบบนอุปกรณ์
- [x] สร้าง Timer ที่หยุดเมื่อแอปออกจาก foreground และทดสอบบนอุปกรณ์
- [x] แยก UI state ไปยัง ViewModel และ StateFlow

Checkpoint: Counter และ Timer ทำงานถูกต้องโดยยังไม่ใช้กล้อง

## Milestone 3 — Navigation และหน้าจอหลัก

- [x] Home screen และทดสอบบนอุปกรณ์
- [x] Training screen และทดสอบบนอุปกรณ์
- [x] Result screen และทดสอบบนอุปกรณ์
- [x] ส่งผล Session ไปยัง Result ผ่าน shared ViewModel และทดสอบบนอุปกรณ์
- [x] เลือก Figma Concept B — Power Sport เป็น UI direction
- [x] ปรับ Home screen ตาม Power Sport และยืนยันว่าเปิด Training ได้บนอุปกรณ์
- [x] ปรับ Training screen ตาม Power Sport และทดสอบบนอุปกรณ์
- [x] ปรับ Result screen ตาม Power Sport และทดสอบบนอุปกรณ์
- [x] เพิ่มทางเข้า Settings และหน้า Settings แบบเต็มจอ
- [x] เพิ่ม on-device preferences สำหรับ nickname, countdown, units, sound และ vibration (รอทดสอบบนอุปกรณ์)
- [x] ทดสอบ Settings navigation, persistence และ training cues บนอุปกรณ์จริง
- [x] เพิ่ม System/Dark/Light theme สำหรับ Home, Settings, Training และ Result โดยคง Camera surface/overlay เป็น Dark (รอทดสอบบนอุปกรณ์)

Checkpoint: เดินทางครบ Home → Training → Result → Home

## Milestone 4 — CameraX Preview

- [x] เพิ่ม CameraX `1.6.1` stable ตามเอกสารทางการและทดสอบบนอุปกรณ์
- [x] เพิ่ม Camera permission และทดสอบ Allow/Deny
- [x] แสดง live preview ด้วยกล้องหลังและทดสอบบนอุปกรณ์
- [x] เปลี่ยนกล้องหน้าเป็นค่าเริ่มต้นและสะท้อน overlay ตาม preview (รอทดสอบบนอุปกรณ์)
- [x] จัดการ permission denied ด้วยคำอธิบายและปุ่มขอใหม่
- [x] ผูกกล้องกับ lifecycle และ unbind เมื่อออกจากหน้า

Checkpoint: ผู้ใช้ยืนยันว่า preview เสถียรบนโทรศัพท์จริง

## Milestone 5 — MediaPipe Pose

- [x] เพิ่ม Pose Landmarker `0.10.35` และ `pose_landmarker_lite.task` และทดสอบบนอุปกรณ์
- [x] แปลง CameraX RGBA frame เป็น `MPImage` และทดสอบบนอุปกรณ์
- [x] ประมวลผลบน single background executor
- [x] ปิด `ImageProxy` ทุกเส้นทาง
- [x] แสดง landmarks โดยรองรับ rotation และ `FILL_CENTER` coordinate conversion และทดสอบบนอุปกรณ์
- [ ] วัด latency และ dropped frames

Checkpoint: landmarks ติดตามร่างกายได้ในสภาพทดสอบที่บันทึกไว้

## Milestone 6 — Basic Bounce Detection

- [x] สร้าง baseline จากข้อเท้าทั้งสองและปรับ threshold ตามความยาวช่วงสะโพกถึงข้อเท้า (รอทดสอบ)
- [x] สร้าง state machine `Calibrating → Grounded → Airborne → Grounded` (รอทดสอบ)
- [x] ใช้ landmark visibility, smoothing และ cooldown ป้องกันการนับซ้ำ (รอทดสอบ)
- [x] ทดสอบ baseline กับการกระโดดจริงและการเคลื่อนไหวที่ไม่ใช่การกระโดด (พบ 1/10 ทั้ง 3 รอบและ false positive)
- [x] เพิ่ม ready-position check และ countdown 5 วินาที
- [x] เปลี่ยนให้ Countdown จบแล้วแสดง `GO!` และเริ่ม Timer ทันที โดยไม่รอ Takeoff แรก (รอทดสอบบนอุปกรณ์)
- [x] ปรับ threshold รอบแรกและใช้ hip/ข้อเท้าทั้งสองร่วมกันจากข้อมูลทดสอบ
- [x] ทดสอบ detector รอบแรก: slow/medium 10/10, fast 4/10, knee-bend false positive 5
- [x] ให้ `START TRAINING` เปิด Ready Detection อัตโนมัติและคงเฉพาะ `RESUME` หลัง Pause (Single Start และ Resume ผ่าน)
- [x] ทดสอบ stability check รอบสองและยืนยันว่าไวเกินไป
- [x] ทดสอบ detector รอบสองและยืนยันการถดถอย fast 1/10, slow/medium 0/10
- [x] คืน detector/Countdown behavior รอบแรกและเพิ่ม diagnostic overlay
- [x] ทดสอบ detector ที่คืนค่าและ Resume บนอุปกรณ์จริง (fast 4/10, slow 10/10, medium 8/10; knee lift ยังถูกนับผิด)
- [x] เพิ่ม `LAST COUNT` แบบค้างหลักฐาน Takeoff โดยไม่เปลี่ยน detector threshold
- [x] เปรียบเทียบ `LAST COUNT` รอบแรกของ Basic Bounce กับ knee lift บนอุปกรณ์จริง
- [x] ยืนยัน DIFF/LIMIT/SYNC จาก Takeoff ด้วย `LAST COUNT V2`; ทุกตัวอย่างผ่าน SYNC และค่าเดิมที่ขัดกับ threshold ไม่เกิดซ้ำ
- [x] เก็บหลักฐานด้วย `COUNT HISTORY V3`: Basic Bounce 9 counts และ knee lift false positive 3 counts
- [x] เพิ่มตัวกรอง Takeoff `hipRise >= averageAnkleRise × 1.10` พร้อม local unit tests
- [x] ทดสอบตัวกรอง hip/ankle บนอุปกรณ์จริง: Slow 10/10, Medium 6/10, Fast 10/10 และ knee lift ซ้าย/ขวา 0/5 false positives
- [ ] วิเคราะห์ Medium Basic Bounce ที่ตรวจพบเพียง 6/10 โดยไม่ทำให้ผล Knee lift ถดถอย
  - [x] เพิ่ม `MEDIUM DIAGNOSTIC V4` เพื่อนับการเปลี่ยนเข้าสู่เหตุผล ANK/HIP/SYNC/AIR/LAND โดยไม่เปลี่ยน `BasicBounceDetector`
  - [ ] ทดสอบ Medium 10 ครั้งและบันทึกค่า V4 เพื่อเลือกการทดลอง detector ทีละตัวแปร

Checkpoint: มีผลความแม่นยำที่วัดซ้ำได้และบันทึกไว้

## Milestone 7 — Local Session Storage

- [ ] นิยามข้อมูล Session ขั้นต่ำ
- [ ] เลือก Room สำหรับ Session หรือทางเลือกอื่นตามรูปแบบ query จริง; DataStore ใช้เฉพาะ Settings แล้ว
- [ ] บันทึกและอ่านผล Session
- [ ] ตรวจ migration/ความเสียหายของข้อมูลตามความเสี่ยง

Checkpoint: ปิดและเปิดแอปใหม่แล้วยังพบผล Session

## Milestone 8 — Real-device Validation

- [ ] ทดสอบ permission และ app interruption
- [ ] ทดสอบแสง ระยะกล้อง เสื้อผ้า และพื้นหลังหลายแบบ
- [ ] วัด FPS, latency, CPU, memory, battery และอุณหภูมิ
- [ ] ทดสอบ Session ต่อเนื่อง
- [ ] สรุป Known Issues และข้อจำกัด

Checkpoint: MVP Success Criteria มีหลักฐานรองรับครบ

## หลัง MVP เท่านั้น

- Backend และ account
- iOS
- ท่ากระโดดเพิ่มเติม
- Scoring, missions, rewards และ leaderboard
- Analytics หรือ cloud features หลังผ่านการทบทวน privacy
