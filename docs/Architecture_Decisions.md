# RopeSkill Architecture Decisions

อัปเดตล่าสุด: 31 กรกฎาคม 2026

## ADR-043 — เพิ่ม passive Landing/State evidence ก่อนเลือก detector candidate

- **Status:** Accepted
- **Decision:** หลัง T-742 แยก 20 misses เป็น READY gate rejection 10, AIR/state-or-landing evidence 9 และ proposal absent/uncertain 1 ให้เตรียม T-743 debug-only passive Landing/State trace ก่อนเสนอการเปลี่ยน takeoff threshold, Landing rule หรือ `BasicBounceDetector`
- **Why:** Cycle-level mapping ยืนยันว่าความแปรผันไม่ได้มี signature เดียวและไม่สัมพันธ์เฉพาะเสื้อผ้า. Existing `T-738` trace ระบุ gate ของ READY misses ได้ แต่ AIR group ยังไม่มี operand ของ `returnedToBaseline`, `descendedFromPeak`, `startedNextRise`, timeout และ reset/recovery ต่อ physical cycle. Aggregate labels ยังมี pulse หลัง physical range จึงใช้เลือก production fix ไม่ได้
- **Safety boundary:** Observer ต้องอ่าน PoseFrame กับ production result หลัง decision, มี bounded ring buffer และ parity tests, ไม่คืน detection decision, ไม่แก้ production phase/threshold/Counter/storage และไม่เก็บภาพหรือ raw landmarks ลง Git
- **Affected areas:** T-742 conclusion, KI-022 และ T-743 diagnostic plan; production detector, thresholds, Landing, cooldown, camera pipeline, Result/History และ storage ไม่เปลี่ยน
- **Revisit when:** T-743 จับ AIR interval กับ close/reset reason และ exact Landing operands ได้โดยไม่เปลี่ยน production output พร้อมผ่าน tests/build, Smoke, controls และ performance/stability

**Implementation and validation update:** T-743 ใช้ read-only `LandingStateEvidence` ที่แนบจาก AIR/Landing path เพื่อให้ observer ภายนอกอ่าน operand ที่ production คำนวณจริง แทนการคำนวณ smoothing/baseline ซ้ำจาก `PoseFrame`. Collector เก็บ bounded frame/interval/pulse history และไม่มี return path กลับ detector. Runtime T-735 ถูกปิดเพื่อลด debug overhead/overlay stacking. Production constants และ decision expressions คงเดิม. Windows tests/build ผ่าน; Smoke `3/3`, Formal/Repeat รวม `43/44`, controls false `0`, PA0/U0 และ performance/stability ผ่าน. Landing/state gap ไม่เกิดซ้ำ; miss 1 ครั้งไม่สร้าง AIR interval จึงยังไม่อนุมัติ detector change และให้ KI-022 อยู่ Monitoring

## ADR-042 — ใช้วิดีโอเดิมทำ cycle-level audit ก่อนเพิ่ม diagnostic หรือปรับ detector

- **Status:** Accepted
- **Decision:** หลัง T-741 Run 1 ได้ `18/22` และหยุด Run 2 ตาม protocol ให้ทำ T-742 โดยใช้ Screen Recordings T-740/T-741 เดิมเพื่อ map physical cycles กับ Counter transitions, visible detector state และ rejection evidence ก่อนออกแบบ passive diagnostic เพิ่มหรือเสนอการเปลี่ยน `BasicBounceDetector`
- **Why:** T-741 ยืนยัน repeatability variance แต่ `T/L18/18`, `SUP0`, post-stop, Result/History, performance และ stability ปกติ. ข้อความ `ANKLE RISE TOO SMALL` และ `FEET NOT SYNCHRONIZED` ที่มองเห็นเป็น aggregate/instantaneous UI evidence และยังไม่พิสูจน์ว่า missed cycle แต่ละรอบถูกปฏิเสธด้วย gate ใดหรือไม่สร้าง proposal
- **Evidence boundary:** Screen Recording ใช้ยืนยัน ground-truth timing, Counter transition, rendered state/reason และ visible pose quality ได้ แต่ไม่มี raw `PoseFrame`, exact operands หรือ retained event ต่อ physical cycle ครบถ้วน จึงต้องจัด `insufficient evidence` แทนการเดาเมื่อ timeline จับคู่ไม่ได้
- **Affected areas:** T-741 conclusion, KI-022 และ T-742 evidence plan; production detector, thresholds, camera pipeline, Counter, Result/History และ storage ไม่เปลี่ยน
- **Revisit when:** T-742 ระบุ repeated cycle-level pattern ที่มี confidence เพียงพอ หรือพบ evidence gap ที่ต้องแก้ด้วย passive instrumentation แบบไม่ขับ Counter/state

## ADR-041 — ไม่ระบุเสื้อผ้าเป็นสาเหตุและคง detector ระหว่างตรวจ repeatability

- **Status:** Accepted
- **Decision:** ปิด T-740 เป็น `Complete / Inconclusive` สำหรับ clothing attribution, เปิด KI-022 และรัน T-741 แบบ same-condition cadence-controlled ก่อนพิจารณาแก้ `BasicBounceDetector` หรือ thresholds
- **Why:** Round A ได้ `21/22`; same-clothing repeats ได้ `15/22` และ `18/22`; Round B ที่เปลี่ยนกางเกงได้ `18/22`. ผล Round B เท่ากับหนึ่งรอบที่ใช้กางเกงเดิม จึงแยก clothing effect จาก repeatability variance ไม่ได้. ทุก session มี `T/L` สมดุล, `SUP0`, หลังหยุดเพิ่ม 0 และ performance/stability ผ่าน
- **Affected areas:** T-740 conclusion, KI-022, T-741 protocol และการตีความ field evidence; production detector, camera pipeline, Counter, Result/History และ storage ไม่เปลี่ยน
- **Revisit when:** T-741 ให้ผลอย่างน้อย `21/22` ทั้งสองรอบและต่างกันไม่เกิน 1 Count, หรือมี missed-cycle evidence ที่ทำซ้ำได้และระบุ gate/state root cause โดยไม่ลด safety controls

## ADR-040 — กู้ asymmetric ankle เฉพาะ strong-hip jump ที่มี weak-foot rise

- **Status:** Accepted
- **Decision:** T-738 V22 เพิ่ม production rescue เฉพาะกรณีที่ bilateral floor `0.008` ผ่านเพียงข้างเดียว, ข้างเด่น rise อย่างน้อย `0.060`, ข้างอ่อนยัง rise อย่างน้อย `0.004`, smoothed ankle ผ่าน rescue floor `0.016`, hip rise อย่างน้อย `0.120`, และยังผ่าน synchronization `0.080` กับ hip-to-ankle ratio `0.85`. Standard, existing strong-hip rescue, Landing, timeout recovery และ cooldown ไม่เปลี่ยน
- **Why:** T-737 Formal Repeat ได้ Actual/App `22/21`; genuine jump `#23` ถูกปฏิเสธด้วย `BR` ที่ `L0.069/R0.005/H0.145`. Takeoff/Landing ที่ยอมรับแล้วสมดุล `21/21`, `UA0`, `X0`, `SUP0` จึงเป็น isolated asymmetric landmark miss ก่อนเข้า AIRBORNE ไม่ใช่ Landing failure
- **Safety boundary:** ไม่ลด bilateral floor แบบกว้าง. Support foot ที่นิ่ง/ลงไม่ผ่าน weak-foot `0.004`; heel raise ที่ hip ต่ำไม่ผ่าน hip `0.120`; knee lift ที่ต่างระดับมากยังไม่ผ่าน sync. Candidate เปิดเฉพาะ production profile T-738 เพื่อคง historical T-729/T-736 profiles
- **Validation:** Regression ครอบคลุม recorded boundary, stationary support foot, insufficient hip rise และ knee-lift/heel-raise controls; Windows `testDebugUnitTest` และ `assembleDebug` ผ่าน. บน Samsung Galaxy S23 Ultra: Smoke `3/3`; Safety Controls standing/knee left/knee right/heel raises false count `0`; Formal และ Formal Repeat `22/22`, `T/L22/22`, `SUP0`, หลังหยุดเพิ่ม `0`; Auto-pause, Result/History, preview และ runtime stability ผ่าน. รับ commit `752af1d` เป็น detector baseline และปิด T-738/KI-020
- **Affected areas:** `BasicBounceDetector` Takeoff decision, production profile, Debug overlay label, tests, T-738 protocol และ KI-020; Counter persistence, Result/History, Room, timeout Landing recovery และ camera pipeline ไม่เปลี่ยน
- **Revisit when:** unit/build fail, Safety Controls เกิด count, Smoke ต่ำกว่า 3/3, Formal/Repeat ต่ำกว่า 22/22, `T/L` ไม่สมดุล, `SUP>0`, Count เพิ่มหลังหยุด หรือ performance/stability ถดถอย

## ADR-039 — กู้ Landing ที่ timeout เฉพาะเมื่อเห็น descent ครบระยะ

- **Status:** Accepted
- **Decision:** เมื่อ Takeoff ผ่าน production gates แล้วและครบ `1,500ms` ให้ปิดวงจรเป็น Landing/Count ได้เฉพาะกรณีที่ ankle และ hip ลงจาก airborne peak ครบ Landing distances แล้ว แต่ไม่เข้า baseline band และไม่เกิด next-rise; รายงาน reason `TIMED_OUT_AFTER_DESCENT`. ถ้าไม่มี descent ครบ, landmarks หาย หรือ Takeoff ไม่ผ่าน gates ให้ recovery เดิม reset/calibrate โดยไม่ Count
- **Why:** T-735 Formal Repeat `Screen_Recording_20260730_172836.mp4` ได้ Actual/App `22/21`, `Q24 M22`, production `T/L22/21`, `SUP0`, แล้วเปลี่ยนจาก `AIRBORNE` เป็น `CALIBRATING` ที่ timeout หลัง jump สุดท้าย. ปัญหารอบนี้อยู่ที่ Landing completion ไม่ใช่ Takeoff gate rejection
- **Fixed-evidence rule:** Screen Recording ยืนยัน ground truth, trace/state transition และ timeout แต่ไม่มี raw `PoseFrame` ทุกเฟรม จึงไม่ใช้เป็น deterministic MediaPipe replay. การยืนยัน `22/22` ต้องทำบนอุปกรณ์จริง
- **Validation:** Pure-Kotlin regression `99/99`, Windows tests/build, V21 Smoke `3/3`, Safety Controls ทุกประเภท `0`, Formal `22/22` และ Formal Repeat `T/L21/21` ผ่านด้าน Landing โดยไม่มี timeout/false recovery. Formal Repeat ขาดหนึ่งครั้งจาก `BR` ก่อน Takeoff ซึ่งแยกไป T-738
- **Affected areas:** `BasicBounceDetector` Landing timeout path, landing reason/Debug labels, T-737 protocol และ KI-020; Takeoff thresholds, cooldown, Counter persistence, Result/History และ Room ไม่เปลี่ยน
- **Revisit when:** timeout recovery สร้าง false positive, Count เพิ่มหลังหยุด, control ใดไม่เป็น 0, `T/L` ไม่สมดุล หรือ Formal ต่ำกว่า `21/22`

## ADR-038 — ปรับ bilateral และ rescue ankle แบบ bounded จาก T-735

- **Status:** Accepted
- **Decision:** อนุมัติ T-736 production candidate โดยลด individual ankle floor `0.010→0.008` และ strong-hip rescue ankle floor `0.020→0.016`; คง standard ankle/hip `0.045/0.060`, rescue hip `0.100`, ratio `0.85`, synchronization, Landing, cooldown และ baseline adaptation
- **Why:** T-735 Formal 22/19 ระบุ genuine misses ขณะ `READY` เป็น `RA ×2` ที่แสดง `P0.017/0.019` และ `BR ×1` ที่แสดง `R0.010` แต่ต่ำกว่า floor เดิมเล็กน้อย. RA `0.016/0.015` และ bilateral `0.006` เคยผ่าน controls แบบแยก gate; candidate ใช้ `0.008` เป็นค่ากลางเพื่อจำกัดความเสี่ยง. Low-hip pulses `H0.042/0.071` ทำให้ไม่มีเหตุผลรองรับการลด RH
- **Fixed-evidence rule:** ใช้วิดีโอ T-735 เดิมยืนยัน ground truth/overlay และใช้ recorded operands เป็น boundary regression; ไม่อ้าง deterministic MediaPipe replay เพราะ Screen Recording ไม่มี PoseFrame inputs เดิมทุกเฟรม
- **Validation:** Pure-Kotlin regression 97/97 และ Windows tests/build ผ่าน. T-736/T-737 Smoke `3/3`, controls false `0` และ Formal `22/22`; T-738 ยืนยัน Formal/Repeat `22/22`, `T/L22/22`, `SUP0`, post-stop, Auto-pause และ stability ผ่านบน baseline ที่สืบทอดค่า `0.008/0.016`
- **Affected areas:** production `BasicBounceDetector` profile, Training detector wiring, Debug gate overlay/tests, T-736 protocol และ KI-020; Counter/Result/History/storage ไม่เปลี่ยน
- **Revisit when:** Windows build fail, device control ใดนับผิด, Smoke ต่ำกว่า 3/3, Formal ต่ำกว่า 21/22, Landing/suppression/performance ถดถอย หรือมี field evidence ต่ำกว่า 95%

## ADR-037 — จับคู่ unmatched pulse กับ production Takeoff peak gates แบบ passive

