# RopeSkill Android MVP Roadmap

อัปเดตล่าสุด: 26 กรกฎาคม 2026

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
- [x] เพิ่ม Positioning Guidance แยก `STEP BACK`, `MOVE CLOSER`, `SHOW FULL BODY` และ `DISTANCE GOOD` โดยไม่เปลี่ยน detector (รอทดสอบบนอุปกรณ์)
  - [x] ยืนยัน `STEP BACK`, `SHOW FULL BODY` และ `DISTANCE GOOD / HOLD STILL` บนอุปกรณ์จริง
  - [x] ปรับเกณฑ์ `MOVE CLOSER` จาก body height 42% เป็น 50% หลังพบว่าเตือนเมื่ออยู่ไกลเกินไป (รอทดสอบซ้ำ)

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
- [x] เพิ่ม debug-only `PERF V1` สำหรับ result FPS, inference latency และ estimated skipped frames
- [x] วัด latency และ estimated skipped frames ต่อเนื่อง 120 วินาทีบนอุปกรณ์จริง

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
- [x] วิเคราะห์ Medium Basic Bounce ที่ตรวจพบเพียง 6/10 โดยไม่ทำให้ผล Knee lift ถดถอย
  - [x] เพิ่ม `MEDIUM DIAGNOSTIC V4` เพื่อนับการเปลี่ยนเข้าสู่เหตุผล ANK/HIP/SYNC/AIR/LAND โดยไม่เปลี่ยน `BasicBounceDetector`
  - [x] ทดสอบ Medium เพิ่ม 3 รอบ: 10/10, 9/10 และ 10/10 รวม 29/30
  - [x] ทดสอบ knee lift ซ้าย/ขวาซ้ำอย่างละ 5 ครั้งและยืนยัน false positive 0/5 ทั้งสองข้าง
- [x] ทดสอบ hip-to-ankle ratio `0.85`: Basic Bounce 20/20 สามรอบ แต่ knee lift ซ้าย/ขวา false positive ข้างละ 2/5
- [x] เพิ่ม bilateral ankle-rise floor `0.010 × leg length` พร้อม regression tests จากหลักฐาน T-707
- [x] ทดสอบ T-708 บนอุปกรณ์จริง: Basic Bounce 60/60 และ knee lift ซ้าย/ขวา 0/5 แต่ heel raise false positive 3/10
- [x] เพิ่ม minimum hip rise `0.060 × leg length` พร้อม heel-raise regression test จากหลักฐาน T-708
- [x] ทดสอบ T-709 บนอุปกรณ์จริง: Unit tests ผ่าน; Basic Bounce 19/20, 19/20, 18/20 และ knee lift 0/5 แต่ heel raise ยัง false positive 2/10
- [x] เพิ่ม `COUNT HISTORY V6` เพื่อเก็บ heel/toe rise ซ้าย–ขวาแบบ passive โดยไม่เปลี่ยน detector
- [x] ทดสอบ T-710 บนอุปกรณ์จริง: Unit tests ผ่าน; Basic Bounce 8/10, heel raise false 0/20 และ V6 อ่านค่า heel/toe ได้เมื่อจัดเฟรมให้เห็นรองเท้าครบ
- [x] เพิ่ม `REJECTED TAKEOFF V7` และ `COOLDOWN V7` แบบ passive เพื่อวิเคราะห์ Count ที่หายโดยไม่เปลี่ยน detector
- [x] ทดสอบ T-711 บนอุปกรณ์จริง: Basic Bounce 10/10, heel raise false 0/20, AIR/LAND 10/10 และ cooldown 0
- [x] ทดสอบ T-712 repeatability: รอบ 20/20, 17/20, 18/20; false controls 0 แต่ไม่ผ่านเป้าหมายอย่างน้อย 18/20 ทุกรอบ
- [x] เพิ่ม Strong-Hip Rescue V8 และทดสอบ T-713: Basic Bounce 59/60 และ false controls 0
- [x] ทดสอบ T-714 session ยาว: 113/130 หรือ 86.9% จึงไม่ผ่านเป้าหมาย 95%
- [x] ลด Strong-Hip Rescue floor เป็น `0.025` และทดสอบ T-715: 127/130 หรือ 97.7%, false controls 0 และไม่ crash/freeze
- [ ] รัน T-716 repeatability จาก detector baseline commit `591b938` แยกจากฟีเจอร์เพลง

Checkpoint: มีผลความแม่นยำที่วัดซ้ำได้และบันทึกไว้

## Milestone 7 — Local Session Storage

