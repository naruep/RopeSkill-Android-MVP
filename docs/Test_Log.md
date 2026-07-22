# RopeSkill Test Log

อัปเดตล่าสุด: 22 กรกฎาคม 2026

## สถานะปัจจุบัน

ยังไม่มีผลทดสอบที่ได้รับการยืนยันบน Emulator หรือโทรศัพท์จริง

สร้าง Android sample project แล้ว แต่ workspace ของ Codex ไม่มี Android SDK และ Gradle จึงยังไม่ได้รัน `assembleDebug` การ Build และ Run ต้องยืนยันผ่าน Android Studio

## Test Environment

| รายการ | ค่า |
|---|---|
| Device model | Samsung Galaxy S23 Ultra (ยืนยันเป็นเครื่องทดสอบหลัก) |
| Android version | รอกรอก |
| CPU / RAM | รอกรอก |
| App version / commit | รอกรอก |
| Build type | `debug` (สมมติฐาน) |
| Camera | รอกรอก: front / back |
| Lighting | รอกรอก |
| Distance from camera | รอกรอก |

## Build and Run Tests

| Test ID | วันที่ | ขั้นตอน | ผลที่คาดหวัง | ผลจริง | สถานะ |
|---|---|---|---|---|---|
| T-001 | รอทดสอบ | Build `debug` | Build สำเร็จ | รอกรอก | Not Run |
| T-002 | รอทดสอบ | ติดตั้งและเปิดแอป | แอปเปิดโดยไม่ crash | รอกรอก | Not Run |

## Feature Tests

| Test ID | Feature | Scenario | Expected | Actual | Status |
|---|---|---|---|---|---|
| T-101 | Counter | กดเพิ่ม Counter | ค่าเพิ่มหนึ่งครั้ง | รอกรอก | Not Run |
| T-102 | Timer | Start, Pause, Resume, Finish | เวลาเปลี่ยนตามสถานะ | รอกรอก | Not Run |
| T-201 | Navigation | Home → Training → Result → Home | ไปแต่ละหน้าถูกต้อง | รอกรอก | Not Run |
| T-301 | Camera permission | Allow | แสดง preview | รอกรอก | Not Run |
| T-302 | Camera permission | Deny | แสดงคำอธิบาย/ทางเลือก | รอกรอก | Not Run |
| T-401 | Pose | ยืนให้เห็นร่างกาย | landmarks ติดตามร่างกาย | รอกรอก | Not Run |
| T-501 | Basic Bounce | กระโดดตามจำนวนที่ทราบ | นับใกล้เคียงจำนวนจริง | รอกรอก | Not Run |
| T-601 | Storage | จบ Session และเปิดแอปใหม่ | ผลยังอยู่ | รอกรอก | Not Run |

## Jump Detection Accuracy Template

| Run | Ground truth jumps | Detected jumps | Missed | False positives | Accuracy note |
|---|---:|---:|---:|---:|---|
| 1 |  |  |  |  |  |
| 2 |  |  |  |  |  |
| 3 |  |  |  |  |  |

สูตรที่ใช้รายงานเบื้องต้น:

```text
Count Error (%) = abs(Detected - Ground Truth) / Ground Truth × 100
```

สูตรนี้ยังไม่เพียงพอสำหรับแยก missed jumps กับ false positives จึงต้องบันทึกทั้งสองค่าด้วยเมื่อวิเคราะห์วิดีโอหรือมีผู้ช่วยนับ

## Performance Template

| Scenario | Preview FPS | Inference FPS | Median latency | CPU | Memory | Battery/Heat | Notes |
|---|---:|---:|---:|---:|---:|---|---|
| Idle preview |  |  |  |  |  |  |  |
| Pose active 1 min |  |  |  |  |  |  |  |
| Training 10 min |  |  |  |  |  |  |  |

## Test Session Template

```text
### Test Session YYYY-MM-DD-NN
- Tester:
- Device / Android:
- App build / commit:
- Environment:
- Preconditions:
- Steps:
- Expected:
- Actual:
- Logs/screenshots:
- Privacy note: ห้ามแนบภาพบุคคลโดยไม่ได้รับความยินยอม
- Result: Pass | Fail | Blocked
- Related issue:
```
