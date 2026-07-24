# RopeSkill Test Log

อัปเดตล่าสุด: 24 กรกฎาคม 2026

## สถานะปัจจุบัน

Milestone 0, Milestone 2, Milestone 3, Milestone 4 และ Pose overlay ใน Milestone 5 ผ่านการทดสอบบน Samsung Galaxy S23 Ultra แล้ว ผู้ใช้ยืนยันว่า Counter, Timer, Navigation, CameraX preview, permission flow, lifecycle และ pose landmarks ทำงานถูกต้อง

เส้นทาง Home → Training → Result → Home และหน้า Training/Result แบบ Power Sport ผ่านการทดสอบบนอุปกรณ์แล้ว Single Start ผ่าน แต่ detector รอบสองถดถอยเป็น fast 1/10, slow 0/10 และ medium 0/10 พร้อม knee-lift false positive 5 ครั้ง Countdown ยกเลิกได้ตลอดแต่ไวต่อการขยับเล็กน้อยเกินไป จึงคืน detector/Countdown behavior รอบแรกและเพิ่ม diagnostic overlay แบบไม่เก็บ pose data; รอทดสอบยืนยันบนอุปกรณ์ Workspace ของ Codex ไม่มี Android SDK และ Gradle Wrapper จึงยังไม่สามารถรัน `assembleDebug` ภายใน workspace ได้

## Test Environment

| รายการ | ค่า |
|---|---|
| Device model | Samsung Galaxy S23 Ultra (ยืนยันเป็นเครื่องทดสอบหลัก) |
| Android version | รอกรอก |
| CPU / RAM | รอกรอก |
| App version / commit | `0.1.0` / `a2bea75` สำหรับ Milestone 5 |
| Build type | `debug` |
| Camera | Front (build ถัดไปรอทดสอบ) |
| Lighting | รอกรอก |
| Distance from camera | รอกรอก |

## Build and Run Tests

| Test ID | วันที่ | ขั้นตอน | ผลที่คาดหวัง | ผลจริง | สถานะ |
|---|---|---|---|---|---|
| T-001 | 22 ก.ค. 2026 | Build `debug` ใน Android Studio | Build สำเร็จ | Android Studio แสดง `BUILD SUCCESSFUL` | Pass |
| T-002 | 22 ก.ค. 2026 | ติดตั้งและเปิดแอปบน Samsung Galaxy S23 Ultra | แอปเปิดโดยไม่ crash | แสดง `RopeSkill` และ `Milestone 0: Android sample app` | Pass |

## Feature Tests

