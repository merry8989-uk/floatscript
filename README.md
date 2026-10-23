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

**Releases are automatic.** `.github/workflows/release.yml` runs on every push to
`main`: it reads `versionName` from `app/build.gradle.kts`, creates the matching
tag (e.g. `v1.1.0`) if it doesn't exist yet, builds the release APK and publishes
a GitHub Release with `FloatScript-<tag>.apk` attached. You can also trigger it
manually (Actions -> Release APK -> Run workflow) or just push a tag yourself.

> The release APK is signed with the **debug keystore** unless you add the
> `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`
> repository secrets — then it is signed with your real release key instead.
> Bump `versionCode`/`versionName` in `app/build.gradle.kts` before releasing.

Other ways, easiest first:

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

The release workflow also produces the **Play-ready `.aab`** and attaches it to
the GitHub release next to the APK.

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
│   │                              # (two-pane on tablets / foldables)
│   ├── editor/ScriptEditorScreen.kt
│   ├── settings/SettingsScreen.kt
│   ├── record/RecordScreen.kt     # CameraX preview + in-app recording
│   ├── icons/AppIcons.kt          # The few icons not in material-icons-core
│   └── theme/                     # Material 3 theme
└── viewmodel/                     # MVVM ViewModels + factory

app/src/test/                      # JVM unit tests (state, tokenizer, backup codec)
store-assets/                      # Play Store icon, feature graphic, listing notes
PRIVACY.md                         # Privacy policy
```

## Diagnostics

FloatScript keeps a **local-only** diagnostics log: a bounded in-memory buffer
(500 entries), a rolling file capped at 256 KB, and a crash report written if
the app dies from an uncaught exception.

Nothing is uploaded — the app declares **no INTERNET permission** and contains
no analytics or crash-reporting SDK. You can view, share, export or clear the
log from **Settings -> Diagnostics** (or the home screen's ⋮ menu). Sharing uses
a `FileProvider`, so the file is handed to whichever app you pick.

This keeps the privacy policy honest while still giving you something concrete
to attach to a bug report.

## Testing

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:lintDebug           # Android lint
```

Both run automatically in CI on every pull request, and before every release.

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
- **Local backup and restore**: export every script to a JSON file and import it
  again later, using the system file picker (no cloud account needed).
- Ships with a sample script; the word highlight is drawn onto the text layout
  instead of rebuilding the text, so long scripts stay smooth.

### D. Floating controls
Play/pause, stop, speed ±, text size ±, reading-mode switch, restart,
hide/show controls and close — all touch-friendly, with a live read-out of the
current speed / text size / mode. **Minimal mode** shows only the script; the
control strip comes back with a tap and the window stays draggable, so minimal
mode never leaves you unable to start playback.

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
  as you speak. The microphone permission is requested when you switch the
  feature on, and repeated recogniser errors stop the follower instead of
  looping.
- Settings changed in the app reach a running overlay immediately, and changes
  made with the floating controls are saved back.
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
- UI strings are English by default with a **Hindi (`values-hi`) translation**;
  other languages fall back to English.
- Release builds have **R8 + resource shrinking enabled**. It is configured with
  keep rules for Room / DataStore / CameraX / the app entry points, but a
  minified build has not been smoke-tested on a device yet — do that before
  shipping.
- The floating window re-clamps itself on rotation, but the saved position is in
  raw pixels, so a rotation can still move the window.
- Icons use only the small `material-icons-core` set plus a few hand-drawn
  vectors in `ui/icons/AppIcons.kt`, deliberately avoiding the ~35 MB
  `material-icons-extended` dependency (which slows dexing a lot for a handful
  of glyphs). A couple of control-strip icons are therefore simplified
  (e.g. text size uses chevrons, hide-controls uses a cross).

## Store assets

`store-assets/` holds the Play Store icon, the feature graphic and
`STORE-LISTING.md` (description text, data-safety answers, content rating and a
release checklist). Screenshots still need to be captured on a real device.

See `PRIVACY.md` for the privacy policy — the app collects nothing, but it does
use the camera and microphone, which must be declared on Play.

## License

MIT — see `LICENSE`.
