# Terminal Pilot (Android)

Terminal Pilot is a Kotlin + Jetpack Compose Android app for running AI coding workflows from a mobile SSH terminal.

This project is an original implementation and does not reuse Moshi branding/assets.

## MVP implemented

- SSH-first mobile terminal workflow
- tmux-first session handling:
  - auto-probe tmux sessions on connect
  - auto-create/attach `terminal-pilot` if none exist
  - show selector dialog when multiple tmux sessions exist
- Connection profiles with CRUD (Room)
- Secret storage for passwords/private keys (Android Keystore-backed encrypted storage)
- Optional biometric gate before using private-key auth
- Known-host fingerprint storage and first-connect trust prompt
- Live terminal output stream with input sending
- tmux-friendly special key row: `Ctrl`, `Esc`, `Tab`, arrows, `PgUp/PgDn`, `Home/End`, tmux prefix key
- fixed quick input chips for `Tab` and `Shift+Tab` to help with menu-driven CLIs
- Clipboard helpers: copy terminal text, paste into session
- Watch rules per session (prefix/regex), local notifications, and last-20 match log
- Voice-to-terminal dictation via `SpeechRecognizer` with editable preview before send
- Privacy screen and no telemetry/analytics behavior

## Tech stack

- Kotlin
- Jetpack Compose (Material 3)
- Coroutines + Flow
- MVVM (`ViewModel` + UI state holders)
- Navigation Compose
- Room
- Square Moshi (JSON import/export for profiles + settings)
- AndroidX Security Crypto (Keystore-backed encrypted preferences)
- mwiede JSch (maintained JSch fork) for SSH transport

### Why JSch fork for MVP

`com.github.mwiede:jsch` was chosen for Android MVP pragmatism:
- maintained fork of JSch
- straightforward shell channel integration for mobile
- smaller integration surface to reach end-to-end MVP quickly

## Project structure

- `app/src/main/java/com/cliagentic/mobileterminal/data/`
- `app/src/main/java/com/cliagentic/mobileterminal/security/`
- `app/src/main/java/com/cliagentic/mobileterminal/ssh/`
- `app/src/main/java/com/cliagentic/mobileterminal/terminal/`
- `app/src/main/java/com/cliagentic/mobileterminal/voice/`
- `app/src/main/java/com/cliagentic/mobileterminal/ui/`

## Build & run

Prerequisites:
- Android Studio (recent stable)
- Android SDK Platform 34
- JDK 17 (Android Studio JBR works)

Android Studio project config:
- Shared Run Configurations are included in `.run/`:
  - `App Debug` (Android App run config)
  - `Assemble Debug`
  - `Install Debug`
  - `Unit Tests (Debug)`
- If Android Studio does not show them immediately, use "File > Sync Project with Gradle Files" and reopen the Run Configuration selector.
- If needed on a new machine, copy `local.properties.example` to `local.properties` and set `sdk.dir`.

Build debug APK:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

Run tests:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

## JSON import/export

Settings screen supports export/import bundle JSON containing:
- non-secret connection profile fields
- app settings

Secrets (passwords/private keys) are intentionally excluded from export for security.

## Security & privacy posture

- No analytics, tracking, or telemetry code is included.
- Passwords/private keys are encrypted at rest.
- Credentials are only used for SSH sessions the user initiates.
- Known-host fingerprints are stored locally and compared on reconnect.
- User can wipe all local data via Android app storage clear.

## Current limitations

- Terminal renderer is MVP-level text/ANSI sanitization, not a full libvterm emulator.
  - Full-screen TUIs and advanced ANSI behavior may be limited.
- Foreground service keepalive is not yet implemented.
- Host key prompt is TOFU-style and fingerprint based (first trust + mismatch detection).

## Advanced milestone notes

### Mosh mode (not integrated yet)

Mosh is not bundled in this MVP due GPL licensing considerations and additional protocol integration work.

Research references:
- Official Mosh site: https://mosh.org/
- Mosh source/license (GPL-3.0): https://github.com/mobile-shell/mosh
- JuiceSSH advertises Mosh support on Android: https://juicessh.com/features
- Termux remote access docs include `mosh`: https://wiki.termux.com/wiki/Remote_Access

Planned approach:
- keep SSH default
- add Mosh behind feature flag only after license/compliance review
- include explicit LICENSE/NOTICE notes if Mosh components are introduced
- reuse the same tmux bootstrap/selector flow for both SSH and future Mosh transports

### Whisper.cpp dictation option

A stub `WhisperDictationEngine` is wired for future on-device dictation.

To implement fully:
- add native `whisper.cpp` bindings (JNI)
- provide model file placement strategy (app-private storage)
- expose setup flags and model selection in Settings
