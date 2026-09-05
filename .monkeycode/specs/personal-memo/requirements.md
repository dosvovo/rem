# Requirements Document

## Introduction

个人备忘录应用（Personal Memo）：一个 PWA 网页应用，用于记录备忘和笔记，数据存储在云端，支持多设备访问。可添加到手机桌面以类 App 方式使用。无提醒功能。

## Glossary

- **笔记（Note）**: 用户记录的一条内容，包含标题和正文
- **备忘**: 短小、快速记录的笔记，与记事在数据模型上统一为"笔记"，通过置顶和颜色区分重要程度
- **Supabase**: 免费云后端服务，提供 PostgreSQL 数据库、用户认证、行级安全
- **PWA**: 渐进式网页应用，可安装到设备主屏幕并支持离线查看
- **RLS（Row Level Security）**: 数据库行级安全策略，确保用户只能访问自己的数据

## Requirements

### Requirement 1: 用户认证

**User Story:** AS 用户, I want 注册并登录应用, so that 我的备忘数据与其他人隔离且可跨设备访问

#### Acceptance Criteria

1. WHEN 用户首次打开应用，系统 SHALL 展示登录界面并支持注册
2. WHEN 用户输入邮箱和密码完成注册，系统 SHALL 创建 Supabase 账户并自动登录
3. WHEN 用户输入正确的邮箱和密码，系统 SHALL 完成登录并展示笔记列表
4. IF 用户输入错误的邮箱或密码，系统 SHALL 显示明确的错误提示
5. WHILE 用户处于登录状态，系统 SHALL 在用户关闭并重新打开应用时保持登录状态

### Requirement 2: 创建笔记

**User Story:** AS 用户, I want 快速创建一条备忘, so that 随时记录想法和待办事项

#### Acceptance Criteria

1. WHEN 用户点击新建按钮，系统 SHALL 打开编辑界面
2. WHEN 用户输入内容并保存，系统 SHALL 将笔记写入云端数据库并返回列表页
3. WHILE 用户在编辑界面，系统 SHALL 允许标题留空（以正文首行作为列表标题展示）
4. IF 用户在新建状态下未输入任何内容就返回，系统 SHALL 丢弃该条空白笔记
5. WHEN 笔记保存成功，系统 SHALL 记录创建时间和最后更新时间

### Requirement 3: 笔记列表

**User Story:** AS 用户, I want 在列表中浏览我的所有笔记, so that 快速找到需要的内容

#### Acceptance Criteria

1. WHEN 用户进入列表页，系统 SHALL 按置顶优先、更新时间倒序展示所有笔记
2. WHEN 笔记超过一屏，系统 SHALL 支持滚动加载
3. WHEN 用户在搜索框输入关键词，系统 SHALL 实时筛选标题或正文包含关键词的笔记
4. WHILE 列表为空，系统 SHALL 展示空状态提示引导用户创建第一条笔记
5. WHEN 列表加载中，系统 SHALL 展示加载状态

### Requirement 4: 编辑笔记

**User Story:** AS 用户, I want 修改已有笔记的内容, so that 保持信息最新

#### Acceptance Criteria

1. WHEN 用户点击列表中的笔记，系统 SHALL 打开该笔记的编辑界面
2. WHEN 用户修改内容并保存，系统 SHALL 更新云端数据并刷新列表顺序
3. WHEN 用户修改内容后未保存就返回，系统 SHALL 提示未保存的更改（丢弃/保存）

### Requirement 5: 删除笔记

**User Story:** AS 用户, I want 删除不再需要的笔记, so that 列表保持整洁

#### Acceptance Criteria

1. WHEN 用户在编辑界面点击删除，系统 SHALL 弹出确认对话框
2. WHEN 用户确认删除，系统 SHALL 从云端数据库删除该笔记并返回列表
3. IF 用户取消删除，系统 SHALL 保留笔记并停留在编辑界面

### Requirement 6: 置顶与颜色标记

**User Story:** AS 用户, I want 置顶重要笔记并用颜色区分, so that 重要信息一眼可见

#### Acceptance Criteria

1. WHEN 用户在编辑界面切换置顶开关并保存，系统 SHALL 将该笔记置顶显示
2. WHEN 用户选择颜色标记并保存，系统 SHALL 在列表中以对应颜色标识该笔记
3. WHEN 多条笔记均置顶，系统 SHALL 按更新时间倒序排列置顶笔记

### Requirement 7: 云同步

**User Story:** AS 用户, I want 数据保存在云端, so that 在手机和电脑上都能看到相同的笔记

#### Acceptance Criteria

1. WHEN 用户在任意设备登录同一账户，系统 SHALL 展示该账户的全部笔记
2. WHEN 用户在任一设备修改笔记，系统 SHALL 将变更持久化到 Supabase 数据库
3. WHEN 用户在其他设备打开应用，系统 SHALL 展示最新已同步的数据
4. WHILE 网络请求失败，系统 SHALL 显示同步失败提示并保留本地未保存内容

### Requirement 8: PWA 安装与离线查看

**User Story:** AS 用户, I want 将应用添加到手机桌面并离线查看笔记, so that 像原生 App 一样使用

#### Acceptance Criteria

1. WHEN 用户通过浏览器"添加到主屏幕"，系统 SHALL 以应用名称和图标安装到桌面
2. WHILE 应用已安装，系统 SHALL 以独立窗口（无浏览器地址栏）运行
3. WHILE 用户离线，系统 SHALL 展示最近缓存的笔记（只读）
4. IF 用户离线时尝试新建或编辑，系统 SHALL 提示需要联网后操作

### Requirement 9: 数据安全

**User Story:** AS 用户, I want 我的笔记只有我自己能看到, so that 隐私得到保护

#### Acceptance Criteria

1. WHILE 数据库启用行级安全（RLS），系统 SHALL 限制每条数据的读写权限为数据所有者
2. IF 未登录用户发起任何数据请求，系统 SHALL 拒绝访问
3. WHEN 用户退出登录，系统 SHALL 清除本地会话并返回登录界面

## 已确认决策

- 内容形式：纯文本（2026-09-05 确认）
- 离线能力：只读离线，离线时新建/编辑提示需联网（2026-09-05 确认）
- 信息结构：备忘与记事统一为"笔记"，通过置顶和颜色区分（2026-09-05 确认）
