# 悬浮提醒（Reminder）

Android 悬浮提醒应用：到点后以悬浮文本形式显示在屏幕任意界面上，可拖动，触摸持续 5 秒（含拖动）后关闭。

## 功能

- 分钟级定时提醒，支持单次 / 每天 / 每周（选星期）/ 每月（选 1-28 号）重复
- 悬浮文本不阻断下层应用操作，仅文本区域接收触摸
- 可拖动，长按（含拖动）累计 5 秒自动关闭，带进度环反馈
- 支持同时显示多个提醒
- 开机自动恢复全部提醒，进程被杀后闹钟仍由系统触发
- 支持多设备通用（Android 8.0 - 15）

## 权限说明（首次使用需手动开启）

1. 悬浮窗权限（必需）：应用内第 1 条横幅引导
2. 精确闹钟权限（保证准时）：应用内第 2 条横幅引导
3. 电池优化白名单（防 ROM 杀后台）：应用内第 3 条横幅引导
4. 通知权限：首次启动自动申请

## 构建

GitHub Actions 自动构建：`reminder-app/**` 有改动即触发，APK 在 Actions 运行详情的 Artifacts 里（reminder-apk）。

本地构建（可选）：`cd reminder-app && gradle assembleDebug`

## 安装

下载 app-debug.apk 到手机安装（需开启"允许安装未知来源应用"），首次打开按横幅引导开权限。

## 目录结构

```
reminder-app/
├── app/src/main/java/com/varedog/reminder/
│   ├── MainActivity.kt      # 提醒管理界面（唯一 Activity）
│   ├── AlarmScheduler.kt    # 精确闹钟注册
│   ├── AlarmReceiver.kt     # 到点接收 + 重复计算
│   ├── OverlayService.kt    # 前台服务，保活悬浮窗口
│   ├── OverlayTextView.kt   # 悬浮文本：拖动 + 5 秒关闭
│   ├── BootReceiver.kt      # 开机恢复
│   └── Reminder.kt / ReminderDao.kt / AppDatabase.kt
└── app/src/main/AndroidManifest.xml
```
