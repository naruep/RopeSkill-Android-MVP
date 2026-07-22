# RopeSkill Architecture Decisions

## ADR-012 — ใช้ ankle-baseline state machine เป็น Basic Bounce baseline

- **Status:** Accepted for testing
- **Decision:** ใช้ค่าเฉลี่ยแกน Y ของข้อเท้าซ้ายและขวา สร้าง standing baseline 15 เฟรม ปรับ takeoff/landing threshold ตามความยาวช่วงสะโพกถึงข้อเท้า และนับเมื่อสถานะเปลี่ยน `Grounded → Airborne → Grounded` พร้อม smoothing และ cooldown 250 ms
- **Reason:** เป็นวิธี on-device ที่เรียบง่าย อธิบายและปรับค่าได้ ไม่ผูกกับความละเอียดภาพหรือระยะกล้องแบบค่าพิกเซลตายตัว และป้องกันการนับหลายครั้งจากการกระโดดครั้งเดียว
- **Affected areas:** Training counter, คำแนะนำการจัดเฟรม, detection accuracy และ test protocol
- **Revisit when:** ผลทดสอบพบ missed jumps/false positives สูง, กล้องสั่น, มุมกล้องเปลี่ยน หรือจำเป็นต้องรวม velocity, hip trajectory, foot contact หรือ temporal model

Detector ไม่เก็บ landmark history นอกหน่วยความจำที่จำเป็นสำหรับ state ปัจจุบัน และล้าง calibration เมื่อ Pause, Finish, Reset หรือ landmarks สำคัญหายไป

## ADR-011 — ใช้ MediaPipe Pose Landmarker Lite ในโหมด Live Stream

- **Status:** Accepted
- **Decision:** ใช้ `tasks-vision 0.10.35`, `pose_landmarker_lite.task`, CPU delegate, ผู้ใช้หนึ่งคน และ `LIVE_STREAM`; CameraX ใช้ `STRATEGY_KEEP_ONLY_LATEST` บน single background executor
- **Reason:** Lite model เหมาะกับการวัด baseline latency บนอุปกรณ์จริง, live-stream tracking ลดงานตรวจจับซ้ำ และ latest-frame strategy ป้องกัน frame backlog
- **Affected areas:** App size, camera analysis, pose latency, overlay และ resource cleanup
- **Revisit when:** การทดสอบความแม่นยำต้องใช้ Full/Heavy model, CPU latency ไม่ผ่านเกณฑ์ หรือ GPU delegate ให้ผลที่เสถียรกว่าบนอุปกรณ์เป้าหมาย

Overlay หมุน input ตาม CameraX metadata ก่อน inference และแปลง normalized landmarks ด้วย center-crop scale เดียวกับ `PreviewView.ScaleType.FILL_CENTER` กล้องหลังจึงไม่ mirror; หากเพิ่มกล้องหน้าต้องเพิ่ม horizontal mirroring ทั้ง input และ overlay อย่างชัดเจน

## ADR-010 — ใช้ CameraX PreviewView ผ่าน Compose AndroidView

- **Status:** Accepted
- **Decision:** ใช้ CameraX `Preview` กับกล้องหลังและแสดงผ่าน `PreviewView` ที่ฝังใน Compose ด้วย `AndroidView`
- **Reason:** เป็น API stable ที่จัดการ surface และ device compatibility ให้ พร้อมต่อยอด `ImageAnalysis` สำหรับ MediaPipe ใน Milestone ถัดไป
- **Affected areas:** Training screen, Camera permission, lifecycle และ camera resource cleanup
- **Revisit when:** CameraX Compose API ให้ประโยชน์ที่วัดได้ หรือการทดสอบกล้องหน้าจำเป็นต่อการจัดวางอุปกรณ์จริง

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

อัปเดตล่าสุด: 22 กรกฎาคม 2026

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

- Status: Proposed
- Decision: ยังไม่เลือกจนกว่าจะถึง Milestone 7
- Why: DataStore เหมาะกับค่าตั้งค่า/ข้อมูลเล็ก ส่วน Room เหมาะกับ Session หลายรายการที่ต้อง query
- Affects: Dependency และ persistence layer
- Revisit when: นิยาม Session schema และรูปแบบการค้นหาชัดเจน

## Template สำหรับ Decision ใหม่

```text
## ADR-XXX — ชื่อการตัดสินใจ
- Status: Proposed | Accepted | Superseded
- Decision:
- Why:
- Affects:
- Revisit when:
```
