# 第三组暂存：统一设置（兼容既有备份入口）

基线：`ae38f495`。未提交、未推送、未部署。

## 8 个暂存文件

相对 frontend/：

- src/components/KnowledgeSeven.vue：外观菜单进入新设置路由。
- src/components/SettingsLayout.vue：统一导航；本组备份入口保留 `/admin/backup`。
- src/router/index.js：只新增 `/settings` 重定向和 `/settings/appearance`，不迁移备份路由。
- src/stores/settings.js：文章渲染风格持久化，存储异常回退。
- src/views/Knowledge/Appearance.vue：布局、配色、密度与文章渲染风格。
- src/views/Knowledge/Tokens.vue：接入统一布局、精简概览、格式化；未纳入 MCP 目录工具新说明。
- src/views/User/Profile.vue：只读资料、实时刷新和密码表单保留。
- tests/settingsNavigation.test.js：匹配当前组的独立路由与导航，不要求新 Backup 页。

本组没有纳入新 Backup 页、备份 API、后台入口迁移或任何后端变化。工作区最终版本保持原样，因此部分文件同时存在已暂存和未暂存差异，请不要整文件重新暂存覆盖该边界。

补丁 SHA-256：`0f6f55dfc3761bbc746352e9d6c89e11964f2081ab8acf88d1e3a8081d115fb3`。

## 独立验证

目录：`/tmp/qblog-stage-settings-GBXHnZ`，以 HEAD 导出后仅应用暂存补丁，独立 npm ci。

- 全量 lint：零错误、零警告。
- 55 项测试通过。
- 生产构建成功；编辑器分包仍约 515 kB。
- 暂存差异格式检查通过。
- localhost:3015 隔离前端，读取运行中后端，验证外观、MCP、账号路由，桌面/手机导航，主题与密度持久化，页面无横向溢出，空密码校验及备份链接指向已有路由。
- 未实际修改密码、创建令牌或更改备份。

首次浏览器运行出现一次 `Cannot read properties of undefined (reading 'path')`，当时测试继续执行后在最终异常检查捕获；启用堆栈采集后重跑并额外连续运行三次，均无脚本错误。该异常未复现，不能认为已定位修复；与此前启动开发服务后立即首次访问的偶发异常一起作为待观察项。

建议提交说明：`feat(settings): unify appearance and account navigation`。

后续备份迁移仍需独立审查与测试，不因设置组通过而自动批准。
