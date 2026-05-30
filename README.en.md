# VideoSee

[中文](README.md)

![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.05.01-blue)
![Tests](https://img.shields.io/badge/tests-passing-brightgreen)

VideoSee is an Android-only local photo and video browser built with Kotlin, Jetpack Compose, Media3, and Coil. It is designed for personal media browsing: folder and author collections, fast fullscreen viewing, 1-3 heart ratings for authors and individual media, manual JSON backups for rating data, and LAN sync from a laptop-side media service.

## Features

- Browse local images and videos by folder collection or author collection.
- Author collections recognize filenames like `{author_id}_{timestamp}_{media_id}` with at least two underscores, then group media by `author_id`.
- Rate authors and individual media with 1 to 3 hearts; unrated items show gray outlined hearts.
- Sort author collections by name, count, newest modified time, or heart level, in ascending or descending order.
- Sort media by name, newest modified time, or heart level, in ascending or descending order.
- Export and import heart-rating data as JSON from the top-left toolbar.
- Long-press a media card to enter multi-select delete mode, then delete selected media from the current collection.
- Open any image or video in fullscreen; fullscreen heart controls stay synchronized with the grid.
- Swipe up or down in fullscreen to move to the previous or next item. At the first or last item, the first extra swipe shows a boundary hint, and a second swipe in the same direction wraps to the other end.
- For videos, double-tap anywhere on the fullscreen surface to play or pause, and swipe left/right anywhere to seek backward/forward by 5 seconds.
- Video controls include a seek bar, current/total time, and fixed playback speeds: `0.25x`, `0.5x`, `0.75x`, and `0.9x`.
- Capture the current video frame to `Pictures/VideoSee`.
- Tap the fullscreen filename label to copy the filename.
- Delete the current fullscreen media from the right-side delete button. Android still shows the system media-delete authorization sheet, and after confirmation VideoSee opens the next item.
- Open the Sync page from the left toolbar, configure server IP, port, token, and device ID, then refresh pending files, download one file, or download all.
- Synced images are saved to `Pictures/VideoSee`, videos to `Movies/VideoSee`, and other files to `Download/VideoSee`.

## Requirements

- Android Studio or a working Android SDK.
- JDK 17 or newer with `javac`.
- `adb` for installing on a connected phone.
- Android device or emulator: min API 26, target API 36.
- A compatible laptop-side sync service when using LAN sync.

## Build And Test

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

If the default Java runtime is only a JRE and does not include `javac`, configure a full JDK first or point Gradle to one in your user Gradle settings.

## Install Debug APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If the phone already has the same package name installed with a different signing key, `adb install -r` may fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstall the old app first, then install the new APK. Uninstalling clears local app data, so export the heart-rating JSON before doing this:

```bash
adb uninstall app.videosee
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Sync Configuration

Open the Sync page from the app's left toolbar. The Android app needs:

| Field | Description |
| --- | --- |
| IP | Laptop LAN IP, for example `192.168.1.23` |
| Port | Sync server port, default `19827` |
| Token | Bearer token configured on the server |
| Device ID | This phone's sync identity, default `videosee-phone` |

Use placeholders in documentation or examples, and never commit a real token:

```env
SYNC_SERVER_ENABLED=true
SYNC_SERVER_HOST=0.0.0.0
SYNC_SERVER_PORT=19827
SYNC_TOKEN=replace-with-a-long-random-token
SYNC_MDNS_ENABLED=true
SYNC_MDNS_NAME=douyinks
```

The Android app currently uses cleartext HTTP on the local network and enables `usesCleartextTraffic` in `AndroidManifest.xml`. Use this only on trusted LANs and do not expose the sync service to the public internet.

## Heart Data Backup

Heart ratings are stored locally in app `SharedPreferences`:

- Ratings are lost if the app is uninstalled, app data is cleared, or a differently signed APK is installed without first exporting JSON.
- Reinstalling over the app with the same signing key usually preserves local data.
- The exported JSON contains author and media heart mappings. Keep a recent copy as your manual backup.
- Importing JSON replaces the current heart-rating data with the backup content.

## Privacy And Security

- The app reads the system image/video media library and deletes media through Android's system authorization flow.
- Screenshots and synced files are written to public MediaStore directories.
- The sync token is stored in local app preferences. Do not use a public or reused sensitive token.
- Do not commit real tokens, personal media, debug APKs, signing keys, `local.properties`, Gradle caches, or build outputs.

## Development

Common checks:

```bash
git status --short
git diff --check
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Before publishing to GitHub, you can also run:

```bash
python3 /Users/txl/.codex/skills/prepare-github-push/scripts/pre_push_audit.py .
rg -n --hidden --glob '!.git' --glob '!**/build/**' --glob '!.gradle/**' --glob '!.kotlin/**' '(api[_-]?key|secret|token|password|passwd|private[_-]?key|BEGIN (RSA|OPENSSH|PRIVATE)|AKIA|ghp_|github_pat_)' .
```

The current `.gitignore` excludes local machine config, Gradle/Kotlin caches, build outputs, APK/AAB files, signing keys, environment files, and logs.

## License

This repository does not currently declare a license. Add a `LICENSE` file before publishing if you want others to know how they may use the code.
