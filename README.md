# 自用 Android 应用合集

个人自用的 Android 应用，全部免费无广告无限制，GitHub Actions 云端构建 APK。

## 项目列表

### reminder-app - 悬浮提醒
纯文本悬浮提醒：分钟级定时、单次/每天/每周/每月重复、可拖动、长按可配秒数关闭、多项同时弹出自动错开。支持背景色/文字色/字号/透明度/弹出位置配置。

### mirror-app - 局域网投屏
手机画面投到电视（同一 WiFi）：MediaProjection 采集 + H.264 硬编码 + 裸 TCP 传输 + 硬件解码，无加密无鉴权，一个 APK 两端通用（电视点"接收"、手机点"发送"）。零第三方依赖。

## 使用方式

1. 到 [Actions 页面](../../actions) 选一次成功的运行，底部 Artifacts 下载对应 APK（reminder-apk / mirror-apk）
2. 传到手机或电视安装（允许"安装未知应用"）

## 本地构建

```bash
cd reminder-app
gradle assembleDebug

cd mirror-app
gradle assembleDebug
```

需要 JDK 17 + Gradle 8.9 + Android SDK 35。

## 文档

- 开发笔记（架构决策、崩溃收集调试体系、踩坑实录）：[docs/DEV_NOTES.md](docs/DEV_NOTES.md)

## 构建

推送自动触发 GitHub Actions（按路径过滤：改 reminder-app/** 只构建 reminder，改 mirror-app/** 只构建 mirror），产物保留在 Artifacts 需登录下载。
