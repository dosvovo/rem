# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

## Entries

[APK 调试标准流程 - 崩溃日志收集方案]
- Date: 2026-09-08
- Context: reminder-app APK 启动闪退排查成功后，用户要求以后所有 APK 调试都沿用此方案
- Category: Troubleshooting & Debugging | Workflow & Collaboration
- Instructions:
  - 新 Android 项目默认内置崩溃收集四件套：CrashHandler（写 filesDir/crash.txt）+ ReminderApp(Application onCreate 安装 handler) + LauncherActivity 预演入口 + CrashReportActivity（android:process=":crash" 独立进程显示日志）
  - 排查真机闪退时优先走"诊断页截图"路径拿日志，adb logcat 仅作为兜底（用户无电脑 adb 环境时这是唯一手段）
  - LauncherActivity 作为 LAUNCHER 入口：先 consume crash.txt，再 try-catch 预演高风险初始化（inflate 主布局、数据库首查），通过后才进 MainActivity；业务崩溃与诊断代码隔离
  - CrashReportActivity 必须独立进程，否则随主进程死亡而消失；文本 setTextIsSelectable 支持长按复制，附"继续进入应用"按钮
  - crash.txt 读后即删（consume），防止每次启动重复弹日志
  - 高频崩溃源：findViewById 的 Kotlin 声明类型必须与 XML 实际类型精确匹配（Material/AppCompat 主题会把 TextView 替换为 MaterialTextView），suspend 函数在 Thread 里调不编译，需用 CoroutineScope+goAsync

[Personal Memo App - Project Status]
- Date: 2026-09-05
- Context: Agent 在实现 personal-memo 项目后记录的项目运行与环境知识
- Category: Operations & Deployment | Environment Configuration
- Instructions:
  - 项目为个人备忘录 PWA，位于 /workspace，技术栈 Vue 3 + Vite + Pinia + Supabase
  - 预览启动命令：`npm run dev -- --host 0.0.0.0 --port 5173`，预览端口为 5173
  - 构建验证命令：`npm run build`
  - Supabase 凭据已在项目根目录 .env 中配置（VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY），anon key 为用户自有 publishable key
  - 用户剩余待办：在 Supabase Dashboard 的 SQL Editor 中执行 supabase/schema.sql 完成建表，然后注册账号
  - Supabase 免费项目 7 天不活跃会被暂停，暂停后到 Dashboard 手动恢复即可
  - 邮件副本功能：保存笔记时前端原生 fetch 调用 Edge Function send-note-email，函数代码存于 supabase/functions/send-note-email/index.ts，依赖项目 Secrets RESEND_API_KEY 和 NOTE_EMAIL_TO，免费模式收件邮箱必须与 Resend 注册邮箱一致
  - Edge Function 必须处理 CORS（响应 OPTIONS + corsHeaders），否则浏览器预检失败，函数日志里只会看到无 Authorization 的空请求
