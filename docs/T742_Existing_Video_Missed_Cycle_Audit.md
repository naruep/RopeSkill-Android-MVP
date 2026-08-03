# T-742 — Existing-Video Missed-Cycle Evidence Audit

สถานะ: Complete — cycle-level audit finished; bounded passive diagnostic required

## เป้าหมาย

ใช้ Screen Recordings ที่มีอยู่จาก T-740 และ T-741 เพื่อระบุตำแหน่ง physical cycles ที่ Counter ไม่เพิ่ม และจัดชั้นหลักฐานที่มองเห็นได้ โดยไม่เปลี่ยน `BasicBounceDetector`, thresholds, camera pipeline หรือ production behavior

T-741 ยืนยัน KI-022 ว่า same-condition repeatability variance เกิดซ้ำที่ `18/22` แต่ rejection labels ที่มองเห็นยังเป็นหลักฐานรวม/ชั่วขณะ ไม่ใช่ causal record ต่อ missed cycle

## Inputs

1. T-740 Round A: `Screen_Recording_20260731_090213.mp4` — `21/22`
2. T-740 same-clothing repeat: `Screen_Recording_20260731_090824.mp4` — `15/22`
3. T-740 same-clothing repeat: `Screen_Recording_20260731_090915.mp4` — `18/22`
4. T-740 changed-clothing Round B: `Screen_Recording_20260731_091726.mp4` — `18/22`
5. T-741 same-condition Run 1: `Screen_Recording_20260731_092805.mp4` — `18/22`

หากชื่อไฟล์ใดไม่ตรงกับหลักฐานต้นฉบับ ให้แก้ mapping ก่อนสรุปผล ห้ามใช้ชื่อไฟล์เป็นหลักฐานแทนเนื้อหาวิดีโอ

## Privacy boundary

- วิเคราะห์เฉพาะไฟล์ที่ผู้ใช้ส่งมาเพื่อโครงการนี้
- ไม่บันทึกภาพบุคคลหรือ pose landmarks ลง Git
- เอกสารเก็บเฉพาะ timestamp, cycle index, Counter/state/reason ที่อ่านได้ และข้อสรุปรวม
- ไม่มีการ upload วิดีโอหรือภาพร่างกายไปยังบริการภายนอก

## Audit method

### 1. Establish each session timeline

- ระบุเวลา `GO`, physical jump แรก/สุดท้าย, ช่วงยืนนิ่ง และ Finish
- นับ physical cycles 1–22 จากการ takeoff/landing ที่มองเห็น
- บันทึก Counter ก่อนและหลังแต่ละ cycle
- ตรวจว่า Counter transition เกิดหลัง Landing หรือ delayed frame โดยไม่ตีความจากเฟรมเดียว

### 2. Classify each missed cycle

ใช้หนึ่งในชั้นหลักฐานต่อไปนี้:

- `VISIBLE_GATE_REJECTION`: มี rejection label/row ที่เวลาเดียวกับ physical cycle และ state พร้อมให้จับคู่
- `STATE_OR_LANDING_EVIDENCE`: มี accepted Takeoff หรือ AIRBORNE/state transition แต่ Count/Landing ไม่ครบ
- `PROPOSAL_ABSENT_OR_UNCERTAIN`: เห็น physical cycle แต่ไม่มี accepted transition และไม่มี rejection evidence ที่จับคู่ได้
- `INSUFFICIENT_EVIDENCE`: overlay ถูกบัง อ่านไม่ได้ timestamp ไม่พอ หรือจับคู่หลาย event ได้

ห้ามเปลี่ยน `PROPOSAL_ABSENT_OR_UNCERTAIN` เป็น gate failure โดยอาศัย aggregate totals หรือข้อความที่ปรากฏหลัง cycle อื่น

### 3. Record visible quality/context

ต่อ missed cycle ให้บันทึกเท่าที่วิดีโอรองรับ:

- เท้า/ข้อเท้าทั้งสองยังอยู่ในเฟรมหรือไม่
- pose overlay มองเห็นและติดตามสองข้างหรือไม่
- มีการเอียงตัว เปลี่ยนความกว้างเท้า หรือ drift ของตำแหน่งหรือไม่
- cadence interval ก่อน/หลัง miss โดยประมาณ
- Counter/state/reason ที่อ่านได้

