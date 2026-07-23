# RopeSkill MVP Requirements

อัปเดตล่าสุด: 23 กรกฎาคม 2026

## สถานะคำสำคัญ

- `Confirmed` — ตกลงเป็นข้อกำหนดของ MVP แล้ว
- `Assumption` — ใช้เป็นสมมติฐานชั่วคราว
- `To Test` — ต้องพิสูจน์ด้วยการทดสอบจริง
- `Later` — เลื่อนไปหลัง MVP

## Functional Requirements

| ID | ข้อกำหนด | สถานะ |
|---|---|---|
| FR-001 | แอปต้องมีหน้าจอ Home, Training และ Result | Confirmed |
| FR-002 | ผู้ใช้ต้องเริ่มและจบ Training Session ได้ | Confirmed |
| FR-003 | หน้าฝึกต้องแสดง Counter และ Workout Timer | Confirmed |
| FR-004 | แอปต้องขอ Camera permission เฉพาะเมื่อจำเป็น | Confirmed |
| FR-005 | หน้าฝึกต้องแสดง CameraX live preview | Confirmed |
| FR-006 | แอปต้องประมวลผล Pose ด้วย MediaPipe Pose Landmarker | Confirmed |
| FR-007 | แอปต้องแสดง Pose Landmarks ซ้อนบนภาพกล้อง | Confirmed |
| FR-008 | แอปต้องตรวจจับและนับ Basic Bounce | Confirmed |
| FR-009 | หน้าผลลัพธ์ต้องแสดงอย่างน้อยจำนวนครั้งและระยะเวลา | Confirmed |
| FR-010 | แอปต้องบันทึกผล Session ไว้ในอุปกรณ์ | Confirmed |
| FR-011 | แอปควรจัดการกรณีผู้ใช้ปฏิเสธ Camera permission | Confirmed |
| FR-012 | แอปควรหยุดหรือคืนทรัพยากรกล้องเมื่อหน้าจอไม่ทำงาน | Confirmed |
| FR-013 | หลังผู้ใช้กด Start/Resume ระบบต้องยืนยันว่ามองเห็นร่างกายและยืนนิ่ง แล้วนับถอยหลัง 5 วินาทีก่อนพร้อมรับการกระโดด | Confirmed |
| FR-014 | Timer ต้องเริ่มเมื่อพบ Takeoff ครั้งแรก และ Counter ต้องนับการกระโดดครั้งแรกเมื่อตรวจพบ Landing | Confirmed |

## Non-Functional Requirements

| ID | ข้อกำหนด | สถานะ |
|---|---|---|
| NFR-001 | Camera และ AI processing ต้องไม่ทำงานหนักบน Main Thread | Confirmed |
| NFR-002 | ต้องปิด `ImageProxy` ทุกเฟรม แม้เกิดข้อผิดพลาด | Confirmed |
| NFR-003 | ต้อง release Camera และ MediaPipe resources ตาม lifecycle | Confirmed |
| NFR-004 | ต้องจัดการ rotation, mirroring, timestamp และ overlay coordinates อย่างชัดเจน | Confirmed |
| NFR-005 | ค่า FPS, latency และความร้อนต้องวัดบนโทรศัพท์จริง | To Test |
| NFR-006 | ต้องไม่บันทึกหรืออัปโหลดภาพ วิดีโอ หรือ landmarks โดยค่าเริ่มต้น | Confirmed |
| NFR-007 | Diagnostic logs ต้องไม่มีภาพหรือข้อมูลส่วนบุคคล | Confirmed |
| NFR-008 | UI ต้องใช้งานได้ระหว่าง Session โดยไม่ค้าง | To Test |

## Acceptance Criteria ของ MVP

- ผู้ใช้เปิดแอปและเข้าสู่ Training ได้
- เมื่ออนุญาตกล้อง ผู้ใช้เห็น live preview
- เมื่อเห็นร่างกายครบตามเงื่อนไข Pose landmarks แสดงในตำแหน่งที่เหมาะสม
- Counter เพิ่มเมื่อเกิด Basic Bounce ที่เข้าเกณฑ์ และไม่เพิ่มซ้ำจากการกระโดดครั้งเดียว
- Timer เริ่มจากการกระโดดครั้งแรกและหยุดตาม Session
- หลังจบ Session หน้าผลลัพธ์แสดงข้อมูลถูกต้อง
- ปิดและเปิดแอปใหม่แล้วยังอ่านผล Session ที่บันทึกไว้ได้
- ผลด้านความแม่นยำและประสิทธิภาพถูกบันทึกใน `Test_Log.md`

## Out of Scope สำหรับ MVP

- Login และ user account
- Backend API และ cloud database
- Social, leaderboard, rewards และ marketplace
- iOS
- การวิเคราะห์ท่ากระโดดหลายชนิดพร้อมกัน
- การวินิจฉัยสุขภาพหรือคำแนะนำทางการแพทย์

## Open Questions

- Samsung Galaxy S23 Ultra เป็นเครื่องทดสอบหลัก โดย Android version และ One UI version ยังรอยืนยัน
- Minimum SDK 26 และ Target SDK 36 ตามโปรเจกต์ปัจจุบัน เหมาะกับอุปกรณ์เป้าหมายหรือไม่
- กล้องหน้าเป็นค่าเริ่มต้น; การสะท้อนซ้าย–ขวายังรอยืนยันโดยตรง
- เกณฑ์ความแม่นยำที่ยอมรับได้ของ Basic Bounce คือเท่าใด
- ต้องการเก็บประวัติ Session กี่รายการหรือไม่จำกัด
