# RopeSkill Architecture Decisions

## ADR-017 — ใช้ Room เก็บ Training Session summary

- **Status:** Accepted
- **Decision:** ใช้ Room `2.7.2` กับ KSP สร้าง database schema version 1 โดยเก็บเฉพาะ exercise type, เวลาเริ่ม/จบ, duration และ jump count; บันทึกเมื่อ Finish เฉพาะ Session ที่เริ่ม Running แล้วและมี duration มากกว่าศูนย์ รุ่นนี้เข้ากับ `kotlinx.serialization` 1.7.3 ที่ dependency graph ของแอปบังคับใช้
- **Reason:** Session history เป็นข้อมูลหลายรายการที่ต้อง query ตามเวลาและรองรับ migration ซึ่งเหมาะกับ Room มากกว่า Preferences DataStore; summary เพียงพอสำหรับ Result/History โดยไม่เก็บข้อมูลร่างกาย
- **Affected areas:** `TrainingViewModel`, local persistence, process recreation, Training History และ migration tests
- **Privacy:** ไม่บันทึกภาพ วิดีโอ pose landmarks หรือ detector diagnostics
- **Revisit when:** เพิ่ม field ใหม่, เปิด Training History, ต้อง export/sync หรือเปลี่ยน database schema version

## ADR-016 — ใช้ jump-rope mark เป็นโลโก้และ Adaptive App Icon

- **Status:** Accepted
- **Decision:** ใช้โลโก้ Dynamic Sport รูปคนสีครีมกระโดดเชือกสีส้มเป็น Hero Logo ขนาด 152 dp เพียงจุดเดียวบนหน้า Home กึ่งกลางเหนือ `TRAIN STRONGER.` โดย Header คงเฉพาะ `ROPESKILL` และ `MVP`; ใช้ตราสัญลักษณ์เดียวกันเป็น Android Adaptive Icon บนพื้นหลังสีน้ำเงินเข้มพร้อม monochrome layer และ inset 10 dp รอบ foreground
- **Reason:** ลดโลโก้ซ้ำ, ทำให้รายละเอียดท่าทางและเชือกหนึ่งเส้นอ่านได้ชัดบนพื้นหลังมืด, รักษาภาพจำเดียวกันระหว่างหน้า Home กับ Launcher และป้องกัน launcher mask ตัดเชือก
- **Affected areas:** Home header, Home hero artwork, Android launcher icon และ themed icon บน Android 13 ขึ้นไป
- **Revisit when:** การทดสอบบนโทรศัพท์จริงพบว่าโลโก้เล็กเกินไป, ถูก launcher mask ตัด, themed icon อ่านรูปทรงไม่ชัด หรือมี brand identity ฉบับสมบูรณ์

## ADR-015 — ลด Card ซ้อนและแยก secondary controls ออกจาก Training

- **Status:** Accepted
- **Decision:** หน้า Home ใช้ jump-rope mark แทนข้อความรองและแสดง workout summary แบบไร้กรอบ; หน้า Training คง Pause/Resume กับ Finish เป็นปุ่มหลัก ย้าย Reset เข้า overflow menu พร้อม confirmation และแสดง TEST +1 เฉพาะ Debug build; หน้า Result ใช้ metric cards ขนาดใหญ่สองใบโดยไม่มี Card ครอบอีกชั้น
- **Reason:** ลดความหนาแน่นของข้อความและ Card ซ้อน พร้อมทำให้ action และผลลัพธ์สำคัญเห็นได้เร็วระหว่างใช้งาน
- **Affected areas:** `Screens.kt`, Debug build configuration และการทดสอบ Home/Training/Result layout
- **Revisit when:** ผู้ใช้หา Reset ไม่พบ, touch target ของ overflow menu ไม่เหมาะสม หรือหน้าจอขนาดเล็กเกิดการล้น

## ADR-014 — จัดลำดับ Training UI ให้กล้องและสถานะเป็นข้อมูลหลัก