| Test ID | Feature | Scenario | Expected | Actual | Status |
|---|---|---|---|---|---|
| T-101 | Counter | กดเพิ่ม Counter ระหว่าง Running | ค่าเพิ่มหนึ่งครั้งต่อการกด | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-102 | Timer | Start, Pause, Resume, Finish, Reset และ background | เวลาและสถานะเปลี่ยนถูกต้อง | ผู้ใช้ยืนยันว่าทุกขั้นตอนทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-201 | Navigation | Home → Training → Result → Home รวม Back และเริ่ม Session ใหม่ | ไปแต่ละหน้าถูกต้อง, Timer หยุดเมื่อออก และ Session ใหม่เริ่มจากศูนย์ | ผู้ใช้ยืนยันว่าทุกขั้นตอนทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-202 | Power Sport Home UI | เปิดแอป ตรวจ layout และกด Start Training | หน้า Home แสดงครบโดยไม่ล้น และปุ่มเปิดหน้า Training ได้ | ผู้ใช้ยืนยันว่าเข้าสู่หน้า Training ได้ตามเดิม; ยังไม่ได้ยืนยันรายละเอียด layout ทุกจุด | Partial Pass |
| T-203 | Power Sport Training UI | เปิด Training ตรวจกล้อง, Counter, Timer และปุ่มควบคุม | องค์ประกอบครบ ไม่ล้นจอ และการทำงานเดิมไม่เปลี่ยน | ผู้ใช้ยืนยันว่า layout, Camera preview, Pose overlay, Counter, Timer และปุ่มควบคุมผ่านทั้งหมด | Pass |
| T-204 | Power Sport Result UI | กด Finish ตรวจผล Session และกลับหน้า Home | แสดง Jumps และ Time ถูกต้อง, layout ไม่ล้น และปุ่มกลับ Home ทำงาน | ผู้ใช้ยืนยันว่า Result layout, Jumps, Time และ Back to Home ผ่านทั้งหมด | Pass |
| T-205 | Power Sport UI hierarchy refresh | ตรวจ Home, Training และ Result หลังปรับลำดับข้อมูล | Home/Result เลื่อนได้เมื่อพื้นที่จำกัด, Jumps/Time อ่านง่าย, camera preview ไม่ถูกบีบ, status pill และปุ่มทุกปุ่มแสดงครบ | Build, Home/Training/Result layout, camera preview, ปุ่มทั้งหมด, Reset responsiveness และ GO โดยไม่ต้องกระโดด ผ่านบนอุปกรณ์จริง | Pass |
| T-206 | Minimal card UI refresh | ตรวจ Home, Training และ Result หลังลดข้อความและ Card ซ้อน | Home แสดง jump-rope mark และ workout แบบไร้กรอบ; Training มี Pause/Finish, Reset อยู่ในเมนูพร้อมยืนยัน และ TEST +1 แสดงเฉพาะ Debug; Result มี metric cards ใหญ่สองใบ | ปรับ Compose UI แล้ว; รอ Build และตรวจบน Samsung Galaxy S23 Ultra | Not Run |
| T-207 | Settings Phase 1 | เปิด Settings จาก Home, แก้ nickname/countdown/units/sound/vibration, ปิดและเปิดแอปใหม่ แล้วเริ่ม Training | ปุ่มเฟืองกดง่าย, ค่าคงอยู่หลังเปิดใหม่, countdown ตรงค่าที่เลือก และ sound/vibration ทำตาม toggle โดย Home/Training/Result เดิมไม่ถดถอย | ผู้ใช้ยืนยันว่า Settings Phase 1 ทำงานปกติบนอุปกรณ์จริงเมื่อ 24 กรกฎาคม 2026 | Pass |
| T-208 | Theme preference | เลือก System default, Dark และ Light จาก Settings แล้วตรวจ Home/Settings/Result/Training รวมปิดและเปิดแอปใหม่ | Theme เปลี่ยนทันทีและคงอยู่ทุกหน้า; Training chrome ใช้ theme ที่เลือก แต่ Camera surface/overlay คง Dark; ข้อความ โลโก้ และปุ่มอ่านได้ชัด | Light theme ทำงานใน Home/Settings/Result แต่ Training ยัง Dark ทั้งหน้า จึงปรับให้ Training chrome ใช้ theme และรอทดสอบซ้ำบน Samsung Galaxy S23 Ultra | Partial Pass |
| T-209 | Ready status feedback | ยืนนิ่งจนระบบผ่าน Positioning และเริ่ม Countdown จากนั้นขยับจนเสีย Ready Position | ระหว่าง Countdown pill แสดง `READY` สีเขียว; เมื่อเสียตำแหน่งกลับเป็น `POSITIONING` สีส้ม โดยการนับและ detector logic ไม่เปลี่ยน | รอทดสอบบน Samsung Galaxy S23 Ultra | Not Run |
| T-301 | Camera permission | Allow | แสดง preview | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-302 | Camera permission | Deny และกลับเข้า Training | แสดงคำอธิบาย/ทางเลือกโดยไม่ crash | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-303 | Camera lifecycle | ออกจาก Training แล้วเข้าใหม่ | กล้องถูกปล่อยและเปิดใหม่ได้ | ผู้ใช้ยืนยันว่าทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-401 | Pose | ยืนให้เห็นร่างกาย, เคลื่อนไหว, หมุนจอ, ออกจากเฟรมและกลับเข้า, ทดสอบ lifecycle | landmarks ติดตามร่างกายและแอปทำงานต่อเนื่อง | ผู้ใช้ยืนยันว่าทุกขั้นตอนทำงานถูกต้องบนอุปกรณ์ | Pass |
| T-402 | Front camera | เปิด Training และยกแขนซ้าย/ขวา | preview แสดงแบบกระจกและ overlay ตรงกับร่างกายด้านเดียวกัน | รอกรอก | Not Run |
| T-501 | Basic Bounce baseline | กระโดด 10 ครั้ง จำนวน 3 รอบ | นับใกล้เคียงจำนวนจริง | นับ 1/10 ทั้ง 3 รอบในแสงปกติ; พลาดการกระโดดต่อเนื่อง | Fail |
| T-502 | Basic Bounce false positive baseline | ยืน, ย่อเข่า, ยกแขน และเดินเล็กน้อยโดยไม่กระโดด | Counter ไม่เพิ่ม | Standing 0, knee bends 1, arm movements 0, small steps 1; การเดินไปกด Pause ถูกนับ 1 | Fail |
| T-503 | Ready position | กด Start แล้วยืนเต็มตัวและนิ่ง | แสดง HOLD STILL แล้วเริ่ม countdown อัตโนมัติ | ผู้ใช้ยืนยัน Ready detection และ countdown 5–1 ทำงาน | Pass |
| T-504 | Countdown cancellation | ขยับ/ออกจากเฟรมระหว่าง 5–1 | ยกเลิกและกลับ HOLD STILL โดย Counter/Timer ยังเป็นศูนย์ | ยกเลิกได้บางช่วง แต่หลังผ่านเลข 5 การขยับบางแบบไม่ยกเลิก | Partial Pass |
| T-505 | First-jump start | รอข้อความ START แล้วกระโดด | Timer เริ่มที่ Takeoff และ Landing แรกนับเป็น 1 | ผู้ใช้ยืนยันทั้ง Timer start และ first jump | Pass |
| T-506 | Tuned Basic Bounce | กระโดด 10 ครั้งที่ fast, slow และ medium | อย่างน้อย 8/10 ต่อรอบ | Fast 4/10, slow 10/10, medium 10/10 | Fail |
| T-507 | Tuned false positives | ยืน, ย่อเข่า, ยกแขน และเดินเล็กน้อย | ไม่เกิน 1 ครั้งต่อกิจกรรม | Standing 0, knee bends 5, arm movements 0, small steps 1 | Fail |
| T-508 | Single-action start | กด `START TRAINING` ที่ Home หนึ่งครั้ง | หน้า Training เข้าสู่ HOLD STILL โดยไม่มีปุ่ม START ซ้ำ | Single Start และไม่มีปุ่ม Start ซ้ำผ่าน; Resume ยังไม่ได้ระบุผลชัดเจน | Partial Pass |
| T-509 | Detector tuning round 2 | ทดสอบ fast/slow/medium และกิจกรรม false positive ซ้ำ | Fast ≥8/10, slow/medium ≥9/10, false positive ≤1 ต่อกิจกรรม | Fast 1/10, slow 0/10, medium 0/10; knee lift 5, รายการอื่น 0 | Fail |
| T-510 | Restored detector with diagnostics | ทดสอบ fast/slow/medium, knee lift และสังเกตข้อความ DETECTOR | คืน slow/medium ≥9/10 และข้อความแสดงเหตุผลก่อน Takeoff โดยไม่เก็บ pose data | Build/Single Start/Resume ผ่าน, Countdown ดีขึ้น; fast 4/10, slow 10/10, medium 8/10; knee lift ซ้าย 3/5 และขวา 5/5 ถูกนับผิด ข้อความหลังนับถูก `CALIBRATING` เขียนทับ | Partial Pass |
| T-511 | Latched count evidence | ทดสอบ Basic Bounce และ knee lift ซ้าย/ขวา แล้วอ่าน `LAST COUNT` หลัง Counter เพิ่ม | ค่า L/R/H/Δ/ms จาก Takeoff ที่นำไปสู่การนับค้างอยู่จนกว่าจะมีการนับใหม่ และถูกล้างเมื่อเริ่มรอบใหม่ โดยไม่เปลี่ยนผลนับจากรุ่น `4855945` | Build ผ่านและการนับปกติไม่เปลี่ยน; ตัวอย่าง Slow Bounce มี ΔR 0.007, knee lift ซ้าย 0.070 และขวา 0.110 แต่ knee lift ขวาที่ ΔR 0.110 ขัดกับ limit 0.080 ใน source จึงต้องยืนยันค่าที่ใช้ตัดสินจริง | Partial Pass |
| T-512 | Takeoff synchronization evidence V2 | ทดสอบ Basic Bounce และ knee lift แล้วอ่าน `LAST COUNT V2` | แสดง DIFF, LIMIT และ SYNC จาก expression เดียวกับที่ detector ใช้ ณ Takeoff โดยไม่เปลี่ยน logic การนับ | Build และการนับปกติผ่าน; Right knee lift 0.0150/0.0262 PASS, Left knee lift 0.0025/0.0256 PASS, Slow Bounce 0.0129/0.0272 PASS จึงยืนยันว่า DIFF/SYNC ไม่สามารถแยก knee lift จาก Basic Bounce ในตัวอย่างนี้ | Pass |
| T-513 | Three-count evidence history | ทดสอบ Slow, Medium, Fast, Left knee lift และ Right knee lift แยก session ละ 3 counts แล้วอ่าน `COUNT HISTORY V3` | แสดงหลักฐาน 3 ครั้งล่าสุดเฉพาะครั้งที่ Counter เพิ่ม ล้างเมื่อ Start/Resume/Reset และไม่เปลี่ยน detector | Build, การนับปกติ และ V3 ผ่าน; Basic Bounce 9 ตัวอย่างมี H/average(L,R) 1.45–2.42 ส่วน knee lift false positive 3 ตัวอย่างมี 0.39–0.79 | Pass |
| T-514 | Hip-to-ankle Takeoff filter | ทดสอบ Slow/Medium/Fast อย่างละ 10 ครั้ง และ knee lift ซ้าย/ขวาอย่างละ 5 ครั้ง | Basic Bounce Slow/Medium ≥9/10, Fast ≥8/10 และ knee lift false positive ≤1/5 ต่อข้าง | Build และ unit tests ผ่าน; Slow 10/10, Medium 6/10, Fast 10/10, knee lift ซ้าย 0/5 และขวา 0/5 false positives; เลข 1 ที่เห็นเกิดจากการกระโดดจริงเพื่อปลดสถานะ ARMED ไม่ใช่ knee lift | Partial Pass |
| T-515 | Reset from Training | ระหว่าง Positioning, Countdown, Running และ Paused กด `RESET` | Counter/Timer/หลักฐานเป็นศูนย์, detector กลับ `POSITIONING` และเข้าสู่ calibration รอบใหม่ได้โดยไม่ต้องออกจากหน้า Training | ผู้ใช้ยืนยันว่า Reset ยังตอบสนองตามปกติบนอุปกรณ์จริง | Pass |
| T-516 | Countdown starts session | ยืนนิ่งจน Countdown 5–1 จบโดยไม่กระโดด | แสดง `GO!` และ Timer เริ่มทันที; Counter คง 0 จนเกิด Basic Bounce จริง | ผู้ใช้ยืนยันว่า GO เริ่มได้โดยไม่ต้องกระโดดบนอุปกรณ์จริง | Pass |
| T-517 | Medium diagnostic V4 | เริ่ม Session ใหม่แล้วกระโดด Medium Basic Bounce 10 ครั้ง จากนั้นอ่าน `MEDIUM DIAGNOSTIC V4` | แสดงจำนวน transition ของ ANK/HIP/SYNC/AIR/LAND โดยไม่เปลี่ยน Counter หรือ detector logic | ผู้ใช้ยืนยัน 10 actual / 10 detected; V4 แสดง ANK 18, HIP 5, SYNC 11, AIR 10 และ LAND 10 | Pass |
| T-518 | Positioning distance guidance | ทดสอบยืนใกล้เกินไป, ไกลเกินไป, ร่างกายไม่ครบ และระยะเหมาะสมก่อน Countdown | แสดง `STEP BACK`, `MOVE CLOSER`, `SHOW FULL BODY` และ `DISTANCE GOOD / HOLD STILL` ตามลำดับ; Countdown เริ่มเฉพาะเมื่อระยะเหมาะสมและ calibration ผ่าน | `STEP BACK`, `SHOW FULL BODY` และ `DISTANCE GOOD / HOLD STILL` ผ่าน; `MOVE CLOSER` ทำงานแต่เตือนเมื่ออยู่ไกลมาก จึงเพิ่ม minimum body height จาก 0.42 เป็น 0.50 และรอทดสอบซ้ำ | Partial Pass |
| T-519 | Training overlay readability | ตรวจคำแนะนำ Positioning ทุกข้อความ รวมแถบ Tracking/Detector ที่ด้านล่างของกล้อง | ข้อความกลางไม่ชนกันและไม่ถูกตัด; Tracking/Detector แบ่งเป็นสองคอลัมน์โดยไม่ซ้อนหรือบังกัน | ผู้ใช้ทดสอบคำแนะนำ Positioning ครบและภาพล่าสุดยืนยันว่า Tracking/Detector แยกสองคอลัมน์อ่านได้ | Pass |
| T-520 | Detector repeatability | Medium Basic Bounce เพิ่ม 2 รอบ รอบละ 10 ครั้ง แล้วทดสอบ knee lift ซ้าย/ขวาอย่างละ 5 ครั้ง | Medium ≥9/10 ทุกรอบ และ knee lift false positive ≤1/5 ต่อข้าง | Medium 9/10 และ 10/10; เมื่อรวมรอบก่อนเป็น 29/30; knee lift ซ้าย 0/5 และขวา 0/5 | Pass |
| T-601 | Room session storage | จบ Session ที่มีเวลาฝึกจริง ตรวจ Result ปิดแอป เปิดใหม่ แล้วตรวจ latest row ด้วย Database Inspector | เก็บ exercise type, start/completion time, duration และ jump count ตรงกับ Result; ไม่บันทึก pose/camera/diagnostic | ผู้ใช้ยืนยัน Session 3 jumps ถูกต้องและ row ยังคงอยู่หลังปิดและเปิดแอปใหม่บน Samsung Galaxy S23 Ultra | Pass |
| T-602 | Ignore incomplete session | เข้า Training แล้วกด Finish ก่อน Countdown จบ | ไม่สร้าง Session row ที่ duration เป็นศูนย์ | ผู้ใช้ยืนยันจำนวน row คงเดิมที่ 1 | Pass |
| T-603 | Training History | เปิด Training History จาก Home เมื่อมี Session เดิม จากนั้นสร้าง Session ใหม่และกลับมาเปิด History อีกครั้ง | แสดงรายการใหม่สุดก่อน พร้อมวันที่/เวลา, jumps และ duration; Back กลับ Home; ข้อมูลคงอยู่หลังเปิดแอปใหม่ และอ่านได้ทั้ง Dark/Light | ผู้ใช้ยืนยัน Session เดิม 3 jumps, Session ใหม่ 2 jumps เรียงถูกต้อง, วันที่/เวลาและ duration ถูกต้อง, Back/การเปิดใหม่/Dark/Light ผ่านทั้งหมด | Pass |
| T-604 | CTA labels without arrows | ตรวจปุ่ม `START TRAINING`, `TRAINING HISTORY` และ `BACK TO HOME` ใน Dark/Light | ทั้งสามปุ่มไม่มีลูกศร ข้อความอยู่กึ่งกลางและ navigation เดิมทำงาน | ผู้ใช้ยืนยันการเปลี่ยนแปลงเรียบร้อย | Pass |
| T-605 | Room schema version 1 | Build เพื่อ export schema แล้วรัน `RoomSchemaTest` บน Samsung Galaxy S23 Ultra | สร้าง `1.json`; test สร้าง database version 1 และพบคอลัมน์ id, exerciseType, start/completion time, duration และ jumpCount ครบ | ผู้ใช้ยืนยัน `RoomSchemaTest` 1/1 และ Build ผ่านบน Samsung Galaxy S23 Ultra; ใช้ Room 2.7.2 เพื่อเข้ากับ serialization 1.7.3 ของแอป | Pass |
| T-606 | Camera permission overlay | เปิด Training ขณะที่ยังไม่อนุญาต Camera permission แล้วอนุญาตจากปุ่ม `ALLOW CAMERA`; ปิดและเปิดแอปแล้วทดสอบซ้ำ | ก่อนอนุญาตเห็นเฉพาะข้อความและปุ่ม permission โดยไม่มี Positioning/Tracking/Detector/diagnostic บัง; หลังอนุญาต overlay กลับมาและกล้องทำงาน | ผู้ใช้ยืนยันการทดสอบรอบสุดท้ายผ่านทั้งก่อนและหลังปิด–เปิดแอป โดย permission message ไม่ถูก overlay บัง | Pass |
| T-607 | Training action colors | ตรวจ `PAUSE`, `RESUME` และ `FINISH` ระหว่าง Training ใน Dark/Light | Pause/Resume ใช้สี primary; Finish ใช้กรอบและข้อความสี error; ข้อความอ่านชัด สถานะ disabled ถูกต้อง และการกดทำงานเหมือนเดิม | ผู้ใช้ยืนยันสีและการทำงานของปุ่มผ่าน | Pass |
| T-701 | App interruption and session integrity | ออกจากแอประหว่าง Positioning, Countdown และ Running; ล็อก/ปลดล็อกระหว่าง Running; กลับมา Finish แล้วตรวจ History และเปิดแอปใหม่ | แอปไม่ crash; กล้องกลับมาทำงาน; Timer ไม่เดินใน background; Session ที่ Finish เพิ่มหนึ่งรายการ ข้อมูลถูกต้องและคงอยู่หลังเปิดใหม่ | ผู้ใช้ยืนยัน Positioning/Camera recovery ผ่าน; Countdown ต้องกด Resume แล้วกลับ Positioning; Timer ไม่เดินระหว่าง background/ล็อกหน้าจอ; History เพิ่มหนึ่งรายการและคงอยู่หลังเปิดใหม่; ไม่มี crash/freeze | Pass |
| T-702 | MediaPipe performance baseline | เปิด Training ให้เห็นร่างกายครบและบันทึก `PERF V1` หลังทำงานต่อเนื่อง 30, 60 และ 120 วินาที | รายงาน result FPS, average/max inference latency, IN/OUT และ estimated SKIP โดยไม่เปลี่ยน detector; แอปไม่ค้างและตัวเลขอัปเดตไม่เกินหนึ่งครั้งต่อวินาที | รอบต่อเนื่องเดียวกันจากวิดีโอ 145.9 วินาที: ~30 วินาที 24.1 FPS, 25/49ms, IN/OUT 710/710, SKIP~0; ~60 วินาที 23.8 FPS, 26/49ms, 1423/1423, SKIP~0; ~120 วินาที 24.0 FPS, 28/63ms, 2862/2862, SKIP~0; PERF อัปเดต, preview ลื่น, ไม่ค้าง/crash, ไม่ auto-pause, แบตเตอรี่แสดง 100→100 และผู้ใช้รายงานว่าเครื่องไม่ร้อน โดยรอบนี้เปิด screen recording จึงไม่เปรียบเทียบ FPS โดยตรงกับรอบก่อนที่ไม่ได้ยืนยันเงื่อนไขเดียวกัน | Pass |
| T-703 | Auto-pause on tracking loss | ระหว่าง Running เดินออกจากเฟรมเกิน 1 วินาที แล้วกลับเข้ากล้อง | Timer หยุด, สถานะเป็น Paused และปุ่มเป็น Resume; กด Resume แล้วผ่าน Positioning/Countdown ก่อนนับต่อ; landmark สะดุดสั้นกว่า 1 วินาทีไม่ทำให้ Pause | ผู้ใช้ยืนยัน long tracking loss ทำให้ auto-pause และ Timer หยุด; Resume กลับ Positioning, เริ่ม Countdown ใหม่ และนับต่อจากค่าเดิม; tracking loss สั้นไม่ pause; History ถูกต้องและไม่มี crash/freeze | Pass |
| T-704 | Stuck-airborne recovery | ระหว่าง Running ทำให้ detector เข้า `AIRBORNE`, Pause/Resume แล้วทดสอบกระโดดต่อหลังผ่าน Positioning/Countdown | ถ้ายังไม่พบ Landing ภายใน 1.5 วินาทีให้เริ่ม calibration ใหม่โดยไม่เพิ่ม Counter; หลัง calibration ต้องนับ Basic Bounce รอบถัดไปได้ และ Result/History ถูกต้อง | เพิ่ม recovery path และ unit regression tests แล้ว; Gradle ใน workspace ยังรันไม่ได้เพราะดาวน์โหลด distribution ไม่ได้ จึงรอ Build/unit test และทดสอบบน Samsung Galaxy S23 Ultra | Not Run |

