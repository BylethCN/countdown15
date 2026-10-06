# 倒计时15秒

一个简洁的 Android 倒计时小工具，界面参考小米时钟的计时页。设定时间、点开始、到点响铃，就这么简单。

## 功能

- 🕐 时/分/秒 滚轮选择，像行李箱密码锁一样滑动，支持循环
- ▶️ 一键开始，点按钮即倒计时
- 🔔 到点响铃，播放系统闹钟铃声
- 📳 触感反馈，每滑过一格轻微震动
- 🎨 黑白双主题，右上角"更多"菜单里一键切换，自动记住
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

- Android Studio
- JDK 17
- Gradle 8.7
- Android Gradle Plugin 8.5.2

构建命令：

./gradlew assembleDebug

或者直接推送到 main 分支，GitHub Actions 会自动构建并生成 APK。

目录结构：

app/src/main/
├── java/com/example/countdown15/
│ ├── MainActivity.kt 主界面逻辑
│ ├── WheelPicker.kt 自定义滚轮控件
│ └── AboutActivity.kt 关于页
├── res/
│ ├── layout/ 界面布局
│ ├── drawable/ 图标、按钮背景
│ ├── values/ 颜色、字符串、自定义属性
│ ├── font/ 字体（MiSans）
│ └── mipmap-*/ App 图标
└── AndroidManifest.xml

技术栈：

- Kotlin
- 自定义 View：WheelPicker 完整手写，不依赖第三方库
- GitHub Actions：自动构建、签名、发布

## 开源协议

本项目采用 [MIT License](LICENSE)，可自由使用、修改、分发。

## 致谢

- 字体：[MiSans](https://hyperos.mi.com/font/zh/)（小米，免费商用）
- 图标生成：[IconKitchen](https://icon.kitchen)
- 界面参考：小米时钟