- **Status:** Accepted
- **Decision:** แสดง Jumps/Time ก่อน camera preview, ใช้ status pill ที่มีทั้งสีและข้อความ, รวม Pause/Resume ไว้ตำแหน่งหลักเดียวกัน และลด Manual count/Reset เป็น secondary actions
- **Reason:** ผู้ใช้ต้องอ่านสถานะและผลนับระหว่างเคลื่อนไหวได้เร็ว พร้อมรักษาพื้นที่ camera preview และลดโอกาสกด action รองโดยไม่ตั้งใจ
- **Affected areas:** Home, Training, Result, Material 3 color scheme และการทดสอบ layout บนอุปกรณ์จริง
- **Revisit when:** การทดสอบบน Samsung Galaxy S23 Ultra พบข้อความล้น, touch target ไม่เหมาะสม, กล้องมีพื้นที่ไม่พอ หรือ diagnostic overlay บังร่างกาย

## ADR-013 — ใช้ Ready Check และ Countdown ก่อนเริ่มนับ

- **Status:** Accepted for testing
- **Decision:** การกด `START TRAINING` ที่หน้า Home ให้เปิด Training และเข้าสู่ `Positioning` อัตโนมัติ โดยไม่แสดงปุ่ม `START` ซ้ำ ส่วน `RESUME` หลัง Pause ยังคงเป็นคำสั่งโดยตั้งใจ จากนั้นใช้ state `Positioning → Countdown (5–1) → Running (GO!)` โดยเริ่ม Timer ทันทีเมื่อ Countdown จบ และนับการกระโดดจริงครั้งแรกหลัง `GO!`
- **Reason:** ผู้ใช้มีเวลาจัดตำแหน่งโดยไม่ต้องเดินกลับไปแตะโทรศัพท์ ลด false positive จากการเดิน และไม่สับสนกับคำสั่ง Start สองครั้ง
- **Affected areas:** `TrainingViewModel`, Training overlay, detector events, Pause/Resume และ real-device test protocol
- **Revisit when:** Countdown ยกเลิกบ่อยเกินไป, ready check ใช้เวลานานเกินไป หรือการตรวจ Takeoff แรกไม่น่าเชื่อถือ

การทดสอบ stability threshold ที่ 2–2.5% ทำให้ Countdown เริ่มใหม่จากการขยับเล็กน้อยบ่อยเกินไป จึงคืน behavior รอบแรกชั่วคราว การยกเลิกตลอดช่วง 5–1 ยังเป็นข้อกำหนดที่ต้องออกแบบใหม่จากข้อมูล landmark jitter จริง ใช้ `SystemClock.elapsedRealtime()` สำหรับ Timer เช่นเดิม

ระหว่าง Running หาก detector รายงาน `WAITING / FULL_BODY_REQUIRED` ต่อเนื่อง 1 วินาที ให้ ViewModel เรียก Pause flow เดิมเพื่อหยุด Timer และบังคับ Resume ผ่าน Positioning ใหม่ การหน่วงเวลานี้อยู่ภายนอก `BasicBounceDetector` เพื่อไม่ให้ landmark สะดุดชั่วครู่หยุด Session และไม่เปลี่ยน threshold การนับ

## ADR-012 — ใช้ ankle-baseline state machine เป็น Basic Bounce baseline

- **Status:** Accepted for testing
- **Decision:** ใช้แกน Y ของข้อเท้าและสะโพกทั้งสองข้าง สร้าง standing baseline 45 เฟรม ปรับ takeoff/landing threshold ตามความยาวช่วงสะโพกถึงข้อเท้า และนับเมื่อสถานะเปลี่ยน `Grounded → Airborne → Grounded` การ Takeoff ต้องรักษาความต่างระดับของเท้าทั้งสองใกล้ baseline, สะโพกกับข้อเท้าเคลื่อนขึ้นในระยะใกล้เคียงกัน และเท้าไม่เลื่อนแนวนอนเกินขอบเขต
- **Reason:** เป็นวิธี on-device ที่เรียบง่าย อธิบายและปรับค่าได้ ไม่ผูกกับความละเอียดภาพหรือระยะกล้องแบบค่าพิกเซลตายตัว และป้องกันการนับหลายครั้งจากการกระโดดครั้งเดียว
- **Affected areas:** Training counter, คำแนะนำการจัดเฟรม, detection accuracy และ test protocol
- **Revisit when:** ผลทดสอบพบ missed jumps/false positives สูง, กล้องสั่น, มุมกล้องเปลี่ยน หรือจำเป็นต้องรวม velocity, hip trajectory, foot contact หรือ temporal model

