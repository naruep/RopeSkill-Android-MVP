# RopeSkill Android MVP Roadmap

อัปเดตล่าสุด: 31 กรกฎาคม 2026

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
- [x] ป้องกัน system screen timeout เฉพาะหน้า Training และคืนค่าเดิมเมื่อออกจากหน้า (T-213 ผ่านบน Samsung Galaxy S23 Ultra)

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
- [x] รัน T-716 repeatability จาก detector baseline commit `591b938` และ Music OFF: Basic Bounce 19/20, 20/20, 20/20 รวม 59/60 หรือ 98.3%; false controls/หลังหยุด 0, AIR=LAND, Result/History ถูกต้อง และไม่ crash/freeze

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
- [x] ยืนยัน auto-pause เมื่อ tracking หายต่อเนื่องระหว่าง Running และ Resume ผ่าน Positioning ใหม่ (T-703 และ T-738 ผ่าน)
- [ ] ทดสอบแสง ระยะกล้อง เสื้อผ้า และพื้นหลังหลายแบบ
  - [x] รัน T-717 Lighting ที่ commit `7c247eb` และ Music OFF: แสงปกติ 17/20 และ 15/20, แสงน้อยลงเล็กน้อย 19/20; performance ปกติและไม่ crash/freeze แต่ไม่ผ่านเป้าหมาย ≥18/20 ทุกรอบ
  - [x] เพิ่ม T-718 Passive Cycle Trace V9 แบบ debug evidence โดยไม่เปลี่ยน detector decision
  - [x] ทดสอบ T-718 ที่ commit `4cf4869`: Unit tests/Build ผ่าน, Basic Bounce 20/20, `AIR/LAND 20/20`, `SUP 0`, rescue 14 และ trace แสดง `T → LC` ครบโดยไม่พบ stability regression
  - [x] รัน T-719 Cycle Trace Repeatability ที่ commit `c7a28f5`: ได้ 19/20, 18/20, 18/20 รวม 55/60; `LAND − SUP` ตรงกับ Counter ทุกครั้ง และ trace ยืนยันวงจร Landing `B` เร็ว/ซ้ำ
  - [x] เพิ่ม T-720 Landing Re-arm Guard ตามข้อตกลง: `B` ต้องเห็น ankle และ hip กลับ baseline โดยคง threshold, rescue floor, cooldown และ `C/BC` เดิม
  - [x] ทดสอบ T-720 ที่ commit `b044672`: Unit tests/Build ผ่าน, Basic Bounce 20/20 ทั้ง 3 รอบ รวม 60/60, `AIR/LAND 60/60`, `SUP 0`; heel raise, knee lift, standing และหลังหยุด false 0 พร้อม Result/History และ stability ผ่าน
  - [x] ทดสอบ T-721 ในแสงสลัวที่ commit `862c978` และ APK ปัจจุบัน: ได้ 20/20, 19/20, 17/20 รวม 56/60; `AIR=LAND`, `SUP 0` และไม่มี Landing cycle แตกซ้ำ แต่ FPS ลดเหลือ 23.5–24.0 และมี takeoff ถูกปฏิเสธต่ำกว่า rescue floor จึงไม่ผ่านเป้าหมาย accuracy
  - [x] เพิ่ม T-722 `TAKEOFF PEAK V10` แบบ passive: แสดง smoothed/raw ankle และ hip peak, จำนวน result frames, rise time และ interval ก่อน/หลัง peak โดยไม่เปลี่ยน detector behavior
  - [x] ทดสอบ T-722 ที่ commit `20e55fe`: Unit tests/Build ผ่าน; แสงปกติ 20/20 และแสงสลัวเดิม 19/20 โดย Music OFF; V10 เก็บ genuine miss เป็น `R A0.020/0.023 H0.141/0.157 F7 D240 P39 N42 ANK`, ยืนยันช่วงก่อน/หลัง peak ครบและ raw ankle ยังต่ำกว่า rescue floor `0.025`; ทั้งสองรอบ `AIR=LAND`, `SUP 0`, หลังหยุดไม่เพิ่ม, Result/History และ stability ผ่าน โดยไม่เปลี่ยน detector behavior
  - [x] เพิ่ม T-723 ตามการอนุมัติ: ลดเฉพาะ Strong-Hip Rescue ankle floor จาก `0.025` เป็น `0.020` พร้อม boundary tests โดยคง threshold, hip gate, bilateral/sync, ratio, smoothing, cooldown และ Landing re-arm guard เดิม
  - [x] ทดสอบ T-723 ที่ commit `70e35e7`: Unit tests/Build ผ่าน; Music OFF; แสงสลัวเดิม 19/20 และแสงปกติ 18/20 โดย `AIR=LAND`, `SUP 0`, หลังหยุดไม่เพิ่ม, Result/History และ stability ผ่าน; heel raise, knee lift และ standing controls ได้ false 0
  - [x] ทดสอบ T-724 Repeatability Confirmation ที่ detector baseline `70e35e7`: แสงสลัว 19/20, 20/20 และ 18/20 รวม 57/60 หรือ 95%; `AIR/LAND 57/57`, `SUP 0`, หลังหยุดไม่เพิ่ม, Result/History และ stability ผ่านทุกรอบ
  - [x] ปิด KI-018 หลัง T-724 ผ่านเป้าหมาย repeatability และ T-723 safety controls ผ่าน; คง floor `0.020`, threshold, hip gate, bilateral/sync, cooldown และ Landing re-arm guard เดิม
