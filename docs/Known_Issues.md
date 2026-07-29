# RopeSkill Known Issues

อัปเดตล่าสุด: 29 กรกฎาคม 2026

## สถานะ

Milestone 0–7 ผ่านส่วนหลักบน Samsung Galaxy S23 Ultra แล้ว T-716 ที่ detector baseline commit `591b938` และ Music OFF ได้ Basic Bounce 59/60 หรือ 98.3% แต่ T-717/T-719 พบความแปรผันและวงจร Landing เร็วซ้ำ; T-720 ที่ commit `b044672` เพิ่ม hip-return guard โดยคง constants เดิมและผ่าน Basic Bounce 60/60, `AIR/LAND 60/60`, `SUP 0` พร้อม false-positive controls 0 จึงปิด KI-013; T-721 ที่ commit `862c978` ยืนยันว่า guard ยังถูกต้องในแสงสลัว แต่ความแม่นยำลดเหลือ 56/60 จึงเปิด KI-018; T-722 เก็บ passive evidence จนพบ miss ใต้ rescue floor และ T-724 ยืนยัน floor `0.020` ด้วยแสงสลัว 57/60, `AIR/LAND 57/57`, `SUP 0` พร้อม safety controls 0 จึงปิด KI-018; T-725 ผ่าน performance soak 5 นาทีโดยมี skip rate 0.064% และไม่พบ performance/stability regression แต่คง KI-015 Monitoring; T-726/T-727/T-728 ที่ baseline `70e35e7` ได้ 85/100, 93/100 และ 83/100 จึงยืนยัน KI-020 ว่า session ต่อเนื่องยังนับ Takeoff ขาด แม้ Landing, cooldown และ performance ปกติ. T-729 บน matched frames ได้ BASE 86/101, BIL-only 86/101 (`D+0`) และ RES-only 88/101 (`D+2`) โดย Landing/cooldown/performance ปกติ จึงยืนยันว่า rescue floor `0.018` มีผลต่อ 2 cycles แต่การลด bilateral หรือ rescue ทีละ gate ยังไม่เพียงพอ. T-730 V12 Smoke-R ได้ Counter 3/3 แต่ global collector เพิ่มจาก standing landmark jitter ถึง pre-jump `P26/R26` และ final `P52 C3 R49 S0 U5 OV43`; attribution จึง Inconclusive และไม่ใช่ detector false count หรือหลักฐาน root cause. T-730 V13 แยก standing landmark jitter ออกจาก primary `WINDOW` ได้ใน device smoke รอบนี้: Smoke แรก Actual/App 3/2 พบ jump สุดท้ายเป็น `#014 T/R` candidate `Q+SH` แต่ event อยู่ใน `TAIL` จึงยังไม่ใช่ causal proof; Smoke-R2 ได้ 3/3 พร้อม `WIN P3 C3 R0 S0 U0`, gate 0 และ `OV0`. KI-020 คง High/Confirmed; V13 Smoke เป็น Conditional Pass รอ History 3/00:22 และ Formal 22-jump trace

## Issue Register

