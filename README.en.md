# VideoSee

[中文](README.md)

![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.05.01-blue)
![Tests](https://img.shields.io/badge/tests-passing-brightgreen)
![License](https://img.shields.io/badge/License-MIT-green)

VideoSee is an Android-only local photo and video browser built with Kotlin, Jetpack Compose, Media3, and Coil. It is designed for personal media browsing: folder, author, and tag collections, fast fullscreen viewing, 1-3 heart ratings for authors and individual media, manual JSON backups for heart and tag data, and LAN downloads from a laptop-side media service.

## Features

- Browse local images and videos by folder, author, or tag collection.
- Includes six remembered themes: Midnight, Graphite, Forest, Snow, Mist, and Sand.
- Author collections recognize filenames like `{author_id}_{timestamp}_{media_id}` with at least two underscores, then group media by `author_id`.
- Tag collections support multi-select intersection filtering. For example, selecting both `Scenery` and `Family` shows only media that has both tags.
- Search the current left-side collection by name: folder names in Folder mode, author names in Author mode, and tag names in Tag mode.
- Rate authors and individual media with 1 to 3 hearts; unrated items show gray outlined hearts.
- Create and rename favorite folders, choose a default folder, and toggle the current fullscreen media into it.
- Sort author collections by name, count, newest modified time, or heart level, in ascending or descending order.
- Sort media by name, newest modified time, or heart level, in ascending or descending order.
- Open the Data Backup page from the top-left toolbar to export/import heart and tag data together as JSON.
- Use the floating fast scroller on the media grid to jump through large collections quickly.
- Video thumbnails are cached in the app cache directory and reused after they are generated, reducing repeated thumbnail work and battery drain.
- Long-press a media card to enter multi-select delete mode, then delete selected media from the current collection.
- Open any image or video in fullscreen with recent-play history, sequential/shuffle playback, and two- or four-pane viewing.
- Toggle tags for the current fullscreen media from the left-side tag rail; tag state stays synchronized with the grid.
- Fullscreen button areas avoid taking over tiny touch movements too early, reducing missed immediate taps on tags or hearts.
- Swipe up or down in fullscreen to move to the previous or next item. At the first or last item, the first extra swipe shows a boundary hint, and a second swipe in the same direction wraps to the other end.
- For videos, double-tap anywhere on the fullscreen surface to play or pause, and swipe left/right by distance to seek quickly.
- When the player has not reported video duration yet, the viewer falls back to media metadata so fullscreen and multi-pane progress and seeking remain available.
- Images and videos support manual rotation. The current angle can be locked so fullscreen viewing, multi-pane viewing, and the floating preview keep using the same default angle.
- Video controls include a seek bar, current/total time, fixed playback speeds (`0.7x`, `0.9x`, `1.2x`, `1.5x`), and named video segments.
- Fullscreen viewing includes tone-curve and brightness/contrast-style color controls with saved presets. Video playback rebuilds its effect pipeline after adjustments and returns to the previous position, reducing missed color updates or lost playback state.
- Capture the current video frame to `Pictures/VideoSee`.
- Tap the fullscreen filename label to copy the filename.
- Delete the current fullscreen media from the right-side delete button. Android still shows the system media-delete authorization sheet, and after confirmation VideoSee opens the next item.
- Open the Download page from the left toolbar, configure server IP, port, token, and device ID, then refresh pending files, download one file, or download all.
- Downloaded images are saved to `Pictures/VideoSee`, videos to `Movies/VideoSee`, and other files to `Download/VideoSee`.
- The Settings page includes `ts视频转换` for scanning the displayed fixed `share91` import directory, converting local `.m3u8`, `.ts` segments, and `tsKey` files to MP4, then publishing them to the system media library.
- TS conversion skips previously downloaded records, ignores remote playlists, reports missing keys or segments, and supports single convert/download or batch convert-and-download.

## Requirements

- Android Studio or a working Android SDK.
- JDK 17 or newer with `javac`.
- `adb` for installing on a connected phone.
- Android device or emulator: min API 26, target API 36.
- A compatible laptop-side download service when using LAN downloads.

## Build And Test

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

If the default Java runtime is only a JRE and does not include `javac`, configure a full JDK first or point Gradle to one in your user Gradle settings.

## Install Debug APK

Gradle is currently configured to generate an `arm64-v8a` debug APK, suitable for most modern Android phones:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

Output path:

```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

If you later switch back to a universal APK, use:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If the phone already has the same package name installed with a different signing key, `adb install -r` may fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstall the old app first, then install the new APK. Uninstalling clears local app data, so export the heart and tag JSON before doing this:

```bash
adb uninstall app.videosee
adb install app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## Download Configuration

Open the Download page from the app's left toolbar. The Android app needs:

| Field | Description |
| --- | --- |
| IP | Laptop LAN IP, for example `192.168.1.23` |
| Port | Download server port, default `19827` |
| Token | Bearer token configured on the server |
| Device ID | This phone's download identity, default `videosee-phone` |

Use placeholders in documentation or examples, and never commit a real token:

```env
SYNC_SERVER_ENABLED=true
SYNC_SERVER_HOST=0.0.0.0
SYNC_SERVER_PORT=19827
SYNC_TOKEN=replace-with-a-long-random-token
SYNC_MDNS_ENABLED=true
SYNC_MDNS_NAME=videosee-sync
```

The Android app currently uses cleartext HTTP on the local network and enables `usesCleartextTraffic` in `AndroidManifest.xml`. Use this only on trusted LANs and do not expose the download service to the public internet.

## TS Video Conversion

The `ts视频转换` page in Settings handles HLS segment folders that already exist on the phone. The app shows a fixed import directory and uses a `share91` folder under the app's private external directory by default. Put each video's local `.m3u8`, `.ts` segments, and optional `tsKey` into a same-name folder, then tap Scan to discover convertible videos.

- Remote `.m3u8` files are treated as outer playlists and ignored. If a same-name child directory contains a local playlist, the local playlist is parsed instead.
- AES-128 keys must be local 16-byte files. Missing keys, missing segments, or playlists without local segments are shown as scan diagnostics.
- Convert first writes an MP4 into the app cache. Download publishes the MP4 to the system media library, defaulting to `Movies/VideoSee`.
- Convert And Download All processes the list in batch and shows the success/failure summary in the top banner.
- The repository-root `Movies/` directory is for local samples or temporary imports only. It is ignored by `.gitignore`; do not commit personal media or HLS segments.

## Heart And Tag Data Backup

Heart ratings and tag data are stored locally in app `SharedPreferences`:

- Ratings and tags are lost if the app is uninstalled, app data is cleared, or a differently signed APK is installed without first exporting JSON.
- Reinstalling over the app with the same signing key usually preserves local data.
- The exported JSON contains author hearts, media hearts, tag names, media-to-tag mappings, favorite folders, and video segments. Keep a recent copy as your manual backup.
- Importing JSON replaces the current heart and tag data with the backup content.

## Privacy And Security

- The app reads the system image/video media library and deletes media through Android's system authorization flow.
- Screenshots and downloaded files are written to public MediaStore directories.
- TS conversion reads the displayed local import directory and writes completed MP4 files to the system media library.
- The download token is stored in local app preferences. Do not use a public or reused sensitive token.
- Do not commit real tokens, personal media, HLS segments, debug APKs, signing keys, `local.properties`, Gradle caches, build outputs, or temporary icon source files.

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

This project is licensed under the [MIT License](LICENSE).
