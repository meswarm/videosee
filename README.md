# VideoSee

[English](README.en.md)

![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.05.01-blue)
![Tests](https://img.shields.io/badge/tests-passing-brightgreen)

VideoSee 是一个面向 Android 的本地图片和视频查看器。它使用 Kotlin、Jetpack Compose、Media3 和 Coil 构建，重点服务个人媒体浏览：按文件夹或作者查看、快速全屏预览、给作者和单个媒体打爱心等级、手动备份爱心数据，并从同一局域网里的笔记本同步未下载媒体。

## 功能

- 左侧支持文件夹集合和作者集合，右侧显示当前集合内的图片和视频。
- 作者集合会识别 `{author_id}_{timestamp}_{media_id}` 这类至少包含两个下划线的文件名，并按 `author_id` 自动归类。
- 作者和单个媒体都支持 1 到 3 颗爱心评分；未评分时显示灰白中空爱心。
- 作者集合支持按名称、数量、最新修改时间、爱心等级排序，并支持正序/倒序。
- 媒体列表支持按名称、最新修改时间、爱心等级排序，并支持正序/倒序。
- 左侧顶部提供爱心 JSON 手动导出和导入，方便在重装或换机前后备份评分数据。
- 右侧媒体卡片支持长按进入多选删除模式，勾选后可批量删除当前集合内的媒体。
- 点击图片或视频进入全屏查看，全屏内的爱心评分和列表页同步。
- 全屏查看支持上滑/下滑切换上一个或下一个媒体；到达第一张或最后一张时第一次提示边界，再次同方向滑动会循环到另一端。
- 视频全屏支持双击播放/暂停，左右滑分别后退/前进 5 秒；手势在整个全屏界面生效，包括视频上下的黑色区域。
- 视频支持进度条拖动、当前时间/总时长显示和 `0.25x`、`0.5x`、`0.75x`、`0.9x` 固定倍速播放。
- 视频支持当前画面截图，截图保存到系统图片库 `Pictures/VideoSee`。
- 全屏文件名信息可点击复制文件名。
- 全屏右侧提供删除按钮；删除会走 Android 系统媒体删除授权，确认后自动打开下一个媒体。
- 同步页支持配置服务端 IP、端口、Token 和设备 ID，可查看待同步文件、单个下载或全部下载。
- 同步下载的图片保存到 `Pictures/VideoSee`，视频保存到 `Movies/VideoSee`，其他文件保存到 `Download/VideoSee`。

## 环境要求

- Android Studio 或可用的 Android SDK。
- JDK 17 或更高版本，并且需要包含 `javac`。
- 连接手机安装时需要 `adb`。
- Android 设备或模拟器：最低 API 26，目标 API 36。
- 如果使用同步功能，需要局域网内运行兼容的笔记本同步服务。

## 构建与测试

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

如果本机默认 Java 只有 JRE、没有 `javac`，请先配置完整 JDK，或在用户级 Gradle 配置中指定可用 JDK。

## 安装 Debug APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

如果手机里已有不同签名的同包名应用，`adb install -r` 可能失败并提示 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`。这种情况需要先卸载旧版本再安装，卸载会清掉应用本地数据；请先导出爱心 JSON：

```bash
adb uninstall app.videosee
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 同步配置

同步页在应用左侧顶部的“同步”按钮进入。手机端需要填写：

| 字段 | 说明 |
| --- | --- |
| IP | 笔记本在局域网内的 IP，例如 `192.168.1.23` |
| 端口 | 同步服务端口，默认 `19827` |
| Token | 服务端配置的 Bearer Token |
| 设备 ID | 当前手机的同步标识，默认 `videosee-phone` |

服务端请使用占位值配置，不要把真实 Token 提交到 Git：

```env
SYNC_SERVER_ENABLED=true
SYNC_SERVER_HOST=0.0.0.0
SYNC_SERVER_PORT=19827
SYNC_TOKEN=replace-with-a-long-random-token
SYNC_MDNS_ENABLED=true
SYNC_MDNS_NAME=douyinks
```

手机端当前使用局域网 HTTP 明文同步，并在 `AndroidManifest.xml` 中开启了 `usesCleartextTraffic`。只建议在可信局域网内使用，不要把同步服务暴露到公网。

## 爱心数据备份

爱心数据默认保存在应用本地 `SharedPreferences` 中：

- 卸载应用、清除应用数据或安装不同签名包前没有导出 JSON，爱心数据会丢失。
- 覆盖安装同签名 APK 时通常会保留本地数据。
- 导出的 JSON 包含作者爱心和媒体爱心映射，建议定期覆盖保存一份最新备份。
- 导入 JSON 会用备份内容替换当前爱心数据。

## 隐私与安全

- 应用会读取系统图片和视频媒体库，并通过 Android 系统授权删除媒体。
- 截图和同步下载文件会写入系统媒体库目录。
- 同步 Token 存储在应用本地偏好设置中；不要使用公开或复用的敏感 Token。
- 仓库不应提交真实 Token、个人媒体文件、Debug APK、签名密钥、`local.properties`、Gradle 缓存或构建产物。

## 开发

常用检查命令：

```bash
git status --short
git diff --check
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

发布到 GitHub 前可以额外运行：

```bash
python3 /Users/txl/.codex/skills/prepare-github-push/scripts/pre_push_audit.py .
rg -n --hidden --glob '!.git' --glob '!**/build/**' --glob '!.gradle/**' --glob '!.kotlin/**' '(api[_-]?key|secret|token|password|passwd|private[_-]?key|BEGIN (RSA|OPENSSH|PRIVATE)|AKIA|ghp_|github_pat_)' .
```

当前 `.gitignore` 已忽略本机配置、Gradle/Kotlin 缓存、构建产物、APK/AAB、签名密钥、环境文件和日志。

## 许可证

当前仓库还没有声明许可证。公开发布前请根据你的用途补充 `LICENSE` 文件。