| ID | วันที่ | อาการ | Severity | สถานะ | Root cause | แนวทางแก้/ขั้นถัดไป |
|---|---|---|---|---|---|---|
| KI-001 | 2026-07-22 | ยังไม่ทราบว่าโปรเจกต์ Build และ Run ได้หรือไม่ | Blocker | Fixed | ทดสอบ sample app บนอุปกรณ์จริงแล้ว | ผู้ใช้ยืนยัน Build และ Run สำเร็จบน Samsung Galaxy S23 Ultra |
| KI-002 | 2026-07-23 | Detector รอบสองนับ fast 1/10, slow/medium 0/10 และ knee lift ผิด 5 ครั้ง | High | Fix awaiting verification | `verticalMotionDifference ≤ 3.5%` ปฏิเสธ Basic Bounce จริงมากเกินไป; ตัวกรองยังไม่แยก knee lift ได้ | คืน detector รอบแรกที่ slow/medium เคยได้ 10/10 และเพิ่ม diagnostic overlay ก่อนปรับ Fast/Knee lift ทีละเงื่อนไข |
| KI-003 | 2026-07-23 | Detector ที่กู้คืนยังนับ fast 4/10, medium 8/10 และนับ knee lift ซ้าย 3/5 ขวา 5/5 | High | Fix awaiting verification | หลักฐาน V3 พบ `hipRise / averageAnkleRise` ของ Basic Bounce 9 ตัวอย่างอยู่ที่ 1.45–2.42 แต่ knee lift false positive 3 ตัวอย่างอยู่ที่ 0.39–0.79 | เพิ่มตัวกรอง Takeoff ขั้นต่ำ 1.10 พร้อม unit tests และทดสอบ Slow/Medium/Fast กับ knee lift ซ้ำบนอุปกรณ์จริง |
| KI-003 | 2026-07-23 | ผู้ใช้ต้องกด `START TRAINING` แล้วกด `START` ซ้ำในหน้า Training | Medium | Fixed | Navigation และการเปิด Ready Detection เป็นคนละคำสั่งแต่ใช้คำว่า Start เหมือนกัน | ผู้ใช้ยืนยัน Single Start และไม่มีปุ่ม Start ซ้ำผ่านบนอุปกรณ์ |
| KI-004 | 2026-07-23 | Countdown รอบสองยกเลิกจากการขยับเล็กน้อยและเริ่มใหม่บ่อยเกินไป | Medium | Fix awaiting verification | ค่า stability 2–2.5% ไวต่อ landmark jitter และ movement ปกติ | คืน behavior รอบแรกตามผลตอบรับผู้ใช้; การยกเลิกการเคลื่อนไหวบางแบบยังเป็นข้อจำกัดที่ต้องออกแบบใหม่จากข้อมูลจริง |
| KI-005 | 2026-07-23 | `LAST COUNT` ของ knee lift ขวาแสดง ΔR 0.110 ทั้งที่ source กำหนด synchronization ratio limit 0.080 | High | Resolved | ค่า V1 ที่อ่านได้ไม่ตรงกับหลักฐาน Takeoff ที่ใช้ตัดสิน; V2 snapshot ตัวแปรเดียวกับเงื่อนไขโดยตรง | ผู้ใช้ยืนยัน V2 แสดง DIFF ≤ LIMIT และ SYNC PASS ครบทั้ง Basic Bounce และ knee lift; ใช้ V2/V3 เป็นหลักฐานรอบถัดไป |
| KI-006 | 2026-07-23 | กด `RESET` ในหน้า Training แล้ว Timer, Counter, Tracking และปุ่มควบคุมหยุดเหมือนแอปค้าง | High | Fix awaiting verification | `resetWorkout()` เปลี่ยนสถานะเป็น `IDLE` แต่หน้า Training ไม่มีปุ่มเริ่มสำหรับสถานะนี้ และ Pose frames จะไม่ถูกประมวลผลขณะ `IDLE` | ให้ปุ่ม `RESET` ล้าง Session แล้วเรียก `startWorkout()` ต่อทันทีเพื่อกลับสู่ `POSITIONING`; รอ Build และทดสอบบนอุปกรณ์จริง |
| KI-007 | 2026-07-23 | Countdown จบแล้วแสดง `START` แต่ Timer และ `GO!` ไม่เริ่มจนกว่าจะกระโดด | Medium | Fix awaiting verification | สถานะ `ARMED` เรียก `beginRunning()` เฉพาะเมื่อ detector พบ `TAKEOFF` | เรียก `beginRunning()` ทันทีเมื่อ Countdown จบ เพื่อให้ `GO!`, Timer และ detector เริ่มพร้อมกัน; รอทดสอบบนอุปกรณ์จริง |
| KI-008 | 2026-07-23 | Medium Basic Bounce ตรวจพบเพียง 6/10 หลังเพิ่ม hip/ankle filter | High | Resolved | ผลรอบเดียวแปรผัน; V4 ยืนยันว่า Takeoff ที่เข้า AIR ลง LAND ครบ และการทดสอบซ้ำไม่พบการถดถอยต่อเนื่อง | Medium เพิ่ม 3 รอบได้ 10/10, 9/10, 10/10 รวม 29/30; knee lift ซ้าย/ขวา 0/5 false positives จึงตรึง detector ปัจจุบันเป็น baseline |
| KI-009 | 2026-07-24 | หลัง Pause/Resume และระหว่างกระโดดเชือกต่อเนื่อง Counter พลาดหลายครั้งพร้อม diagnostic ค้าง `AIRBORNE` | High | Fix awaiting verification | Timeout ช่วยให้ไม่ค้างถาวรแต่ไม่แก้ cadence จริง; วิดีโอเป้าหมายประมาณ 125–140 jumps/min แสดงว่ารอบใหม่เริ่มก่อน ankle landmark กลับเข้า landing band ของ baseline เก่า | คง thresholds และ timeout เดิม แต่ยอมรับ Landing เมื่อเห็นวงจรขึ้น→ลงครบระยะและเริ่มขึ้นรอบถัดไป พร้อมใช้ตำแหน่งต่ำสุดของ cycle เป็น baseline รอบต่อไป; รอ unit test และ T-705 บนอุปกรณ์จริง |
| KI-010 | 2026-07-24 | กระโดดเชือกจริงที่ประมาณ 133 jumps/min นับได้ 11/20 และรอบยืนยัน V5 ได้ 16/20 แม้ไม่มี false count หลังหยุด | High | Resolved | รอบ V5 แสดง `AIR 16 / LAND 16`; rejected cycles ทั้ง 3 มี ankle/hip ผ่าน, synchronization ผ่าน แต่ hip-to-ankle ratio `0.78`, `0.90`, `0.94` ต่ำกว่า threshold `1.10` จึงถูกปฏิเสธก่อน Takeoff | ลด `MIN_HIP_TO_ANKLE_RISE_RATIO` เป็น `0.85`; T-707 ได้ Basic Bounce 20/20 ทั้ง 3 รอบและ false count หลังหยุด 0 |
| KI-011 | 2026-07-25 | T-707 นับ knee lift ซ้ายและขวาผิดข้างละ 2/5 หลัง Basic Bounce ดีขึ้นเป็น 60/60 | High | Resolved | วิดีโอและ Count Evidence ยืนยันว่าขารับน้ำหนักมี ankle rise `-0.020–0.000` แต่ smoothed average ยังผ่าน Takeoff และตำแหน่งเท้ากลับมาใกล้กันจน synchronization ผ่าน | เพิ่ม bilateral ankle-rise floor `0.010 × leg length`; T-708 ยืนยัน knee lift ซ้าย/ขวา 0/5 และ Basic Bounce 60/60 |
| KI-012 | 2026-07-25 | T-709 ยังนับ heel raise ผิด 2/10 หลังเพิ่ม minimum hip rise เป็น `0.060` | High | Resolved | Heel raise ยก ankle/heel ขึ้นทั้งสองข้างและ accepted evidence เคยมี hip rise `0.062–0.076`; T-710 ถึง T-713 ไม่พบ false count ใน heel raise รวม 80 ครั้ง และ strong-hip gate `0.100` ยังแยกได้ | T-715 ยืนยัน heel raise false 0/20 หลังลดเฉพาะ rescue ankle floor |
| KI-013 | 2026-07-25 | Basic Bounce อาจต่ำกว่าเป้าหมายแม้ repeatability รอบก่อนผ่าน | High | Resolved | T-719 ได้ 55/60 โดย AIR/LAND รวม 77/77 และ SUP 22; `LAND − SUP = 55` ตรงกับ Counter แสดงว่าการกระโดดจริง 60 ครั้งถูกแตกเป็นวงจร detector 77 รอบ; trace พบ `LC/LS` เหตุผล `B` และ airborne สั้นผิดปกติถึง 1–145ms พร้อมรูปแบบ `T → LC → T → LS`; สาเหตุหลักคือ Strong-Hip Rescue รับ ankle rise ตั้งแต่ `0.025` แต่ Returned-to-Baseline เดิมตรวจเฉพาะ ankle landing band `0.040` ทำให้ rescue takeoff ช่วง `0.025–0.040` เริ่ม AIRBORNE ทั้งที่ข้อเท้ายังอยู่ใน landing band และจบ Landing ได้ทันที; Run 2 ยังพบ rejected `A0.023 H0.168` ต่ำกว่า rescue floor เป็นปัจจัยรอง; performance ปกติและ Landing ที่มีปัญหาเป็น `B` ไม่ใช่ `C/BC` | T-720 กำหนดให้ `B` ต้องเห็นทั้งข้อเท้าและสะโพกกลับ baseline โดยคง threshold, rescue floor, cooldown และเส้นทาง `C/BC` เดิม; commit `b044672` ผ่าน Unit tests/Build และ real-device verification: Basic Bounce 60/60, `AIR/LAND 60/60`, `SUP 0`, controls/หลังหยุด false 0 และไม่พบ stability regression |
| KI-014 | 2026-07-26 | Training Music Phase 1 ยังไม่ผ่านการทดสอบบนอุปกรณ์จริงครบทุกกรณี | Medium | Resolved | ต้องยืนยัน codec, lifecycle, file-access failure, audio output และ performance บนอุปกรณ์จริง | T-210 ผ่าน MP3/AAC-M4A, manual/auto-pause lifecycle, end-of-track loop, ไฟล์หาย, ถอดหูฟัง และ performance โดยไม่ crash/freeze |
| KI-015 | 2026-07-26 | PERF V1 รอบ Music OFF ก่อน reset เคยแสดง `IN 2888 / OUT 2257 / SKIP ~630` หนึ่งครั้ง | Medium | Monitoring | ยังไม่ทราบสาเหตุของค่าครั้งแรก; `PERF V1` เป็นค่าสะสมตั้งแต่สร้าง PoseDetector และ asynchronous live-stream processing อาจไม่มี result สำหรับ input บางเฟรมโดยไม่เกิด queue หรือ preview ค้าง | หลังออก Home รอบ OFF/ON ได้ `SKIP ~0`; T-716 ได้ `SKIP ~0–3`; T-725 ระยะ 5 นาทีได้ `IN 12,506 / OUT 12,497 / SKIP 8` หรือ 0.064%, ผลต่างใหม่ต่อ checkpoint สูงสุด 3, FPS/LAT คงที่และไม่กระตุกหรือ crash จึงไม่เกิดอาการ `SKIP ~630` ซ้ำ; คง Monitoring โดยไม่ปรับ detector |
| KI-016 | 2026-07-26 | Cold start เดิมแสดงพื้นขาว; หลังเพิ่ม branded splash โลโก้ยังใหญ่และ system mask ตัดศีรษะกับห่วงเชือก | Low | Resolved | Starting activity เดิมไม่มี splash attributes; รอบ 224dp ถึง 128dp ยังถูก mask ตามขนาดและการปัดเศษขอบ | ใช้ canvas 288dp, พื้น `#071426` และภาพภายใน 124dp; T-211 ยืนยันว่าห่วงเชือกครบ มี margin ตัวคนชัด และส่วนอื่นถูกต้อง |
| KI-017 | 2026-07-26 | วงเชือกใน Adaptive App Icon อยู่ใกล้ขอบ Launcher mask มากกว่ามุมมอง Cold Start | Low | Resolved | foreground inset 10dp เหลือกรอบภาพ 88dp ทำให้ขอบเขตรัศมีของ asset ประมาณ 73.6dp เกิน safe zone 66dp ของ Adaptive Icon | เพิ่ม foreground inset เป็น 16dp ทำให้ขอบเขต artwork ประมาณ 63.6dp และใช้ drawable เดียวกันกับ monochrome; T-212 ยืนยันว่า App Icon ถูกต้องบน Samsung Galaxy S23 Ultra |
| KI-018 | 2026-07-28 | Basic Bounce บน APK ปัจจุบันนับขาดในแสงสลัว: T-721 ได้ 20/20, 19/20 และ 17/20 รวม 56/60 | High | Resolved | การนับที่รับแล้วมี `AIR=LAND`, `SUP 0` และ trace ไม่มี `LS` หรือ Landing cycle แตกซ้ำ จึงไม่ใช่ regression ของ KI-013; T-722 ที่ commit `20e55fe` จับ genuine miss เป็น `R A0.020/0.023 H0.141/0.157 ... ANK`; T-723 ลด rescue floor เป็น `0.020` พร้อม boundary tests และผ่าน normal-light sanity กับ false-positive controls; T-724 ที่ detector baseline `70e35e7` ได้แสงสลัว 19/20, 20/20 และ 18/20 รวม 57/60 หรือ 95%, `AIR/LAND 57/57`, `SUP 0`, performance ปกติและไม่มี stability regression | ยืนยัน floor `0.020` และปิด Issue หลัง T-724 ถึงเป้าหมาย repeatability พร้อม controls false 0; คง standard threshold, hip gate, bilateral/sync, cooldown และ Landing re-arm guard เดิม และเปิด Issue ใหม่หากผลภาคสนามต่ำกว่าเกณฑ์อีก |
| KI-019 | 2026-07-29 | หน้าจอโทรศัพท์ดับตาม system screen timeout ระหว่างใช้หน้า Training เกินประมาณ 1 นาที | Medium | Resolved | `TrainingScreen` ไม่ได้ขอให้ Activity คงหน้าจอเปิดระหว่าง Session | ตั้ง `View.keepScreenOn` ด้วย `DisposableEffect` เฉพาะหน้า Training และคืนค่าเดิมเมื่อออกจากหน้า; T-213 ยืนยันว่า Training เปิดค้าง 3 นาทีและ Home คืน system timeout ถูกต้องบน Samsung Galaxy S23 Ultra |
| KI-020 | 2026-07-29 | Continuous 100 Jumps ในแสงปกตินับได้ T-726 85/100, T-727 93/100, T-728 83/100 และ T-729 BASE 86/101 ต่ำกว่าเป้าหมาย 95%; รอบที่ไม่มี rope trip ยังเกิดซ้ำ | High | Confirmed / V13 Smoke conditional; History/formal pending | Root cause ของ undercount ยังไม่ยืนยัน. T-729 ตัดเฉพาะ BIL `0.006` และ RES `0.018` แบบลดทีละ gate ออกจากการเป็น fix ที่เพียงพอ. T-730 V12 attribution ใช้ไม่ได้เพราะรวม standing jitter. T-730 V13 Smoke แรก Actual/App 3/2 จับ jump สุดท้ายเป็น `#014 T/R` candidate `Q+SH` แต่ event อยู่ใน `TAIL` เพราะไม่มี closing accepted bookend จึงยังไม่ยืนยัน root cause. ใน Smoke-R2 รอบนี้ Actual/App 3/3 และ rejected events ทั้ง 51 รายการอยู่ใน LEAD/TAIL ขณะที่ `WIN R0`; รอบที่ไม่มี miss นี้จึงยืนยัน window separation เฉพาะรอบ แต่ไม่สามารถชี้ blocker ของ KI-020 ได้ | คง BASE `0.010/0.020` และไม่แก้ `BasicBounceDetector`, threshold หรือ Counter. ยืนยัน History ล่าสุดเป็น 3 jumps / 00:22 เพื่อปิด Smoke จากนั้นรัน Formal 22 jumps ตาม `docs/T730_V13_Formal_22_Jumps.md` พร้อม counted opening/closing bookends, ช่วงนิ่งหลังจบ และ Screen Recorder; ออกด้านข้างเพื่อจับ `SEALED POST-EXIT` ก่อนกลับมากด Finish. วิเคราะห์เฉพาะ rejected events ใน `WINDOW` |

