# FocusFilter

A premium Android notification filtering app. 100% local-first, offline, and private — no data ever leaves your device.

## Features

- **Notification Listener** — intercepts notifications in real-time using `NotificationListenerService`
- **Rule-based Filtering Engine** — VIP contacts, allowed apps, keyword rules, custom rules
- **4 Focus Modes** — Gaming, Work, Sleep, Custom
- **Filtered Inbox** — view, restore, or delete held/blocked notifications
- **DND Integration** — auto-toggles Do Not Disturb per active mode
- **Local Classifier** — keyword-based on-device classification (OTP, payment, spam, social, etc.)

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Kotlin 1.9.x
- Java 17
- Gradle 8.4

## Build Instructions

### 1. Open in Android Studio

```
File → Open → select the FocusFilter/ directory
```

### 2. Sync Gradle

Android Studio will auto-sync. If not:
```
File → Sync Project with Gradle Files
```

### 3. Build Debug APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Build Release APK (sideloadable)

```
Build → Generate Signed Bundle / APK → APK → create/use a keystore → Release
```

Output: `app/build/outputs/apk/release/app-release.apk`

### 5. Install via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Command Line Build

```bash
cd FocusFilter
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (needs signing config)
```

## First-Time Setup on Device

1. Install the APK
2. Open **FocusFilter**
3. Go to **Settings** tab
4. Tap **Grant** next to "Notification Access" → enable FocusFilter in system settings
5. Tap **Grant** next to "Do Not Disturb Access" → enable in system settings
6. Return to the **Home** tab
7. Select a focus mode (Gaming, Work, Sleep, or Custom)
8. Toggle the Focus switch ON

## Project Structure

```
app/src/main/
├── kotlin/com/focusfilter/
│   ├── FocusFilterApplication.kt       # App entry, dependency wiring
│   ├── MainActivity.kt                 # Single activity, bottom nav host
│   ├── adapter/                        # RecyclerView adapters
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt          # Room database (seeded with defaults)
│   │   │   ├── dao/                    # Data access objects
│   │   │   └── entities/               # Room entities
│   │   └── repository/                 # Repository layer
│   ├── model/                          # FocusModeType, NotificationAction enums
│   ├── service/
│   │   ├── FocusFilterNotificationService.kt  # NotificationListenerService
│   │   ├── NotificationClassifier.kt          # Classifier interface
│   │   ├── SimpleClassifier.kt                # Keyword-based implementation
│   │   └── BootReceiver.kt                    # BroadcastReceiver for boot
│   ├── ui/
│   │   ├── home/HomeFragment.kt        # Dashboard + mode selector
│   │   ├── inbox/InboxFragment.kt      # Filtered notification inbox
│   │   ├── rules/RulesFragment.kt      # Rule management
│   │   ├── logs/LogsFragment.kt        # Activity log
│   │   └── settings/SettingsFragment.kt
│   ├── util/
│   │   ├── DndManager.kt               # Do Not Disturb control
│   │   └── PreferencesManager.kt       # SharedPreferences wrapper
│   └── viewmodel/                      # ViewModels for each screen
├── res/
│   ├── layout/                         # XML layouts (dark Material You)
│   ├── drawable/                       # Vector icons + shape backgrounds
│   ├── values/colors.xml               # Dark palette (#0D0F1A base)
│   ├── values/themes.xml               # Material3 dark theme
│   ├── navigation/nav_graph.xml        # Navigation graph
│   └── menu/bottom_nav_menu.xml        # Bottom navigation items
└── AndroidManifest.xml
```

## Architecture

- **MVVM** — ViewModel + LiveData + Repository pattern
- **Room** — local SQLite with coroutines Flow
- **Navigation Component** — single-activity, fragment-based
- **Material You (Material3)** — dark theme, `#0D0F1A` background

## Privacy

- Zero network permissions
- No analytics, no crash reporting, no ads
- No third-party SDKs
- All data stored in local Room SQLite database on device
- Notification content never leaves the device