- [x] รัน T-725 Passive Performance Soak ที่ detector baseline `70e35e7`: Music OFF, แสงปกติ และ `RUNNING` ต่อเนื่อง 5 นาที; Counter/หลังหยุด 0, FPS 29.7–30.3, LAT 28–32/59–72ms, skip 8/12,506 หรือ 0.064%, thermal status 0, memory/CPU ไม่เพิ่มต่อเนื่อง และไม่พบ stability regression; ผ่านโดยคง KI-015 Monitoring
- [x] รัน T-726 Continuous 100 Jumps Session รอบ 1 ที่ detector baseline `70e35e7`: Music OFF และแสงปกติ; Actual/App 100/85 หรือ 85% โดยผู้ใช้สะดุดเชือก 1 ครั้ง; หลังหยุดเพิ่ม 0, `AIR/LAND 85/85`, `SUP 0`, performance, Result/History และ stability ผ่าน แต่ accuracy ต่ำกว่าเป้าหมาย 95% จึงเปิด KI-020
- [x] รัน T-727 Continuous 100 Jumps Repeatability ที่ detector baseline `70e35e7`: Music OFF, แสงปกติ และไม่สะดุดเชือก; Actual/App 100/93 หรือ 93%, หลังหยุดเพิ่ม 0, `AIR/LAND 93/93`, `SUP 0`, performance, Result/History และ stability ผ่าน แต่ยังต่ำกว่าเป้าหมาย 95% จึงยืนยันว่า KI-020 เกิดซ้ำใน session ต่อเนื่อง
- [x] เพิ่ม T-728 Passive Rejected Bilateral Ankle Evidence ตามการอนุมัติ: `TAKEOFF PEAK V11` บันทึก raw left/right ankle-rise ratio, individual floor `0.010` และ PASS/FAIL ของแต่ละข้างใน retained rejected peak โดยไม่เปลี่ยน decision หรือ threshold
- [x] ทดสอบ T-728 บนอุปกรณ์จริงด้วย Music OFF, แสงปกติ และ Continuous 100 Jumps: Actual/App 100/83 หรือ 83%, หลังหยุดเพิ่ม 0, `AIR/LAND 83/83`, `SUP 0`, performance, Result/History และ stability ผ่าน; V11 พบ per-side floor fail (`L0.007 FAIL/R0.051 PASS`) แต่ peak เดียวกันมี `H0.006` จึงยังไม่ใช่ isolated bilateral cause ส่วนตัวอย่าง bilateral ผ่านแต่ smoothed rise ใกล้ rescue floor (`L0.023 PASS/R0.017 PASS`, `A0.019/0.020`, `H0.121/0.119`) เป็น clean rescue boundary; T-728 ผ่านเป้าหมาย passive evidence แต่ไม่ผ่าน accuracy
- [x] เตรียม T-729 Controlled Two-Gate Experiment เป็น matched Debug shadows บนเฟรมเดียวกัน: `BASE 0.010/0.020`, exploratory `BIL-only 0.006/0.020` และ `RES-only 0.010/0.018`; BASE เท่านั้นควบคุม Counter/Result/History, ไม่มี profile ที่ลดสอง gate พร้อมกัน และรอบจะเริ่มสะสมต่อเมื่อทุก arm พร้อมกัน
- [x] รัน Unit tests/Build, T-729 smoke/performance และ formal matched shadow บน Samsung Galaxy S23 Ultra: วิดีโอ `Screen_Recording_20260729_161001.mp4` ยืนยัน Actual 101, App/Result/BASE 86 (85.1%), BIL-only 86 (`D+0`) และ RES-only 88 (`D+2`, 87.1%); ทุก arm มี `AIR=LAND` และ `SUP 0`, หลัง Landing สุดท้าย Counter ไม่เพิ่ม, `PROC 60/796µs`, FPS 30.0, LAT 28/65ms, IN/OUT 1853/1852, SKIP ~0 และไม่พบ preview กระตุกหรือ crash/freeze. RES-only กู้ได้สุทธิ 2 count แต่ยังต่ำกว่าเกณฑ์ ≥95% ซึ่งต้องอย่างน้อย 96/101 จึงไม่มี candidate สำหรับ promotion; ยุติ controls/active confirmation โดยไม่ถือว่า controls ผ่าน และคง production BASE `0.010/0.020` เดิม
- [x] เตรียม T-730 V12 Passive Gate Attribution เป็น Debug external collector บน BASE result: ให้ completed peaks มี window ordinal ร่วมกัน, แสดง blocker set จาก exact-paired retained evidence สำหรับ `SYNC/BIL-L/BIL-R/RATIO/STD-H/RES-A/RES-H`, `UNATTRIBUTED`, overflow และ measurement invalid guard โดยไม่แก้ `BasicBounceDetector`, threshold, Counter, Result หรือ History; Pure-Kotlin regression ผ่าน 64/64
- [x] รัน T-730 V12 smoke 3 jumps สองรอบบน Samsung Galaxy S23 Ultra: รอบแรก Counter/Result ผ่าน 3/3 แต่ collector เป็น `INVALID-RESTART` และคง `P0`; Smoke-R ผ่าน GO boundary และ Counter 3/3 แต่ก่อนกระโดดจริงเพิ่มถึง `P26 C0 R26 U4 OV20` และจบรอบที่ `P52 C3 R49 S0 U5 OV43`. จึงปิด V12 formal plan เป็น Attribution Inconclusive เพราะ global collector รวม standing landmark jitter และ overflow; ไม่ถือเป็น detector false count หรือ root cause ของ KI-020
- [x] เตรียม T-730 V13 Timestamped Window Trace: เก็บ raw completed-peak trace สูงสุด 512 events พร้อมเวลาเทียบ `GO`, ใช้ accepted `COUNTED/SUPPRESSED` cycles เป็น bookends เพื่อจัด `LEAD/WINDOW/BOUNDARY/TAIL` แบบ post-hoc, ใช้เฉพาะ `WINDOW` สำหรับ primary gate totals, แยก raw trace ล่าสุด 6 rows บน overlay ออกจาก overflow, แสดง explicit invalid reason และ freeze หลักฐานเป็น `SEALED POST-EXIT` เมื่อออกจาก full-body หลัง Landing อย่างน้อย 2 วินาที; Pure-Kotlin compile/regression ผ่าน 77/77 โดยไม่แก้ `BasicBounceDetector`, threshold หรือ Counter
- [ ] บันทึกหลักฐาน Android `testDebugUnitTest` และ `assembleDebug` ของ V13 หากยังไม่มีผลยืนยันแยกจากการที่ APK ติดตั้งและทำงานบนอุปกรณ์จริง
- [x] รัน T-730 V13 Smoke รอบแรกบน Samsung Galaxy S23 Ultra: Actual/App 3/2 และ Result 2 jumps / 00:18 จึงไม่ผ่าน Counter smoke แต่ trace integrity ผ่าน; `ALL P30 C2 R28 S0 TR30 OV0`, `SEG L11 W2 B0 T17`, `WIN P2 C2 R0 S0 U0`, gate ใน `WINDOW` เป็น 0 และ BASE `A/L2/2 SUP0 RES1`. Event `#014 T/R` ของ jump สุดท้ายเป็น candidate `Q+SH` แต่ยังอยู่ใน `TAIL` เพราะไม่มี closing accepted bookend จึงไม่ใช้เป็น root cause ของ KI-020
- [x] รัน T-730 V13 Smoke-R2: Actual/App 3/3; `ALL P54 C3 R51 S0 TR54 OV0`, `SEG L17 W3 B0 T34`, `WIN P3 C3 R0 S0 U0`, gate ทุกตัว 0, BASE `A/L3/3 SUP0 RES1`, หลัง Landing ยืนนิ่งเต็มเฟรมประมาณ 13 วินาที, Counter หลังหยุดไม่เพิ่ม, Result 3 jumps / 00:22 และไม่พบ preview กระตุกหรือ crash/freeze. Snapshot ก่อนออกไม่เป็น `INVALID`; ไม่พบ `SEALED POST-EXIT` เพราะเดินเข้าหากล้องขณะยังถูกติดตาม จึงถือว่า Counter/Attribution Window ผ่าน
- [x] เปิด `VIEW HISTORY` และยืนยัน Smoke-R2 ล่าสุดเป็น 3 jumps / 00:22; ปิด Smoke เป็น Pass
- [x] รัน T-730 V13 Formal rerun พร้อมวิดีโอครบ: Actual/App 22/19 (86.4%), `WIN P19 C19 R0 S0 U0`, WINDOW gates 0, BASE `A/L19/19 SUP0 RES17`, หลังหยุดเพิ่ม 0, `SEALED POST-EXIT +32.403`, Result/History 19 jumps / 00:33 และ stability ผ่าน. Counter Fail จาก undercount 3; accepted spans `#015` 1,303ms และ `#019` 743ms เป็น cycle-separation candidates แต่ยังไม่ใช่ causal proof
- [x] เตรียม T-730 V14 Passive Cycle-Separation Trace: เพิ่ม AIR frame samples, AIR/GAP/T2T timing, Landing reason และ count interval ใน external collector เท่านั้น; Pure-Kotlin compile/regression ผ่าน 78/78 โดย `BasicBounceDetector`, thresholds, Counter และ storage ไม่มี diff
- [x] รัน `testDebugUnitTest` และ `assembleDebug` ของ V14 บนเครื่องผู้ใช้: ผ่านทั้งสองคำสั่ง
- [x] รัน V14 Smoke 3 jumps: Actual/App 3/3, Result/History 3/00:25, หลังหยุดเพิ่ม 0, `CYC N3`, `T2T 498–499ms`, `SEALED POST-EXIT` และ stability ผ่าน
- [x] รัน V14 Formal 22 jumps: Actual/App 22/19 (86.4%), Result/History 19/00:28, หลังหยุดเพิ่ม 0 และ stability ผ่าน; `WIN P21 C19 R2`, gate `RA2/BR1`, longest `T2T 1027ms/GAP 862ms` จึงจำกัด undercount เป็น 1 cycle-separation miss + 2 gate rejections
- [x] เตรียม T-730 V15 Passive Rejection & Cycle-Miss Trace: เพิ่ม signed gate margins จาก exact evidence operands และ longest T2T decomposition `previous AIR + current GAP + READY frames` ใน external collector เท่านั้น โดยไม่แก้ detector/threshold/Counter
- [x] รัน Android `testDebugUnitTest`, `assembleDebug` และ V15 Smoke 3 jumps: ผ่าน 3/3, Result/History 3/00:29, `LONG ... E+0`, `SEALED POST-EXIT` และ stability
- [x] รัน V15 Formal 22 jumps: ผ่าน 22/22, `WIN P22 C22 R0`, `LONG T527=A440+G87 RF2 E+0`, BASE `A/L22/22 SUP0`, หลังหยุดเพิ่ม 0, Result/History 22/00:35 และ stability
- [x] รัน T-731 V15 Repeatability Confirmation Round 1 ที่ checkpoint `2b6a3f4`: Actual/App 22/18, `WIN P22 C18 R4`, proposals ครบจึงไม่มี cycle miss; rejected ทั้ง 4 ติด `RA`, โดย 2 รายการเป็น RA-only และอีก 2 รายการติด bilateral gate ร่วม; หลังหยุด 0, Result/History 18/00:35 และ stability ผ่าน
- [x] เตรียม T-732 V16 Passive RA Counterfactual Trace: แยก RA-only ออกจาก RA+other และคำนวณ `ONE/ALL` recovery floor จาก operands จริงภายใน external collector โดยไม่เปลี่ยน detector/threshold/Counter; Pure-Kotlin regression 80/80 ผ่าน
- [x] รัน T-732 V16 Smoke 3/3 และ Formal 22/19: Formal มี `P21 C19 R2`, proposal miss 1, RA-only 2, `ONE0.0153 ALL0.0151`; Result/History, seal และ stability ผ่าน แต่ Counter ต่ำกว่าเป้าหมาย
- [x] เตรียม T-733 V17 matched RA candidate shadow: BASE `0.020`, candidates `0.016/0.015`, เฉพาะ BASE ขับ Counter; Pure-Kotlin regression 86/86 ผ่านและ `BasicBounceDetector.kt` ไม่มี diff
- [x] รัน T-733 Windows tests/build และ Smoke: Actual/App 3/3, BASE/RA16/RA15 ทุก arm เป็น 3, `D+0`, หลังหยุด 0, Result/History 3/00:24 และ stability ผ่าน
- [x] รัน T-733 Formal และ Formal Repeat: ทั้งสองรอบ Actual/App 22/21, BASE/RA16/RA15 ทุก armเป็น 21 และ `D+0`; `A/L21/21`, `SUP0`, หลังหยุด 0, Result/History และ stability ผ่าน แต่ Counter ต่ำกว่าเป้าหมาย
- [x] รัน T-733 safety controls: heel raises 20, left/right knee lifts 5/5 และ standing still ให้ BASE/RA16/RA15 เป็น 0 ทุก arm; candidates ปลอดภัยใน controls ชุดนี้แต่ไม่ให้ efficacy จึงไม่เปลี่ยน RA production
- [x] เตรียม T-734 V18 Passive Proposal/Cycle-Miss Pulse Trace: ปิด RA shadows, ใช้ production BASE เดิมหนึ่ง detector และ external collector แยก raw/qualified motion pulse ที่ matched กับ Takeoff ออกจาก unmatched pulse โดยเฉพาะขณะ BASE ยัง `AIRBORNE`; ไม่แก้ `BasicBounceDetector`, thresholds, Counter หรือ storage; Pure-Kotlin regression 91/91 ผ่าน
- [x] รัน T-734 Windows tests/build และ Smoke 3/3; `Q6 M3 U3 UA0`, Result/History 3/00:23, Auto-pause และ stability ผ่าน
- [x] รัน T-734 Formal และ Formal Repeat: ทั้งสองรอบ Actual/App 22/21 และ clean trace `Q22 M21 U1 UA0 T/L21/21 X0`; unmatched เป็น `QU ... READY` ซ้ำ, หลังหยุด 0, Result/History, Auto-pause และ stability ผ่าน จึงปิด AIRBORNE cycle-lock hypothesis สำหรับสองรอบนี้
- [x] เตรียม T-735 V19 Passive Production Takeoff Gate Trace: จับคู่ `QU` กับ production rejected `TakeoffPeakEvidence` ภายใน ±120ms, แสดง exact route/blockers หรือ `NP`, พร้อม pending `P`; Pure-Kotlin regression 94/94 ผ่านและ `BasicBounceDetector.kt` ไม่มี diff
- [x] รัน T-735 Windows tests/build, Smoke Repeat 3/3 และ Formal 22 jumps พร้อม clean `P0`; Formal ได้ 22/19 และระบุ blockers ของ genuine misses เป็น `RA ×2`, `BR ×1`
- [x] วิเคราะห์ T-735 Formal Repeat `Screen_Recording_20260730_172836.mp4`: Actual/App 22/21, `Q24 M22`, `T/L22/21`; final accepted Takeoff timeout จาก `AIRBORNE` ไป `CALIBRATING` โดยไม่ Count
- [x] เตรียม T-736 V20 bounded production candidate จาก fixed T-735 evidence: bilateral `0.008`, rescue ankle `0.016`, คง RH `0.100` และ gates/state อื่น; Pure-Kotlin regression 97/97 ผ่าน
- [x] เตรียม T-737 V21 bounded timeout landing recovery: Count เฉพาะ accepted Takeoff ที่เห็น ankle+hip descent ครบระยะก่อน timeout; no-descent ยังไม่ Count; Pure-Kotlin regression 99/99 ผ่าน
- [x] รัน T-736/T-737 Windows tests/build, Smoke 3/3, Safety Controls false 0 และ Formal 22/22; Formal Repeat ได้ 21/22 จาก isolated `BR` ที่ `L0.069/R0.005/H0.145` โดย `T/L21/21`
- [x] เตรียม T-738 V22 asymmetric ankle rescue แบบ bounded: strong/weak ankle `0.060/0.004`, hip `0.120`, คง bilateral production floor `0.008`, sync/ratio/Landing/cooldown เดิม และเพิ่ม recorded-boundary กับ safety regression cases
- [x] รัน T-738 Windows tests/build และ device acceptance บน Samsung Galaxy S23 Ultra: Smoke `3/3`, Safety Controls false count `0`, Formal `22/22` และ Formal Repeat `22/22`; `T/L22/22`, `SUP0`, หลังหยุดเพิ่ม `0`, Auto-pause, Result/History และ stability ผ่าน
- [x] รับ commit `752af1d` เป็น detector baseline, ปิด T-738/KI-020 และสรุปเงื่อนไขเปิด issue ใหม่เมื่อ accuracy ต่ำกว่า 95%, controls มี false count, `T/L` ไม่สมดุล, `SUP>0` หรือ stability ถดถอย

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
- [x] วัดภาพรอบ 128dp และลดเป็น 124dp หลังยอดวงเชือกยังถูก mask ราว 1px
- [x] คง Launcher icon, Home logo และ detector เดิม
- [x] รัน unit tests และ Build บนเครื่องผู้ใช้
- [x] ทดสอบ T-211 หลังลดขนาดโลโก้เป็น 124dp: วงเชือกครบ มี margin ตัวคนชัด และส่วนอื่นถูกต้อง

