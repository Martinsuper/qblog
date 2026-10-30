# 紧凑 UI 提交拆分方案

## 审查结论

当前分支基线为 `911ce349`。工作区存在本轮开始前的后端、备份、品牌、路由、编辑器及样式改动；不能把 `git diff HEAD` 全部归为本轮 UI 工作，也无法仅凭最终差异精确重建开始时的补丁。

本方案不暂存、不提交、不还原任何文件。禁止直接 `git add .`。以下是建议的逻辑边界，不是已生成的可独立应用补丁；混合文件必须逐块审查，拆分后每个提交都要重新验证。

## 已确认的依赖

| 使用方 | 必需依赖 | 当前风险 |
|---|---|---|
| KnowledgeSeven | `utils/knowledgePaneLayout.js` | 新文件，尚未跟踪；遗漏会导致编译失败 |
| KnowledgeSeven | KnowledgeEditor 的 `toolbarTarget` 属性和 Teleport 工具栏 | 属于此前编辑器改动，不能只提交调用方 |
| KnowledgeSeven | knowledgeDensity、knowledgeFeedback | 本轮新文件，需与使用方一起提交 |
| Appearance | SettingsLayout、knowledgePreferences、knowledgeDensity、settings store | store 另有此前修改，需区分必要与非必要差异 |
| SettingsLayout | `/settings/appearance` 等路由及各设置页 | 不能遗漏新页面和相应路由 |
| Backup | api/backup、api/knowledge、backupSchedule | 整个页面此前未跟踪，并非本轮全新业务实现 |
| Backup Markdown 导出 | KnowledgeBackupController、KnowledgeMarkdownExportService 及测试 | 运行中服务已支持不等于目标环境已支持，不能纯前端发布后假定接口存在 |
| Backup 定时设置 | BackupSettingsDTO、BackupServiceImpl 等后端改动 | 频率字段、校验和持久化需要兼容性验收 |
| Tokens | 此前 MCP 文章和目录工具相关实现 | 本轮只精简布局，不能把已有工具说明误认为独立 UI 变更 |

## 建议顺序

### A. 前置能力：备份与 MCP（单独审核，不冒充 UI 提交）

建议按业务继续拆分备份与 MCP 两组。范围候选：

- 后端 KnowledgeBackupController、KnowledgeMarkdownExportService 及测试。
- BackupSettingsDTO、BackupServiceImpl、JsonBackupExporter/Importer 及相关测试。
- McpController、McpSecurityTest 和相关安全测试。
- 前端 api/backup、api/knowledge、backupSchedule 及测试。
- 新 Backup 页的原有业务实现。
- 原后台备份入口迁移：router 中 backup 路由、AdminLayout、旧 BackupManage 删除。

上述是候选关联范围，不代表所有差异都必需。后端应独立运行 Maven 测试、审核恢复语义与安全权限；本轮 UI 的前端测试不能替代这项验收。

### B. 工作台与编辑器基础

建议提交说明：`feat(workspace): refine resizable knowledge panes and editor toolbar`

- knowledgePaneLayout 与测试。
- KnowledgeEditor 工具栏挂载相关差异。
- KnowledgeSeven、knowledge-seven.css 中分栏及对应基础差异。
- Index 中目录和笔记操作、删除保护及相关 knowledgeActions 测试。

这些文件与 C 组高度交织。若逐块拆分不能保持构建和行为正确，允许 B+C 合并为一个连贯提交，但不要为了拆分而重写现有工作区。

### C. 紧凑工作台与反馈

建议提交说明：`feat(workspace): streamline note list and improve feedback`

- KnowledgeSeven 和 knowledge-seven.css 的信息精简、密度、目录、键盘、深色及长内容修复。
- knowledgeDensity、knowledgeFeedback 和对应测试。
- Index 的 noteError、失败重试、返回原笔记逻辑。
- knowledgeCompact 测试、knowledgeActions 中对应断言。

### D. 统一设置

建议提交说明：`feat(settings): unify compact settings navigation and appearance`

- SettingsLayout、Appearance、settingsNavigation 测试。
- Profile 的只读资料、实时刷新、保留密码功能，移除无效保存入口。
- Tokens 与 Backup 的设置布局及说明精简差异；保留 A 组业务能力。
- router 中 `/settings` 与 `/settings/appearance` 路由。
- KnowledgeSeven 的外观导航入口。

D 依赖 C 的密度工具，以及 A 的备份页面和路由。若目标是 UI-only 发布，需要另行制作兼容旧备份入口的补丁，不能直接按当前文件整体提交。

### E. 文档和验收材料

- knowledge-compact-ui、settings-compact-ui、knowledge-feedback-ui 文档。
- compact-ui-release-checklist 与本方案。
- `docs/verification/compact-ui/` 截图。

截图和文档不得包含真实凭证或用户正文；当前边界截图使用合成内容。

## 默认不并入本轮 UI 的内容

除非经独立审核明确需要：docker/nginx.conf、start.sh、application.yml、包开发脚本、Vite host 配置、品牌替换、博客页面批量样式、原型文件、其他截图、API 超时及 MCP/备份业务扩展均不应因“UI 优化”自动纳入。

其中一些可能是目标部署必需配置，应在部署提交中处理，而不是简单丢弃。不要还原这些已有改动。

## 操作与验收

1. 先决定是否连同 A 组业务能力发布；若否，先准备兼容补丁。
2. 保存当前工作区及未跟踪文件的可靠备份，不使用会漏掉未跟踪文件的仅 `git diff` 备份方式。
3. 使用 `git diff -- <path>` 查看上下文；对混合文件用交互式逐块审查，不盲目整文件暂存。
4. 每组暂存后检查 `git diff --cached --stat`、`git diff --cached --check` 和完整暂存差异。
5. 在只含目标提交内容的隔离环境运行前端 lint、测试、构建；不能拿混合工作区测试通过证明拆分提交可用。
6. 包含后端则另跑后端测试，再验证目标环境接口。
7. 提交和部署需明确确认，当前均未执行。

本轮已通过的 67 项测试与构建针对当前完整工作区；尚未验证上述拆分后的各个提交。