- [x] นิยามข้อมูล Session ขั้นต่ำ: exercise type, start/completion time, duration และ jump count
- [x] เลือก Room `2.7.2` สำหรับ Session history; DataStore ใช้เฉพาะ Settings
- [x] เพิ่ม Room schema version 1, DAO, repository, บันทึกเมื่อ Finish และอ่าน latest Session (รอทดสอบบนอุปกรณ์)
- [x] ยืนยัน Session 3 jumps ยังคงอยู่หลังปิดและเปิดแอปใหม่บน Samsung Galaxy S23 Ultra
- [x] ยืนยัน Finish ก่อน Countdown จบไม่สร้าง Session row
- [x] เพิ่ม Training History เป็นหน้าจอแยก แสดง completed Sessions เรียงใหม่สุดก่อน (รอทดสอบบนอุปกรณ์)
- [x] เปิด Room schema export และเพิ่ม `room-testing` infrastructure สำหรับ schema version 1
- [x] Generate/commit schema `1.json` และรัน instrumentation test บนอุปกรณ์
- [ ] เพิ่ม migration และ migration test เมื่อ schema เปลี่ยนเป็น version 2; ห้ามใช้ destructive fallback กับ Training History

Checkpoint: ปิดและเปิดแอปใหม่แล้วยังพบผล Session

## Milestone 8 — Real-device Validation

- [x] ทดสอบ permission และ app interruption
- [ ] ยืนยัน auto-pause เมื่อ tracking หายต่อเนื่องระหว่าง Running และ Resume ผ่าน Positioning ใหม่
- [ ] ทดสอบแสง ระยะกล้อง เสื้อผ้า และพื้นหลังหลายแบบ
- [ ] วัด FPS, latency, CPU, memory, battery และอุณหภูมิ
- [ ] ทดสอบ Session ต่อเนื่อง
- [ ] สรุป Known Issues และข้อจำกัด

Checkpoint: MVP Success Criteria มีหลักฐานรองรับครบ

## Milestone 9 — Training Music Phase 1

- [x] เพิ่ม Media3 ExoPlayer สำหรับเพลง local หนึ่งไฟล์
- [x] เพิ่ม file picker ผ่าน Storage Access Framework และ persisted read permission
- [x] เพิ่ม Settings สำหรับเลือก/ลบไฟล์, เปิด/ปิด และปรับระดับเสียง
- [x] ผูกเพลงกับ `RUNNING`, Pause/Resume, auto-pause, Finish และ Reset
- [x] เพิ่ม audio focus, headphone-disconnect handling, loop และ fail-soft error
- [x] รัน unit tests และ Build บนเครื่องผู้ใช้
- [x] ทดสอบ T-210 ด้วย MP3 และ AAC/M4A บน Samsung Galaxy S23 Ultra
- [x] เปรียบเทียบ FPS/latency/SKIP ระหว่าง Music OFF กับ ON โดยไม่แก้ `BasicBounceDetector`
- [x] ยืนยันว่า tracking-loss auto-pause หยุดเพลงและ Resume เล่นต่อจากตำแหน่งเดิม
- [x] ยืนยันว่าเพลงเล่นวนเมื่อไฟล์จบระหว่าง `RUNNING`

Checkpoint: Pass — เพลงทำงานตาม Training lifecycle, การฝึกไม่ crash เมื่อไฟล์หาย และไม่พบ detector/performance regression ที่สัมพันธ์กับเพลง

## Milestone 10 — Branded System Splash

- [x] ยืนยันจากวิดีโอว่า Android system splash ใช้พื้นขาวและ adaptive Launcher icon
- [x] เพิ่ม AndroidX Core SplashScreen และ starting theme แยก
- [x] ใช้พื้น `#071426` และ splash logo canvas 288dp
- [x] ตรวจวิดีโอรอบแรกและลดภาพภายในจาก 224dp เป็น safe area 192dp หลังพบโลโก้ใหญ่และถูก system mask
- [x] ตรวจวิดีโอรอบ 192dp และลดภาพเป็น 136dp หลังห่วงเชือกด้านบนยังถูก mask โดยเผื่อ margin จากขนาดชนขอบประมาณ 141dp
- [x] วัดภาพรอบ 136dp และลดเป็น 128dp หลังพบว่ายอดห่วงเชือกเกิน mask ประมาณ 3–5px
- [x] คง Launcher icon, Home logo และ detector เดิม
- [x] รัน unit tests และ Build บนเครื่องผู้ใช้
- [ ] ทดสอบ T-211 ซ้ำหลังลดขนาดโลโก้เป็น 128dp โดยตรวจ cold/warm start รวม Dark, Light และ System theme บน Samsung Galaxy S23 Ultra

Checkpoint: system splash ไม่มีพื้นขาว, คนและเส้นเชือกเห็นครบ, transition เข้า Home ไม่กะพริบ และ launch time ไม่เพิ่มโดยตั้งใจ

## หลัง MVP เท่านั้น

- Backend และ account
- iOS
- ท่ากระโดดเพิ่มเติม
- Scoring, missions, rewards และ leaderboard
- Analytics หรือ cloud features หลังผ่านการทบทวน privacy