- **Status:** Complete — diagnostic objective passed; superseded at runtime by ADR-038
- **Decision:** ปิด T-734 หลัง Formal สองรอบยืนยัน `Q22 M21 U1 UA0` และ `QU ... READY` ซ้ำ แล้วแทน runtime overlay ด้วย external `T735PassiveTakeoffGateCollector`. Collector รักษา pulse trace เดิมและจับคู่ unmatched qualified pulse กับ `TakeoffPeakEvidence` ที่ production BASE publish ภายในหน้าต่างเวลา ±120ms. ถ้าจับคู่ได้จะประเมิน blockers เดิม `SYNC/BIL-L/BIL-R/RATIO/STD-H/RES-A/RES-H`; ถ้าไม่มี evidence หลังหน้าต่างจะระบุ `NP`; `P` แสดงจำนวน pulse ที่ยังรอ evidence และ Formal snapshot ต้องเป็น `P0`
- **Why:** T-734 พิสูจน์ว่าครั้งที่ขาดมี kinematic pulse ขณะ BASE `READY` แต่ยังไม่แยกว่า production สร้าง rejected peak แล้วติด gate หรือไม่สร้าง completed peak. Evidence อาจ publish เหลื่อมจาก raw pulse หนึ่งถึงหลายเฟรมเพราะ production ใช้ smoothing จึงต้องใช้ bounded temporal match และแสดง pending state แทนการผูกเฉพาะเฟรมเดียว
- **Gate parity:** Standard route ใช้ smoothed ankle `0.045`, smoothed hip `0.060`, bilateral `0.010` และ hip/raw-ankle ratio `0.85`; rescue route ใช้ smoothed ankle `0.020`, smoothed hip `0.100`, bilateral และ ratio เดิม. `FEET_NOT_SYNCHRONIZED` จาก peak diagnostic ระบุ SY blocker. Collectorอ่าน operands/evidence หลัง production decision และไม่ส่งผลกลับ
- **Isolation:** `BasicBounceDetector.kt`, BASE thresholds, Takeoff/Landing state machine, baseline adaptation, cooldown, Counter, Result, History, Room และ auto-pause ไม่มีการเปลี่ยน. T-735 เป็น Debug-only, bounded 128 pulses, pending match window 120ms และเก็บเฉพาะ normalized ratios/counters/enums ใน memory
- **Validation:** Pure-Kotlin regression 94/94 ผ่าน รวม T-734 parity เดิม, delayed evidence match, exact rescue `RA`, multi-blocker standard route, `NP`, jitter, missing landmarks, disabled collector และ formatter. Android Gradle ยังต้องรันบน Windows ก่อนติดตั้ง
- **Affected areas:** Debug collector/overlay, Training Debug state, tests, protocol, T-734 records และ KI-020 เท่านั้น
- **Revisit when:** Smoke ต้องได้ 3/3, `P0`, matched rows ครบและ performance ปกติ. Formal ต้องอ่าน clean pre-exit snapshot ก่อนออกจากเฟรม; ถ้า `P>0`, Q contamination, evidence timing ambiguity หรือ performance regression ให้หยุดตีความและแก้ instrumentation ก่อน

## ADR-036 — ใช้ passive kinematic pulse trace แยก proposal miss กับ cycle merge

- **Status:** Complete — diagnostic objective passed; superseded at runtime by ADR-037
- **Decision:** หลัง T-733 ปิด RA candidate evaluation ให้ Debug APK กลับไปใช้ production BASE `0.010/0.020` เพียง detector เดียว และเพิ่ม external `T734ProposalCycleMissCollector` ที่อ่าน PoseFrame เดียวกันกับผล BASE หลัง decision. Collector จับ joint ankle/hip upward→downward pulse, รายงาน raw/qualified pulse, overlap กับ production Takeoff และ unmatched qualified pulse โดยแยกว่า peak เกิดขณะ BASE `AIRBORNE` หรือไม่
- **Why:** T-733 Formal สองรอบให้ Actual/App 22/21 เหมือนกัน และ BASE/RA16/RA15 ทุก arm เป็น 21 (`D+0`, `A/L21/21`). RA candidates ไม่กู้ miss แม้ heel raise/knee lift/standing controls เป็น 0 ทุก arm จึงไม่มีเหตุผลรองรับการลด RA. ต้องแยกว่าครั้งที่ขาดเกิดระหว่าง detector ยัง AIRBORNE จาก cycle ก่อน หรือไม่มี production proposal ขณะ GROUNDED
- **Isolation:** Collector ไม่คืนค่าเข้า detector, state machine, baseline adaptation, Counter, Result, History, Room หรือ auto-pause. `BasicBounceDetector.kt` และ production thresholds ต้องไม่มี diff. T-733 shadow code/tests คงเป็น record แต่ runtime shadows ถูกปิดเพื่อลด per-frame work
- **Interpretation:** broad trace floor `ankle 0.006/hip 0.040` ใช้คัด evidence สำหรับ video correlation เท่านั้น ไม่ใช่ detector threshold และ `Q` ไม่ใช่ jump count. `U/UA` เป็น unmatched kinematic candidates ไม่ใช่ causal proof; standing jitter, sampling และ landmark quality ยังอาจสร้างหรือซ่อน pulse ได้
- **Performance/privacy:** เก็บ normalized ratios, elapsed milliseconds, status enums และ counters แบบ bounded สูงสุด 128 pulses ใน memory ของ Debug session; overlay รักษา unmatched ล่าสุดสูงสุด 3 rows. ไม่เก็บภาพ วิดีโอ หรือ landmark coordinates และไม่ persist diagnostics
- **Validation:** Pure-Kotlin compile/regression ผ่าน 91/91 รวม parity เดิม 86 tests และ T-734 tests สำหรับ matched pulse, unmatched AIRBORNE pulse, raw jitter, missing-landmark interruption, disabled mode และ formatter. Android Gradle ใน sandboxยังเริ่มไม่ได้เพราะ wrapper distribution download ถูก network block
- **Affected areas:** T-734 collector, Training Debug integration/overlay, tests, protocol และ KI-020; production detector/Counter/storage ไม่เปลี่ยน
- **Device result:** Windows tests/build และ Smoke 3/3 ผ่าน. Formal สองรอบได้ Actual/App 22/21, `Q22 M21 U1 UA0 T/L21/21 X0`; unmatched เป็น `QU ... READY` ทั้งคู่. Result/History, หลังหยุด, Auto-pause และ stability ผ่าน
- **Revisit when:** T-734 ปิดแล้ว; ใช้ ADR-037/T-735 ระบุ production peak/gate ของ `QU`. Trace ที่มี jitter (`Q > Actual`/standing U) หรือ performance regression ยังต้องหยุดและแก้ instrumentation ก่อนตีความ

## ADR-034 — เพิ่ม passive cycle-separation timing trace สำหรับ T-730 V14

- **Status:** Complete — device validation recorded in T-730 V14; superseded by later T-730 diagnostics
- **Decision:** ขยาย external `T730PassiveGateAttributionCollector` ให้บันทึกหลักฐานของ accepted cycle ที่มีอยู่แล้ว ได้แก่ observed/reported `TAKEOFF→LANDING`, จำนวน sampled `AIRBORNE` frames, `Landing→next TAKEOFF` re-arm gap, จำนวน sampled `READY` frames ระหว่าง cycles, `TAKEOFF→TAKEOFF`, `landingReason` และ `countInterval`. Overlay `T-730 TRACE V14` แสดง median/maximum ของ AIR/GAP/T2T พร้อม event ID และแสดง raw cycle fields ต่อ accepted row
- **Why:** V13 Formal รอบที่เก็บหลักฐานครบได้ Actual/App 22/19 โดย `WIN P19 C19 R0 S0 U0`; undercount 3 ครั้งจึงไม่มี rejected WINDOW event ให้ gate attribution. Trace ชี้ว่า accepted spans `#015` 1,303ms และ `#019` 743ms ยาวผิดปกติและอาจรวม physical jump มากกว่าหนึ่งครั้ง แต่ V13 ไม่แสดง airborne frame count, re-arm gap หรือ landing path จึงยังแยกไม่ได้ว่า Landing/re-arm ช้า, detector อยู่ AIRBORNE นาน หรือ cycle ถูกปิดด้วย next-rise path
- **Isolation:** V14 อ่านเฉพาะ `BounceDetectionResult`, timestamp และ cycle evidence ที่ production detector ส่งออกอยู่แล้ว. ไม่มีค่าใด feed back เข้า `BasicBounceDetector`, thresholds `0.010/0.020`, state machine, baseline adaptation, Counter, Result, History หรือ Room; `BasicBounceDetector.kt` ต้องไม่มี diff
- **Interpretation:** Timing ที่ยาวหรือ event ที่เป็น maximum เป็น candidate สำหรับ video correlation เท่านั้น ไม่ใช่ causal proof และ V14 ไม่เพิ่ม efficacy/safety threshold. `AIR` คือ Takeoff ถึง Landing, `GAP` คือ Landing ก่อนหน้าถึง Takeoff ปัจจุบัน, `T2T` คือ Takeoff ก่อนหน้าถึง Takeoff ปัจจุบัน, `F` คือจำนวน timestamped samples ที่ collector เห็นเป็น AIRBORNE และ `LR R/C/B` คือ Returned-to-Baseline / Completed-Vertical-Cycle / Both
- **Performance/privacy:** เก็บเฉพาะ counters, enums และ elapsed milliseconds แบบ bounded in-memory ร่วมกับ trace capacity เดิม 512; ไม่เก็บภาพ, วิดีโอ, landmark coordinates หรือข้อมูลร่างกาย และไม่ persist ลง storage
- **Validation:** Pure-Kotlin compile/regression ผ่าน 78/78 ครอบคลุม production detector, T-729 parity และ T-730 V14 airborne frame sampling, re-arm gap, takeoff interval, landing reason, count interval, median/maximum/event ID และ formatter. Runtime นี้ยังรัน Android Gradle ไม่ได้เพราะ wrapper ต้องดาวน์โหลด `gradle-9.3.0-bin.zip` แต่ network sandbox ไม่อนุญาต; ต้องรัน `testDebugUnitTest` และ `assembleDebug` บนเครื่องผู้ใช้ก่อนติดตั้ง
- **Affected areas:** T-730 external collector/overlay/tests, V14 protocol และ KI-020 เท่านั้น
- **Revisit when:** หลัง V14 smoke ผ่าน ให้รัน Formal 22 jumps และเทียบ event ที่มี AIR/GAP/T2T สูงกับวิดีโอ; วิเคราะห์หลักฐานก่อนเสนอ detector change ทุกครั้ง

## ADR-033 — ใช้ timestamped accepted-bookend window trace สำหรับ T-730 V13

- **Status:** Superseded for cycle-separation diagnosis by ADR-034; V13 trace integrity passed / Formal Counter failed
- **Decision:** Debug build เก็บ completed-peak trace แบบ bounded สูงสุด 512 events พร้อม timestamp ที่อ้างอิงจาก `GO`. Event ที่จบเป็น `COUNTED` หรือ `SUPPRESSED` ทำหน้าที่เป็น accepted-cycle anchors; หลังมี anchors แล้ว collector จัดกลุ่มทุก event แบบ post-hoc เป็น `LEAD`, `WINDOW`, `BOUNDARY` หรือ `TAIL` ตามช่วงเวลาเทียบกับ accepted Takeoff แรกและ accepted Landing ล่าสุด. Aggregate หลักสำหรับ gate attribution ใช้เฉพาะ rejected events ใน `WINDOW`; `LEAD`, `BOUNDARY` และ `TAIL` คงอยู่ใน raw trace เพื่ออธิบาย noise และขอบเขต แต่ไม่รวมใน primary gate totals
- **Why:** T-730 V12 Smoke-R มี Actual/App 3/3 แต่ก่อนกระโดดจริง collector เพิ่มถึง `P26 C0 R26 U4 OV20` และเมื่อจบรอบเป็น `P52 C3 R49 S0 U5 OV43`. Completed rejected peaks จำนวนมากจึงมาจาก landmark jitter ขณะยืนนิ่งและทำให้ global aggregate/overflow แยก missed-jump window ไม่ได้. ผลนี้เป็นข้อจำกัดของ V12 collector ไม่ใช่ detector false count หรือหลักฐาน root cause ของ KI-020
- **Isolation:** V13 consume เฉพาะ evidence และ cycle trace ของ BASE `0.010/0.020`; ไม่คืนค่าเข้า `BasicBounceDetector`, threshold, state machine, baseline adaptation, Counter, Result, History หรือ Room. `BasicBounceDetector.kt` ต้องไม่มี diff และ App Counter ยังคงอ่านเฉพาะ production BASE result
- **Window semantics:** Trace เก็บ raw completed peaks ตามลำดับเดียวกันตลอดรอบ. Accepted cycles เป็น bookends ที่กำหนดช่วงวิเคราะห์: event ที่อยู่ก่อนช่วงเป็น `LEAD`, อยู่ครบภายในช่วงเป็น `WINDOW`, คาบขอบเป็น `BOUNDARY` และอยู่หลังช่วงเป็น `TAIL`. การจัดกลุ่มอาจเปลี่ยนแบบ post-hoc เมื่อ accepted Landing ล่าสุดขยายปลาย window; จึงห้ามตีความ region จาก snapshot กลางรอบว่าเป็นผลสุดท้าย. Formal protocol ใช้ counted opening bookend 1 ครั้ง + target jumps 20 ครั้ง + counted closing bookend 1 ครั้ง รวม Actual 22 ครั้ง
- **Diagnostics:** Overlay `T-730 TRACE V13` แสดง timestamps relative to `GO`, raw totals, `SEG L/W/B/T`, primary `WIN P/C/R/S/U`, gate totals ของ `WINDOW` และ raw trace ล่าสุด 6 rows. หกรายการบนจอเป็นเพียง display window แยกจาก trace capacity 512; การเลื่อนรายการออกจากจอไม่ใช่ overflow. `TRACE_OVERFLOW` เกิดเมื่อ raw trace เกิน 512 เท่านั้นและต้องทำรอบ invalid. Completed rejected peak ที่มี `RejectedTakeoffEvidence` ใน result เดียวกันต้องมี `REJECTED_TAKEOFF` timestamp/sequence ที่ตรงกัน; เฉพาะ peak-only fallback ที่ result เดียวกันไม่มี `RejectedTakeoffEvidence` เท่านั้นที่อนุญาต completion sequence ว่างและถูกนับเป็น `U` เพื่อไม่อ้าง attribution เกินหลักฐาน. หากร่างกายหลุด full-body หลัง accepted Landing ล่าสุดอย่างน้อย 2 วินาทีและไม่มี accepted cycle ค้าง collector จะ freeze หลักฐานเป็น `SEALED POST-EXIT`; หากเกิดก่อน 2 วินาทีเป็น `INVALID FULL-BODY` และหากเกิดระหว่าง Takeoff–Landing เป็น `INVALID TRACK-CYCLE`
- **Validity:** แสดง invalid/stop reason พร้อมเวลาที่ชัดเจนสำหรับ full-body/tracking loss, recalibration, timestamp/sequence mismatch, overlapping/orphan cycle, outcome mismatch, airborne interval mismatch, missing accepted peak และ trace overflow. `SEALED POST-EXIT` ทำให้ collector หยุดเปลี่ยน snapshot ก่อน lifecycle reset แต่ auto-pause ที่ตามมาอาจล้าง overlay; จึงต้องใช้ Screen Recorder จับ sealed frame และ snapshot ก่อนออกจากเฟรม. สถานะ `SEALED` ไม่ได้พิสูจน์ว่าได้ยืนนิ่งครบ protocol. รอบใช้วิเคราะห์ได้เมื่อ snapshot ก่อนออกจากเฟรมหรือ sealed frame ในวิดีโอไม่เป็น `INVALID`, วิดีโอยืนยันช่วงนิ่งหลังจบครบ 10 วินาที, มี accepted bookends ครบ, raw trace ไม่ overflow, Counter หลังหยุดไม่เพิ่ม, `AIR=LAND`, `SUP 0`, Result/History ถูกต้อง และวิดีโอยืนยันลำดับ opening bookend, target 20 และ closing bookend. Gate attribution ยังคงเป็น blocker classification จาก retained evidence ไม่ใช่ causal proof หรือการจับคู่ event กับ physical jump แบบหนึ่งต่อหนึ่ง
- **Performance/privacy:** เก็บเฉพาะ normalized evidence, cycle sequence, outcome และเวลา elapsed แบบ bounded in-memory; ไม่เก็บหรืออัปโหลดภาพ วิดีโอ หรือ landmark coordinates และไม่ persist trace ลง Room
- **Validation:** Pure-Kotlin compile/regression เดิมผ่าน 77/77. Smoke-R2 และ History ผ่านที่ 3/00:22. Formal rerun Actual/App 22/19, `WIN P19 C19 R0 S0 U0`, BASE `A/L19/19 SUP0 RES17`, หลังหยุดเพิ่ม 0, `SEALED POST-EXIT +32.403`, Result/History 19/00:33 และ stability ผ่าน. V13 จึงผ่าน trace integrity แต่ Counter Fail; accepted spans `#015` 1,303ms และ `#019` 743ms ส่งต่อเป็น cycle-separation candidates ให้ ADR-034
- **Affected areas:** T-730 Debug collector/overlay/tests, protocol และ KI-020; ไม่มีการเปลี่ยน production detector, threshold หรือ Counter
- **Revisit when:** V13 ปิดรอบแล้ว; ใช้ ADR-034/V14 วิเคราะห์ timing ของ accepted cycles ก่อนเสนอ detector change