## Risks ที่ต้องเฝ้าระวัง

รายการเหล่านี้ยังไม่ใช่ปัญหาที่เกิดขึ้นจริง:

- Camera permission ถูกปฏิเสธหรือถูกถอนระหว่างใช้งาน
- `ImageProxy` ไม่ถูกปิด ทำให้ pipeline ค้าง
- Pose inference ช้ากว่าอัตราเฟรมและเกิด frame backlog
- Rotation หรือ mirroring ของกล้องหน้าอาจทำให้ overlay ไม่ตรงจนกว่าจะผ่านการทดสอบจริง
- Landmark visibility ต่ำเมื่อแสงน้อยหรือร่างกายไม่ครบเฟรม
- Basic Bounce อาจยังถูกนับซ้ำหรือพลาดจาก threshold รอบใหม่ที่ยังไม่ผ่านการทดสอบจริง
- การเคลื่อนโทรศัพท์ระหว่าง Session อาจเลื่อน ankle baseline และทำให้เกิด false positive
- โทรศัพท์ร้อนหรือแบตเตอรี่ลดเร็วใน Session ยาว
- Lifecycle interruption เช่น lock screen, app background หรือสายเรียกเข้า

## วิธีบันทึก Issue ใหม่

1. บันทึกอาการที่สังเกตเห็น ไม่สรุปสาเหตุก่อนมีหลักฐาน
2. เก็บ error แรกที่มีความหมายจาก Logcat
3. ระบุขั้นตอนทำซ้ำ รุ่นอุปกรณ์ และ app build
4. เปลี่ยน Root cause เมื่อยืนยันแล้วเท่านั้น
5. บันทึกผลทดสอบหลังแก้ใน `Test_Log.md`

## Issue Detail Template

```text
### KI-XXX — ชื่อสั้น
- Date:
- Device / Android version:
- App build / commit:
- Observed symptom:
- Reproduction steps:
- First meaningful error:
- Hypotheses:
- Confirmed root cause:
- Fix:
- Verification result:
- Status: Open | Investigating | Fixed | Deferred
```
