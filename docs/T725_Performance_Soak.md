# T-725 Passive Performance Soak — Music OFF

## เป้าหมาย

ตรวจ FPS, latency, frame backlog, CPU, memory, battery และอุณหภูมิบน Samsung Galaxy S23 Ultra เป็นเวลา 5 นาที โดยใช้ detector baseline `70e35e7` และไม่เปลี่ยน detector behavior

## เงื่อนไข

- Music OFF
- แสงปกติ
- โทรศัพท์ไม่เสียบสายชาร์จและปิด Power Saving
- ความสว่างหน้าจอคงที่ประมาณ 50%
- เปิด Basic Bounce จนถึงสถานะ `RUNNING`
- ให้เห็นร่างกายเต็มตัวและไม่กระโดดโดยตั้งใจตลอด 5 นาที
- เชื่อมต่อ USB debugging กับคอมพิวเตอร์เพียงเครื่องเดียว

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
- `IN/OUT` ต่างกันไม่เกิน 5 และ `SKIP` ไม่เพิ่มต่อเนื่อง
- Battery ลดไม่เกิน 2% ใน 5 นาที
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
