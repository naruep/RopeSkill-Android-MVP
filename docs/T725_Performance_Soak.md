# T-725 Passive Performance Soak — Music OFF

## เป้าหมาย

ตรวจ FPS, latency, frame backlog, CPU, memory, battery และอุณหภูมิบน Samsung Galaxy S23 Ultra เป็นเวลา 5 นาที โดยใช้ detector baseline `70e35e7` และไม่เปลี่ยน detector behavior

## เงื่อนไข

- Music OFF
- แสงปกติ
- ปิด Power Saving
- ความสว่างหน้าจอคงที่ประมาณ 50%
- เปิด Basic Bounce จนถึงสถานะ `RUNNING`
- ให้เห็นร่างกายเต็มตัวและไม่กระโดดโดยตั้งใจตลอด 5 นาที
- เชื่อมต่อ USB debugging กับคอมพิวเตอร์เพียงเครื่องเดียว
- เนื่องจาก USB debugging อาจชาร์จโทรศัพท์ ค่า Battery percent เป็นข้อมูลประกอบ ไม่ใช้ตัดสิน Pass/Fail

## วิธีทดสอบ

1. เปิดแอปและเข้า Basic Bounce จนแสดง `RUNNING`
2. วางโทรศัพท์และยืนเต็มตัวในเฟรม
3. เปิด PowerShell ที่โฟลเดอร์โปรเจกต์
4. รันคำสั่งต่อไปนี้:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\Capture-T725Performance.ps1
```

5. อย่าแตะโทรศัพท์หรือกระโดดระหว่างที่สคริปต์ทำงาน สคริปต์จะเก็บข้อมูลและภาพหน้าจอเมื่อเริ่ม จากนั้นทุก 1 นาทีจนถึงนาทีที่ 5
6. เมื่อ PowerShell แสดง `T-725 capture completed` ให้กด Pause บนโทรศัพท์ ตรวจว่า Counter ไม่เพิ่ม แล้วกด Finish เพื่อตรวจ Result/History
7. ส่งไฟล์ ZIP `RopeSkill-T725-<เวลา>.zip` จาก Downloads พร้อมแบบฟอร์มด้านล่าง

## เกณฑ์ผ่าน

- Counter และ false count หลังหยุดเป็น 0
- ไม่ crash/freeze, AIRBORNE ค้าง หรือ preview กระตุก
- FPS ในแสงปกติไม่ลดต่อเนื่องและควรอยู่ประมาณ 28–30
- average latency ไม่เกิน 40ms และ peak latency ไม่เกิน 150ms
- ตาม ADR-029 ผลต่าง `ΔIN − ΔOUT` ต่อ checkpoint ไม่เกิน 5, cumulative skip rate ไม่เกิน 0.10% และไม่มีแนวโน้ม backlog จาก FPS/latency
- Battery temperature เพิ่มไม่เกิน 3.0°C และเครื่องไม่รู้สึกร้อนผิดปกติ
- Memory/CPU ไม่มีแนวโน้มเพิ่มต่อเนื่องผิดปกติระหว่าง checkpoints

## แบบส่งผล

```text
# T-725 Passive Performance Soak

Detector baseline: 70e35e7
Duration: 5 minutes
Music OFF: Yes
Lighting: Normal

App displayed jumps:
Count increase after stopping:
Result/History:
Crash/freeze:
AIRBORNE freeze:
Preview stuttering:
Phone heat at end: Cool / Slightly warm / Hot
ZIP filename:
```

## ผลทดสอบ 29 กรกฎาคม 2026

- **Device:** Samsung Galaxy S23 Ultra (`SM-S918B`), Android 16 / SDK 36
- **Detector baseline:** `70e35e7`
- **Evidence:** `RopeSkill-T725-20260729-111223-recovered.zip`
- **Capture window:** 11:12:27–11:17:43 หรือ 316.4 วินาที

| Checkpoint | Training timer | FPS | LAT avg/peak | IN / OUT | SKIP | Total PSS (KB) | Battery °C |
|---|---:|---:|---:|---:|---:|---:|---:|
| minute-0 | 01:26 | 29.7 | 28 / 59ms | 2,993 / 2,992 | 0 | 364,633 | 32.1 |
| minute-1 | 02:30 | 30.3 | 29 / 59ms | 4,912 / 4,912 | 0 | 328,271 | 32.7 |
| minute-2 | 03:33 | 30.0 | 30 / 68ms | 6,805 / 6,802 | 2 | 349,784 | 33.2 |
| minute-3 | 04:36 | 30.3 | 31 / 72ms | 8,697 / 8,692 | 4 | 364,310 | 33.7 |
| minute-4 | 05:40 | 30.0 | 31 / 72ms | 10,585 / 10,578 | 6 | 369,559 | 34.1 |
| minute-5 | 06:43 | 30.2 | 32 / 72ms | 12,506 / 12,497 | 8 | 360,800 | 34.4 |

ผลต่าง input/output ใหม่ต่อช่วง 1 นาทีสูงสุด 3 เฟรม และ cumulative skip rate คือ `8 / 12,506 = 0.064%`; FPS/latency ไม่เสื่อมต่อเนื่อง App CPU จาก `top` อยู่ 138–181% และจบ 176%; PSS แกว่งในช่วง 328,271–369,559KB และจบต่ำกว่าจุดสูงสุด; Thermal Status เป็น 0 ตลอด อุณหภูมิแบตเพิ่ม 2.3°C และผู้ใช้รายงานอุณหภูมิเครื่องปกติ

Counter และ false count หลังหยุดเป็น 0, Result/History ผ่าน และไม่พบ crash/freeze, AIRBORNE ค้าง หรือ preview กระตุก การสร้าง ZIP ครั้งแรกเกิด file lock หลังเก็บ minute-0 ถึง minute-5 ครบแล้ว; ZIP ที่กู้คืนมีหลักฐานครบและใช้ตัดสินผลได้

**ผลสรุป:** Pass โดยคง KI-015 เป็น Monitoring; ไม่เปลี่ยน `BasicBounceDetector`