ผลปรับรอบแรกบน Samsung Galaxy S23 Ultra คือ fast `4/10`, slow `10/10`, medium `10/10`; false positive คือ standing 0, knee bends 5, arm movements 0 และ small steps 1 รอบสองที่เพิ่ม vertical coherence พร้อมเปลี่ยน threshold, smoothing และ cooldown ถดถอยเป็น fast `1/10`, slow/medium `0/10` และ knee lift 5 จึงยกเลิกรอบสองและคืนค่ารอบแรก เพิ่ม diagnostic overlay แบบ transient เพื่อแสดงเงื่อนไขล่าสุดโดยไม่ log หรือเก็บ landmark ก่อนปรับทีละตัวแปร

ผลยืนยัน detector ที่คืนค่าแล้วคือ fast `4/10`, slow `10/10`, medium `8/10`; knee lift ซ้ายถูกนับผิด `3/5` และขวา `5/5` แต่ diagnostic ถูกสถานะ calibration หลัง Landing เขียนทับ จึงให้ค้างเฉพาะค่าเชิงสรุปจาก Takeoff ที่นำไปสู่ Count (`L/R/H/Δ` เป็นสัดส่วนต่อความยาวขาและ airborne time) บนหน้าจอ ไม่บันทึกภาพ, landmark หรือ diagnostic ลง storage และยังไม่เปลี่ยน threshold จนกว่าจะมีข้อมูลเปรียบเทียบจากอุปกรณ์จริง

หลักฐาน `COUNT HISTORY V3` จาก Basic Bounce ที่ถูกนับ 9 ตัวอย่างและ knee lift false positive 3 ตัวอย่างพบว่า `hipRise / averageAnkleRise` ของ Basic Bounce อยู่ที่ `1.45–2.42` แต่ knee lift อยู่ที่ `0.39–0.79` จึงเพิ่มเงื่อนไข Takeoff `hipRise >= averageAnkleRise × 1.10` โดยคง threshold, smoothing, cooldown และ landing logic เดิม ค่า `1.10` ต้องผ่านการทดสอบ Slow/Medium/Fast และ knee lift ซ้ำบนอุปกรณ์จริง และให้ทบทวนเมื่อมีตัวอย่าง Basic Bounce ต่ำกว่า threshold หรือ knee lift ผ่าน threshold

Detector ไม่เก็บ landmark history นอกหน่วยความจำที่จำเป็นสำหรับ state ปัจจุบัน และล้าง calibration เมื่อ Pause, Finish, Reset หรือ landmarks สำคัญหายต่อเนื่อง

## ADR-011 — ใช้ MediaPipe Pose Landmarker Lite ในโหมด Live Stream

- **Status:** Accepted
- **Decision:** ใช้ `tasks-vision 0.10.35`, `pose_landmarker_lite.task`, CPU delegate, ผู้ใช้หนึ่งคน และ `LIVE_STREAM`; CameraX ใช้ `STRATEGY_KEEP_ONLY_LATEST` บน single background executor
- **Reason:** Lite model เหมาะกับการวัด baseline latency บนอุปกรณ์จริง, live-stream tracking ลดงานตรวจจับซ้ำ และ latest-frame strategy ป้องกัน frame backlog
- **Affected areas:** App size, camera analysis, pose latency, overlay และ resource cleanup
- **Revisit when:** การทดสอบความแม่นยำต้องใช้ Full/Heavy model, CPU latency ไม่ผ่านเกณฑ์ หรือ GPU delegate ให้ผลที่เสถียรกว่าบนอุปกรณ์เป้าหมาย

