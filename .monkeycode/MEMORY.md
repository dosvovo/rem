# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

## Entries

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
