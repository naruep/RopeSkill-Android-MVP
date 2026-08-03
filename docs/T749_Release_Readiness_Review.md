# T-749 — Release Readiness Review

## Objective

ทบทวนความพร้อมหลัง T-747 production signing/package, T-748 production-signed clean-device smoke และการยืนยัน secure upload-key backup โดยไม่เผยแพร่แอป, ไม่แก้ production runtime และไม่เปลี่ยน `BasicBounceDetector`

## Baseline

- Repository: `naruep/RopeSkill-Android-MVP`
- Branch: `fix/adaptive-icon-safe-zone`
- GitHub checkpoint: `ba399c0`
- Review date: 31 July 2026
- Distribution candidate: Google Play

## Confirmed release gates

- Package identity: `com.ropeskill.app`
- Version: `versionCode 1`, `versionName 0.1.0`
- `compileSdk 36` / `targetSdk 36`: ผ่านข้อกำหนด Android 16 / API 36 สำหรับ new apps และ updates ตั้งแต่ 31 August 2026
- Production APK/AAB: signed และผ่าน signature, certificate, package และ SHA-256 verification ใน T-747
- Production identity: ไม่พบ `DEBUGGABLE` ใน T-748
- Production behavior smoke: Home, Training, Camera permission, Pause/Resume/Finish, Result และ History ผ่านบน Android 16 / API 36 clean emulator
- Permission boundary: ขอ `CAMERA` และ `VIBRATE`; ไม่มี `INTERNET`, storage, location หรือ advertising permission
- Data boundary: camera frames และ pose landmarks ประมวลผลบนอุปกรณ์; ไม่บันทึกหรืออัปโหลด
- Local backup boundary: `allowBackup=false` และ storage domains ถูก exclude ตาม T-746
- Signing secrets: ไม่ถูก Git track และ upload key/recovery information ได้รับการสำรองอย่างปลอดภัยตามคำยืนยันของผู้ใช้

## Blocking gaps

### KI-025 — 16 KB page-size compatibility ยังไม่ยืนยัน

Google Play กำหนดให้ new apps และ updates ที่ target Android 15+ รองรับ 16 KB page sizes ตั้งแต่ 1 November 2025. RopeSkill ใช้ MediaPipe ซึ่งมี native libraries แต่ T-747/T-748 ตรวจบน Android 16 emulator แบบปกติและยังไม่มี:

- Production AAB/APK native-library alignment report
- 16 KB page-size emulator/device smoke
- Play Console pre-launch confirmation สำหรับ requirement นี้

จึงห้ามสรุปว่ารองรับหรือไม่รองรับจนกว่าจะตรวจ artifact จริงใน T-750

### KI-026 — Public privacy policy และ disclosure ยังไม่ครบ

แอปมี in-app dialog สำหรับ camera/pose data แต่ยังไม่มี active public privacy-policy URL และข้อความปัจจุบันยังไม่ครอบคลุม local data ทั้งหมด ได้แก่:

- nickname ใน DataStore
- Training History ใน Room
- training settings และ persisted music URI permission
- retention, deletion และ backup behavior
- contact channel สำหรับคำถามด้าน privacy

RopeSkill ขอใช้กล้องและอยู่ในหมวด exercise/fitness จึงต้องมี privacy policy ที่ตรงกับ behavior จริง ทั้งใน store listing และในแอปก่อนส่ง review

## Play Console submission work

รายการต่อไปนี้ยังไม่มีหลักฐานว่าเสร็จ และต้องจัดทำใน T-751/T-752:

- Public privacy-policy URL และ Data safety declaration
- Health apps declaration สำหรับ exercise/fitness scope
- Ads declaration, App access, Target audience และ Content rating
- Store category, developer contact และ support contact
- App name, short description, full description และ release notes
- Play Store icon, feature graphic และ phone screenshots
- Play App Signing / upload-key enrollment decision
- Country/region, pricing และ rollout decision
- Closed-test requirement ตามชนิดและวันที่สร้าง developer account

## Verification limitation

พยายามรัน `testDebugUnitTest`, `lintRelease` และ `assembleRelease` ใน Codex workspace แต่ environment ไม่มี Gradle 9.3 distribution cache และ network policy ไม่อนุญาตให้ดาวน์โหลด จึงไม่เกิด Gradle test result ใหม่. ข้อนี้ไม่ใช่ app failure; T-749 อ้างผล Windows production packaging ที่ผ่านใน T-747 และไม่แทนที่หลักฐานนั้น

## Decision

```text
T-749 review: COMPLETE
Technical MVP checkpoint: ACCEPTED
Target API 36: PASS
Production signing/package: PASS
Production clean-device smoke: PASS
Secure upload-key backup: CONFIRMED
16 KB page-size compatibility: BLOCKED — KI-025
Public privacy policy/disclosure: BLOCKED — KI-026
Play Console/store package: NOT PREPARED
Public release readiness: HOLD
```

T-749 เป็น review checkpoint ไม่ใช่ publication approval. ณ เวลาที่ review เสร็จ ห้ามอัปโหลด Production AAB ไป Production track หรือเผยแพร่ต่อสาธารณะจนกว่า KI-025, KI-026 และ Play Console submission checklist จะปิดครบ

Post-review update 31 July 2026: T-750 ผ่าน Production artifact audit และ automated/manual smoke บน Emulator ที่ยืนยัน `PAGE_SIZE=16384` แล้ว จึงปิด KI-025. Public release ยังคง Hold เพราะ KI-026 และ Play Console/store preparation ยังไม่เสร็จ

## Next tasks

1. **T-750 — Production 16 KB Page-Size Compatibility Audit — PASS**
2. **T-751 — Privacy Policy and Play Console Data Declarations**
3. **T-752 — Store Listing Package and Test-Track Plan**

## Official references

- Target API requirements: https://developer.android.com/google/play/requirements/target-sdk
- 16 KB page-size support: https://developer.android.com/guide/practices/page-sizes
- Prepare app for review: https://support.google.com/googleplay/android-developer/answer/9859455
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Health app declarations: https://support.google.com/googleplay/android-developer/answer/13996367
- Preview assets: https://support.google.com/googleplay/android-developer/answer/9866151
- New personal-account testing requirements: https://support.google.com/googleplay/android-developer/answer/14151465