## ADR-032 — ใช้ exact-paired retained-evidence blocker classification ภายนอก detector สำหรับ T-730

- **Status:** Superseded for device attribution by ADR-033
- **Supersession:** V12 classification logic คงเป็นประวัติและเป็น helper สำหรับการจัด blocker ของ rejected evidence แต่ global measurement window, aggregate, overflow และ overlay protocol ถูกแทนด้วย V13 timestamped accepted-bookend trace หลัง Smoke-R ยืนยันว่า standing landmark jitter ทำให้ `P/R/U/OV` เพิ่มก่อน physical jump
- **Decision:** Debug build ใช้ `T730PassiveGateAttributionCollector` อ่านเฉพาะ `TakeoffPeakEvidence` และ `RejectedTakeoffEvidence` จากผล BASE ที่ `BasicBounceDetector` ส่งออกอยู่แล้ว โดยไม่แก้ไฟล์ detector. ทุก completed peak ใน measurement window ได้ ordinal `#N` ร่วมกันทั้ง `COUNTED`, `REJECTED` และ `SUPPRESSED`; rejected peak ถูกจัดประเภทเฉพาะเมื่อ A/H/diagnostic ของหลักฐานทั้งสองชุดตรงกันแบบ exact แล้วประเมิน gate `SYNC`, `BIL-L`, `BIL-R`, `RATIO`, `STD-H`, `RES-A` และ `RES-H`. `RATIO` ใช้รูปแบบคูณเดียวกับ production คือ `smoothedHip >= rawAverageAnkle × 0.85` ไม่ใช้ reported division ratio. หาก smoothed ankle ผ่าน `STD-A 0.045` ให้ใช้ standard route; หากไม่ผ่านให้ใช้ rescue route. หนึ่ง evidence pair อาจรายงาน blocker หลาย gate และ aggregate จึงซ้อนกันได้
- **Why:** T-729 แสดงว่า BIL `0.006` ไม่เพิ่ม count และ RES `0.018` เพิ่มเพียง 2/101 แต่ยังไม่อธิบาย miss ที่อาจมี blocker มากกว่าหนึ่งรายการ. `BounceDiagnostic` เป็นเพียง first-failure label ตามลำดับเงื่อนไข จึงไม่แสดง gate conditions อื่นที่ไม่ผ่าน. External collector ให้หลักฐานละเอียดขึ้นโดยไม่เปลี่ยน threshold, state machine, baseline adaptation, Landing, cooldown หรือ Counter
- **Isolation:** T-730 consume เฉพาะ BASE result และไม่คืนค่าใดเข้า detector, Training lifecycle, auto-pause, Counter, Result, History หรือ Room. T-729 shadows ถูกปิดเฉพาะ Debug integration ของ APK นี้เพื่อลด Main-thread work แต่โค้ดและ parity tests เดิมยังคงอยู่; production BASE ยังเป็น bilateral/rescue `0.010/0.020`
- **Validity:** `#N` คือ completed-peak ordinal ภายใน T-730 measurement window ไม่ใช่หมายเลข physical jump และ rejected peak ไม่เท่ากับ genuine miss แบบหนึ่งต่อหนึ่ง. บาง physical miss อาจไม่สร้าง peak event จึงต้องเทียบ `P = C + R + S` กับจำนวนจริงจากวิดีโอ. Evidence pair ที่ A/H raw bits หรือ diagnostic ไม่ตรงกันแบบ exact ต้องเป็น `UNATTRIBUTED`; full-body loss, `CALIBRATING` หรือ rejected observation ที่เริ่มก่อน `GO` ต้องขึ้น `INVALID-RESTART`. หลังเห็น `ACTIVE P0` ต้องยืนนิ่งอย่างน้อยหนึ่ง pose frame โดย protocol ใช้ประมาณ 1 วินาทีก่อนเริ่ม เพื่อไม่ให้ observation แรกคาบขอบ `GO`; ประวัติบน overlay เก็บ rejected ล่าสุด 6 รายการและรอบใช้ได้เฉพาะ `U0`, `OV0`
- **Diagnostics:** Overlay `T-730 GATE V12` แสดง `P/C/R/S/U/OV`, blocker counts, thresholds และรายการ `#N route[blockers] A/L/R/H/Q/SY` แบบ 4 ตำแหน่ง. ค่าเหล่านี้เป็น blocker conditions ที่ reconstruct จาก exact-paired retained evidence; ไม่รับรองว่า operand ทุกตัวมาจาก frame เดียวกันและไม่ใช่ causal proof. หากไม่มี reject, มี `U > 0`, `OV > 0`, tracking invalid หรือ `P` ไม่ครอบคลุม actual jumps ให้สรุป attribution เป็น Inconclusive
- **Performance/privacy:** Collector ไม่ allocate หรือ publish state ต่อ frame ที่ไม่มี completed peak; formatter cache ตาม snapshot; เก็บเฉพาะ normalized ratios, gate masks และ window-relative IDs แบบ bounded in-memory. ไม่ log, persist หรืออัปโหลดภาพ วิดีโอ landmark coordinates หรือ diagnostics
- **Validation:** Pure-Kotlin regression ของ V12 ผ่าน 64/64 รวม `BasicBounceDetectorTest`, `T729ControlledExperimentTest` และ T-730 detector-boundary/direct-ratio/truth-table/lifecycle tests. Device smoke รอบแรกได้ Counter 3/3 แต่ attribution เป็น `INVALID-RESTART/P0`; Smoke-R ได้ Counter 3/3 แต่ pre-jump เป็น `P26/R26` และ final `P52 C3 R49 S0 U5 OV43` จึงสรุป Attribution Inconclusive. ผล Android Gradle tests/Build ไม่ได้ถูกบันทึกแยกในรายงาน smoke
- **Affected areas:** T-730 collector, `TrainingViewModel`, Debug Training overlay, unit tests, T-730 และ KI-020; `BasicBounceDetector.kt` ต้องไม่มี diff
- **Revisit when:** ไม่รัน formal ด้วย V12 global aggregate; ใช้ ADR-033 และผ่าน V13 smoke ก่อนเก็บ formal device attribution

## ADR-031 — ใช้ matched shadow detectors แยกสอง Takeoff gate สำหรับ T-729

- **Status:** Complete — candidate efficacy failed; no promotion
- **Decision:** Debug build รัน detector 3 profile แบบ synchronous ด้วย `PoseFrame` และ timestamp เดียวกัน: `BASE` ใช้ bilateral/rescue `0.010/0.020`, `BIL-only` ลดเฉพาะ bilateral floor เป็น `0.006` โดยคง rescue `0.020`, และ `RES-only` ลดเฉพาะ rescue floor เป็น `0.018` โดยคง bilateral `0.010`; ไม่สร้าง profile ที่ลดทั้งสอง gate พร้อมกัน
- **Why:** T-726–T-728 แกว่ง `83–93%` ระหว่าง Continuous 100 Jumps จึงเปรียบเทียบ APK คนละรอบได้ยากและเพิ่มผลจาก fatigue/cadence/lighting. `RES 0.018` เป็น minimal step ใต้ clean rounded boundary `A0.019 H0.121` ที่ bilateral ผ่าน. `BIL 0.006` เป็น exploratory minimal step ใต้ rounded `L0.007` และยังสูงกว่า support-leg evidence ของ knee lift เดิม `-0.020–0.000`; peak `L0.007/R0.051` เดียวกันมี `H0.006` จึงยังไม่ใช่ isolated bilateral cause. Matched shadows ใช้ input เดียวกันเพื่อคัดเลือก gate ก่อน active confirmation
- **Isolation:** เฉพาะผล `BASE` ควบคุม Training state, Counter, auto-pause, Result, History และ Room; shadow metrics เป็น Debug in-memory summary เท่านั้นและไม่ย้อนกลับเข้า production detector. ทั้งสาม profile calibrate/process/reset พร้อมกัน และเริ่มสะสมพร้อมกันเฉพาะเมื่อทุก arm `READY` ในเฟรมเดียวกัน; overlay ต้องเป็น `MATCHED`, ไม่มี `WARM` และทุก `J0` ก่อนกระโดด หากมี motion ก่อนพร้อมให้แสดง `INVALID-RESTART` และห้ามใช้รอบนั้น
- **Diagnostics/performance:** Overlay `T-729 SHADOW V1` แสดงสถานะ matched window, threshold 4 ตำแหน่ง, `J`, `AIR/LAND`, `SUP`, `RES`, delta เทียบ BASE และเวลา detector ensemble average/max. MediaPipe inference ยังรันครั้งเดียว แต่ detector callback อยู่ Main thread จึงต้องยืนยัน FPS/LAT/IN/OUT/SKIP, preview และ `PROC` บนอุปกรณ์จริงก่อนรอบ formal; `PROC max ≤10ms` เป็น provisional smoke guard ส่วน FPS/LAT/SKIP/preview เป็น end-to-end hard gates
- **Interpretation:** Aggregate ของแต่ละ profile เป็น matched selection evidence แต่เมื่อ profile รับ Takeoff เพิ่ม state/baseline อาจแยกจาก BASE; ห้ามถือทุก delta เป็นการ rescue ราย jump. Candidate ต้องได้ `95–100/100`, `D > 0`, `AIR=LAND`, `SUP 0` และ controls/หลังหยุด 0 โดย `BASE J = App Counter = Result/History`. ถ้า BASE ได้ `95–100/100` และ candidates `D0` ให้คง baseline และทำ repeatability; ถ้าทั้งสอง candidate ผ่านให้พิจารณา `RES-only` ก่อนเพราะคง bilateral gate ที่แก้ knee lift. ผู้ชนะยังต้องผ่าน active single-profile confirmation ก่อนเปลี่ยน production baseline
- **Validation:** Unit tests/Build และ smoke ผ่านบน Samsung Galaxy S23 Ultra. Formal T-729 อยู่ในสถานะ `MATCHED`: Actual 101, App/Result/BASE 86, BIL-only 86 (`D+0`), RES-only 88 (`D+2`), ทุก arm มี `AIR=LAND` และ `SUP 0`; หลัง Landing สุดท้าย Counter ไม่เพิ่ม; `PROC 60/796µs`, FPS 30.0, LAT 28/65ms, IN/OUT 1853/1852, SKIP ~0 และไม่พบ preview กระตุกหรือ crash/freeze. Matched runner, BASE isolation และ performance ผ่าน แต่ BIL/RES candidates ไม่ถึง ≥95% ซึ่งต้องอย่างน้อย 96/101 จึงไม่มี winner, ไม่รัน controls/active confirmation และไม่ promote threshold ใด. ผลนี้แสดงว่า RES `0.018` มีผลสุทธิ 2 cycles ในรอบนี้แต่ไม่เพียงพอเป็น fix; production BASE `0.010/0.020` คงเดิม
- **Privacy:** เก็บเฉพาะ aggregate counters/timing ในหน่วยความจำ Debug Session; ไม่เก็บภาพ วิดีโอ landmark coordinates หรือ shadow result ลง Room
- **Affected areas:** `BasicBounceDetector` threshold configuration, T-729 runner, Training Debug overlay, unit tests, T-729 และ KI-020
- **Revisit when:** Shadow ใดได้ `95–100/100`, `D > 0`, controls ทุก arm เป็น `J0 A/L0/0 S0`, หลังหยุด `A=L` และ `SUP 0` โดยไม่เกิด performance/stability regression หรือเมื่อทั้งสอง shadow ไม่ดีขึ้น/เกิด false positive; smoke fail, BASE mismatch, overcount หรือ regression ให้หยุดก่อน 100 ครั้งและคืน baseline

## ADR-030 — เก็บ bilateral ankle gate evidence ก่อนปรับ detector

- **Status:** Complete — evidence objective passed; superseded by ADR-031
- **Decision:** Debug build ใช้ `TAKEOFF PEAK V11` โดยเพิ่ม raw left/right ankle-rise ratio จาก pose-result frame เดียวกับ retained peak, individual floor `0.010` และ PASS/FAIL ของแต่ละข้างสำหรับ rejected takeoff; ใช้วิธีเลือก peak และ retention policy เดิมจาก V10
- **Why:** T-727 มี rejected peak ที่ average ankle rise ผ่าน rescue floor `0.020` และ hip rise ผ่าน `0.100` แต่ยังเป็น `ANK`; V10 แสดงเฉพาะค่าเฉลี่ยจึงแยกไม่ได้ว่าข้อเท้าข้างใดต่ำกว่า bilateral floor หรือห่างจาก floor เท่าใด
- **Isolation:** ค่า V11 เป็น output-only evidence; boolean gate ที่ detector คำนวณอยู่แล้วถูกส่งไปแสดงผลโดยไม่คืนค่ากลับเข้า `standardTakeoff`, `strongHipRescue`, Takeoff/Landing state, Counter หรือ cooldown และไม่เปลี่ยน constants ใด
- **Privacy/performance:** เก็บเฉพาะ normalized ratios กับผล gate ของ retained peaks ในหน่วยความจำ Debug Session; ไม่เก็บภาพ วิดีโอ landmark coordinates หรือข้อมูลลง Room
- **Affected areas:** `BasicBounceDetector` evidence snapshot, Debug Training overlay, unit tests, T-728 และ KI-020
- **Revisit when:** T-728 บนอุปกรณ์จริงยืนยัน rejected genuine jumps พร้อมค่า L/R ที่อ่านได้; ก่อน experiment ใดต้องพิจารณา margin, ความสม่ำเสมอของข้างที่ตก gate และ safety controls สำหรับ heel raise, knee lift และ standing