Checkpoint: Pass — system splash ไม่มีพื้นขาว, คนและเส้นเชือกเห็นครบ, มี margin จาก mask และส่วนอื่นทำงานถูกต้อง

## Milestone 11 — Adaptive Launcher Icon Safe Zone

- [x] ตรวจภาพ Launcher icon และยืนยันว่าวงเชือกของ inset 10dp อยู่ใกล้ขอบ
- [x] คำนวณขอบเขต artwork เทียบ Adaptive Icon safe zone 66dp
- [x] เพิ่ม foreground/monochrome inset เป็น 16dp โดยคงพื้น `#071426`
- [x] คง Home logo, splash และ detector เดิม
- [x] ทดสอบ T-212 บน Samsung Galaxy S23 Ultra และยืนยันว่า App Icon ถูกต้อง

Checkpoint: Pass — คนและวงเชือกอยู่ภายใน Launcher mask โดยมี margin ใกล้เคียง Cold Start และยังอ่านรายละเอียดได้

## Project Records Consistency

- [x] รัน T-739 ตรวจความสอดคล้องของ `Roadmap.md`, `Test_Log.md`, `Known_Issues.md` และ `Architecture_Decisions.md`
- [x] ปรับ auto-pause ให้ตรงกับหลักฐาน T-703/T-738
- [x] ปิดสถานะเก่าที่มีผลทดสอบ Pass ชัดเจน และคง Countdown cancellation เป็น Monitoring
- [x] เปลี่ยน duplicate `KI-003` ฝั่ง Single Start เป็น `KI-021` โดยรักษาประวัติเดิม
- [x] ปรับสถานะ ADR เชิงทดลอง/diagnostic ที่ทดสอบหรือถูกแทนแล้ว โดยไม่เปลี่ยนหมายเลขอ้างอิงเดิม