ข้อมูลเหล่านี้เป็น correlation evidence ไม่ใช่ root cause จนกว่าจะมี detector operands/state trace รองรับ

## Session mapping

เวลาในตารางเป็นเวลาจาก passive trace หลัง `GO` (`+s`) ไม่ใช่เวลานาฬิกาของโทรศัพท์. ใช้ physical range ที่ต่อเนื่อง 22 pulse ต่อ session เพื่อกัน pulse ระหว่างเตรียมตัวและ pulse หลังหยุดออกจาก ground truth:

| Session | Physical trace range | Actual/App | Missed-cycle classification |
|---|---|---:|---|
| `090213` Round A | `#08–#29` | `22/21` | State/Landing 1 |
| `090824` same clothing | `#14–#35` | `22/15` | Visible gate 7 |
| `090915` same clothing | `#18–#39` | `22/18` | State/Landing 3; Proposal absent/uncertain 1 |
| `091726` changed clothing | `#07–#28` | `22/18` | Visible gate 3; State/Landing 1 |
| `092805` T-741 | `#03–#24` | `22/18` | State/Landing 4 |

Gate-attributed pulses `090213 #30`, `090824 #37`, `091726 #29` และ `092805 #25` เกิดหลัง physical range 22 ครั้ง จึงไม่ถูกใช้เป็นเหตุผลของ miss. การอ่าน aggregate `G`, `UA` หรือ rejected label โดยไม่ตัด pulse เหล่านี้ออกจะ over-attribute สาเหตุ

## Evidence table

เท้าอยู่ในเฟรมและ pose overlay ติดตามสองข้างตลอด physical ranges ที่บันทึกด้านล่าง; ทุก session มี `X0`, ไม่มี frame-flow หรือ stability anomaly. `Normal` ในคอลัมน์ cadence หมายถึงช่วงใกล้เคียงรอบข้างประมาณ `0.4–0.55s`; เป็น correlation evidence ไม่ใช่ root cause