## ADR-029 — ประเมิน PERF V1 ระยะยาวด้วย delta และอัตรา skip

- **Status:** Accepted
- **Decision:** สำหรับ performance soak ตั้งแต่ 5 นาที ให้ประเมิน frame backlog จากผลต่าง `ΔIN − ΔOUT` ของแต่ละ checkpoint, cumulative skip rate และแนวโน้ม FPS/latency ร่วมกัน แทนการใช้ค่า `IN − OUT` หรือ `SKIP` สะสมแบบ absolute เพียงค่าเดียว; เกณฑ์โครงการคือผลต่างต่อช่วง 1 นาทีไม่เกิน 5, cumulative skip rate ไม่เกิน 0.10%, FPS ไม่ลดต่อเนื่อง, average latency ไม่เกิน 40ms และ peak latency ไม่เกิน 150ms
- **Why:** `PERF V1` สะสม `IN`, `OUT`, latency และ `SKIP` ตั้งแต่สร้าง `PoseDetector`; ค่า `SKIP` จึงลดลงไม่ได้และ absolute limit ไม่สเกลตามระยะเวลา ขณะที่ CameraX ใช้ `STRATEGY_KEEP_ONLY_LATEST` และ MediaPipe ใช้ asynchronous live-stream processing ซึ่งยอมทิ้ง input บางเฟรมเพื่อไม่สร้าง queue ที่ทำให้ preview ค้าง
- **Validation:** T-725 บน Samsung Galaxy S23 Ultra มีผลต่างใหม่ต่อ checkpoint สูงสุด 3 เฟรม, `SKIP 8 / IN 12,506` หรือ 0.064%, FPS 29.7–30.3 และ LAT 28–32/59–72ms โดยไม่ crash/freeze, AIRBORNE ค้าง หรือ preview กระตุก จึงไม่พบ sustained backlog
- **Affected areas:** เกณฑ์ T-725 และ performance soak ในอนาคต, การตีความ `PERF V1` และ KI-015; ไม่เปลี่ยน CameraX, MediaPipe, Counter หรือ `BasicBounceDetector`
- **Revisit when:** skip rate เกิน 0.10%, ผลต่างต่อ checkpoint เกิน 5, ค่าเร่งสูงขึ้นต่อเนื่อง, FPS ต่ำกว่า 28, average/peak latency เกินเกณฑ์, preview กระตุก หรือความแม่นยำถดถอย

## ADR-028 — เปิด Keep Screen On เฉพาะหน้า Training

- **Status:** Accepted
- **Decision:** ตั้ง `View.keepScreenOn = true` ด้วย Compose `DisposableEffect` ตลอดช่วงที่ `TrainingScreen` อยู่ใน Composition และคืนค่าเดิมของ View เมื่อออกจากหน้า Training
- **Why:** System screen timeout ทำให้จอดับระหว่าง Session และทำให้ T-725 ที่ต้องปล่อยแอปทำงาน 5 นาทีหยุดกลางทาง; การจำกัด flag ไว้ที่หน้า Training แก้ปัญหาโดยไม่เปลี่ยนค่าระบบ ไม่ต้องขอ permission และไม่ใช้ `WakeLock`
- **Validation:** T-213 ผ่านบน Samsung Galaxy S23 Ultra เมื่อ 29 July 2026: หน้า Training เปิดค้าง 3 นาทีโดยไม่ดับ และเมื่อกลับหน้า Home แล้ว system screen timeout ทำงานตามเดิม
- **Affected areas:** `TrainingScreen`, Training lifecycle, battery usage ระหว่าง Session และ T-213/T-725; ไม่กระทบ `BasicBounceDetector`
- **Revisit when:** หน้า Training กลับมาดับตาม system timeout, หน้าอื่นไม่คืนพฤติกรรม timeout เดิม หรือมี requirement ให้คงหน้าจอเปิดเฉพาะบางสถานะของ Session

## ADR-027 — ลด Strong-Hip Rescue ankle floor เป็น 0.020

- **Status:** Accepted
- **Decision:** ลดเฉพาะ `STRONG_HIP_RESCUE_ANKLE_RISE_RATIO` จาก `0.025` เป็น `0.020`; คง standard ankle threshold `0.045`, bilateral ankle floor `0.010`, strong-hip floor `0.100`, hip-to-ankle ratio `0.85`, synchronization limit `0.08`, smoothing `0.60`, Landing distance `0.04`, cooldown `250ms` และ Landing re-arm guard เดิม
- **Why:** T-722 ที่ commit `20e55fe` ได้แสงปกติ 20/20 และแสงสลัว 19/20; genuine miss แสดง `R A0.020/0.023 H0.141/0.157 F7 D240 P39 N42 ANK` ซึ่งผ่าน strong-hip, timing และ frame evidence แต่ smoothed ankle ต่ำกว่า floor เดิม ขณะที่ T-715/T-720 เคยยืนยันว่า strong-hip gate, bilateral/sync gates และ controls แยก heel raise กับ knee lift ได้
- **Validation:** T-724 ที่ detector baseline `70e35e7` ได้แสงสลัว 19/20, 20/20 และ 18/20 รวม 57/60 หรือ 95%; `AIR/LAND 57/57`, `SUP 0`, controls/หลังหยุด false 0 และไม่มี stability regression จึงปิด KI-018 โดยไม่ลด gate หรือ guard อื่น
- **Affected areas:** `BasicBounceDetector`, rescue boundary tests, T-723 และ KI-018
- **Revisit when:** T-723 แสงสลัวต่ำกว่า 19/20, false-positive controls หรือหลังหยุดมากกว่า 0, `AIR != LAND`, `SUP` กลับมา, Landing cycle แตกซ้ำ หรือ stability ถดถอย

## ADR-026 — ใช้ passive takeoff peak/frame-timing evidence แยกผลแสงกับ sampling

- **Status:** Complete — evidence objective passed; superseded by ADR-027
- **Decision:** Debug build แสดง `TAKEOFF PEAK V10` สูงสุด 3 รายการหลัง cycle จบ โดยคง rejected ที่มี smoothed ankle peak สูงสุดไว้ได้ถึง 2 รายการและเติมพื้นที่ที่เหลือด้วย counted/cooldown-suppressed ล่าสุด; กำหนดเวลา peak จากตำแหน่ง smoothed ankle ที่สูงสุดจริง แล้วรายงาน `C/S/R`, ankle และ hip rise ทั้งค่าหลัง smoothing กับค่า raw ใน pose-result frame เดียวกัน, จำนวน result frames ระหว่างช่วงขึ้น, เวลาจากเริ่มขึ้นถึง peak และ interval ก่อน/หลัง peak
- **Reason:** T-721 ในแสงสลัวได้ 56/60 ขณะที่ FPS ลดจากประมาณ 30 เหลือ 23.5–24.0, `AIR=LAND`, `SUP 0`, latency/SKIP ปกติ และพบ rejected `A0.023 H0.153`; ต้องแยกว่าค่า peak ต่ำเพราะ result sampling ห่างขึ้นหรือ pose landmark เปลี่ยนคุณภาพก่อนพิจารณาแก้ threshold
- **Isolation:** Evidence tracker อ่านค่าที่ detector คำนวณแล้วและ timestamp ของ pose result แต่ไม่คืนค่าใดเข้ากฎ Takeoff/Landing; threshold, rescue floor, smoothing, cooldown, Landing re-arm guard, Counter และ Result/History เดิมไม่เปลี่ยน
- **Privacy/performance:** เก็บเฉพาะ normalized ratios กับ timing ของ 3 cycles ในหน่วยความจำ Debug Session; ไม่เก็บภาพ วิดีโอ landmark coordinates หรือข้อมูลลง Room และล้างเมื่อ Pause/Reset/เริ่มรอบใหม่
- **Revisit when:** T-722 เปรียบเทียบแสงปกติกับแสงสลัวครบ หรือพบว่า overlay กระทบ frame rate, detector behavior หรืออ่านค่าจากวิดีโอไม่ได้

## ADR-025 — ใช้ branded system splash และแยก splash logo จาก Launcher icon

- **Status:** Accepted
- **Decision:** ใช้ AndroidX Core SplashScreen `1.2.0` กับ starting theme พื้น `#071426`; แสดง PNG โลโก้เดิมผ่าน drawable canvas 288dp โดยจำกัดภาพไว้ที่ 124dp เพื่อให้ขอบเขตจริงของคนและห่วงเชือกมี margin ภายใน system mask จากนั้นเปลี่ยนเข้า `Theme.RopeSkill` ทันทีโดยไม่ตั้ง delay
- **Reason:** Android 12+ บังคับ system splash ตอน cold/warm start แต่ branch ปัจจุบันไม่มี splash attributes จึงใช้พื้นขาวและ adaptive launcher icon ซึ่งถูก mask จนเส้นเชือกอ่านไม่ครบ การแยก asset ป้องกันไม่ให้การแก้ splash กระทบ Launcher icon หรือ Home logo
- **Affected areas:** launch theme, `MainActivity`, Android manifest และ startup visual continuity
- **Not affected:** Home logo, adaptive Launcher icon, Training UI, `BasicBounceDetector` และ Level Score
- **Revisit when:** T-211 พบพื้นขาว, คนหรือเชือกถูก mask, สัดส่วนโลโก้ไม่เหมาะสม, splash ค้างนาน, transition กะพริบ หรือ Dark/Light theme เปิด Home ไม่ถูกต้อง

## ADR-024 — ใช้ Media3 กับ Storage Access Framework สำหรับ Training Music

- **Status:** Accepted
- **Decision:** ใช้ Media3 ExoPlayer เล่นเพลงหนึ่งไฟล์ที่ผู้ใช้เลือกผ่าน `OpenDocument`; เก็บ persisted read permission กับ content URI และชื่อแสดงผลใน DataStore โดยไม่คัดลอกไฟล์ เพลงเล่นแบบ foreground-only, ขอ audio focus, pause เมื่อ audio output เปลี่ยน, เริ่มเมื่อเข้า `RUNNING`, รักษาตำแหน่งเมื่อ Pause และ rewind เมื่อ Finish/Reset
- **Reason:** Storage Access Framework ให้ผู้ใช้เลือกไฟล์จาก internal storage หรือ document provider โดยไม่ขอสิทธิ์เข้าถึง storage กว้างเกินจำเป็น ส่วน Media3 จัดการ codec ที่อุปกรณ์รองรับ, lifecycle, audio focus และ output change ได้สม่ำเสมอกว่าเขียน player เอง
- **Affected areas:** Settings preferences/UI, Training lifecycle, audio resources และ real-device performance
- **Privacy:** RopeSkill เก็บเฉพาะ URI permission, ชื่อไฟล์ และระดับเสียง ไม่อ่านเพื่อวิเคราะห์ ไม่คัดลอก และไม่อัปโหลดเนื้อหาเพลง
- **Limit:** Phase 1 รองรับหนึ่งเพลง ไม่มี playlist, streaming, background service หรือการจัดการ DRM; codec ที่เล่นได้จริงขึ้นกับ Android/device/provider
- **Revisit when:** T-210 พบการเริ่ม/หยุดผิด state, audio focus ผิดพฤติกรรม, latency/FPS ถดถอย หรือมีความต้องการ playlist/background playback ที่ชัดเจน

## ADR-017 — ใช้ Room เก็บ Training Session summary

- **Status:** Accepted
- **Decision:** ใช้ Room `2.7.2` กับ KSP สร้าง database schema version 1 โดยเก็บเฉพาะ exercise type, เวลาเริ่ม/จบ, duration และ jump count; บันทึกเมื่อ Finish เฉพาะ Session ที่เริ่ม Running แล้วและมี duration มากกว่าศูนย์ รุ่นนี้เข้ากับ `kotlinx.serialization` 1.7.3 ที่ dependency graph ของแอปบังคับใช้
- **Reason:** Session history เป็นข้อมูลหลายรายการที่ต้อง query ตามเวลาและรองรับ migration ซึ่งเหมาะกับ Room มากกว่า Preferences DataStore; summary เพียงพอสำหรับ Result/History โดยไม่เก็บข้อมูลร่างกาย
- **Affected areas:** `TrainingViewModel`, local persistence, process recreation, Training History และ migration tests
- **Privacy:** ไม่บันทึกภาพ วิดีโอ pose landmarks หรือ detector diagnostics
- **Revisit when:** เพิ่ม field ใหม่, เปิด Training History, ต้อง export/sync หรือเปลี่ยน database schema version

## ADR-016 — ใช้ jump-rope mark เป็นโลโก้และ Adaptive App Icon

- **Status:** Accepted
- **Decision:** ใช้โลโก้ Dynamic Sport รูปคนสีครีมกระโดดเชือกสีส้มเป็น Hero Logo ขนาด 152 dp เพียงจุดเดียวบนหน้า Home กึ่งกลางเหนือ `TRAIN STRONGER.` โดย Header คงเฉพาะ `ROPESKILL` และ `MVP`; ใช้ตราสัญลักษณ์เดียวกันเป็น Android Adaptive Icon บนพื้นหลังสีน้ำเงินเข้มพร้อม monochrome layer และ inset 16 dp รอบ foreground
- **Reason:** ลดโลโก้ซ้ำ, ทำให้รายละเอียดท่าทางและเชือกหนึ่งเส้นอ่านได้ชัดบนพื้นหลังมืด, รักษาภาพจำเดียวกันระหว่างหน้า Home, Launcher และ Cold Start; inset 16dp ทำให้ขอบเขตรัศมีจริงของ artwork ประมาณ 63.6dp อยู่ภายใน adaptive-icon safe zone 66dp โดยมี margin เล็กน้อย
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

- **Status:** Accepted
- **Decision:** การกด `START TRAINING` ที่หน้า Home ให้เปิด Training และเข้าสู่ `Positioning` อัตโนมัติ โดยไม่แสดงปุ่ม `START` ซ้ำ ส่วน `RESUME` หลัง Pause ยังคงเป็นคำสั่งโดยตั้งใจ จากนั้นใช้ state `Positioning → Countdown (5–1) → Running (GO!)` โดยเริ่ม Timer ทันทีเมื่อ Countdown จบ และนับการกระโดดจริงครั้งแรกหลัง `GO!`
- **Reason:** ผู้ใช้มีเวลาจัดตำแหน่งโดยไม่ต้องเดินกลับไปแตะโทรศัพท์ ลด false positive จากการเดิน และไม่สับสนกับคำสั่ง Start สองครั้ง
- **Affected areas:** `TrainingViewModel`, Training overlay, detector events, Pause/Resume และ real-device test protocol
- **Revisit when:** Countdown ยกเลิกบ่อยเกินไป, ready check ใช้เวลานานเกินไป หรือการตรวจ Takeoff แรกไม่น่าเชื่อถือ