Overlay หมุน input ตาม CameraX metadata ก่อน inference และแปลง normalized landmarks ด้วย center-crop scale เดียวกับ `PreviewView.ScaleType.FILL_CENTER` เมื่อใช้กล้องหน้าจะสะท้อนเฉพาะพิกัด X ของ overlay ให้ตรงกับ preview แบบกระจก โดยไม่สะท้อน input ของ MediaPipe

Debug build แสดง `PERF V1` ไม่เกินหนึ่งครั้งต่อวินาที โดยรายงาน Pose result FPS, average/max inference latency, จำนวน frame ที่ส่ง/ได้ผล และ skipped frames โดยประมาณหลังหักหนึ่ง frame ที่อาจกำลังประมวลผล Metrics อยู่ในหน่วยความจำเฉพาะ Session และไม่บันทึกลง Room

## ADR-010 — ใช้ CameraX PreviewView ผ่าน Compose AndroidView

- **Status:** Accepted
- **Decision:** ใช้ CameraX `Preview` กับกล้องหน้าเป็นค่าเริ่มต้นและแสดงผ่าน `PreviewView` ที่ฝังใน Compose ด้วย `AndroidView`
- **Reason:** เป็น API stable ที่จัดการ surface และ device compatibility ให้ ผู้ฝึกเห็นภาพตัวเองระหว่าง Session และต่อยอด `ImageAnalysis` สำหรับ MediaPipe ได้
- **Affected areas:** Training screen, Camera permission, lifecycle และ camera resource cleanup
- **Revisit when:** CameraX Compose API ให้ประโยชน์ที่วัดได้ หรือผู้ใช้ต้องการปุ่มสลับกล้องหน้า/หลัง

## ADR-009 — ใช้ Navigation Compose และ shared ViewModel สำหรับผล Session ชั่วคราว

- **Status:** Accepted
- **Decision:** ใช้ Navigation Compose สำหรับ Home, Training และ Result โดยเก็บผล Session ปัจจุบันใน `TrainingViewModel` ที่ scope ระดับ Activity
- **Reason:** Navigation component จัดการ back stack อย่างสม่ำเสมอ และ Result อ่าน immutable UI state เดิมได้โดยไม่ส่ง object ซับซ้อนผ่าน route
- **Affected areas:** Home, Training, Result และ navigation back stack
- **Revisit when:** เพิ่ม local session storage หรือรองรับ process recreation ระหว่าง Session

## ADR-003 — ใช้ ViewModel และ StateFlow สำหรับสถานะการฝึก

- **Decision:** เก็บ `jumpCount`, `elapsedMillis` และ `WorkoutStatus` ใน `TrainingViewModel` และเผยแพร่เป็น `StateFlow`
- **Reason:** แยก state ออกจาก Compose UI, รักษา state ระหว่าง configuration change และทำให้ UI รับข้อมูลทิศทางเดียว
- **Affected areas:** Counter, Workout Timer และหน้าจอ Training ในอนาคต
- **Revisit when:** ต้องบันทึก session หลัง process ถูกปิด หรือต้องแชร์ state ระหว่างหลาย navigation destination

Timer ใช้ `SystemClock.elapsedRealtime()` เพื่อคำนวณเวลาที่ผ่านไป ไม่สะสมจากจำนวนรอบของ `delay()` และหยุดอัตโนมัติเมื่อหน้าจอออกจาก lifecycle สถานะ Started

อัปเดตล่าสุด: 24 กรกฎาคม 2026

## ADR-001 — ใช้ Kotlin Native สำหรับ Android MVP

- Status: Accepted
- Decision: ใช้ Kotlin และ Native Android เป็นเทคโนโลยีหลัก
- Why: CameraX และ MediaPipe เป็นแกนหลักของแอป การทำงานใน Android stack เดียวช่วยลดความซับซ้อนด้าน bridge, lifecycle และ real-time processing
- Affects: โครงสร้าง source code, tooling, การเรียนรู้ และการทดสอบ
- Revisit when: MVP ผ่านการพิสูจน์และมีความต้องการ iOS ที่ชัดเจน

## ADR-002 — ใช้ Jetpack Compose และ Material 3

