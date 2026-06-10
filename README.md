# FocusFilter

> On-device AI notification filter for Android. Blocks spam. Keeps OTPs, bank alerts, and emergencies. 100% local — your data never leaves your phone.

![Android](https://img.shields.io/badge/Android-26%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-purple?logo=kotlin)
![License](https://img.shields.io/badge/License-GPL%20v3-blue)
![No Tracking](https://img.shields.io/badge/Tracking-None-brightgreen)

---

## What It Does

FocusFilter intercepts every notification and runs it through a 12-step AI pipeline before deciding whether to allow, hold, or block it. All processing is on-device using a 4.3MB quantized BERT model.

**Always allowed:** OTPs, bank transactions, payment alerts, emergency calls, security alerts

**Filtered:** Promo offers, flash sales, spam, irrelevant marketing

**Held for review:** Ambiguous notifications — reviewed in the Filtered Inbox

---

## How It Works — The Pipeline

Every notification passes through up to 12 steps:

1. Focus mode active check
2. System app guard
3. User trusted apps check
4. Finance safelist (TNG, GrabPay, GoPay, GCash, MAE, Boost + global banks)
5. BERT AI inference (bert-tiny, INT8 quantized, ONNX Runtime)
6. Keyword classifier (80+ seeds across OTP, bank, delivery, security)
7. Critical label safety net — OTP/payment always allowed regardless of BERT score
8. Cross-validation — BERT and keyword classifier must agree before blocking
9. User-defined rule engine
10. Sender reputation (per-app allow/block history)
11. Mode default action
12. Log and deliver with full reason string

---

## Features

- **4 Focus Modes** — Gaming, Work, Sleep, Custom
- **On-Device BERT** — bert-tiny-finetuned-sms-spam-detection, INT8 quantized, 4.3MB
- **Filtered Inbox** — review held notifications anytime
- **Activity Logs** — full audit trail with classifier reason strings
- **Trusted Apps** — user-defined apps that always pass through
- **Keyword Safelist** — 80+ default seeds, fully editable
- **Custom Rules** — per-app and per-keyword rules
- **SEA Market Coverage** — TNG eWallet, GrabPay, GoPay, GCash, MoMo, ZaloPay, MAE, Boost
- **Auto-purge** — logs deleted after 14 days (non-permanent entries)
- **Zero Internet Permission** — architecturally impossible to transmit data

---

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Kotlin 1.9.x
- Java 17
- Gradle 8.4
- Min SDK: Android 8.0 (API 26)

---

## Build Instructions

### 1. Clone

```bash
git clone https://github.com/yourusername/FocusFilter.git
cd FocusFilter
```

### 2. Open in Android Studio

```
File → Open → select the FocusFilter/ directory
```

### 3. Sync Gradle

Android Studio will auto-sync. If not:
```
File → Sync Project with Gradle Files
```

### 4. Build Debug APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### 5. Command Line

```bash
./gradlew assembleDebug    # debug APK
./gradlew assembleRelease  # release APK (needs signing config)
./gradlew test             # run unit tests
```

---

## First-Time Setup on Device

1. Install the APK (enable "Install from unknown sources")
2. Open FocusFilter
3. Complete the legal consent and trusted apps onboarding
4. Grant **Notification Access** when prompted
5. Select a focus mode and toggle ON

---

## Project Structure

```
app/src/main/
├── kotlin/com/focusfilter/
│   ├── FocusFilterApplication.kt        # App init, rules cache, DI wiring
│   ├── MainActivity.kt                  # Single activity, bottom nav host
│   ├── adapter/                         # RecyclerView adapters (5)
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt           # Room DB, v9, all migrations
│   │   │   ├── dao/                     # 4 DAOs
│   │   │   ├── entities/                # 4 Room entities
│   │   │   └── converters/              # Type converters
│   │   ├── keyword/                     # KeywordSafelist entity, DAO, manager
│   │   └── repository/                  # 3 repositories
│   ├── model/                           # FocusModeType, NotificationAction enums
│   ├── service/
│   │   ├── FocusFilterNotificationService.kt  # 12-step pipeline (core)
│   │   ├── BertSpamDetector.kt                # ONNX Runtime inference
│   │   ├── BertTokenizer.kt                   # BERT tokenizer
│   │   ├── NotificationClassifier.kt          # Classifier interface
│   │   ├── SimpleClassifier.kt                # Keyword fallback classifier
│   │   ├── RuleEngine.kt                      # User rule matching
│   │   ├── SafelistManager.kt                 # Tiered SEA/global safelist
│   │   └── BootReceiver.kt                    # Restarts service on boot
│   ├── ui/                              # 12 Fragments
│   ├── util/
│   │   └── PreferencesManager.kt        # SharedPreferences wrapper
│   └── viewmodel/                       # 8 ViewModels
├── assets/
│   ├── model_int8.onnx                  # BERT model (4.3MB, INT8 quantized)
│   ├── vocab.txt                        # 30,522 token vocabulary
│   ├── tokenizer.json                   # Tokenizer config
│   └── config.json                      # Model architecture config
└── res/
    ├── layout/                          # 20 XML layouts
    ├── drawable/                        # Vector icons + backgrounds
    ├── navigation/nav_graph.xml         # Navigation graph
    └── values/                          # colors, themes, strings, dimens
```

---

## Architecture

- **MVVM** — ViewModel + LiveData + Repository
- **Room** — local SQLite with coroutines Flow, 9 migrations
- **Navigation Component** — single-activity, fragment-based
- **ONNX Runtime Mobile** — on-device BERT inference
- **Material You (Material3)** — dark theme, `#0D0F1A` base

---

## The AI Model

| Property | Value |
|---|---|
| Base model | bert-tiny-finetuned-sms-spam-detection (mrm8488) |
| Format | ONNX, INT8 quantized |
| Size | 4.3MB |
| Architecture | 2-layer BERT, 128 hidden size, 2 attention heads |
| Vocabulary | 30,522 tokens |
| Runtime | ONNX Runtime Mobile |
| Internet required | No |

---

## Privacy

- **No INTERNET permission** — verifiable in `AndroidManifest.xml`
- No analytics, no crash reporting, no ads, no tracking
- No third-party data SDKs
- Notification content processed in memory only — never stored in full
- Logs auto-deleted after 14 days
- All data in local Room SQLite — uninstall removes everything

---

## Why Not on Google Play?

- Developer is under 18 (Google requires 18+)
- $25 registration fee
- Google Play SDKs conflict with zero-tracking architecture

Planned for F-Droid once requirements are met.

---

## Contributing

PRs welcome. Please:
- Keep the zero-internet-permission constraint
- Don't add analytics or tracking of any kind
- Test on API 26+ before submitting

---

## License

GPL-3.0 — see `LICENSE` file.

---

*Built by a teenager on a phone.*