การทดสอบ stability threshold ที่ 2–2.5% ทำให้ Countdown เริ่มใหม่จากการขยับเล็กน้อยบ่อยเกินไป จึงคืน behavior รอบแรกชั่วคราว การยกเลิกตลอดช่วง 5–1 ยังเป็นข้อกำหนดที่ต้องออกแบบใหม่จากข้อมูล landmark jitter จริง ใช้ `SystemClock.elapsedRealtime()` สำหรับ Timer เช่นเดิม

ระหว่าง Running หาก detector รายงาน `WAITING / FULL_BODY_REQUIRED` ต่อเนื่อง 1 วินาที ให้ ViewModel เรียก Pause flow เดิมเพื่อหยุด Timer และบังคับ Resume ผ่าน Positioning ใหม่ การหน่วงเวลานี้อยู่ภายนอก `BasicBounceDetector` เพื่อไม่ให้ landmark สะดุดชั่วครู่หยุด Session และไม่เปลี่ยน threshold การนับ

## ADR-012 — ใช้ ankle-baseline state machine เป็น Basic Bounce baseline

- **Status:** Accepted as foundational baseline; evolved by later detector ADRs
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

บันทึกส่วนฐานเดิม ณ 24 กรกฎาคม 2026

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

## ADR-019 — รองรับ Basic Bounce ต่อเนื่องด้วยวงจรการเคลื่อนที่

- **Status:** Accepted
- **Decision:** คงเงื่อนไข Takeoff, synchronization, hip-to-ankle ratio, Landing distance และ cooldown เดิม แต่เพิ่มทางจบ `AIRBORNE` เมื่อข้อเท้าและสะโพกขึ้นถึงจุดสูงสุด ลงครบระยะ Landing เดิม และเริ่มขึ้นรอบถัดไป จากนั้นใช้จุดต่ำสุดที่สังเกตใน cycle เป็น baseline ท้องถิ่น
- **Why:** วิดีโอการกระโดดเชือกจริงอยู่ประมาณ 125–140 jumps/min และรอบถัดไปเริ่มก่อน landmark กลับถึง calibration baseline เก่า ทำให้ detector แบบ discrete jump ค้าง `AIRBORNE` และพลาดหลายรอบ
- **Affected areas:** `BasicBounceDetector`, count evidence, continuous-cadence regression tests และ T-705
- **Revisit when:** T-705 ต่ำกว่า 18/20, false positives ของ knee lift/การเดินถดถอย หรือ session ยาวทำให้ baseline เลื่อนสะสม

## ADR-020 — กำหนดให้ข้อเท้าทั้งสองข้างยกขึ้นจริงก่อน Takeoff

- **Status:** Accepted
- **Decision:** คง smoothed average ankle threshold และ hip-to-ankle ratio `0.85` แต่เพิ่มเงื่อนไขว่า left/right ankle rise แต่ละข้างต้องไม่น้อยกว่า `0.010 × leg length` ณ เฟรม Takeoff โดยคำนวณ baseline ของแต่ละข้างจาก average ankle baseline และ baseline ankle difference
- **Why:** T-707 ได้ Basic Bounce 60/60 แต่ knee lift ซ้าย/ขวาถูกนับผิดข้างละ 2/5; Count Evidence ของ false positives แสดงว่าขารับน้ำหนักมี rise `-0.020–0.000` ขณะที่ Basic Bounce จริงที่มีหลักฐานล่าสุดยกทั้งสองข้างอย่างน้อย `0.053` จึงมีช่วงแยกที่กว้างโดยไม่ต้องคืน ratio เป็น `1.10`
- **Affected areas:** `BasicBounceDetector`, Takeoff evidence, knee-lift regression tests และ T-708
- **Revisit when:** T-708 Basic Bounce ต่ำกว่า 18/20 ในรอบใดรอบหนึ่ง, knee lift ยังเกิด false positive หรือหลักฐาน Basic Bounce จริงมี individual ankle rise ต่ำกว่า `0.010`

## ADR-021 — เพิ่ม minimum hip rise เพื่อแยก Heel Raise

- **Status:** Accepted
- **Decision:** เพิ่ม `HIP_TAKEOFF_LEG_RATIO` จาก `0.025` เป็น `0.060` โดยคง hip-to-ankle ratio `0.85`, bilateral ankle-rise floor `0.010`, average ankle threshold และ state machine เดิม
- **Why:** T-708 ได้ Basic Bounce 60/60 และ knee lift 0/5 ทั้งสองข้าง แต่ heel raise ถูกนับผิด 3/10; false Count Evidence มี hip rise `0.042–0.052` ขณะที่ Basic Bounce จริงที่มีหลักฐานอยู่ที่ `0.088–0.132` จึงมีช่วงแยกที่ชัดเจนและการคืน ratio เป็น `1.10` ยังไม่ปฏิเสธ heel raise ตัวอย่างแรก
- **Affected areas:** `BasicBounceDetector`, rejected-takeoff threshold evidence, heel-raise regression test และ T-709
- **Revisit when:** T-709 Basic Bounce ต่ำกว่า 18/20 ในรอบใดรอบหนึ่ง, heel raise ยังเกิด false positive หรือ Basic Bounce จริงมี hip rise ต่ำกว่า `0.060`

ผล T-709 ทำให้เงื่อนไขทบทวนเกิดขึ้นแล้ว: heel raise ยัง false positive 2/10 และ Basic Bounce ลดจาก 60/60 เป็น 56/60 แม้แต่ละรอบยังผ่านขั้นต่ำ 18/20 จึงไม่เพิ่ม hip threshold ต่อโดยไม่มีหลักฐานชนิดใหม่

## ADR-022 — เก็บ Foot Contact Evidence ก่อนเพิ่ม Heel-Raise Gate

- **Status:** Accepted
- **Decision:** เพิ่ม heel และ foot-index/toe rise ซ้าย–ขวาใน `COUNT HISTORY V6` เฉพาะเป็น diagnostic ของ Takeoff ที่ถูกนับ โดยค่าขาดหายแสดง `FOOT N/A` และไม่เปลี่ยน threshold, state transition หรือ Counter
- **Why:** T-709 แสดงการชนกันของ hip threshold: false heel raise มี hip rise `0.062–0.076` ขณะที่ Basic Bounce จริงที่มีหลักฐานต่ำสุด `0.088`; การใช้ foot-index ช่วยทดสอบสมมติฐานว่า heel raise ยังมีปลายเท้าสัมผัสพื้น แต่ Basic Bounce ยกทั้งเท้า
- **Affected areas:** `BasicBounceDetector`, `CountEvidence`, debug overlay, regression tests และ T-710
- **Revisit when:** T-710 มีตัวอย่าง Basic Bounce และ heel raise อย่างน้อยอย่างละ 3 accepted counts หรือ foot landmarks ไม่เสถียรพอที่จะให้ช่วงแยกที่ทำซ้ำได้

T-710 พบว่า foot landmarks ใช้งานได้เมื่อรองเท้าอยู่ในเฟรมพร้อมพื้นที่ด้านล่าง แต่ Basic Bounce ได้ 8/10 และ heel raise ไม่มี accepted count จึงยังเปรียบเทียบสองกิจกรรมตรงกันไม่ได้

## ADR-023 — แยก Ankle Rejection ออกจาก Cooldown Suppression ก่อนปรับ Detector

- **Status:** Accepted
- **Decision:** เพิ่ม heel/toe evidence ให้ rejected cycle ณ observation ที่มี ankle rise สูงสุด และเพิ่มจำนวน/interval ของ Landing ที่ถูก `COUNT_COOLDOWN_MILLIS` ระงับใน debug overlay โดยไม่เปลี่ยน threshold, cooldown หรือ state machine
- **Why:** วิดีโอ T-710 แสดง `AIR 9/LAND 9` แต่ Counter 8 และมี genuine Basic Bounce หนึ่งครั้งถูกปฏิเสธที่ ankle `0.039/0.045`; ต้องทราบว่า Count ที่หายอีกครั้งเกิดจาก cooldown หรือเส้นทางอื่นก่อนแก้ detector
- **Affected areas:** `BasicBounceDetector`, `TrainingUiState`, debug overlay, regression tests และ T-711
- **Revisit when:** T-711 แสดง rejected Basic Bounce/heel raise foot evidence อย่างน้อย 3 ตัวอย่าง หรือมี cooldown suppression ระหว่าง cadence เป้าหมาย

## ADR-024 — Re-arm Returned-to-Baseline ด้วย Hip Return

- **Status:** Accepted
- **Decision:** ให้ Landing เหตุผล `RETURNED_TO_BASELINE` ต้องเห็นทั้งข้อเท้ากลับภายใน `LANDING_LEG_RATIO` และสะโพกกลับภายใน `HIP_TAKEOFF_LEG_RATIO`; คงค่า Takeoff, Strong-Hip Rescue floor, Landing distance, cooldown และเส้นทาง `COMPLETED_VERTICAL_CYCLE` เดิมทั้งหมด
- **Why:** T-719 ได้ 55/60 และ trace แสดง AIR/LAND 77 รอบจากการกระโดดจริง 60 ครั้ง โดย Landing `B` หลายครั้งเกิดหลัง rescue takeoff เพียง 1–145ms; rescue รับ ankle rise `0.025–0.040` ซึ่งยังอยู่ใน ankle landing band `0.040` แต่ accepted Takeoff มี hip-rise evidence อยู่แล้ว การรอให้ hip กลับ baseline จึงยืนยันว่าการเคลื่อนที่ลงเกิดขึ้นก่อนจบ `B` โดยไม่ต้องเพิ่ม threshold ใหม่
- **Affected areas:** `BasicBounceDetector`, landing state-transition regression tests, T-720 และ KI-013
- **Revisit when:** T-720 ยังพบ `B` สั้น/วงจรซ้ำ, AIRBORNE ค้าง, Basic Bounce รวมต่ำกว่า 95% หรือ false-positive controls ถดถอย

## ADR-025 — วัด Gate Margin และ T2T Decomposition ก่อนทดลอง Production Fix

- **Status:** Accepted
- **Decision:** ให้ T-730 V15 คำนวณ signed gate margins จาก operands/thresholds ที่ production detector ส่งออกอยู่แล้ว และแยก longest accepted takeoff interval เป็น previous AIR + current re-arm GAP พร้อม READY frame samples ภายใน external diagnostic collector เท่านั้น
- **Why:** V14 Formal แสดง undercount 3 ครั้งเป็น 1 cycle-separation miss + 2 gate rejections แต่ gate totals ไม่บอกความห่างจาก threshold และ T2T maximum ไม่บอกว่า delay อยู่ก่อนหรือหลัง Landing; การเลือกแก้ gate หรือ state transition ก่อนโดยไม่มีสองข้อมูลนี้เสี่ยงรวมหลายตัวแปรในการทดลองเดียว
- **Affected areas:** `T730PassiveGateAttribution`, debug overlay, regression tests, V15 protocol และ KI-020; ไม่กระทบ `BasicBounceDetector`, Counter หรือ storage
- **Validation:** V15 ที่ commit `df58399` ผ่าน Pure-Kotlin 79/79, Windows tests/build และ Smoke 3/3. Formal ได้ 22/22, `WIN P22 C22 R0`, `LONG T527=A440+G87 RF2 E+0`, BASE `A/L22/22 SUP0`, Result/History/post-stop/seal/stability ผ่าน. Instrumentation จึง validated แต่ Formal ไม่มี rejected WINDOW row และไม่เกิด V14 anomaly จึงยังไม่มีหลักฐานให้เลือก production fix
- **Revisit when:** T-731 repeatability ทำซ้ำ rejected/long-cycle pattern, residual ของ T2T decomposition ไม่เป็นศูนย์, exact evidence pairing ไม่ผ่าน หรือมี pattern อื่นนอก accepted-bookend window

## ADR-026 — แยก RA-only Counterfactual ก่อนเลือก Rescue-Floor Experiment

- **Status:** Accepted for testing
- **Decision:** ให้ T-732 V16 external collector แยก rejected WINDOW proposals ที่ติด `RA` เป็น RA-only กับ RA+other และคำนวณ `ONE/ALL` recovery-floor bounds จาก smoothed ankle operand จริงของ RA-only rows โดยไม่รัน shadow detectorและไม่ใช้ค่าเหล่านี้ควบคุม production result
- **Why:** T-731 Round 1 ได้ proposals 22/22 แต่ Counter 18; rejected ทั้ง 4 ติด RA. สองรายการติด RA เพียง gate เดียว ขณะที่อีกสองรายการยังติด bilateral gate. Gate total `RA4` เพียงอย่างเดียวจึงประเมินผลของการลด RA สูงเกินจริง และการเลือก threshold ก่อนแยก multi-gate blockers จะรวมสาเหตุที่ไม่สามารถแก้ด้วย RA
- **Affected areas:** `T730PassiveGateAttribution`, Debug overlay/tests, T-731/T-732 records และ KI-020; ไม่กระทบ `BasicBounceDetector`, BASE `0.010/0.020`, Counter หรือ storage
- **Interpretation:** `ONE` คือ floor สูงสุดที่ช่วย RA-only อย่างน้อยหนึ่ง row และ `ALL` คือ floor ต่ำสุดที่ช่วย RA-only ทุก row ในรอบนั้น. ทั้งสองเป็น diagnostic bounds ไม่ใช่ production recommendation; ต้องอาศัยหลายรอบและ safety controls ก่อน active experiment
- **Validation:** V16 Smoke ผ่าน 3/3 พร้อม `ONLY0`. Formal ได้ Actual/App 22/19, `WIN P21 C19 R2`; rejected ทั้งสองเป็น RA-only ที่ operands `0.0153/0.0151`, ขณะที่อีกหนึ่ง miss ไม่สร้าง proposal. Result/History, seal และ stability ผ่าน
- **Revisit when:** V16 Formal ไม่มี RA-only row, bounds แปรผันมากระหว่างรอบ, counterfactual ไม่ตรงกับ signed margins หรือ active candidate ทำให้ heel raise/knee lift/standing false positives

## ADR-027 — ใช้ matched RA candidate shadow ก่อน active threshold change

