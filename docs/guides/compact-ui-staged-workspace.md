# 第二组暂存：紧凑工作台

## 范围

基线：`50033ffd`（已包含分栏与编辑器基础能力）。

仅暂存 9 个文件：

- components/KnowledgeSeven.vue
- styles/knowledge-seven.css
- utils/knowledgeDensity.js
- utils/knowledgeFeedback.js
- views/Knowledge/Index.vue
- tests/knowledgeActions.test.js
- tests/knowledgeCompact.test.js
- tests/knowledgeDensity.test.js
- tests/knowledgeFeedback.test.js

以上路径均相对于 `frontend/`。包括紧凑列表、密度、筛选、目录与笔记操作、删除保护、加载错误反馈、键盘操作、分栏调用、深色与长内容适配。

本组未提交，未纳入备份、MCP、设置页与后端。

## 跨组依赖处理

暂存版工作台沿用外观弹窗事件，备份链接指向基线中已有的 `/admin/backup`，不依赖未提交的 `/settings/appearance` 和 `/settings/backup`。

工作区文件没有被改写，仍保留最终设置页导航。因此 KnowledgeSeven.vue 和 Index.vue 同时存在已暂存和未暂存差异，这是有意拆分，不应通过整文件 `git add` 覆盖本组索引。

暂存补丁 SHA-256：`d9e2d3571406f77606f88a7d1af766f86a25cf598e05d1baaeb0dca35c58b630`。

## 隔离验证

临时目录：`/tmp/qblog-stage-workspace-ELnl78`。

使用 `git archive HEAD` 与暂存补丁构建，没有纳入其余工作区文件。

- npm ci：成功。
- 全量 lint：0 错误，基线其他文件 164 条警告。
- 本组 JS/Vue 文件 ESLint --max-warnings 0：通过。
- 52 项测试通过。
- 生产构建成功，仍有编辑器分包大小警告。
- 暂存差异格式检查通过。

在隔离版本的 localhost:3014 上连接本地后端，只读取现有数据，并用 GET 拦截模拟失败：

- 列表方向键、Shift+F10、菜单方向键、Esc 返回焦点。
- 无结果时清除条件。
- 打开失败保留草稿、返回当前笔记与恢复后重试。
- 列表失败后重试。
- 手机列表反馈无横向溢出。

首次浏览器运行出现一次 Vue `Cannot read properties of null (reading 'ce')` 异常；随后启用堆栈采集重跑，并再连续重跑三次，均通过、没有脚本异常。该偶发问题未能复现，不能视为已定位修复，后续浏览器验收需继续观察。

本轮未写入现有笔记、未提交、未推送、未部署。建议提交说明：`feat(workspace): streamline notes and improve interaction feedback`。