- Status: Accepted
- Decision: สร้าง UI ด้วย Jetpack Compose และ Material 3
- Why: เป็นแนวทาง UI สมัยใหม่ของ Android และเหมาะกับ state-driven UI
- Affects: Home, Training, Result และ UI components
- Revisit when: พบข้อจำกัดที่พิสูจน์ได้กับ camera overlay หรือ performance

## ADR-003 — ใช้ CameraX สำหรับกล้อง

- Status: Accepted
- Decision: ใช้ CameraX สำหรับ preview และ image analysis
- Why: จัดการ lifecycle และความเข้ากันได้ของอุปกรณ์ Android ได้เหมาะกับ MVP
- Affects: Permission, camera preview, frame pipeline และ resource cleanup
- Revisit when: อุปกรณ์เป้าหมายมีข้อจำกัดที่ CameraX แก้ไม่ได้

## ADR-004 — ใช้ MediaPipe Pose Landmarker บนอุปกรณ์

- Status: Accepted
- Decision: ประมวลผล Pose Landmarks บนอุปกรณ์ด้วย MediaPipe
- Why: ลด latency และลดความเสี่ยงด้านความเป็นส่วนตัวของภาพร่างกาย
- Affects: Model asset, frame processing, performance และ privacy
- Revisit when: ผลทดสอบบนอุปกรณ์ไม่ผ่านเกณฑ์และมีทางเลือกที่รักษาความเป็นส่วนตัวได้

## ADR-005 — พัฒนาแบบ Sequential Milestones

- Status: Accepted
- Decision: ทำ Counter/Timer และ Navigation ก่อน CameraX, จากนั้น MediaPipe และ Jump Detection
- Why: แต่ละขั้น Build และทดสอบแยกได้ ช่วยหาสาเหตุของปัญหาได้ง่ายสำหรับผู้เริ่มต้น
- Affects: Roadmap และลำดับการเพิ่ม dependency
- Revisit when: ไม่มี — เป็นหลักการหลักของ MVP

## ADR-006 — ประมวลผล Camera และ Pose นอก Main Thread

- Status: Accepted
- Decision: ห้ามทำ inference หรือแปลงภาพหนักบน Main Thread และต้องปิด `ImageProxy` เสมอ
- Why: ป้องกัน UI ค้าง, frame backlog และ resource leak
- Affects: Camera analyzer, coroutine/executor, MediaPipe callback และ cleanup
- Revisit when: ไม่มี ยกเว้น API ทางการกำหนด threading model ใหม่

## ADR-007 — เก็บผล Session ในเครื่องก่อน

- Status: Accepted
- Decision: ใช้ local storage และยังไม่เพิ่ม backend
- Why: MVP ต้องพิสูจน์ detection ก่อนเพิ่มระบบเครือข่ายและบัญชี
- Affects: Result history และ data model
- Revisit when: MVP ผ่านการยืนยันและมี requirement การ sync ข้ามอุปกรณ์

## ADR-008 — เลือก Room หรือ DataStore หลังนิยามข้อมูลจริง

- Status: Superseded in part by ADR-012
- Decision: ใช้ DataStore สำหรับค่าตั้งค่าแล้ว ส่วน Session history ยังไม่เลือกจนกว่าจะถึง Milestone 7
- Why: DataStore เหมาะกับค่าตั้งค่า/ข้อมูลเล็ก ส่วน Room เหมาะกับ Session หลายรายการที่ต้อง query
- Affects: Dependency และ persistence layer
- Revisit when: นิยาม Session schema และรูปแบบการค้นหาชัดเจน

## ADR-010 — ใช้ Power Sport เป็นทิศทางการออกแบบ UI

- **Status:** Accepted
- **Decision:** ใช้ Figma Concept B — Power Sport เป็นแนวทางหลัก โดยใช้พื้นหลังเข้ม สีส้มสำหรับ primary action และตัวเลข/ข้อความสำคัญขนาดใหญ่
- **Reason:** ผู้ใช้เลือก Concept B และรูปแบบมี contrast สูง เหมาะกับการมองระหว่างการเคลื่อนไหว โดยยังสร้างด้วย Material 3 และ Jetpack Compose ได้โดยไม่เปลี่ยน training logic
- **Affected areas:** Home, Training, Result, color tokens และ Compose previews
- **Revisit when:** การทดสอบ accessibility, sunlight visibility หรือการใช้งานบนอุปกรณ์จริงพบปัญหา