- **Status:** Accepted for testing
- **Decision:** ให้ T-733 V17 รัน production BASE RA `0.020` คู่กับ Debug-only shadows RA `0.016` และ `0.015` บน `PoseFrame`/timestamp เดียวกัน; เฉพาะ BASE result ถูกคืนให้ workout state และ Counter
- **Why:** T-731 RA-only operands อยู่ประมาณ `0.0195/0.0161` และ T-732 อยู่ที่ `0.0153/0.0151`. V16 counterfactual ใช้ได้กับ accepted-bookend jump window แต่ safety controls ที่ควรไม่มี accepted count อาจไม่มี window จึงต้องวัด candidate false counts ด้วย matched detector state จริง
- **Affected areas:** `T733RaCandidateShadow`, Debug Training overlay/tests, T-733 protocol และ KI-020; ไม่กระทบ `BasicBounceDetector.kt`, production BASE, Counter, Result/History หรือ storage
- **Decision gate:** Reject candidate ที่นับ heel raise, knee lift หรือ standing ผิด; candidate ต้องแสดง benefit ใน Basic Bounce และไม่ทำให้ Landing/suppression/performance/stability ถดถอย; ขออนุมัติชัดแจ้งก่อนเปลี่ยน production threshold
- **Revisit when:** arms ไม่เข้าสู่ MATCHED state, BASE ไม่ตรง standalone production detector, shadow overhead ทำให้ performance ถดถอย หรือ RA15 ยังไม่ลด undercount

## ADR-028 — Diagnostic UI และ Payload ต้องเป็น Debug-only

- **Status:** Accepted / Verified
- **Decision:** ให้ count evidence, transition totals, cooldown/rescue summaries และ T-series passive traces สะสมและแสดงเฉพาะเมื่อ `BuildConfig.DEBUG`; ใช้ pure boundary helper ที่ทดสอบ Release-hidden ได้โดยไม่ต้อง render Compose
- **Why:** T-744 static audit พบว่า inner trace บางส่วนมี Debug guard แต่ outer diagnostic panels และ totals ยังเปิดทางให้ Release แสดงหรือสะสมข้อมูลดิบ
- **Affected areas:** `TrainingUiState` update path, Training diagnostic overlays, release-readiness regression tests และ T-745; ไม่กระทบ `BasicBounceDetector`, Counter, thresholds, Result หรือ History
- **Revisit when:** Release APK smoke พบ diagnostic string/row, Debug evidence หาย หรือ production behavior ต่างจาก accepted detector baseline

## ADR-029 — Local Data Backup Boundary

- **Status:** Accepted / Verified
- **Decision:** ปิด Android backup ด้วย `allowBackup=false` และกำหนด explicit exclusions สำหรับทุก storage domain ทั้ง legacy full-backup, Android 12+ cloud backup และ device transfer เพื่อให้ Room History, DataStore settings/nickname และ persisted music URI อยู่ภายในอุปกรณ์
- **Why:** T-744 final privacy audit พบว่า manifest ปัจจุบันเปิด Auto Backup โดยไม่มี exclusions แม้แอปไม่มี INTERNET permission และไม่อัปโหลดกล้อง/pose data เอง
- **Affects:** Android manifest backup configuration, privacy copy, Release merged-manifest verification และ upgrade-install persistence smoke; ไม่กระทบ detector, Counter หรือ Training lifecycle
- **Revisit when:** ผลิตภัณฑ์ตั้งใจรองรับ encrypted user backup/restore พร้อม disclosure และ data-retention design ที่ได้รับอนุมัติ
- **Verification:** T-746 ผ่าน Windows tests/lint/Debug/Release build; Release packaged manifest มี `allowBackup=false` และ rule references ครบ; upgrade install รักษา History เดิม 3 sessions และไม่พบ regression ใน Home/Settings/Training/Result

## ADR-030 — External Production Signing Boundary

- **Status:** Accepted / Verified
- **Decision:** อ่าน production signing properties ผ่าน `ROPESKILL_SIGNING_PROPERTIES` โดย properties และ keystore ต้องอยู่นอก repository; ใช้ task/script เฉพาะสำหรับสร้างและตรวจ signed APK/AAB
- **Why:** private key และ password เป็น release credentials ที่ห้ามอยู่ใน source control ขณะเดียวกัน build ปกติที่ไม่มี secret ต้องยังทำงานได้สำหรับ audit และ development
- **Affects:** `app/build.gradle.kts`, Windows release packaging script, Release APK/AAB verification และ T-747; ไม่กระทบ application runtime, detector, Counter, History หรือ Room schema
- **Revisit when:** เลือก distribution channel, เปิด Play App Signing, ใช้ CI secret store หรือกำหนด key rotation/recovery policy
- **Verification:** T-747 ผ่าน Windows tests/lint/Debug/Release build และ `packageProductionRelease`; signed APK/AAB ผ่าน signature, certificate, package และ SHA-256 verification; Git ไม่ track keystore หรือ signing properties จริง

## ADR-031 — ใช้ Clean Emulator ตรวจ Production Signature โดยรักษาข้อมูลเครื่องหลัก

- **Status:** Accepted / Verified
- **Decision:** ใช้ Android 16 / API 36 clean emulator สำหรับ production-signed identity และ behavior smoke; ไม่ถอน Debug app จาก Samsung Galaxy S23 Ultra และไม่ใช้ผล Emulator เป็นหลักฐานความแม่นยำของ detector
- **Why:** Production certificate ต่างจาก Debug certificate จึงติดตั้งทับแอปบนเครื่องหลักไม่ได้ และการถอน Debug app จะลบ local History ตาม backup boundary ของ T-746
- **Affects:** T-748 release qualification, production installation procedure และการรักษา local History บนเครื่องหลัก; ไม่กระทบ `BasicBounceDetector`, Counter, storage schema หรือ production runtime
- **Revisit when:** มีอุปกรณ์ทดสอบ production แยก, มีแผนย้ายข้อมูลที่ผู้ใช้อนุมัติ หรือจำเป็นต้องทำ hardware/camera acceptance บน production-signed build
- **Verification:** T-748 ยืนยัน `versionCode 1`, `versionName 0.1.0`, ไม่มี `DEBUGGABLE`; Home, Training, Camera permission, Pause/Resume/Finish, Result และ History ผ่านบน Android 16 / API 36 emulator โดยไม่พบ crash/freeze

## ADR-032 — ประกาศ Local-Only Data Boundary และ Health Scope ตาม Production Behavior

- **Status:** Accepted / Verification pending
- **Decision:** ประกาศ Data safety ว่าไม่มี required user data ถูก collect/share ออกจากอุปกรณ์; เปิด Health apps scope เฉพาะ `Activity and Fitness`; เผยแพร่ Privacy Policy แบบ HTML และแสดง Camera disclosure ก่อน runtime permission พร้อม in-app summary/link
- **Why:** Production Manifest ไม่มี `INTERNET` และ data-flow audit ยืนยันว่า Camera/pose อยู่ใน memory, nickname/History/settings/music URI อยู่ใน private local storage และ Android backup/device transfer ถูกปิด ขณะที่ Google Play ยังต้องการ Privacy Policy, Health apps declaration และ disclosure สำหรับ Camera ใน fitness feature แม้ข้อมูลไม่ออกจากอุปกรณ์
- **Affects:** Camera permission copy, Settings privacy UI, public policy page, Data safety/Health apps responses, T-751 และ KI-026; ไม่กระทบ detector, Counter, Room schema, Training lifecycle หรือ release signing
- **Revisit when:** เพิ่ม Internet permission, analytics, ads, account, backend/cloud sync, remote processing, Health Connect, data export/share, new health category หรือ third-party SDK ที่ส่งข้อมูล
- **Verification:** policy/declarations และ source ถูกเตรียมที่ T-751 candidate; ต้องรัน Windows tests/build, manual disclosure/link smoke, signed-out public URL check และบันทึก Play Console forms ก่อนเปลี่ยนสถานะเป็น Verified

## ADR-037 — แยก Speed 30 Landing Classification และ Right-Foot Counter จาก Basic Bounce

- **Status:** Accepted for pilot testing
- **Decision:** เพิ่ม `WorkoutMode.SPEED_30`, `PoseSpeedLandingClassifier` และ `SpeedStepDetector` เป็น pipeline แยก; ใช้ MediaPipe source timestamp กับกฎ right-foot alternation; ห้ามแก้ `BasicBounceDetector`; ยังไม่บันทึก Speed ลง Room History
- **Why:** Speed counting ต้องจำแนก landing ซ้าย/ขวา, ปฏิเสธ repeated-right/both/unclear และบังคับเวลา 30 วินาที ซึ่งเป็น semantics ต่างจาก Basic Bounce อย่างชัดเจน การแยก pipeline ทำให้ unit-test กฎ IJRU ได้โดยไม่เสี่ยง regression ของ detector ที่ผ่านการทดสอบแล้ว
- **Affects:** `PoseFrame`, `PoseDetector`, Camera performance callback, `TrainingViewModel`, Home/Training/Result UI, Speed diagnostics และ unit tests; ไม่กระทบ `BasicBounceDetector.kt` หรือ Room schema
- **Safety boundary:** landmark visibility loss ไม่สร้าง count ชดเชย, tracking loss ไม่ปลด alternation gate, mirrored preview ไม่สลับ anatomical left/right, และ Debug diagnostics ไม่เก็บภาพหรือ landmark list
- **Revisit when:** Windows tests/build ผ่านและ Samsung Galaxy S23 Ultra ให้ข้อมูล standing/left-only/right-only/both-feet/slow-alternation/tracking-loss เพียงพอสำหรับปรับ pilot thresholds หรือออกแบบ Speed History migration

## ADR-038 — วัด Speed Foot Landmark Evidence ก่อนปรับ Lift Threshold

- **Status:** Accepted for diagnostic testing
- **Decision:** หลัง Speed smoke ได้ actual right `32` / app `0` ให้เพิ่ม bounded Debug-only observer ที่รายงาน anatomical left/right phase และ current/maximum rise ratios ของ average, ankle, heel และ foot index/toe โดย reset maxima ที่ `GO`; คง classifier thresholds และ output เดิม
- **Why:** Pose tracking และ frame performance ปกติแต่ classifier ไม่สร้าง landing event ทุกชนิด จึงต้องแยกว่าค่าเฉลี่ยสาม landmark ไม่ถึง `0.08` หรือทุก landmark เคลื่อนต่ำจริงก่อนเลือก threshold/feature ใหม่ การปรับโดยไม่มีค่าจริงอาจเพิ่ม false positives
- **Affects:** `PoseSpeedLandingClassifier` diagnostics, `TrainingViewModel`, Debug Speed overlay, tests และ T-752 V2 record; ไม่กระทบ `BasicBounceDetector.kt`, Speed count gate, timer, Result หรือ Room History
- **Safety boundary:** เปิด evidence accumulation เฉพาะ `BuildConfig.DEBUG`, เก็บเพียง latest snapshot และ maxima, ไม่เก็บภาพ/landmark list/time series และใช้ pure helper test ยืนยัน Release-hidden panel
- **Revisit when:** Samsung Galaxy S23 Ultra V2 แสดง maximum AVG/ANK/HEEL/TOE และ state transitions ชัดพอให้เลือก classifier candidate แบบหนึ่งตัวแปร

## ADR-039 — ผูก Speed 30 Audio Cues กับ Monotonic Timer

- **Status:** Accepted for device testing
- **Decision:** ใช้ one-shot scheduler ที่ตรวจการข้าม threshold `10/20/25/26/27/28/29/30` วินาทีจาก elapsed time เดียวกับ Speed timer; ใช้ Android `TextToSpeech` ภาษาอังกฤษสำหรับคำพูด, fallback tone เมื่อ speech engine ไม่พร้อม และ completion tone 1 วินาทีที่อยู่รอดระหว่างนำทางไป Result
- **Why:** การเทียบเวลาแบบเท่ากับวินาทีเป๊ะอาจพลาดจากรอบอัปเดต 100ms หรือโหลด inference และการผูกกับ Compose/pose frame อาจทำให้ cue ซ้ำหรือถูกตัดเมื่อ Training screen ถูกถอดออก
- **Affects:** `SpeedAudioCueScheduler`, `SpeedAudioCuePlayer`, `TrainingViewModel`, music ducking และ unit tests; ทำงานเฉพาะ Speed 30 และไม่กระทบ `BasicBounceDetector`, Speed classifier thresholds, Room หรือ History
- **Safety boundary:** เคารพ `Sound cues`; Reset, manual Finish และ lifecycle interruption ต้องหยุด cue; cue ลดระดับ Training music ชั่วคราว; ไม่บันทึกเสียง กล้อง pose หรือข้อมูลผู้ใช้
- **Revisit when:** มีไฟล์เสียงแบรนด์ที่ได้รับอนุญาตให้บรรจุในแอป, TTS ภาษาอังกฤษไม่พร้อมบนอุปกรณ์เป้าหมาย, cue timing เกิน tolerance หรือ audio focus/ducking ไม่ผ่าน device smoke

## ADR-040 — ตั้ง Speed Foot Baseline ใหม่จาก Countdown ก่อน GO

- **Status:** Accepted for device testing
- **Decision:** เมื่อ Speed 30 เข้า Countdown ให้เก็บ `FootSample` ที่ valid ล่าสุดเพียง 6 เฟรมแบบ rolling; เมื่อเข้า `GO` ให้ตั้ง average/evidence baseline จากหน้าต่างล่าสุดและ reset foot phase, pending landing และ evidence maxima โดยคง `0.08/0.03/70 ms`; หาก Countdown ถูกยกเลิกให้ทิ้ง window
- **Why:** Evidence V2 พบ motion สูงชัดเจนแต่ phase ติด `AIRBORNE/AIRBORNE` ตั้งแต่ก่อน `GO` เพราะ baseline 6 เฟรมแรกจาก Positioning ไม่ตรงกับท่าสุดท้ายหลัง Countdown จึงไม่เกิด landing event การ re-anchor ที่ boundary แก้ lifecycle cause โดยไม่ลด safety thresholds
- **Affects:** `PoseSpeedLandingClassifier`, Speed path ใน `TrainingViewModel`, unit tests และ T-752 V3 record; ไม่กระทบ `BasicBounceDetector.kt`, audio cues, Speed counter gates, Room หรือ History
- **Safety boundary:** เก็บเพียง 6 samples ต่อเท้าใน memory, ไม่เก็บภาพหรือ landmark time series, ไม่ reset diagnostic totals/source timestamp ordering และไม่สร้าง count ระหว่าง Countdown
- **Revisit when:** V3 device test ยังเริ่ม phase ผิด, first-cycle landing หาย, controls เกิด false count หรือ pose jitter ใน 6 เฟรมสุดท้ายทำให้ baseline ไม่เสถียร

## ADR-041 — ใช้ MediaProjection สำหรับ Integrated Speed 30 Evidence Recording

