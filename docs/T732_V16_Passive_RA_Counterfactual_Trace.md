# T-732 V16 — Passive RA Counterfactual Trace

สถานะ: Prepared — Pure-Kotlin 80/80 Pass; รอ Windows tests, build และ device smoke

## เป้าหมาย

แยก rejected proposals ที่มี `RA` เป็น:

1. `RA-only`: gate อื่นทั้งหมดผ่าน และอาจกู้คืนได้ด้วยการเปลี่ยน rescue ankle floor เพียงตัวเดียว
2. `RA + other`: แม้ลด RA แล้ว proposal ยังถูกปฏิเสธด้วย bilateral, sync, hip หรือ ratio gate

V16 คำนวณจาก exact-paired operands ที่ V15 เก็บอยู่แล้วเท่านั้น ไม่สร้าง shadow detector และไม่ส่งค่ากลับไปยัง production decision path.

## Overlay

Summary:

```text
CF RA-B<n> ONLY<n> ONE<floor> ALL<floor>
```

- `RA-B`: จำนวน rejected WINDOW proposals ที่ติด RA
- `ONLY`: จำนวนที่ติด RA เพียง gate เดียว
- `ONE`: floor สูงสุดที่ต่ำพอจะช่วย RA-only candidate อย่างน้อยหนึ่งรายการ
- `ALL`: floor ต่ำสุดที่ต้องใช้เพื่อช่วย RA-only candidates ทั้งหมดในรอบ
- `---`: ไม่มี RA-only candidate

Rejected row ที่ติด RA:

```text
CF RA<=<operand> OTH[<other blockers>]
```

- `RA<=operand` คือเงื่อนไข counterfactual ที่ทำให้ RA ผ่านสำหรับ proposal นั้น
- `OTH[-]` หมายถึง RA-only
- Gate ใน `OTH[...]` ยังทำให้ proposal ถูกปฏิเสธแม้ RA ผ่าน

ค่า `ONE/ALL` เป็น diagnostic bounds จากข้อมูลรอบนั้น ไม่ใช่ recommendation, production threshold หรือหลักฐาน safety.

## Isolation

- ไม่แก้ `BasicBounceDetector.kt`
- คง BASE bilateral/rescue `0.010/0.020`
- ไม่เปลี่ยน state machine, Counter, Timer, Result/History หรือ Room
- ไม่เก็บภาพ, landmark coordinates หรือ trace ลง storage
- ใช้เฉพาะ Debug in-memory collector/formatter

## Verification

1. Pure-Kotlin regression 80/80 ผ่าน; รัน Android `testDebugUnitTest` บน Windows
2. `assembleDebug`
3. V16 Smoke 3 jumps: Actual/App 3/3, หลังหยุด 0, Result/History ตรงกัน, `U0`, `OV0`, seal/stability ผ่าน
4. V16 Formal 22 jumps เฉพาะหลัง Smoke ผ่าน
5. หากมี rejected proposals ให้ยืนยัน `CF` เทียบ signed margins; ยังไม่เปลี่ยน production threshold

## Decision gate หลัง Formal

- ถ้า `ONLY0`: RA-only experiment ไม่มีหลักฐานรองรับ
- ถ้า `ONLY>0`: ใช้ `ONE/ALL` ร่วมกับผลหลายรอบเพื่อเลือก candidate เดียว แล้วต้องผ่าน matched/active safety controls ก่อนพิจารณา production
- รายการ `RA + other` ต้องวิเคราะห์ gate อื่นแยกต่างหาก ห้ามนับเป็น expected recovery ของ RA-only experiment