Checkpoint: เอกสารทั้งสี่สอดคล้องกับ accepted detector baseline `752af1d`; ไม่มี source code หรือ `BasicBounceDetector` เปลี่ยน

## Environmental Validation

- [x] เตรียม T-740 Lower-Body Clothing Validation แบบ A/B โดยเปลี่ยนเฉพาะกางเกง และคง Music OFF, แสงปกติ, พื้นหลัง, กล้อง, ระยะและตำแหน่งเดิม
- [x] รัน Round A Reference: Actual/App `22/21`; `T/L21/21`, `SUP0`, หลังหยุดเพิ่ม 0, Result/History `21/00:32` และ stability ผ่าน
- [x] รัน same-clothing repeats เพิ่มสองรอบ: `15/22` และ `18/22`; safety/stability ผ่าน แต่ต่ำกว่า accuracy gate
- [x] รัน Round B ด้วยกางเกงขาสั้นสีเทา/ฟ้าอ่อน: `18/22`; `T/L18/18`, `SUP0`, หลังหยุดเพิ่ม 0, Result/History `18/00:31` และ stability ผ่าน
- [x] ปิด clothing attribution เป็น Inconclusive: Round B เท่ากับหนึ่ง same-clothing repeat จึงยังแยกผลของเสื้อผ้าจาก repeatability variance ไม่ได้
- [x] เปิด KI-022 ตามเงื่อนไข field accuracy ต่ำกว่า 95% หลัง accepted baseline `752af1d`

