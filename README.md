# 倒计时15秒

一个简洁的 Android 倒计时小工具，界面参考小米时钟的计时页。设定时间、点开始、到点响铃，就这么简单。

## 功能

- 🕐 时/分/秒 滚轮选择，像行李箱密码锁一样滑动，支持循环
- ⏱ 圆环倒计时：大圆面 + 内缩轨道 + 蓝色剩余进度弧 + 弧末端圆点
- ✨ 数字飞入飞出：开始倒计时时数字从滚轮收拢到圆心，停止时沿原路飞回
- ▶️ 一键开始，按下瞬间以「正中间的数字」为准（刚滑完立刻点也准确）
- 🔔 到点响铃，播放系统闹钟铃声
- 📳 触感反馈，每滑过一格轻微震动
- 🎨 黑白双主题，右上角「更多」菜单里一键切换，自动记住
- 📱 沉浸式全屏，没有多余的状态栏和标题栏
- ⚙️ 关于页，包含开源代码、问题反馈、导出日志

## 界面预览

（截图待补充）

## 下载

从 [Releases](https://github.com/BylethCN/countdown15/releases) 页面下载最新的 `countdown15-vX.X.X.apk`，传到手机安装即可。

- 最低支持：Android 7.0（API 24）
- 目标版本：Android 14（API 34）

## 使用说明

1. 打开 App，滑动时/分/秒滚轮设定倒计时时长
2. 点下方 ▶ 开始倒计时
3. 倒计时中，点屏幕中间的数字可以重新开始
4. 倒计时结束，响铃并出现 ■ 停止按钮
5. 点 ■ 停止铃声，回到设定界面

右上角 ⋮ 更多菜单：

- 切换主题：黑 / 白之间切换
- 关于：查看开源代码、反馈问题、导出日志

## 开发

环境：

- Android Studio（或命令行 SDK）
- JDK 17 及以上
- Gradle **9.6.0**（仓库自带 wrapper，`./gradlew` 会自动下载；GitHub Actions 里用的是 8.7）
- Android Gradle Plugin 8.5.2
- Android SDK Platform 34、Build-Tools 34.0.0

构建命令：

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

`local.properties` 里的 `sdk.dir` 指向本机 SDK 路径，不要提交（已在 `.gitignore` 中）。

### 签名

`debug` 构建使用 `fixedDebug` 配置，读取 `~/.android/debug.keystore`（storePassword / keyPassword 均为 `android`）。GitHub Actions 通过 `secrets.DEBUG_KEYSTORE_BASE64` 还原同一份 keystore；如果换了 keystore，记得同步更新该 Secret，否则本地和 CI 打出来的 APK 签名不一致，无法互相覆盖安装。

### 持续集成

推送到 `main` 会触发 `.github/workflows/build.yml`：编译 debug APK，并以 `versionName` 为 tag 创建 Release（如 `v0.0.115`），APK 作为附件上传。

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

## 当前版本

`0.0.115`
