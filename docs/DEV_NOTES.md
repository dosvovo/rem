# 悬浮提醒 APK 开发笔记

本文档记录本项目的完整开发思路、架构决策、调试体系与踩坑实录，供后续开发与排查问题参考。

## 一、需求与选型

需求：纯文本悬浮提醒（无标题）、分钟级定时、支持不重复/每天/每周选周几/每月选 1-28 号；悬浮文本可拖动、不阻断下层应用、触摸累计 5 秒（可配置）关闭；支持多窗口。

选型决策：

| 决策 | 理由 |
|------|------|
| 传统 View + Material 3 | 界面简单，Compose 学习/调试成本不值 |
| Room + KSP | 本地持久化标配，KSP 编译快 |
| AlarmManager + 前台服务悬浮窗 | 免推送服务、无后端依赖、纯离线可用 |
| minSdk 26 / targetSdk 35 | 覆盖主流设备，适配 Android 15 |
| GitHub Actions 云端构建 | 本地无 Android SDK 环境，用 Actions 免费出 debug APK 直接安装 |

## 二、架构与模块

```
LauncherActivity（入口 + 诊断预演）
    └─> MainActivity（列表 / 新建对话框 / 设置对话框）
AlarmScheduler（三级降级调度）
    └─> AlarmReceiver（goAsync + 协程：弹窗 + 计算下一次）
            ├─> OverlayService（前台服务，保活窗口）
            │       └─ OverlayTextView（拖动 + 进度环 + 长按关闭）
            └─> [失败降级] 高优先级通知
BootReceiver（开机后恢复所有未触发提醒）
CrashHandler + CrashReportActivity(:crash)（崩溃收集）
Prefs（悬浮窗样式与行为配置，SharedPreferences）
```

关键实现点：

1. **调度三级降级**：优先 `setAlarmClock`（最精确、不需要 SCHEDULE_EXACT_ALARM 权限、触发时进程活跃允许启动前台服务）→ 失败退 `setExactAndAllowWhileIdle` → 再失败退 `setAndAllowWhileIdle`
2. **重复提醒**：触发后由 AlarmReceiver 计算 nextTriggerMillis 写回数据库并重新注册，数据库始终是唯一事实来源
3. **悬浮窗穿透**：`FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL`，下层应用可正常操作
4. **长按关闭**：`onTouchEvent` 里计时，`onDraw` 画进度环，`postInvalidateDelayed(50)` 刷新，达到时长震动 + 关闭
5. **多窗口叠层**：按活跃窗口序号偏移 40dp，避免完全重叠
6. **设置项**：背景颜色 / 文字颜色 / 字体大小 / 背景透明 / 弹出 XY / 关闭秒数，SharedPreferences 持久化，下次弹出生效

## 三、崩溃收集调试体系（四件套）

没有电脑 adb 环境时，这套体系是拿真机崩溃日志的唯一低成本手段。

### 组成

1. **CrashHandler**：`Thread.setDefaultUncaughtExceptionHandler` 捕获所有未处理异常，写入 `filesDir/crash.txt`（线程名 + 完整堆栈），写完转交系统默认 handler
2. **Application 注册**：`ReminderApp.onCreate` 里 install，覆盖最早时机
3. **LauncherActivity 诊断入口**：
   - 启动先 `consume()` crash.txt（读后即删），有日志就跳诊断页
   - 主动 try-catch 预演启动路径上的高风险操作：inflate 主布局、数据库初始化 + 首查。预演能比被动崩溃更快暴露根因
   - 全部通过才进 MainActivity
4. **CrashReportActivity**：显示日志，`setTextIsSelectable` 支持长按复制，附"继续进入应用"按钮

### 成败关键：独立进程

CrashReportActivity 必须在 Manifest 声明 `android:process=":crash"`。同进程的日志页会随主进程死亡而消失，用户什么都看不到。

### 注意事项

- 盲区：崩溃发生在 Application.onCreate 之前（ContentProvider 阶段）时 handler 未安装，抓不到，只能 adb
- crash.txt 读后即删，防止每次启动重复弹窗
- 文件放 filesDir 私有目录，不碰存储权限
- 预演要覆盖主界面启动路径上的所有关键操作

## 四、踩坑实录

| # | 现象 | 根因 | 修复 |
|---|------|------|------|
| 1 | 编译错误：suspend 函数只能从协程调用 | Thread lambda 里直接调 Room suspend 方法 | `goAsync()` + `CoroutineScope(Dispatchers.IO).launch`，finally 里 `pending.finish()` |
| 2 | 编译错误：Unresolved reference 'it' | 尾 lambda 当回调传，lambda 无参数却引用 it | 回调改为 `var onClose: (() -> Unit)?` 属性，赋值时捕获已初始化的 view |
| 3 | 启动即闪退 ClassCastException | Material 主题把 XML 里的 `<TextView>` 自动替换成 `MaterialTextView`，Kotlin 变量声明为 `LinearLayout` 强转失败 | findViewById 的声明类型必须与 XML 实际类型精确匹配（声明父类如 CompoundButton 最稳） |
| 4 | 闪退后再打开看不到错误日志 | 日志页与主界面同进程，主界面崩进程死日志页一起死 | 日志页放 `:crash` 独立进程 |
| 5 | 到设定时间不自动弹出，打开 APK 才显示 | ① 无精确闹钟权限时降级为可被 Doze 推迟的闹钟；② Android 12+ 禁止后台启动前台服务 | ① 改用 `setAlarmClock`；② startForegroundService 包 try-catch，失败降级为 IMPORTANCE_HIGH 通知（CATEGORY_ALARM） |
| 6 | 顶部横幅被状态栏盖住无法点击 | targetSdk 35 在 Android 15 上强制 edge-to-edge，内容延伸到状态栏底下 | 根布局加 `android:fitsSystemWindows="true"` |

## 五、GitHub Actions 构建流程

- workflow 触发条件 `paths: reminder-app/**`——空提交不会触发，需附带仓库文件改动（如 README）才能触发
- 使用 `gradle/actions/setup-gradle@v4` + Gradle 8.9，不提交 wrapper，构建约 2.5 分钟
- APK 产物在运行详情页底部 Artifacts，下载需登录 GitHub
- 偶发 `Failed to FinalizeArtifact 403` 是 GitHub 服务端临时故障，APK 编译实际成功，重新提交触发构建即可
- fine-grained token 需要 Contents 与 Workflows 读写权限；token 注入 remote URL 推送后必须立即还原，测试完在 GitHub 设置里删除
- 若重跑失败任务返回 403 "Resource not accessible"，说明 token 缺 Actions 权限，直接推新提交触发更省事

## 六、真机调试标准流程

1. 装新版 APK（覆盖安装即可）
2. 打开应用：
   - 直接进主界面 → 正常
   - 出现"错误详情"页 → 长按复制/截图，按日志 `Caused by` + `at 类名(文件:行号)` 定位修复
   - 直接闪退且无日志页 → 崩溃在系统加载最早期，需要电脑 adb logcat 兜底
3. 功能验证：建一个 1 分钟后的提醒，退出应用等弹出（别开着 App 等），验证自动弹出、样式、拖动、长按关闭
