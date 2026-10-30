# 紧凑 UI 发布前检查清单

## 状态

本轮实现已分组提交；最新结果与提交列表见 `compact-ui-delivery.md`。未推送、未部署。下文保留各阶段检查记录，真实设备、Nginx 容器配置和敏感流程隔离验收仍待发布方执行。当前工作区仍有此前的无关改动，不应直接全量提交。

## 本轮主要文件

工作台：
- `frontend/src/components/KnowledgeSeven.vue`
- `frontend/src/styles/knowledge-seven.css`
- `frontend/src/views/Knowledge/Index.vue`
- `frontend/src/utils/knowledgeDensity.js`
- `frontend/src/utils/knowledgeFeedback.js`

设置：
- `frontend/src/components/SettingsLayout.vue`
- `frontend/src/views/Knowledge/Appearance.vue`
- `frontend/src/views/Knowledge/Backup.vue`（该文件包含此前未提交的备份实现，本轮只接入设置布局）
- `frontend/src/views/Knowledge/Tokens.vue`
- `frontend/src/views/User/Profile.vue`
- `frontend/src/router/index.js`（还包含此前路由修改）

测试：`knowledgeCompact`、`knowledgeDensity`、`knowledgeFeedback`、`settingsNavigation`，以及适配后的 `knowledgeActions` 测试。

以上部分文件在本轮开始前已经有改动，发布时需要逐块审查差异及依赖，不宜仅按文件列表盲目暂存。

## 已执行

- [x] 前端全量 ESLint：`npm run lint`。
- [x] 前端单元测试：67 项通过。
- [x] 生产构建：成功；编辑器分包仍有超过 500 kB 的警告。
- [x] `git diff --check`。
- [x] 真实后端标题、正文自动保存及直接读取持久化结果。
- [x] 搜索、收件箱切换、Markdown 下载、取消及确认删除临时笔记。
- [x] 设置路由、导航、外观与密度持久化、密码空表单校验。
- [x] GET 请求失败模拟：列表重试、打开失败保留草稿、恢复后重试。
- [x] 列表键盘导航、Shift+F10 菜单、Esc 返回焦点。
- [x] 深色工作台：超长无空格标题、小标题、摘要、标签、三级目录及表格。
- [x] 1440、1024、768、390、320 像素宽度下正文阅读区无横向溢出。
- [x] 桌面与手机设置页无页面横向溢出，浏览器无脚本异常。

## 本次边界修复

- 超长正文目录标题正确换行，不再撑宽阅读区域。
- 超长标签限制宽度并换行，目录名称与路径保持截断。
- 窄屏路径不再挤压工具栏按钮。
- 深色目录边线、目录选中按钮、账号菜单按钮和保存警告使用适配颜色。

## 截图

`docs/verification/compact-ui/`：
- `dark-desktop.png`：深色桌面长内容。
- `dark-narrow.png`：深色 320 像素窄屏。
- `settings-desktop.png`：桌面外观设置。
- `settings-mobile.png`：手机备份设置。

边界检查使用浏览器 GET 响应替换，未向数据库写入极端内容。真实联调临时笔记已清理。

## 发布前仍需确认

- [ ] 在真实 Safari/iOS 和 Android 触屏设备上检查软键盘、滚动和下载。
- [ ] 浏览器 200% 缩放与读屏工具人工验收（窄视口检查不等价于实际缩放）。
- [ ] 若发布包含后端未提交改动，单独执行后端测试并审核数据库迁移与兼容性。
- [ ] 恢复备份、导入、令牌创建撤销、真实修改密码等敏感流程在隔离环境验收。
- [ ] 确认最终发布范围；保留回滚构建，再进行部署。

本地联调请使用 `http://localhost:3001/blog/`，不要将 `127.0.0.1` 的 CORS 拒绝误判为自动保存逻辑故障。
