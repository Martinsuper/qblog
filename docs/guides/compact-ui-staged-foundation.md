# 第一组暂存：工作台基础能力

## 暂存范围

基线：`911ce349c6b9edb5fc7b3719f20c238835d3ec6f`。

仅暂存以下 3 个文件，未提交：

- `frontend/src/components/KnowledgeEditor.vue`：可选 toolbarTarget、Teleport 挂载、工具栏点击保留编辑器选择、按钮颜色使用主题变量；不传 toolbarTarget 时维持内联挂载。
- `frontend/src/utils/knowledgePaneLayout.js`：分栏宽度约束与正文最小空间计算。
- `frontend/tests/knowledgePaneLayout.test.js`：宽度、折叠恢复、窄窗口计算测试。

这组能力来自当前工作区已有的基础改动，为紧凑工作台提供前置支持；没有纳入工作台调用方、备份、MCP、设置页或后端内容。

暂存补丁 SHA-256：`ec0ad995427d3ef0dd0f4f016a4b9354af9ec5b6bf28510fe030bcebfe15a1f2`。

可复核：

```sh
git diff --cached --name-only
git diff --cached --check
git diff --cached --binary | shasum -a 256
```

后续暂存操作会改变补丁摘要，文档记录的是本次验证输入。

## 独立验证

临时目录：`/tmp/qblog-stage-foundation-TXy04W`。

以 `git archive HEAD` 导出基线，再应用暂存补丁；没有复制未暂存工作区，也没有使用原 node_modules。

执行结果：

- `npm ci`：成功。
- `npm run lint`：退出码 0；0 错误、164 条警告，警告位于本组之外的基线文件。
- 对本组 3 个文件执行 ESLint `--max-warnings 0`：通过。
- `npm test`：37 项全部通过。
- `npm run build`：成功；仍有编辑器约 515 kB 的分包警告。
- `git diff --cached --check`：通过。

完整工作区有 67 项测试，第一组隔离环境只有 37 项，是因为尚未纳入其他未暂存测试；不是丢失或跳过测试。

## 后续

建议提交说明：`feat(workspace): add pane layout helpers and toolbar mounting`。

未自动执行 commit。暂存区应保留本组作为待审查单元；其他组不要混入后直接提交。提交前若 HEAD 或暂存内容发生变化，需要重新检查并验证。

后续工作台与设置提交仍需处理备份路由及相关前置业务依赖，不能因本组验证通过而跳过。
