# T-751 — Privacy Policy and Play Console Data Declarations

## Objective

จัดทำ Privacy Policy และคำตอบ Play Console จาก behavior ของ Production candidate จริง พร้อมเพิ่ม disclosure ก่อนขอ Camera permission และทางเข้า Privacy Policy ในแอป โดยไม่เปลี่ยน detector, Counter, Room schema หรือ release signing

## Baseline

- Repository: `naruep/RopeSkill-Android-MVP`
- Branch: `fix/adaptive-icon-safe-zone`
- Starting checkpoint: `7cacbac`
- Package: `com.ropeskill.app`
- Version: `0.1.0` (`versionCode 1`)
- Audit date: 31 July 2026
- Developer/publisher: Naruep Jukping
- Privacy contact: `naruep.j@gmail.com`
- Intended public URL: `https://naruep.github.io/RopeSkill-Android-MVP/privacy-policy.html`

## Verified data-flow inventory

| Data or access | Source and purpose | Storage | Off-device collection or sharing |
|---|---|---|---|
| Live camera frames | CameraX preview and MediaPipe pose estimation during Training | In-memory processing only; no image/video retention | None |
| Pose landmarks | Basic Bounce estimation and count decisions | In-memory processing only; not stored in History | None |
| Optional Speed 30 screen recording | User explicitly enables `Record this workout` before `START WORKOUT`; visible RopeSkill UI includes camera preview and diagnostics | MP4 in shared `Movies/RopeSkill`; no microphone/internal audio; user deletes through Gallery/Files | None by RopeSkill; the user may share the file themselves from Gallery or a video player |
| Optional nickname | User entry for local Home greeting | DataStore in private app storage | None |
| Training History | Exercise type, jump count, duration, start/completion timestamps | Room in private app storage | None |
| Training preferences | Sound, vibration, countdown, units, theme, music state/volume | DataStore in private app storage | None |
| Selected music reference | Android document URI, display name, and persistable read grant for a user-selected audio file | DataStore/Android URI grant; audio file remains with its provider | None |
| Vibration | Local training feedback | Not retained | None |

Production Manifest requests `CAMERA`, `VIBRATE`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_MEDIA_PROJECTION`. It does not request `INTERNET`, broad storage, location, microphone, advertising ID, activity recognition, body sensors, contacts, or account permissions. MediaProjection consent is requested from Android for each opt-in recording session. The app contains no ads, analytics, account, backend, cloud sync, or remote-processing SDK.

`android:allowBackup="false"` is set and both legacy and Android 12+ rules exclude every supported storage domain from cloud backup and device transfer.

## Public Privacy Policy

Publication asset: `docs/privacy-policy.html`

The page covers:

- RopeSkill and publisher identity
- privacy contact
- Camera permission and on-device pose processing
- optional local-only MediaProjection screen recording, storage, sharing, and deletion
- local nickname, History, settings, and selected-music URI access
- no off-device collection, sharing, ads, analytics, accounts, or cloud sync
- retention, per-feature deletion, uninstall deletion, and backup boundary
- local security measures
- health/fitness and non-medical disclaimer
- effective date and change policy

Google Play requires an active, public, non-geofenced, non-editable web URL rather than a PDF. The intended GitHub Pages URL must be opened in a signed-out browser and verified before it is entered in Play Console.

## Data safety form — prepared answers

Complete `Policy and programs > App content > Data safety` using the final Production AAB.

| Play Console question | Prepared answer | Basis |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | Camera/pose and all retained data stay on the device; no data is transmitted to the developer or a third party |
| Is all of the user data collected by your app encrypted in transit? | **Not applicable** | RopeSkill does not collect or transmit user data |
| Do you provide a way for users to request that their data is deleted? | **Not applicable to server data / no account** | There is no account or server-held data; local History/settings/music access can be deleted in-app and all app data is removed by uninstall |
| Does the app support account creation? | **No** | No account or sign-in feature exists |

Google's Data safety definition does not treat data that is accessed and processed only on the user's device as “collected.” The Privacy Policy still discloses local access and storage because the broader User Data and Health Apps policies require transparent disclosure.

Do not select any Data safety data category for the current build. In particular, do not declare Photos/Videos, Health and Fitness, Personal Info, App Activity, Device IDs, Audio, or Files/Documents as collected because none leaves the device. Re-audit the final AAB and all SDKs before every declaration update.

## Health apps declaration — prepared answers

Complete `Policy and programs > App content > Health apps`.

| Play Console field | Prepared answer |
|---|---|
| Does the app have health-related features? | **Yes** |
| Health category | **Activity and Fitness** only |
| Activity and Fitness scope | Camera-assisted Basic Bounce jump-rope practice; on-device pose estimation counts movements and records local session totals |
| Medical device | **No** |
| Diagnosis, treatment, cure, prevention, clinical decision support | **No** |
| Human-subject research | **No** |
| Health Connect | **Not used** |
| External health hardware | **Not used** |
| Health data sale/sharing | **None** |

Suggested explanation:

> RopeSkill is a general exercise and fitness app for jump-rope practice. During Training it uses the camera for a live preview and on-device pose estimation to count movements. Camera frames and pose landmarks are not retained or transmitted. A user may explicitly choose to save a Speed 30 recording of the visible RopeSkill screen locally; it is not uploaded automatically. RopeSkill is not a medical device and does not provide diagnosis or treatment.

Select no other health category unless the production feature set changes. The Store listing prepared in T-752 must include the non-medical disclaimer and device/camera compatibility statement required for the published fitness scope.

## In-app disclosure and consent

Immediately before the Android Camera permission prompt, RopeSkill now states:

> RopeSkill uses the camera during Training to show your preview and estimate body pose on this device. Camera frames and pose landmarks are not retained or uploaded. If you enable Record this workout before START WORKOUT, the visible RopeSkill screen is saved as a video in Movies/RopeSkill without microphone audio. RopeSkill does not upload or share it; you can share it yourself from Gallery or your video player.

The affirmative `Allow Camera` action then launches the Android runtime permission request. Settings includes a local summary covering all retained data and a `VIEW FULL POLICY` action to the public URL.

## Validation gates

T-751 is not complete until all checks pass:

- [ ] Unit tests, lint, Debug build, and Release build pass on Windows
- [ ] Camera disclosure appears immediately before the system Camera permission request
- [ ] Denying Camera permission does not start camera processing or crash
- [ ] Settings privacy summary covers camera/pose, local data, deletion, backup, developer, and contact
- [ ] `VIEW FULL POLICY` opens the intended HTTPS page
- [ ] Public URL loads while signed out and is not a PDF or editable document
- [ ] Play Console Data safety answers are entered and saved as prepared
- [ ] Health apps declaration selects only `Activity and Fitness` and is saved
- [ ] Final Production AAB and dependency/permission inventory still match this document

## Current decision

```text
Privacy/data-flow audit: COMPLETE
Policy and declarations: PREPARED
In-app disclosure/link: IMPLEMENTED — verification pending
Public policy URL: PUBLICATION AND SIGNED-OUT CHECK PENDING
Play Console forms: ENTRY PENDING
T-751: IN PROGRESS
KI-026: OPEN
Public release: HOLD
```

## Official references

- Google Play Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play User Data and Privacy Policy requirements: https://support.google.com/googleplay/android-developer/answer/17190352
- Health Content and Services policy: https://support.google.com/googleplay/android-developer/answer/16679511
- Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Health app categories and Camera permission scope: https://support.google.com/googleplay/android-developer/answer/13996367
- Android data-use declaration guidance: https://developer.android.com/privacy-and-security/declare-data-use
