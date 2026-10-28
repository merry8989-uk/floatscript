# FloatScript — Privacy Policy

_Last updated: 2026-10-23_

FloatScript is a floating teleprompter for Android. This policy describes exactly
what the app does with your data. The short version: **everything stays on your
device. There is no account, no analytics, no advertising and no server
operated by us.**

## What the app stores, and where

| Data | Where it is stored | Why |
|---|---|---|
| Your scripts (title + text) | Local database on your device (Room) | So your scripts are there next time |
| Settings (font, colour, speed, mode, …) and floating-window position | Local app preferences (DataStore) | To restore your setup |
| Videos you record with the built-in recorder | Your device's gallery (MediaStore) | That is the recording you asked for |
| Diagnostics log and crash reports | Private app storage on your device | To help you (or us) diagnose a problem |

None of this leaves your device unless **you** explicitly export it (see
"Backup and restore" below).

## Permissions and what they are used for

- **Display over other apps (`SYSTEM_ALERT_WINDOW`)** — required to draw the
  teleprompter on top of other apps. Without it the core feature cannot work.
- **Foreground service** — keeps the overlay alive while you use another app.
- **Camera** — only for the built-in recorder. Not used otherwise.
- **Microphone** — used for the built-in recorder's audio, and for the optional
  speech-following mode.
- **Notifications** — shows the ongoing "FloatScript is running" notification
  required for a foreground service.
- **Internet** — used only for the optional update check and for downloading a
  newer APK from GitHub. No analytics or tracking traffic.
- **Install unknown apps** — lets the downloaded update APK be handed to the
  system installer (not used by Play-installed copies).

You can deny camera, microphone or notification permission and still use the app
as an external teleprompter.

## Speech recognition

The optional speech-following mode uses Android's system speech recogniser
(`SpeechRecognizer`). **Depending on your device and language, the recogniser may
send audio to the device manufacturer's or Google's speech service.** That
processing is performed by the operating system, not by FloatScript, and is
governed by your device's own privacy settings. If you do not want this, leave
speech-following switched off.

## Backup and restore

The backup feature writes a plain JSON file containing your scripts to a
location **you choose** through the system file picker. FloatScript does not
upload it anywhere.

Separately, if you have Android's device backup enabled in system settings,
Android may include the app's local data in your device backup. That is
controlled by your Android settings, not by the app.

## Diagnostics and crash reports

The app keeps a small rolling diagnostics log and, if it crashes, writes a crash
report containing the stack trace, the app version, your Android version and
device model, and the last few log lines.

**All of this stays on your device.** No diagnostics data is ever uploaded. You
can review, share, export or delete these logs yourself from
**Settings -> Diagnostics**; they are only ever sent anywhere if *you* choose to
share them.

## Update check

FloatScript can check whether a newer version exists by calling the public GitHub
Releases API for this project
(`https://api.github.com/repos/merry8989-uk/floatscript/releases/latest`).

That request carries **no personal data and no identifiers** — it is an ordinary
HTTPS request, which necessarily reveals your IP address to GitHub in the same
way visiting any web page does. The app stores nothing about the result beyond a
timestamp of the last check, and nothing is sent to the developer.

If a newer version exists you may download the APK from GitHub and install it.
**Copies installed from Google Play never use this path** — Play handles those
updates and the in-app updater is disabled.

## Analytics, ads and tracking

There are none. The app contains no analytics SDK, no advertising SDK and no
third-party tracking. It does not send usage data anywhere.

## Children

FloatScript is a general-purpose utility and is not directed at children.

## Your control over the data

- Delete individual scripts in the app.
- Clear everything by clearing the app's storage in Android settings.
- Uninstalling the app removes all locally stored scripts and settings.

## Changes

If this policy changes, the updated version will be committed to this repository
with a new "last updated" date.

## Contact

Questions about this policy: open an issue at
https://github.com/merry8989-uk/floatscript/issues