## Jump Detection Accuracy Template

| Run | Ground truth jumps | Detected jumps | Missed | False positives | Accuracy note |
|---|---:|---:|---:|---:|---|
| 1 |  |  |  |  |  |
| 2 |  |  |  |  |  |
| 3 |  |  |  |  |  |

สูตรที่ใช้รายงานเบื้องต้น:

```text
Count Error (%) = abs(Detected - Ground Truth) / Ground Truth × 100
```

สูตรนี้ยังไม่เพียงพอสำหรับแยก missed jumps กับ false positives จึงต้องบันทึกทั้งสองค่าด้วยเมื่อวิเคราะห์วิดีโอหรือมีผู้ช่วยนับ

## Performance Template

| Scenario | Preview FPS | Inference FPS | Median latency | CPU | Memory | Battery/Heat | Notes |
|---|---:|---:|---:|---:|---:|---|---|
| Idle preview |  |  |  |  |  |  |  |
| Pose active 1 min |  |  |  |  |  |  |  |
| Training 10 min |  |  |  |  |  |  |  |

## Test Session Template

```text
### Test Session YYYY-MM-DD-NN
- Tester:
- Device / Android:
- App build / commit:
- Environment:
- Preconditions:
- Steps:
- Expected:
- Actual:
- Logs/screenshots:
- Privacy note: ห้ามแนบภาพบุคคลโดยไม่ได้รับความยินยอม
- Result: Pass | Fail | Blocked
- Related issue:
```
