# FloatScript — Transparent Floating Teleprompter for Android

FloatScript is a transparent floating teleprompter. It shows your script in a
window that floats **above other apps**, so you can read it while you record
video with your phone's camera (or with the built-in CameraX recorder).

Built with **Kotlin + Jetpack Compose**, **MVVM**, **Room**, **DataStore**,
**CameraX** and the native **WindowManager** overlay APIs.

---

## Requirements

| | |
|---|---|
| Minimum Android | 8.0 (API 26) |
| Target / compile SDK | 35 |
| Language | Kotlin 2.0 |
| Build system | Gradle 8.9 + Kotlin DSL |
| UI | Jetpack Compose (Material 3) |
| JDK | 17 |

## Getting the APK

Three ways, easiest first:

**1. GitHub Actions (no local setup needed).** A ready-made workflow is included
at `.github/workflows/android.yml`. Push the project to a GitHub repo and the
action builds the debug APK and attaches it as a downloadable artifact named
`FloatScript-debug-apk` (Actions tab -> latest run -> Artifacts).

**2. Android Studio.** Open the project folder, let it sync, then
Run -> Build APK(s). Android Studio provisions the Gradle wrapper and SDK.

**3. Command line** (needs JDK 17 + Android SDK, `ANDROID_HOME` set):

```bash
./gradlew :app:assembleDebug
```

In every case the debug APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`
(you must allow installing from unknown sources).

> The Gradle wrapper (`gradlew`, `gradle/wrapper/*`) is included, so no local
> Gradle install is required — only a JDK and the Android SDK.

---

## Project structure

```
app/src/main/java/com/floatscript/app/
├── FloatScriptApp.kt              # Application + tiny manual DI container
├── MainActivity.kt                # Single-activity Compose host + navigation
├── data/                          # Room entities/DAO/DB, repositories, DataStore
│   ├── AppDatabase.kt
│   ├── ScriptDao.kt
│   ├── ScriptEntity.kt
│   ├── ScriptRepository.kt
│   ├── SettingsRepository.kt      # DataStore-backed ScriptSettings + geometry
│   └── SampleScripts.kt           # First-run sample
├── model/                         # Pure domain types
│   ├── Script.kt
│   ├── ScriptSettings.kt
│   └── OverlayGeometry.kt
├── overlay/                       # The floating teleprompter itself
│   ├── OverlayService.kt          # Foreground service + WindowManager
│   ├── OverlayLifecycleOwner.kt   # Lets a ComposeView live outside an Activity
│   ├── TeleprompterView.kt        # Compose UI inside the floating window
│   ├── ScrollEngine.kt            # Continuous / line / word scrolling
│   ├── FloatingControls.kt        # Compact control strip
│   ├── TextTokenizer.kt           # Word splitting for reading modes
│   └── OverlayConstants.kt
├── speech/SpeechFollower.kt       # Optional speech-following (SpeechRecognizer)
├── ui/
│   ├── home/HomeScreen.kt         # Script list, import, paste, start overlay
│   ├── editor/ScriptEditorScreen.kt
│   ├── settings/SettingsScreen.kt
│   ├── record/RecordScreen.kt     # CameraX preview + in-app recording
│   └── theme/                     # Material 3 theme
└── viewmodel/                     # MVVM ViewModels + factory
```

## Feature map

### A. Transparent floating teleprompter
- Window added through `WindowManager` with `TYPE_APPLICATION_OVERLAY`.
- Background opacity adjustable 0–100 % and a configurable background tint.
- Text colour, font size, weight, line spacing, alignment and margin controls.
- Portrait and landscape (the service is orientation-agnostic).
- Drag the top strip (or the handle in the control strip) to move the window;
  drag the bottom-right grip to resize it.

### B. Script scrolling
- Smooth, frame-clock–driven auto-scroll (stays smooth across refresh rates).
- Speed slider with pause / resume / restart / stop.
- Reading modes: **continuous**, **line-by-line** and **word-by-word**
  (word-by-word highlights the active word using the live text layout).
- Manual touch scrolling; configurable countdown before scrolling starts;
  auto-hide of the control strip during playback.

### C. Script management
- Create, edit, rename, duplicate and delete scripts.
- Saved locally with **Room**; settings and window geometry with **DataStore**.
- Import `.txt` files and paste from the clipboard.
- Remembers the last script, font size, scroll speed and window position.
- Ships with a sample script; long scripts are handled off the main thread.

### D. Floating controls
Play/pause, speed ±, text size ±, restart, hide/show controls, close — all
touch-friendly, with an optional **minimal mode** that shows only the script.

### E. Camera & recording workflow
- **External camera workflow:** a shortcut opens the device's camera app while
  the overlay stays on top. Note that *not every OEM camera app permits
  third-party overlays* — this is a platform/app limitation, not a bug.
- **Built-in recording:** `RecordScreen` uses **CameraX** with a front-camera
  preview and video capture. It requests `CAMERA` and `RECORD_AUDIO`, writes to
  the gallery via MediaStore, and handles permission denial and camera errors.

### F. Advanced
- Horizontal mirroring and vertical flip (for teleprompter mirrors).
- Adjustable margins and line spacing.
- Countdown timer; keep-screen-awake while the overlay is active.
- Optional speech-following mode (`SpeechRecognizer`) that advances the script
  as you speak, with microphone permission and error handling.
- Battery-efficient: the overlay is a single foreground service that stops
  completely when closed.

---

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the floating window over other apps. |
| `FOREGROUND_SERVICE` / `..._SPECIAL_USE` | Keep the overlay alive and visible. |
| `POST_NOTIFICATIONS` | Show the ongoing "FloatScript is running" notification (Android 13+). |
| `WAKE_LOCK` | Keep the screen on while prompting (optional). |
| `CAMERA`, `RECORD_AUDIO` | Only for the built-in CameraX recorder and speech-follow. |

All runtime permissions are requested in context; the app works fully as an
external-teleprompter even if camera/mic permissions are denied.

---

## Known limitations / notes

- **Overlays over the camera app** depend on the camera app. Many OEM camera
  apps (and some Android versions) block third-party overlays; the built-in
  CameraX recorder is provided precisely so recording and prompting work
  together regardless.
- **Word-by-word** mode approximates pacing from the scroll-speed setting; it
  is not a beat-perfect karaoke timing.
- **Speech-following** depends on the device's on-device speech recogniser and
  network availability for some languages.
- The sample script and UI strings are in English.
- Icons use only the small `material-icons-core` set plus a few hand-drawn
  vectors in `ui/icons/AppIcons.kt`, deliberately avoiding the ~35 MB
  `material-icons-extended` dependency (which slows dexing a lot for a handful
  of glyphs). A couple of control-strip icons are therefore simplified
  (e.g. text size uses chevrons, hide-controls uses a cross).

## License

MIT — see `LICENSE`.