| Session | Physical cycle | Trace time | Counter before → after | Visible state/reason | Cadence | Classification | Confidence |
|---|---:|---|---|---|---|---|---|
| `090213` | 19 | `+17.720` | `18 → 18` | `QU ... AIR NP` | Short interval but inside continuous range | `STATE_OR_LANDING_EVIDENCE` | High |
| `090824` | 10 | `+11.775` | `9 → 9` | `QU ... READY RES[BL]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090824` | 17 | `+15.212` | `15 → 15` | `QU ... READY RES[BL+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090824` | 18 | `+15.652` | `15 → 15` | `QU ... READY RES[BL+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090824` | 19 | `+16.178` | `15 → 15` | `QU ... READY RES[BL+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090824` | 20 | `+16.643` | `15 → 15` | `QU ... READY RES[BL+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090824` | 21 | `+17.188` | `15 → 15` | `QU ... READY RES[BL+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090824` | 22 | `+17.616` | `15 → 15` | `QU ... READY RES[BL+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `090915` | 1 | `+5.607` | `0 → 0` | `rU A0.047 H0.034 ... READY`; no qualified proposal/gate row | First physical pulse | `PROPOSAL_ABSENT_OR_UNCERTAIN` | High |
| `090915` | 4 | `+7.446` | `2 → 2` | `QU ... AIR NP` | Normal | `STATE_OR_LANDING_EVIDENCE` | High |
| `090915` | 8 | `+9.345` | `5 → 5` | `QU ... AIR NP` | Normal | `STATE_OR_LANDING_EVIDENCE` | High |
| `090915` | 10 | `+10.352` | `6 → 6` | `QU ... AIR NP` | Normal | `STATE_OR_LANDING_EVIDENCE` | High |
| `091726` | 6 | `+9.674` | `5 → 5` | `QU ... AIR NP` | Short interval but inside continuous range | `STATE_OR_LANDING_EVIDENCE` | High |
| `091726` | 13 | `+12.981` | `11 → 11` | `QU ... READY RES[BR+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `091726` | 14 | `+13.490` | `11 → 11` | `QU ... READY RES[BL+BR+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `091726` | 20 | `+16.308` | `16 → 16` | `QU ... READY RES[BL+BR+RA]` | Normal | `VISIBLE_GATE_REJECTION` | High |
| `092805` | 9 | `+6.315` | `8 → 8` | `QU ... AIR NP` | Normal | `STATE_OR_LANDING_EVIDENCE` | High |
| `092805` | 11 | `+7.286` | `9 → 9` | `QU ... AIR NP` | Normal | `STATE_OR_LANDING_EVIDENCE` | High |
| `092805` | 12 | `+7.644` | `9 → 9` | `QU ... AIR NP` | Short interval; frame/counter sequence confirms separate physical cycle | `STATE_OR_LANDING_EVIDENCE` | Medium |
| `092805` | 14 | `+8.720` | `10 → 10` | `QU ... AIR NP` | Normal | `STATE_OR_LANDING_EVIDENCE` | High |

## Audit result

```text
# T-742 Existing-Video Missed-Cycle Evidence Audit

Sessions reviewed: 5
Total physical cycles: 110
Total app counts: 90
Total misses: 20

VISIBLE_GATE_REJECTION: 10
STATE_OR_LANDING_EVIDENCE: 9
PROPOSAL_ABSENT_OR_UNCERTAIN: 1
INSUFFICIENT_EVIDENCE: 0

Repeated pattern: Two signatures recur — READY takeoff-gate rejection and a physical
motion pulse while production remains AIR with no matched production peak.
Clothing-specific pattern: No. Both signatures occur across same-clothing and
changed-clothing sessions.
Performance/stability anomaly: None visible; T/L accepted counts stay balanced,
SUP0, X0, post-stop count 0 and recorded performance remains normal.
Evidence limitation: Screen Recording does not retain per-frame landing operands,
returned-to-baseline component margins, completed-vertical-cycle operands or timeout
path for the nine AIR-state misses.
Recommended next step: T-743 bounded debug-only passive Landing/State trace. Do not
change takeoff thresholds, landing rules or BasicBounceDetector production behavior.
Result: Complete
```

## Decision

T-742 รองรับทางเลือกที่ 1: หลักฐานพอออกแบบ bounded passive diagnostic แต่ยังไม่พอเสนอ detector candidate. กลุ่ม `VISIBLE_GATE_REJECTION` มี cycle-level attribution แล้ว ส่วนช่องว่างสำคัญคือ `STATE_OR_LANDING_EVIDENCE` 9/20 ซึ่งต้องเห็น operands ของ `returnedToBaseline`, `descendedFromPeak`, `startedNextRise`, timeout และ phase transition ก่อนตัดสินว่าเป็น Landing failure, prolonged AIR state หรือ pulse matching gap

## Completion criteria

T-742 เสร็จเมื่อ:

- ตรวจทั้ง 5 sessions และยืนยัน Actual/App mapping
- ระบุตำแหน่ง missed cycles ทุกจุดที่วิดีโอรองรับ หรือทำเครื่องหมาย `INSUFFICIENT_EVIDENCE`
- สรุปจำนวน miss ตาม classification โดยไม่รวม accepted cycle ซ้ำ
- ระบุว่ามี repeated pattern ข้าม same-clothing และ changed-clothing sessions หรือไม่
- ตัดสินหนึ่งทางเลือกถัดไป:
  1. หลักฐานพอออกแบบ bounded passive diagnostic
  2. หลักฐานพอเสนอ bounded detector candidate พร้อม safety controls
  3. หลักฐานยังไม่พอและต้องหยุดโดยไม่แก้ detector

## Stop rules

- ไม่ทำการกระโดดเพิ่มใน T-742
- ไม่ปรับ threshold จากภาพท่าทางหรือ rejection label เพียงอย่างเดียว
- ไม่ใช้ Screen Recording เป็น deterministic `PoseFrame` replay
- หาก overlay ไม่สามารถจับคู่ event ต่อ cycle ได้ ให้บันทึก evidence gap แทนการคาดเดา

## Result form

```text
# T-742 Existing-Video Missed-Cycle Evidence Audit

Sessions reviewed:
Total physical cycles:
Total app counts:
Total misses:

VISIBLE_GATE_REJECTION:
STATE_OR_LANDING_EVIDENCE:
PROPOSAL_ABSENT_OR_UNCERTAIN:
INSUFFICIENT_EVIDENCE:

Repeated pattern:
Clothing-specific pattern:
Performance/stability anomaly:
Evidence limitation:
Recommended next step:
Result: Complete / Inconclusive / Blocked
```
