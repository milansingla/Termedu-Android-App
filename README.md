<![CDATA[<div align="center">

# 📚 TERM Edu — Learning Management System

**A native Android wrapper for the [termedu.in](https://app.termedu.in) LMS platform,  
delivering a seamless, app-like learning experience with push notifications,  
media controls, and deep-link support.**

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-brightgreen)
![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue)
![Language](https://img.shields.io/badge/Language-Java-orange?logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FCM%20%2B%20Analytics-FFCA28?logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 🎯 Overview

**TERM Edu** is a production Android application that wraps the TERM Edu web platform (`app.termedu.in`) inside a feature-rich WebView shell. It goes far beyond a simple browser wrapper — the app integrates **Firebase Cloud Messaging**, **UnifiedPush**, **media session controls**, a **JavaScript bridge API**, and native permission handling to provide students with a first-class mobile learning experience.

---

## ✨ Features

### Core
- **WebView-Powered LMS** — Full access to the TERM Edu web platform with native app feel
- **Pull-to-Refresh** — Swipe down to reload content seamlessly
- **Deep Linking** — Handles `http(s)://app.termedu.in` and `termedu.in` URLs natively
- **File Downloads** — Download manager integration for course materials and documents
- **File Uploads** — File chooser support for assignments, profile pictures, and submissions
- **Error Recovery** — Dedicated error screen with retry functionality on connection failures
- **Double-Back-to-Exit** — Prevents accidental app closure

### Push Notifications
- **Firebase Cloud Messaging (FCM)** — Real-time push notifications with automatic token management
- **UnifiedPush Support** — Open-standard push notification protocol as an FCM alternative
- **Smart Routing** — Notification taps route to specific in-app pages (e.g., support)

### Media
- **Media Session Integration** — Lock screen and notification media controls (play/pause/skip)
- **Fullscreen Video** — Immersive video playback with auto-landscape rotation for real video players
- **Background Playback Controls** — Control media without opening the app
- **TTS Guard** — Prevents AI exam text-to-speech from echoing student questions

### Native Integration
- **JavaScript Bridge (`WebToApk`)** — Rich bidirectional communication between web and native
- **Camera & Microphone** — WebRTC-ready with runtime permission handling
- **Geolocation** — Location access for location-based features
- **Session Persistence** — Auth tokens stored via SharedPreferences, injected into localStorage
- **Custom User-Agent** — Strips WebView marker so embedded content (YouTube, etc.) renders fully

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    MainActivity                      │
│  ┌───────────────────────────────────────────────┐  │
│  │              SwipeRefreshLayout                │  │
│  │  ┌─────────────────────────────────────────┐  │  │
│  │  │              WebView                     │  │  │
│  │  │  ┌─────────────────────────────────┐    │  │  │
│  │  │  │  app.termedu.in (LMS Platform)  │    │  │  │
│  │  │  └─────────────────────────────────┘    │  │  │
│  │  └─────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌──────────────┐  ┌──────────────────────────────┐ │
│  │ WebAppInterface│  │  CustomWebViewClient        │ │
│  │ (JS Bridge)   │  │  CustomWebChrome             │ │
│  └──────┬───────┘  └──────────────────────────────┘ │
└─────────┼───────────────────────────────────────────┘
          │
    ┌─────┴──────┐     ┌───────────────────┐
    │  Services  │     │   Receivers        │
    ├────────────┤     ├───────────────────┤
    │ MyFirebase │     │ UnifiedPush       │
    │ Service    │     │ Receiver          │
    ├────────────┤     └───────────────────┘
    │ MediaPlay  │
    │ backService│
    └────────────┘
```

---

## 📁 Project Structure

```
Termedu-LMS-Learning-Management-System/
├── app/
│   ├── src/main/
│   │   ├── java/in/termedu/app/
│   │   │   ├── MainActivity.java          # Main activity with WebView, permissions, FCM
│   │   │   ├── MyFirebaseService.java     # FCM token refresh & message handling
│   │   │   ├── MediaPlaybackService.java  # Lock screen & notification media controls
│   │   │   ├── UnifiedPushReceiver.java   # UnifiedPush endpoint management
│   │   │   └── UserScriptManager.java     # JavaScript shim injection for push & media
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml      # Main layout (SwipeRefresh + WebView)
│   │   │   │   ├── auth_dialog.xml        # HTTP auth prompt dialog
│   │   │   │   └── error.xml              # Connection error screen
│   │   │   ├── anim/                      # Fade-in animations
│   │   │   ├── drawable/                  # Adaptive icon layers
│   │   │   ├── mipmap-*/                  # App launcher icons (all densities)
│   │   │   ├── values/
│   │   │   │   ├── strings.xml            # App strings & error messages
│   │   │   │   ├── themes.xml             # Light theme with white status bar
│   │   │   │   └── colors.xml             # Color definitions
│   │   │   └── xml/                       # Backup & data extraction rules
│   │   ├── assets/                        # ADI registration config
│   │   └── AndroidManifest.xml            # Permissions, activities, services, deep links
│   ├── build.gradle                       # App-level dependencies & signing config
│   └── google-services.json               # Firebase configuration
├── build.gradle                           # Root Gradle config
├── settings.gradle                        # Project settings (JitPack, Google, MavenCentral)
├── gradle.properties                      # JVM arguments
└── README.md                              # This file
```

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Language** | Java 11 |
| **UI Framework** | Android SDK (AppCompat) |
| **WebView** | AndroidX WebKit 1.11.0 |
| **Push (Primary)** | Firebase Cloud Messaging (BOM 33.1.0) |
| **Push (Alt)** | UnifiedPush Connector 2.4.0 |
| **Networking** | Volley 1.2.1 |
| **Media** | AndroidX Media 1.7.0 (MediaSession) |
| **Analytics** | Firebase Analytics |
| **Build System** | Gradle (Version Catalog) |
| **Min SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK** | 36 |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2) or later
- **JDK 11** or later
- **Android SDK** with API Level 36 installed
- A physical device or emulator running **Android 7.0+**

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/Termedu-LMS-Learning-Management-System.git
   cd Termedu-LMS-Learning-Management-System
   ```

2. **Open in Android Studio:**
   - File → Open → Select the project root directory
   - Wait for Gradle sync to complete

3. **Firebase Configuration:**
   - The `google-services.json` is included in the `app/` directory
   - To use your own Firebase project, replace this file with one from the [Firebase Console](https://console.firebase.google.com/)

4. **Build & Run:**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use the **Run** button in Android Studio.

### Signing (Release)

The release signing config is defined in `app/build.gradle`. For production builds:

```bash
./gradlew assembleRelease
```

> **Note:** Update the keystore credentials in `app/build.gradle` before publishing. Never commit keystore passwords to version control in production.

---

## 🔑 Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Load the LMS web platform |
| `ACCESS_NETWORK_STATE` | Detect connectivity for error handling |
| `POST_NOTIFICATIONS` | Display push notifications (Android 13+) |
| `VIBRATE` / `WAKE_LOCK` | Notification alerts |
| `RECEIVE_BOOT_COMPLETED` | Re-register push endpoints after reboot |
| `ACCESS_FINE_LOCATION` | Location-based LMS features |
| `CAMERA` | Photo capture for assignments & exams |
| `RECORD_AUDIO` | Microphone access for audio features |
| `WRITE_EXTERNAL_STORAGE` | File downloads (API ≤ 28 only) |

---

## 🌉 JavaScript Bridge API

The app exposes a `WebToApk` JavaScript interface that the web platform can call:

### Toast Messages
```javascript
WebToApk.showShortToast("Hello!");
WebToApk.showLongToast("This stays longer");
```

### Notifications
```javascript
WebToApk.hasNotificationPermission();        // → boolean
WebToApk.requestNotificationPermission();
WebToApk.getNotificationPermissionState();   // → "granted" | "prompt"
WebToApk.showNotification("Title", "Body");
```

### Authentication
```javascript
WebToApk.saveToken("jwt-session-token");
WebToApk.saveUserId("user-123");  // Also triggers FCM token sync
```

### Sharing
```javascript
WebToApk.share("Check this out", "Description text", "https://termedu.in/course/123");
```

### Media Controls
```javascript
WebToApk.updateMediaMetadata("Song Title", "Artist", "Album", "https://art.url/cover.jpg");
WebToApk.updateMediaPlaybackState("playing");   // "playing" | "paused" | "none"
WebToApk.setMediaActionHandlers(["play", "pause", "nexttrack", "previoustrack"]);
WebToApk.updateMediaPositionState(duration, playbackRate, currentPosition);
```

### Camera & Microphone
```javascript
WebToApk.hasCameraPermission();       // → boolean
WebToApk.hasMicrophonePermission();   // → boolean
WebToApk.requestCameraPermission();
WebToApk.requestMicrophonePermission();
```

### UnifiedPush
```javascript
WebToApk.unifiedPushSubscribe("vapid-public-key");
WebToApk.unifiedPushUnregister();
WebToApk.getUnifiedPushSubscriptionJson();   // → JSON string
```

---

## 📦 Build Variants

| Variant | Description |
|---|---|
| `debug` | Development build with debugging enabled |
| `release` | Signed production build (ProGuard disabled) |

---

## 🔧 Configuration

Key configuration flags are defined at the top of `MainActivity.java`:

```java
String  mainURL = "https://app.termedu.in";  // Base URL
boolean requireDoubleBackToExit = true;       // Double-tap back to exit
boolean allowSubdomains         = true;       // Allow subdomain navigation
boolean enableExternalLinks     = true;       // Open external links
boolean enableMicrophone        = true;       // Mic access
boolean enableCamera            = true;       // Camera access
boolean forceVideoFullscreenRotation = true;  // Auto-rotate for video
boolean geolocationEnabled      = false;      // Location access
boolean forceDarkTheme          = false;      // Force dark mode
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is proprietary software developed for **TERM Edu (termedu.in)**.  
All rights reserved.

---

<div align="center">

**Built with ❤️ for TERM Edu**

[Website](https://termedu.in) · [Report Bug](https://github.com/your-username/Termedu-LMS-Learning-Management-System/issues) · [Request Feature](https://github.com/your-username/Termedu-LMS-Learning-Management-System/issues)

</div>
]]>