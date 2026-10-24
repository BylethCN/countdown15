# 倒计时15秒 · countdown15

一个参考**小米时钟「计时」页**的极简 Android 倒计时应用：三列滚轮选时间，按下开始后数字飞进圆环，圆环逐帧走完，到点响铃。

- 包名：`com.example.countdown15`
- minSdk 24 / targetSdk 34 / compileSdk 34
- 当前版本：**0.0.115**

## 功能

- **滚轮选择**：时（0–23）/ 分（0–59）/ 秒（0–59）三列，自研 `WheelPicker`，不依赖第三方滚轮库；23↔0 循环、惯性滑动、震动反馈
- **圆环倒计时**：大圆面 + 内缩轨道 + 蓝色剩余进度弧 + 弧末端圆点（圆点左右各留空隙，与小米一致）
- **平滑动画**：`ValueAnimator` 逐帧更新进度（跟随屏幕刷新率），数字只在秒变化时刷新
- **深浅色主题**：浅色 `#F7F7F7` / 深色纯黑；圆盘、轨道、弧色分别取 `#FAFAFA`、`#EDEDED`、`#3481FF`（深色模式为 `#272727`、`#383838`、`#2679F9`）
- **飞入 / 飞回**：开始倒计时时数字从三个滚轮的位置收拢到圆心；停止时先切回设定时长，再沿原路飞回滚轮，滚轮在落位后才出现
- **快速开始**：滑动后立刻点开始，以「当前正中间的数字」为准，不会用到惯性动画的目标值
- **到点响铃**：播放系统闹钟铃声
- **关于页**：版本与开源信息

## 构建

需要：

- JDK 17 及以上（Android Studio 自带的 JBR 亦可）
- Android SDK **Platform 34**、**Build-Tools 34.0.0**、Platform-Tools
- Gradle **9.6.0**（仓库已带 wrapper，`./gradlew` 会自动下载）

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

`local.properties` 里的 `sdk.dir` 指向本机 SDK 路径，不要提交（已在 `.gitignore` 中）。

### 签名

`debug` 构建使用 `fixedDebug` 配置，读取 `~/.android/debug.keystore`（storePassword / keyPassword 均为 `android`）。
仓库的 GitHub Actions 通过 `secrets.DEBUG_KEYSTORE_BASE64` 还原同一份 keystore；如果换了 keystore，记得同步更新该 Secret，否则本地与 CI 的 APK 签名会不一致——不同签名的 APK 无法覆盖安装。

> JDK 25 下 Kotlin 编译守护进程可能起不来，`gradle.properties` 里已设置 `kotlin.compiler.execution.strategy=in-process` 规避。

## 目录结构

```
app/src/main/java/com/example/countdown15/
├── MainActivity.kt      # 状态机、主题、飞入/飞回动画、倒计时
├── RingTimerView.kt     # 圆环：圆盘、轨道、进度弧、圆点、阴影
├── WheelPicker.kt       # 自研滚轮
└── AboutActivity.kt
app/src/main/res/
├── layout/activity_main.xml   # 滚轮页 + 圆环页 + 按钮
├── layout/activity_about.xml
├── layout/popup_menu.xml
└── font/robotoregular.ttf
```

## 版本

`0.0.115` — 圆环逐帧动画、圆点两侧留空、阴影 α12 / 46dp、深色配色对齐小米、快速开始取当前居中值。
