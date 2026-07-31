# T-742 — Existing-Video Missed-Cycle Evidence Audit

สถานะ: Prepared — desktop review only; ไม่ต้องทดสอบบนอุปกรณ์เพิ่ม

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

## Evidence table

| Session | Physical cycle | Video time | Counter before → after | Visible state/reason | Feet/pose visibility | Cadence note | Classification | Confidence |
|---|---:|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |  |

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