Checkpoint: T-740 Complete / Inconclusive — safety และ stability ผ่านทุก session; ยังไม่มีหลักฐานพอให้แก้ `BasicBounceDetector` หรือ thresholds

## Detector Repeatability Follow-up

- [x] เตรียม T-741 Same-Condition Cadence-Controlled Repeatability บน detector baseline `752af1d`
- [x] รัน T-741 Run 1 ภายใต้เสื้อผ้า แสง กล้อง พื้นหลัง ระยะ และจังหวะเดิม: Actual/App `22/18`
- [x] ตรวจ `T/L18/18`, `SUP0`, หลังหยุดเพิ่ม 0, Result/History `18/00:29`, PERF และ stability ผ่าน
- [x] ใช้ stop rule เพราะ Run 1 ต่ำกว่า `21/22`; ไม่ทำ Run 2 และไม่เพิ่มภาระทดสอบ
- [x] ยืนยัน KI-022 ว่า same-condition repeatability variance เกิดซ้ำ
- [x] เตรียม T-742 Existing-Video Missed-Cycle Evidence Audit โดยใช้วิดีโอ T-740/T-741 เดิม
- [x] ทำ timeline audit ครบ 5 sessions / 110 physical cycles / 90 app counts และแยก 20 misses เป็น visible gate 10, state/landing 9, proposal absent/uncertain 1, insufficient 0
- [x] ตัด gate-attributed pulse หลัง physical range ออกจาก causal mapping และยืนยันว่า aggregate rejected labels อาจ over-attribute miss
- [x] ยืนยันว่า READY gate rejection และ AIR/state-or-landing signature เกิดทั้ง same-clothing และ changed-clothing sessions จึงไม่ใช่ clothing-specific pattern
- [x] เลือก bounded passive diagnostic ก่อน detector change และเตรียม T-743 Passive Landing/State Trace
- [x] เพิ่ม debug-only T-743 observer, bounded AIR/frame/pulse evidence, compact overlay และ parity/reset/bounds tests โดยไม่เปลี่ยน production thresholds, Landing decision หรือ Counter behavior
- [x] รัน Windows `testDebugUnitTest` และ `assembleDebug` ผ่านด้วย OpenJDK 21.0.10
- [x] ทดสอบ T-743 Smoke `3/3`, Formal `21/22`, Formal Repeat `22/22` รวม `43/44` หรือ 97.7%
- [x] ยืนยัน T/L สมดุล, SUP0, AIR intervals ปิดครบ, physical pulses while AIR 0 และ unresolved 0
- [x] ยืนยัน heel raises, knee lifts ซ้าย/ขวา, standing 15s+, หลังหยุด, Result/History และ performance/stability ผ่าน
- [x] ปิด T-743 เป็น Pass; Landing/state evidence gap ไม่เกิดซ้ำ และยังไม่ปรับ detector