- **Decision:** เพิ่มเส้นทาง opt-in `RECORD & START` ที่ขอ system consent ก่อน workout, เริ่ม foreground service ชนิด `mediaProjection`, encode เฉพาะภาพหน้าจอเป็น H.264/MP4 สูงสุด long edge 1920 ที่ 30 FPS แล้วเริ่ม Speed 30 หลัง recorder รายงาน `Recording`; หยุดหลัง Result 1.5 วินาทีหรือทันทีเมื่อ Back/app background/system stop
- **Why:** หลักฐาน T-752 ต้องเห็น camera preview, Counter, phase, diagnostic overlay และ Result ใน timeline เดียวกัน; CameraX `VideoCapture` ไม่รวม Compose overlay และจะเพิ่ม use case ใน camera pipeline ที่กำลังทำ Preview + ImageAnalysis
- **Affects:** Home Speed start flow, `MainActivity`, `ScreenRecordingService`, Training REC indicator, Result video actions, Manifest foreground-service declarations และ performance/privacy test plan; ไม่แก้ `BasicBounceDetector.kt`, `PoseSpeedLandingClassifier`, thresholds, Audio Cues, Room หรือ History
- **Revisit when:** ต้องรวม internal audio, รองรับ Android ต่ำกว่า 10, ต้องการ background recording, encoder ทำให้ FPS/LAT/SKIP ถดถอยบนอุปกรณ์จริง หรือระบบ capture API/Play policy เปลี่ยน

## ADR-042 — ขอ Default-display Capture และแยก Recording Disclosure ออกจาก Start Dialog

- **Status:** Accepted
- **Decision:** บน Android 14+ เรียก `createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())`; รุ่นเก่าใช้ `createScreenCaptureIntent()` ตามเดิม หน้าเริ่มแสดงเพียงว่า recording เป็น optional, local และไม่ upload ส่วนรายละเอียดแสดงเป็นรายการ 6 ข้อภายใต้ `RECORDING DETAILS` ที่ผู้ใช้เปิดเอง
- **Why:** T-752 Recorder V1 Smoke บน Samsung Galaxy S23 Ultra แสดงค่าเริ่มต้น `Share one app` แล้วเปิด `Choose app to share`; ผู้ใช้จึงออกจาก picker ก่อน consent สำเร็จ ทำให้ recorder/workout ไม่เริ่ม ขณะที่ disclosure เดิมยาวและทำให้ start dialog รก
- **Affects:** MediaProjection consent intent และ Home Speed 30 start dialogs เท่านั้น; ปุ่มไม่บันทึกใช้ชื่อสั้น `START` และ Android ยังคงขอ consent ทุก session
- **Validation:** Windows verification gates ผ่าน และ Samsung Galaxy S23 Ultra ยืนยัน main dialog 2 ประโยค, details เปิด–ปิด/ครบถ้วน, consent และ workout start ทำงานถูกต้อง โดยไม่มี crash/freeze
- **Revisit when:** ผู้ผลิตอุปกรณ์ override default-display opt-out, Android เปลี่ยน consent UI หรือการทดสอบ privacy/lifecycle พบว่าการ capture ทั้งจอเก็บเนื้อหานอก RopeSkill

## ADR-043 — ให้ผู้ใช้แชร์ Recording จาก Gallery หรือ Video Player

- **Status:** Accepted
- **Decision:** ก่อนเปิด Android consent แสดงคำอธิบายสั้นว่า Android จะขอสิทธิ์ capture หน้าจอ RopeSkill และไม่มีการ upload; หลังบันทึก Result แสดงเฉพาะ `VIEW VIDEO` และไม่สร้าง in-app share intent ผู้ใช้เลือกแชร์เองจาก Gallery หรือ video player
- **Why:** Consent Re-Smoke ผ่านครบ แต่คำว่า “Share your screen” ของ Android และปุ่ม `SHARE VIDEO` ในแอปเป็นคนละขั้นตอนที่ทำให้ผู้ใช้สับสน การตัด share action ออกจาก RopeSkill ทำให้ start/result flow ชัดขึ้นโดยไม่เปลี่ยนความสามารถบันทึก
- **Affects:** Home Speed 30 start copy, Result recording action, in-app privacy summary และ public policy เท่านั้น; ไม่กระทบ recorder lifecycle, detector, thresholds, calibration, Audio Cues, Room หรือ History
- **Validation:** Device smoke ยืนยัน `VIEW VIDEO` ทำงาน, ไม่มี `SHARE VIDEO`, no-upload disclosure แสดง และ preview ไม่กระตุก; Start Dialog Final Cleanup รอบถัดมายืนยัน copy/layout ผ่านครบ
- **Revisit when:** ผู้ใช้ต้องการ workflow ส่งหลักฐานจากในแอปโดยตรง หรือมี destination ที่ระบุชัดและผ่าน privacy review

## ADR-044 — บันทึก Speed 30 ใน Room โดยแยกความหมายของ Count ตาม Exercise Type

- **Status:** Accepted / Verified
- **Decision:** เมื่อ Speed 30 เริ่มจับเวลาแล้ว ให้บันทึก session ด้วย `exerciseType = SPEED_30` ใน Room schema version 1 เดิม; field `jumpCount` เก็บจำนวน right-foot landings สำหรับชนิดนี้; History แสดง `RIGHT STEP(S)` และ Home summary/Last ของ Basic Bounce กรองเฉพาะ `BASIC_BOUNCE`
- **Why:** Device test แสดง Result `RIGHT STEPS 1` แต่ History ว่าง เพราะ `finishWorkout()` ตัดทุก mode ที่ไม่ใช่ Basic Bounce ก่อน `save()` ขณะที่ schema มี `exerciseType` พร้อมอยู่แล้ว การแยกการตีความด้วย type แก้ persistence โดยไม่ migration และป้องกันหน่วยต่างชนิดถูกรวมกัน
- **Affects:** `TrainingViewModel`, session constants, History labels, Home Basic Bounce filtering, unit tests และเอกสาร; ไม่กระทบ Room schema, detector ทั้งสอง, thresholds `0.08/0.03/70 ms`, recorder, Audio หรือ session deletion
- **Revisit when:** เพิ่ม Speed summary บน Home, เพิ่ม workout type ใหม่, ต้อง query/filter ข้อมูลจำนวนมากที่ DAO หรือเปลี่ยนชื่อ generic count field ใน schema รุ่นถัดไป
- **Verification:** Windows tests/lint/Debug/Release ผ่าน; Samsung Galaxy S23 Ultra ยืนยัน Result `3`, History `SPEED 30 — 3 RIGHT STEPS — 00:30`, Basic Bounce Home summary ไม่เปลี่ยน และไม่มี crash/freeze; GitHub checkpoint `9ccc31a`

## ADR-045 — ใช้ Recording Switch กับปุ่ม Start เพียงปุ่มเดียว

- **Status:** Accepted / Verification pending
- **Decision:** หน้าเริ่ม Speed 30 ใช้ switch `Record this workout` ซึ่ง reset เป็น OFF ทุกครั้งที่เปิด dialog และมีปุ่มหลักเพียง `START WORKOUT`; เมื่อ switch OFF ให้เริ่ม workout ทันที เมื่อ ON จึงเข้าสู่ camera-permission/MediaProjection consent และรอ recorder พร้อมตาม lifecycle เดิม; คง `RECORDING DETAILS`
- **Why:** Device testing พบว่าปุ่ม `START` และ `RECORD & START` ที่อยู่ติดกันเสี่ยงให้ผู้ใช้กดผิด การแยก recording เป็นตัวเลือกที่เห็นสถานะได้ก่อนกดปุ่มเดียวลดความคลาดเคลื่อน โดยยังรักษา opt-in และ consent ทุก session
- **Affects:** Home Speed 30 start dialog, privacy wording และ regression tests เท่านั้น; ไม่กระทบ recorder service/lifecycle หลังเลือก, detector, thresholds `0.08/0.03/70 ms`, calibration, Audio, Room หรือ History
- **Revisit when:** usability test พบว่า switch ไม่ชัดเจน, accessibility test พบ touch/semantics issue หรือ Android เปลี่ยน consent requirements

## ADR-046 — วัด Speed AIRBORNE Phase Latch ก่อนเปลี่ยน Landing Re-arm

- **Status:** Accepted for diagnostic testing
- **Decision:** เพิ่ม Debug-only passive evidence ต่อเท้า ได้แก่ exact rise ratio ที่ classifier ใช้จริง, current/maximum AIRBORNE duration และ minimum rise ภายใน current/longest AIR episode; คง lift `0.08`, landing `0.03`, simultaneous window `70 ms` และ event/count output เดิม
- **Why:** V3 เริ่ม `GO` ที่ `G/G` และแก้ปัญหานับ 0 แต่ Round 2 actual/app `35/15`; timeline พบ Raw `R` ค้างที่ 4 และ 11 ขณะ Raw `L` เพิ่มและ `PHASE R=A`, โดย `RR=2`, OOS/TL=0 ค่าจาก Evidence V2 ใช้ baseline คนละชุดกับ state machine จึงยังวัด landing margin ที่แท้จริงไม่ได้
- **Affects:** `PoseSpeedLandingClassifier` diagnostics, Debug Speed overlay, regression tests, T-752 V4 และ KI-027; ไม่กระทบ `SpeedStepDetector`, `BasicBounceDetector.kt`, thresholds, recorder lifecycle, audio, Room หรือ History
- **Revisit when:** Samsung Galaxy S23 Ultra V4 แสดง longest AIR duration และ minimum exact rise ชัดพอเลือก bounded re-arm pilot หรือพบว่า phase latch ไม่เกิดซ้ำ

## ADR-047 — Conservative Speed Phase Re-arm ด้วย recovery band สองเฟรม

- **Status:** Pilot failed accuracy / Evidence retained
- **Decision:** คง lift `0.08`, strict landing `0.03` และ simultaneous window `70 ms`; เพิ่มเฉพาะขณะ foot phase เป็น `AIRBORNE` ให้ re-arm ได้เมื่อ exact classification rise อยู่ไม่เกิน `0.04` ต่อเนื่อง 2 valid frames จากนั้น re-anchor local baseline ที่ sample ยืนยันและส่ง landing เพียงหนึ่งครั้ง พร้อม Debug counter `REARM L/R`
- **Why:** V4 ได้ actual/app `33/19`, Raw `R=19`, `RR/OOS/TL=0`, right AIR latch สูงสุด `4,275 ms` และ current latched minimum `0.038`; จึงมีหลักฐานว่าการกลับใกล้พื้นจริงยังไม่ข้าม strict `0.03`. การกำหนด recovery band แคบและต้องยืนยันสองเฟรมลดความเสี่ยงจาก one-frame jitter เมื่อเทียบกับการขยาย strict threshold โดยตรง
- **Affects:** `PoseSpeedLandingClassifier`, Debug Speed overlay, classifier regression tests, T-752 V5 และ KI-027; ไม่กระทบ `BasicBounceDetector.kt`, `SpeedStepDetector`, recorder, audio, Room schema หรือ History
- **Revisit when:** device accuracy ยังต่ำกว่า 90%, safety control ใดเกิด false right step, `BOTH` เพิ่มผิดปกติ, duplicate landing เกิดขึ้น, AIR latch หลายวินาทียังคงอยู่ หรือ performance/stability ถดถอย

## ADR-048 — วัด consecutive near-ground streak ก่อนปรับ V5 อีกครั้ง

- **Status:** Accepted for diagnostic testing
- **Decision:** คง V5 production behavior และค่า `0.08/0.03/0.04/2 valid frames/70 ms` ทุกค่า; เพิ่ม Debug-only observer ที่อ่าน exact classification rise และ phase เดียวกับ state machine ก่อน production update เพื่อรายงาน current/maximum near-ground streak, streak starts, out-of-band breaks, strict-landing completions และ tracking-loss breaks แยกซ้าย/ขวา โดย observer ไม่ส่ง eventและไม่ขับ Counter
- **Why:** V5 Accuracy Round 1 ได้ actual/app ประมาณ `35/17`, Raw `L/R=22/22`, `REARM L/R=2/2`, `RR=5` และ AIR latch สูงสุด `L4,443/R6,766 ms`. V5 กู้ได้จริงเพียง 2 ครั้งต่อข้าง แต่ aggregate minima `0.025/0.019` ยังไม่บอกว่าเฟรมใน recovery band เกิดต่อเนื่องครบ 2 เฟรมหรือถูกตัดด้วยเฟรม out-of-band, strict landing หรือ tracking loss
- **Affects:** `PoseSpeedLandingClassifier` diagnostics, Debug Speed overlay, parity/regression tests, T-752 V6 และ KI-027; ไม่กระทบ event/count output, `BasicBounceDetector.kt`, `SpeedStepDetector`, recorder, audio, Room schema หรือ History

## ADR-049 — วัด ground reference และองค์ประกอบ normalized gap ก่อนปรับ threshold

- **Status:** Evidence collected / Accuracy failed
- **Decision:** คง V5 production behavior และ thresholds ทั้งหมด; เพิ่ม Debug-only observer ที่อ่าน production baseline, average foot-ground Y, leg length และ phase ก่อน `updateFoot()` เพื่อรายงาน baseline ที่ `GO`, baseline shift และองค์ประกอบของ closest airborne gap แยกซ้าย/ขวา โดยไม่เขียนกลับเข้า state machine
- **Why:** V6 actual/app ประมาณ `27/7` แสดง `NG max 2/2`, right `NGS/NGB/NGX/NGL = 7/0/3/0` และ `REARM R4`; ดังนั้นกฎสองเฟรมทำงานเมื่อเข้า band แต่ raw right landing มีเพียง 12. ต้องแยกว่า genuine contact ไม่กลับถึง ground reference, reference เลื่อน หรือ normalization ด้วย leg length ทำให้ exact ratio ยังสูงกว่า `0.04`
- **Affects:** `PoseSpeedLandingClassifier` diagnostics, Debug Speed overlay, parity/regression tests, T-752 V7 และ KI-027; ไม่กระทบ event/count output, `BasicBounceDetector.kt`, `SpeedStepDetector`, recorder, audio, Room schema หรือ History
- **Evidence:** V7 actual/app ประมาณ `30±1/18`, raw `L/R=28/20`, `RR=2`, right maximum baseline shift `0.148`, right `NGS/NGB=14/6`, tracking loss 0 และ skipped frames ประมาณ 0; ยังไม่เพียงพอให้เปลี่ยน threshold เพราะบางรอบ `LOWGAP` กลับถึงหรือต่ำกว่า reference
- **Revisit when:** V8 fixed-reference shadow แสดงจำนวน raw landings และ strict/recovery split บนเฟรมเดียวกับ production ชัดพอแยก baseline drift จาก movement variance

## ADR-050 — เปรียบเทียบ adaptive baseline กับ fixed-at-GO shadow ก่อนเปลี่ยน behavior

