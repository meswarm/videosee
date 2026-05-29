# videosee

一个只面向 Android 的自定义图片和视频查看器。项目使用 Kotlin、Jetpack Compose、Media3、Coil 构建，目标是做一个适合个人浏览本地媒体的轻量查看工具。

## 当前功能

- 左侧文件夹列表，右侧显示该文件夹内的图片和视频缩略图。
- 点击图片或视频后进入全屏查看。
- 上滑/下滑切换上一个或下一个媒体。
- 视频双击播放/暂停。
- 视频左右滑分别后退/前进 5 秒，并显示操作提示。
- 视频支持进度条拖动、当前时间/总时长显示。
- 视频支持 `0.25x`、`0.5x`、`0.75x`、`0.9x` 固定倍速播放。
- 视频支持当前画面截图，截图保存到系统图片库 `Pictures/VideoSee`。
- 深色主题，绿色作为高亮色。

## 环境要求

- Android Studio 或可用的 Android SDK。
- JDK 17 或更高版本，并且需要包含 `javac`。
- 连接手机安装时需要 `adb`。

## 构建

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

如果本机默认 Java 只有 JRE、没有 `javac`，请先配置完整 JDK，或在用户级 Gradle 配置中指定可用 JDK。

## 安装 Debug APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## GitHub 上传前检查

上传前建议确认：

```bash
git status --short
git check-ignore -v local.properties .gradle/file-system.probe .kotlin build app/build
rg -n --hidden --glob '!.git' --glob '!node_modules' --glob '!dist' --glob '!build' '(api[_-]?key|secret|token|password|passwd|private[_-]?key|BEGIN (RSA|OPENSSH|PRIVATE)|AKIA|ghp_|github_pat_)' .
```

当前 `.gitignore` 已忽略本机配置、Gradle 缓存和构建产物，包括 `local.properties`、`.gradle/`、`.kotlin/`、`build/`、`app/build/`。
