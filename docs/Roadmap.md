# RopeSkill Android MVP Roadmap

อัปเดตล่าสุด: 22 กรกฎาคม 2026

หลักการ: ทำทีละ Milestone และเริ่มขั้นถัดไปเมื่อขั้นก่อนหน้า Build และทดสอบผ่านแล้ว

## Milestone 0 — ตรวจสภาพโปรเจกต์

- [ ] ตรวจโครงสร้างไฟล์ Android project
- [ ] ตรวจ `build.gradle.kts`, `settings.gradle.kts` และ version catalog
- [ ] Build แอปโดยไม่มีการแก้โค้ด
- [ ] Run แอปตัวอย่างบน Emulator หรือโทรศัพท์
- [ ] บันทึกผลใน `Test_Log.md`

Checkpoint: แอปตัวอย่าง Build สำเร็จ และผู้ใช้ยืนยันผลการ Run

## Milestone 1 — Kotlin และ Compose ที่จำเป็น

- [ ] เข้าใจ `val`, `var`, function และ data class
- [ ] เข้าใจ Composable, state และ recomposition ในระดับที่ใช้กับโปรเจกต์
- [ ] แก้ข้อความหรือ UI เล็กน้อยแล้ว Run ได้

Checkpoint: ผู้ใช้แก้ UI ง่าย ๆ และเห็นผลบนอุปกรณ์

## Milestone 2 — Counter และ Workout Timer

- [ ] สร้าง Counter
- [ ] สร้างปุ่ม Start, Pause และ Finish
- [ ] สร้าง Timer ที่จัดการ lifecycle ได้
- [ ] แยก UI state ไปยัง ViewModel เมื่อเหมาะสม

Checkpoint: Counter และ Timer ทำงานถูกต้องโดยยังไม่ใช้กล้อง

## Milestone 3 — Navigation และหน้าจอหลัก

- [ ] Home screen
- [ ] Training screen
- [ ] Result screen
- [ ] ส่งผล Session ไปยัง Result อย่างปลอดภัย

Checkpoint: เดินทางครบ Home → Training → Result → Home

## Milestone 4 — CameraX Preview

- [ ] เพิ่ม dependency จากเอกสารทางการล่าสุด
- [ ] เพิ่ม Camera permission
- [ ] แสดง live preview
- [ ] จัดการ permission denied
- [ ] จัดการ lifecycle และคืนทรัพยากร

Checkpoint: ผู้ใช้ยืนยันว่า preview เสถียรบนโทรศัพท์จริง

## Milestone 5 — MediaPipe Pose

- [ ] เพิ่ม Pose Landmarker และ model asset ที่เหมาะสม
- [ ] แปลง CameraX frame เป็น input ที่ MediaPipe รองรับ
- [ ] ประมวลผลนอก Main Thread
- [ ] ปิด `ImageProxy` ทุกเส้นทาง
- [ ] แสดง landmarks พร้อม rotation/mirroring ที่ถูกต้อง
- [ ] วัด latency และ dropped frames

Checkpoint: landmarks ติดตามร่างกายได้ในสภาพทดสอบที่บันทึกไว้

## Milestone 6 — Basic Bounce Detection

- [ ] กำหนดสัญญาณที่ใช้ตรวจจับ เช่น ตำแหน่งข้อเท้า/สะโพกตามเวลา
- [ ] สร้าง state machine ป้องกันการนับซ้ำ
- [ ] เพิ่ม confidence threshold และ cooldown
- [ ] ทดสอบกับการกระโดดจริงและการเคลื่อนไหวที่ไม่ใช่การกระโดด
- [ ] ปรับ threshold จากข้อมูลทดสอบ

Checkpoint: มีผลความแม่นยำที่วัดซ้ำได้และบันทึกไว้

## Milestone 7 — Local Session Storage

- [ ] นิยามข้อมูล Session ขั้นต่ำ
- [ ] เลือก Room หรือ DataStore ตามรูปแบบข้อมูลจริง
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