Checkpoint: T-743 ผ่านบน implementation commit `06b2b72`; passive observer ไม่เปลี่ยน production output, thresholds, Landing decision หรือ Counter. Formal รวม `43/44` และ safety controls false `0`

## MVP Release Readiness

- [x] เตรียม T-744 MVP Release Readiness Audit โดยคง detector baseline `752af1d` และไม่เปลี่ยน production behavior
- [x] รัน Windows `testDebugUnitTest`, `lintDebug`, `assembleDebug` และ `assembleRelease`
- [x] ตรวจ static release diagnostic boundary และแก้ blocker ผ่าน T-745
- [x] ตรวจ permissions, privacy, lifecycle และ local History migration policy ครบ; พบ Android backup boundary blocker `KI-024`
- [x] รัน device smoke: 2 jumps, Pause/Resume, 3 jumps, post-stop, Result/History และ persistence หลังเปิดแอปใหม่
- [x] ยืนยัน Release APK จริงไม่มี debug overlay/diagnostic และ basic behavior ผ่าน Actual/App `10/10`
- [x] บันทึก known limitations และตัดสิน checkpoint: T-744 Blocked / ยังไม่พร้อม public release

T-744 พบ release diagnostic boundary blocker จึงเปิด T-745 แบบ bounded fix:

- [x] ครอบ count-evidence และ diagnostic summary panels ด้วย Debug boundary
- [x] หยุดสะสม UI diagnostic payload ใน Release โดยไม่เปลี่ยน Counter/detector
- [x] เพิ่ม regression tests สำหรับ Release-hidden และ Debug-visible behavior
- [x] รัน Windows verification และ Release APK boundary smoke

Checkpoint: T-745 Pass และ KI-023 Resolved; T-744 กลับมาดำเนิน audit ส่วน permissions/privacy, known limitations และ completion decision ต่อ

T-744 final audit พบ `android:allowBackup="true"` โดยไม่มี backup exclusions จึงเปิด `KI-024` และเตรียม T-746:

- [x] กำหนด local-data backup boundary สำหรับ Room, DataStore และ persisted music URI
- [x] เพิ่ม source-level regression verification สำหรับ Manifest และ backup rules
- [x] ยืนยัน merged Release manifest บน Windows
- [x] รัน tests/lint/Debug/Release build และ upgrade-install persistence smoke
- [ ] เตรียม production signing/release packaging นอก repository หลัง privacy blocker ปิด

Checkpoint: T-746 Pass และ KI-024 Resolved; upgrade install รักษา History เดิม 3 sessions และเส้นทาง Home/Settings/Training/Result ผ่านโดยไม่มี stability regression

T-744 ผ่าน completion gate และรับ MVP audit checkpoint แล้ว:

- [x] Automated, Debug/Release boundary, permissions/privacy, Room v1 policy และ device smoke ผ่าน
- [x] บันทึก known limitations
- [x] ปิด KI-023 ผ่าน T-745 และ KI-024 ผ่าน T-746
- [ ] จัดทำ production signing/release packaging โดยไม่เก็บ keystore หรือ secret ใน Git

Public release ยังไม่พร้อมจนกว่า production signing/package และ final signed-artifact verification จะผ่าน งานถัดไปคือ `T-747 — Production Release Signing and Packaging`

## หลัง MVP เท่านั้น

- Backend และ account
- iOS
- ท่ากระโดดเพิ่มเติม
- Scoring, missions, rewards และ leaderboard
- Analytics หรือ cloud features หลังผ่านการทบทวน privacy