## ADR-011 — ตรึง Primary Action ไว้ด้านล่าง

- **Status:** Accepted
- **Decision:** วางปุ่ม `START TRAINING` และ `BACK TO HOME` ใน `Scaffold.bottomBar` โดยให้เนื้อหาหลักเลื่อนได้แยกจากปุ่ม
- **Reason:** ทำให้ตำแหน่งปุ่มหลักสม่ำเสมอ ลดพื้นที่ว่างที่ไม่สมดุล และรักษาการเข้าถึงปุ่มบนหน้าจอขนาดเล็ก
- **Affected areas:** Home และ Result layout
- **Revisit when:** การทดสอบบนอุปกรณ์จริงพบว่าคีย์บอร์ด, system bars หรือขนาดหน้าจอทำให้ปุ่มบังเนื้อหา

## ADR-012 — แยก Settings Preferences ออกจาก Session History

- **Status:** Accepted
- **Decision:** ใช้ Preferences DataStore เก็บ nickname, countdown, หน่วยวัด, sound, vibration และ theme แบบ on-device; ยังไม่ใช้ DataStore เก็บ Session history
- **Why:** ค่าตั้งเป็นข้อมูล key-value ขนาดเล็ก แต่ Session history ต้องค้นและสรุปตามช่วงเวลา จึงควรรอนิยาม schema แล้วพิจารณา Room
- **Affected areas:** Settings, Training countdown/cues, Home greeting และ Milestone 7
- **Revisit when:** ต้อง sync ข้ามอุปกรณ์, มี account หรือ Session schema พร้อม

## ADR-013 — Theme เปลี่ยนตามผู้ใช้แต่ Camera surface คง Dark

- **Status:** Accepted
- **Decision:** ให้ Home, Settings, Training และ Result รองรับ System/Dark/Light โดยใช้ Material color scheme; คงเฉพาะ Camera preview และ overlay บนภาพเป็นโทนมืด
- **Why:** Theme ควรทำงานสม่ำเสมอทั่วทั้งแอปตามความคาดหวังของผู้ใช้ ขณะที่ camera preview, pose overlay และข้อความบนภาพยังต้องรักษา contrast ที่ผ่านการใช้งานจริงแล้ว
- **Affected areas:** Theme tokens, Home, Settings, Result และ navigation
- **Revisit when:** การทดสอบ accessibility หรือการใช้งานกลางแจ้งแสดงว่า Camera surface รูปแบบอื่นอ่านได้ดีกว่า

## ADR-018 — กู้คืน Detector เมื่อ AIRBORNE ค้างโดยไม่สร้าง Count

- **Status:** Accepted
- **Decision:** หาก detector ยังอยู่ `AIRBORNE` และไม่พบ Landing ภายใน 1.5 วินาที ให้ยกเลิก Takeoff ที่ค้างและเริ่ม calibration ใหม่จาก frame ปัจจุบัน โดยไม่เพิ่ม Counter
- **Why:** วิดีโอทดสอบสองรอบและ state-machine source แสดงว่า landmark ที่ไม่กลับเข้า landing band ทำให้ `AIRBORNE` ค้างและพลาดการกระโดดต่อเนื่องได้; recovery ต้องไม่ตีความ timeout เป็นการลงพื้น
- **Affected areas:** `BasicBounceDetector`, diagnostic state หลัง recovery และการทดสอบ Pause/Resume
- **Revisit when:** ข้อมูล real-device แสดงว่า Basic Bounce จริงมี airborne duration ใกล้หรือเกิน 1.5 วินาที หรือ recovery ยังเกิดซ้ำโดยไม่กลับมานับ

## Template สำหรับ Decision ใหม่

```text
## ADR-XXX — ชื่อการตัดสินใจ
- Status: Proposed | Accepted | Superseded
- Decision:
- Why:
- Affects:
- Revisit when:
```
