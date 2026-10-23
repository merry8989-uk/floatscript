# Play Store listing — working document

Everything needed to publish FloatScript, and what is still missing.

## Assets

| Asset | Requirement | Status |
|---|---|---|
| App icon | 512 × 512 PNG, 32-bit, no alpha | ✅ `play-icon-512.png` |
| Round icon | 512 × 512 (optional) | ✅ `play-icon-round-512.png` |
| Feature graphic | 1024 × 500 PNG/JPEG, no alpha | ✅ `feature-graphic-1024x500.png` |
| Phone screenshots | 2–8, min 320 px, 16:9 or 9:16 | ❌ **need a real device** |
| 7" / 10" tablet screenshots | optional | ❌ need a device |
| App bundle | `.aab` | ✅ produced by the release workflow |

Screenshots to capture once a device is available: home/script list, the floating
teleprompter over a camera app, the settings screen, the in-app recorder.

## Store text

**App name (30 chars max)**
```
FloatScript: Floating Prompter
```

**Short description (80 chars max)**
```
Transparent teleprompter that floats over any app so you can read while you record.
```

**Full description**
```
FloatScript is a transparent floating teleprompter. It shows your script in a
window that sits on top of other apps, so you can read it while you record video
with your phone's camera.

READ WHILE YOU RECORD
• Start the teleprompter, open your camera app — the script stays on screen
• Or use the built-in recorder (CameraX) and prompt from the same screen
• Drag to move it, resize it, and it remembers where you left it

MAKE IT YOURS
• Transparent background, adjustable from 0% to 100% opacity, with a tint
• Text colour, size, weight, line spacing, alignment and margins
• Mirror horizontally and flip vertically for teleprompter mirrors
• Portrait and landscape

SCROLL THE WAY YOU LIKE
• Smooth continuous scrolling that follows your screen's refresh rate
• Line-by-line and word-by-word modes
• Countdown before you start, auto-hide controls while scrolling
• Speech-following mode advances the script as you read aloud

YOUR SCRIPTS, ON YOUR DEVICE
• Create, edit, rename, duplicate and delete scripts
• Import .txt files or paste from the clipboard
• Back up all your scripts to a file and restore them later
• No account, no ads, no tracking — everything stays on your device

Note: some camera apps block third-party overlays. That is why FloatScript also
includes its own recorder, so prompting and recording always work together.
```

## Categorisation

- **Category:** Video Players & Editors (alternative: Tools)
- **Tags:** teleprompter, video, creator, camera
- **Contains ads:** No
- **In-app purchases:** No

## Data safety form

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | N/A — nothing is transmitted |
| Do you provide a way for users to request that their data is deleted? | Yes — delete scripts in-app, clear app storage, or uninstall |
| Data types collected | None |

Diagnostics: the app writes a local log and crash reports, but **nothing leaves
the device** (no INTERNET permission, no crash-reporting SDK), so it still counts
as not collecting data. If you ever add a remote reporter, this answer must
change.

Note: the app uses the camera and microphone **on-device only**. Android's data
safety form treats on-device-only processing as not "collected", but you must
declare the camera/microphone *permissions* and explain them in the listing.

## Content rating questionnaire

- Violence, sexuality, language, controlled substances: **none**
- User-generated content shared between users: **no** (scripts stay on-device)
- Expected rating: **Everyone / PEGI 3**

## Policy links

- Privacy policy URL: point at `PRIVACY.md` in this repository (publish it via
  GitHub Pages, or host the file anywhere public and paste that URL).

## Release checklist

- [ ] Bump `versionName` / `versionCode` (the release workflow does this
      automatically from the tag and run number)
- [ ] Confirm the `.aab` from the GitHub release is signed with the **upload key**
      (the keystore in `KEYSTORE-README.txt`)
- [ ] Upload the `.aab` to Play Console
- [ ] Fill in the Data safety form and content rating
- [ ] Add the screenshots and the feature graphic
- [ ] Set the privacy policy URL
- [ ] Roll out to internal testing first