- **Status:** Accepted for diagnostic testing
- **Decision:** คง V5 production behavior และ thresholds ทุกค่า; เพิ่ม Debug-only shadow state machine ต่อเท้าที่ล็อก baseline ณ `GO` แล้วอ่าน FootSample/leg length เดียวกับ production ด้วย `0.08/0.03/0.04 × 2 frames` เพื่อรายงาน phase, current ratio, recovery streak, airborne transitions และ strict/recovery/total landings โดยไม่ส่ง event
- **Why:** V7 พบ right baseline shift สูงสุด `0.148` และ production raw right เพียง `20` จากประมาณ `30` contacts แต่ diagnostics ไม่ได้คำนวณว่า fixed reference จะกู้ cycle ได้จริงกี่ครั้ง การจำลอง shadow บนเฟรมเดียวกันจะแยก baseline drift จาก threshold และความต่างของแต่ละ test round ได้โดยตรง
- **Affects:** `PoseSpeedLandingClassifier` Debug diagnostics, Speed overlay, regression tests, T-752 V8 และ KI-027; ไม่กระทบ production baseline/phase/events, `SpeedStepDetector`, Counter, `BasicBounceDetector.kt`, recorder, audio, Room schema หรือ History
- **Revisit when:** V8 device evidence เปรียบเทียบ `FXLAND R` กับ production raw `R`, actual right contacts และ strict/recovery split ได้ครบ

## ADR-051 — เพิ่ม Video Test Mode แบบ Debug-only สำหรับหลักฐานที่ทำซ้ำได้

- **Status:** Implemented / Aggregate and cadence evidence passed
- **Decision:** เพิ่ม route เฉพาะ `BuildConfig.DEBUG` ให้ผู้ใช้เลือกวิดีโอด้วย Storage Access Framework, กำหนดตำแหน่ง `GO`, อ่านช่วงก่อน GO 2 วินาทีและหลัง GOสูงสุด 30 วินาทีที่ประมาณ 30 FPS แล้วประมวลผลบน background thread ด้วย MediaPipe `VIDEO`, production `PoseSpeedLandingClassifier`, `SpeedStepDetector` และ V8 fixed-reference shadow ชุดเดิม; export CSV เกิดเฉพาะเมื่อผู้ใช้กดและเลือกปลายทาง
- **Why:** การกระโดดใหม่แต่ละรอบเปลี่ยน cadence/technique จึงเปรียบเทียบ detector คนละเวอร์ชันได้ไม่ยุติธรรม วิดีโอเดียวกันทำให้ input ทำซ้ำได้และวัด production raw landings เทียบ fixed-reference shadow โดยไม่ปน movement variance
- **Affects:** Debug Home entry, `VideoTestScreen`, `VideoTestViewModel`, synchronous `VideoPoseProcessor`, in-memory report และ tests เท่านั้น; อ่าน `content://` ต้นฉบับโดยไม่คัดลอกวิดีโอ, ไม่สร้าง Recording, Training session, Room/History entry และไม่แตะ `BasicBounceDetector.kt`, production thresholds หรือ Counter behavior
- **Privacy:** ประมวลผลภาพและ landmark บนอุปกรณ์, ไม่บันทึกภาพ/landmark, ไม่ upload และไม่ persist URI permission; CSV ที่มีเฉพาะ metrics และ landing timestamps เกิดเมื่อผู้ใช้สั่งเท่านั้น
- **Device evidence:** วิดีโอ `demo.mp4` ที่ ground truth right-foot landings = 55 ให้ production R=25, counted right=24 และ fixed-reference shadow R=55; aggregate ของ fixed-reference ตรง ground truth แต่ยังพิสูจน์ event timing ไม่ได้จาก CSV สรุปรวมเดิม
- **Extension:** CSV ส่งออกเพิ่ม event rows ที่มี source video timestamp, เวลาเทียบ GO, detector source, foot, strict/recovery, counted-right flag และ counter reject reason โดยใช้ callback diagnostic ที่ไม่ส่ง eventเข้า production Counter
- **Event evidence:** V8 right events 55 จุดตรวจต่อเนื่องถึง +29.845s, interval 495–594ms
  (median 528ms) และไม่พบ duplicate cadence; production right events มีเพียง 25 และหยุดที่
  +14.137s ส่วน V8 left มี 52 จุด จึงต้องจำลอง alternation ก่อนพิจารณา promotion
- **Revisit when:** event-level CSV ยืนยัน false positive/miss รายจังหวะ, device test พบ extraction ช้าเกินใช้งาน, variable-frame-rate timestamp ทำให้ผลต่างจาก CameraX มาก, orientation/codec อ่านไม่ได้, ต้องการ frame-accurate MediaCodec pipeline หรือ Release build ต้องมีโหมดนี้สำหรับผู้ใช้ทั่วไป

## ADR-052 — จำลอง V8 ผ่าน Counter เดิมก่อนเปลี่ยน production behavior

- **Status:** Device evidence passed / Candidate undercounted
- **Decision:** เพิ่ม V9 evaluator เฉพาะ Video Test Mode ที่นำ fixed-reference landing events มา
  รวม opposite-foot ภายใน simultaneous window 70ms แบบเดียวกับ production classifier แล้วส่ง
  event ที่ได้เข้า `SpeedStepDetector` instance แยกต่างหาก; แสดง candidate right steps,
  accepted/rejected totals, repeated L/R, BOTH และ CSV decisions พร้อม state ก่อน/หลังและ count
- **Why:** V8 พบ right ครบ 55 แต่ left เพียง 52; raw right aggregate จึงยังไม่ตอบว่า alternation
  rule จะรับครบกี่ครั้ง การจำลองด้วย Counter class เดิมเปิดเผยผลจริงและจุด reject โดยไม่ต้องเปลี่ยน
  production classifier หรือ Counter ที่ผู้ใช้ใช้งาน
- **Affects:** `VideoTestCandidateCounter`, Video Test result/CSV และ unit tests เท่านั้น; ไม่กระทบ
  `PoseSpeedLandingClassifier` production output, `SpeedStepDetector`, `BasicBounceDetector.kt`,
  thresholds, Training, Room, History, recorder หรือ Release route
- **Device evidence:** `demo.mp4` ที่ GO 2,000ms ได้ candidate right `53`, accepted/rejected
  `105/2`, repeated `L0/R2`, BOTH `0`; right ที่ +15.193s และ +15.721s ถูก reject หลัง V8
  พลาด left สองจุด จึงยืนยันว่าการบังคับ alternation ทำให้เสีย valid right events
- **Revisit when:** V9 device CSV ยืนยัน candidate count/reject timestamps, same-video repeat ให้ผล
  deterministic และ safety-control videos พร้อมสำหรับ standing, one-foot, both-feet และ tracking loss

## ADR-053 — ทดสอบ Right-primary Counter พร้อม bounded refractory

- **Status:** Accepted for diagnostic testing
- **Decision:** เพิ่ม V10 evaluator เฉพาะ Video Test Mode ที่อ่านเฉพาะ V8 fixed-reference right
  landing หลัง GO และนับโดยตรงเมื่อห่างจาก right ที่รับล่าสุดอย่างน้อย 300ms; บันทึก accepted/rejected,
  refractory reject และช่วง min/median/max ระหว่าง right ที่รับใน result/CSV โดยไม่แก้ V8/V9 เดิม
- **Why:** V8 ตรวจ right ของ `demo.mp4` ครบ 55 จุดด้วย cadence 495–594ms แต่ V9 เหลือ 53
  เพราะ left miss สองจุด การใช้ refractory 300ms กัน duplicate ที่ชิดกว่าหลักฐานครั้งจริงอย่างมี margin
  และไม่ผูกความถูกต้องของ right count กับ left detector; ค่านี้ยังเป็น candidate ที่ต้องผ่าน controls
- **Affects:** `VideoTestRightPrimaryCounter`, Video Test result/CSV และ unit tests เท่านั้น; ไม่กระทบ
  production classifier/thresholds, `SpeedStepDetector`, `BasicBounceDetector.kt`, Training, Room,
  History, recorder หรือ Release route
- **Revisit when:** same-video device rerun ไม่ได้ 55/55, right event ใดถูก refractory reject,
  safety control เกิด false right count, cadence ที่ถูกต้องต่ำกว่า 300ms หรือมีหลักฐานว่าควรใช้
  adaptive cadence/refractory แทนค่าคงที่

## ADR-054 — ผูกผล Video Test กับ manual ground truth ก่อนวิเคราะห์

- **Status:** Accepted for diagnostic testing
- **Decision:** V11 Video Test Mode ต้องรับจำนวน anatomical right steps ที่ตรวจด้วยตนเองก่อน
  analysis โดยใช้ `0` สำหรับ negative control; ผลและ CSV แสดง signed error, absolute error,
  PASS/FAIL และ bounded count agreement เฉพาะเมื่อ ground truth มากกว่า 0
- **Why:** ยอด detector อย่างเดียวไม่ใช่ accuracy และการใช้ production output เป็น reference ทำให้
  สรุป V8 ผิดในรอบก่อน การบันทึก ground truth คู่กับผลทุกคลิปทำให้ positive/negative controls
  เปรียบเทียบได้โดยไม่คำนวณภายนอก และไม่แสดง percentage ที่ไม่มีความหมายเมื่อ expected = 0
- **Affects:** `VideoTestUiState`, Video Test result/CSV, debug UI และ unit tests เท่านั้น; ไม่กระทบ
  production classifier/thresholds, Counter, `BasicBounceDetector.kt`, Training, Room, History,
  recorder หรือ Release route
- **Revisit when:** ต้องบันทึก event-level manual annotations แทน aggregate ground truth หรือมี
  validation dataset ที่จัดการ metadata/labels ภายนอก Video Test Mode

## ADR-055 — Guard right-primary ด้วยหลักฐาน alternation ที่ยืนยันแล้ว

- **Status:** Accepted for diagnostic testing
- **Decision:** เพิ่ม V12 evaluator เฉพาะ Video Test Mode โดยคง V9/V10 ไว้เป็น baseline;
  ก่อนนับต้องพบ bounded alternating sequence `R-L-R` หรือ `L-R-L` ที่ transition แต่ละช่วง
  ไม่เกิน 900ms และ right spacing ไม่น้อยกว่า refractory 300ms หลังยืนยันแล้ว right ปกติต้องมี
  left ใหม่ภายใน 900ms แต่อนุญาต cadence bridge เมื่อ left หายได้ไม่เกิน 2 right events ต่อเนื่อง
- **Why:** V10 แก้ reference undercount ได้ `55/55` แต่ confirmed left-only สร้าง false right 2 จุด
  หลัง left ล่าสุดมากกว่า 20 วินาที ขณะที่ reference พลาด left ติดกันเพียง 2 จุดระหว่าง right cadence
  528–561ms; bounded establishment ป้องกัน one-foot sequence ไม่ให้เริ่ม counter และ bridge จำกัด
  รักษา genuine right สองจุดที่ V9 เคยตัดทิ้ง
- **Affects:** `VideoTestAlternationGuardCounter`, Video Test result/CSV/debug UI และ unit tests
  เท่านั้น; ไม่กระทบ V8/V9/V10, production classifier/thresholds, `SpeedStepDetector`,
  `BasicBounceDetector.kt`, Training, Room, History, recorder หรือ Release route
- **Offline evidence:** replay event rows เดิมคาด reference/right-only/left-only เป็น `55/0/0`;
  reference ใช้ sequence/recent-left/bridge `2/51/2` และไม่มี reject ส่วน left-only right ที่
  `30.063s`/`31.878s` ถูก reject เป็น `UNCONFIRMED_ALTERNATION`
- **Revisit when:** device replay ไม่ตรง offline result, slow/fast alternation ที่ถูกต้องถูกตัด,
  negative control อื่นสร้าง false count, tracking gap ทำให้ bridge ผิด หรือมี dataset มากพอ
  กำหนด cadence window แบบ adaptive โดยไม่ผูกกับคลิป reference เดียว

## ADR-056 — กำหนด minimum gap สำหรับ side transition ใน V13 diagnostic

- **Status:** Accepted and device-validated for diagnostic testing; production promotion pending
- **Decision:** เพิ่ม V13 evaluator แยกจาก V12 ใน Video Test Mode และกำหนด
  `LEFT↔RIGHT` transition อย่างน้อย 100ms ทั้งตอนสร้าง `R-L-R`/`L-R-L` bootstrap และตอนรับ
  `RECENT_LEFT`; คง maximum gap 900ms, right refractory 300ms และ cadence bridge limit 2 เดิม
- **Why:** V12 ผ่าน reference `55/55` และ left-only `0/0` แต่ right-only ได้ false positives 3;
  frame/CSV review ยืนยัน `LEFT→RIGHT` 33ms สองคู่เป็นการเคลื่อนไหวเดียวที่สลับ side label
  ไม่ใช่ anatomical alternation. Reference มี transition ต่ำสุด 132ms จึงมี evidence margin 32ms
  เหนือ candidate 100ms โดยไม่ตัด count ในชุดปัจจุบัน
- **Affects:** `VideoTestMinimumTransitionGapCounter`, V13 result/CSV/debug UI และ unit tests
  เท่านั้น; V12 ยังคงแสดงเป็น baseline และไม่กระทบ production classifier/thresholds,
  `SpeedStepDetector`, `BasicBounceDetector.kt`, Training, Room, History, recorder หรือ Release
- **Offline evidence:** replay event tables จาก device CSV ให้ reference/left-only/right-only
  `55/0/0`; right-only ไม่สามารถสร้าง bootstrap จาก 33ms transition ขณะที่ reference คง 55
- **Device validation:** Windows 4 gates ผ่านที่ `d2ff4a0`; Samsung Galaxy S23 Ultra replay
  reference/right-only/left-only ได้ `55/0/0` ตรงกับ offline result. One-foot controls ไม่สร้าง
  History และไม่พบ crash/freeze. C1 standing และ C2 both-feet controls ได้ ground truth/V13
  `0/0` ทั้งคู่โดยไม่สร้าง History หรือเกิด crash/freeze; V13 ปฏิเสธ same-movement side-label
  flips ที่ `0ms`/`33ms` ได้ตามวัตถุประสงค์. ผลนี้ยืนยัน negative controls ที่ทำแล้ว แต่ยังไม่
  อนุมัติ production replacement จนกว่า valid slow/fast alternation และ deterministic repeats จะผ่าน
- **Revisit when:** Windows/device replay ไม่ตรง offline result, valid fast alternation มี transition
  ต่ำกว่า 100ms, frame sampling rate เปลี่ยน, หรือ controls เพิ่มเติมแสดง side-label flip ที่ยาวกว่า
  minimum gap ปัจจุบัน

## Template สำหรับ Decision ใหม่

```text
## ADR-XXX — ชื่อการตัดสินใจ
- Status: Proposed | Accepted | Superseded
- Decision:
- Why:
- Affects:
- Revisit when:
```
