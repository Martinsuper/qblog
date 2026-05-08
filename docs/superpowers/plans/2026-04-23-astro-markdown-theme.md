# Astro Markdown 主题实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 QBlog 新增 Astro 风格的 Markdown 渲染主题，复刻 Tailwind Typography 的视觉效果。

**Architecture:** 纯 CSS 方案，复用现有 markdown-it + highlight.js 渲染器，新增 astro.css 样式文件并通过 settings store 扩展主题选项。

**Tech Stack:** Vue 3, CSS, Pinia, markdown-it, highlight.js

---

## Task 1: 创建 Astro 主题 CSS 文件

**Files:**
- Create: `src/styles/markdown/astro.css`

- [ ] **Step 1: 创建 astro.css 文件**

创建 `src/styles/markdown/astro.css`，包含完整的 Astro 主题样式：

```css
/* ========================================
   Astro 风格样式（复刻 Tailwind Typography）
   ======================================== */

/* 基础容器 */
.markdown-body.astro-theme {
  font-size: 16px;
  line-height: 1.75;
  color: var(--text-primary);
  max-width: 720px;
}

/* 标题样式 - 简洁，H1/H2 有微妙底部边框 */
.markdown-body.astro-theme h1 {
  font-size: 2.25em;
  font-weight: 700;
  margin-top: 0;
  margin-bottom: 0.8888889em;
  line-height: 1.1;
  color: var(--text-primary);
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.3em;
}

.markdown-body.astro-theme h2 {
  font-size: 1.5em;
  font-weight: 700;
  margin-top: 2em;
  margin-bottom: 1em;
  line-height: 1.3;
  color: var(--text-primary);
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.3em;
}

.markdown-body.astro-theme h3 {
  font-size: 1.25em;
  font-weight: 600;
  margin-top: 1.6em;
  margin-bottom: 0.6em;
  line-height: 1.4;
  color: var(--text-primary);
}

.markdown-body.astro-theme h4 {
  font-size: 1em;
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  color: var(--text-primary);
}

.markdown-body.astro-theme h5,
.markdown-body.astro-theme h6 {
  font-size: 0.875em;
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  color: var(--text-secondary);
}

/* 段落 */
.markdown-body.astro-theme p {
  margin-top: 0;
  margin-bottom: 1.25em;
}

/* 链接 - 蓝色文字，hover 下划线 */
.markdown-body.astro-theme a {
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.15s ease;
}

.markdown-body.astro-theme a:hover {
  text-decoration: underline;
  color: #2563eb;
}

/* 列表 */
.markdown-body.astro-theme ul {
  list-style-type: disc;
  margin-top: 1.25em;
  margin-bottom: 1.25em;
  padding-left: 1.625em;
}

.markdown-body.astro-theme ol {
  list-style-type: decimal;
  margin-top: 1.25em;
  margin-bottom: 1.25em;
  padding-left: 1.625em;
}

.markdown-body.astro-theme ul ul {
  list-style-type: circle;
}

.markdown-body.astro-theme ul ul ul {
  list-style-type: square;
}

.markdown-body.astro-theme ol ol {
  list-style-type: lower-alpha;
}

.markdown-body.astro-theme ol ol ol {
  list-style-type: lower-roman;
}

.markdown-body.astro-theme li {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

.markdown-body.astro-theme li::marker {
  color: #6b7280;
}

/* 引用块 - 左侧细灰线，无背景 */
.markdown-body.astro-theme blockquote {
  margin-top: 1.6em;
  margin-bottom: 1.6em;
  padding-left: 1em;
  padding-right: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-left: 4px solid #d0d7de;
  color: #6b7280;
  font-style: italic;
  background: transparent;
}

.markdown-body.astro-theme blockquote p {
  margin: 0;
}

/* 代码块 - 圆角边框，浅色背景 */
.markdown-body.astro-theme pre {
  margin-top: 1.5em;
  margin-bottom: 1.5em;
  padding: 1em 1.25em;
  background: #f6f8fa;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.875em;
  line-height: 1.7;
}

.markdown-body.astro-theme pre code {
  background: transparent;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
  color: #24292f;
}

/* 行内代码 - 小圆角，浅灰背景 */
.markdown-body.astro-theme code:not(.hljs) {
  background: rgba(175, 184, 193, 0.2);
  color: inherit;
  padding: 0.2em 0.4em;
  border-radius: 6px;
  font-size: 0.875em;
  font-weight: 400;
}

/* 表格 - 无外边框，单元格细边框，隔行变色 */
.markdown-body.astro-theme table {
  width: 100%;
  table-layout: auto;
  text-align: left;
  margin-top: 2em;
  margin-bottom: 2em;
  border-collapse: collapse;
  border: none;
}

.markdown-body.astro-theme thead {
  border-bottom: 1px solid #d0d7de;
}

.markdown-body.astro-theme th {
  font-weight: 600;
  padding: 0.5714286em 0.8571429em;
  border: 1px solid #d0d7de;
  background: #f6f8fa;
}

.markdown-body.astro-theme td {
  padding: 0.5714286em 0.8571429em;
  border: 1px solid #d0d7de;
}

.markdown-body.astro-theme tbody tr {
  border-bottom: 1px solid #d0d7de;
}

.markdown-body.astro-theme tbody tr:last-child {
  border-bottom: none;
}

.markdown-body.astro-theme tbody tr:nth-child(odd) {
  background: transparent;
}

.markdown-body.astro-theme tbody tr:nth-child(even) {
  background: #f6f8fa;
}

/* 图片 - 圆角，居中 */
.markdown-body.astro-theme img {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin-top: 2em;
  margin-bottom: 2em;
}

/* 水平线 */
.markdown-body.astro-theme hr {
  border: none;
  height: 2px;
  background: #e5e7eb;
  margin-top: 3em;
  margin-bottom: 3em;
}

/* 删除线 */
.markdown-body.astro-theme del {
  color: #9ca3af;
}

/* 自定义容器 - 简洁边框风格 */
.markdown-body.astro-theme .custom-container {
  margin-top: 1.5em;
  margin-bottom: 1.5em;
  padding: 1em 1.25em;
  border-radius: 6px;
  border-left: none;
  border: 1px solid;
}

.markdown-body.astro-theme .custom-container-title {
  font-size: 0.875em;
  font-weight: 600;
  margin-bottom: 0.5em;
  display: flex;
  align-items: center;
  gap: 0.5em;
}

.markdown-body.astro-theme .custom-container-title svg {
  width: 1.125em;
  height: 1.125em;
}

.markdown-body.astro-theme .custom-container-content {
  font-size: 0.9375em;
  line-height: 1.6;
}

/* tip 容器 */
.markdown-body.astro-theme .custom-container.tip {
  background: #f0fff4;
  border-color: #42b983;
}

.markdown-body.astro-theme .custom-container.tip .custom-container-title {
  color: #22863a;
}

.markdown-body.astro-theme .custom-container.tip .custom-container-content {
  color: #2a5c3a;
}

/* warning 容器 */
.markdown-body.astro-theme .custom-container.warning {
  background: #fffbdd;
  border-color: #e7c000;
}

.markdown-body.astro-theme .custom-container.warning .custom-container-title {
  color: #b08800;
}

.markdown-body.astro-theme .custom-container.warning .custom-container-content {
  color: #7a5a00;
}

/* danger 容器 */
.markdown-body.astro-theme .custom-container.danger {
  background: #ffeef0;
  border-color: #cc0000;
}

.markdown-body.astro-theme .custom-container.danger .custom-container-title {
  color: #cb2431;
}

.markdown-body.astro-theme .custom-container.danger .custom-container-content {
  color: #8a1a1a;
}

/* info 容器 */
.markdown-body.astro-theme .custom-container.info {
  background: #f1f8ff;
  border-color: #3b82f6;
}

.markdown-body.astro-theme .custom-container.info .custom-container-title {
  color: #0366d6;
}

.markdown-body.astro-theme .custom-container.info .custom-container-content {
  color: #1a4d6d;
}

/* PlantUML 图表 */
.markdown-body.astro-theme .plantuml-diagram {
  margin-top: 2em;
  margin-bottom: 2em;
  padding: 1.5em;
  background: #f6f8fa;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  text-align: center;
}

.markdown-body.astro-theme .plantuml-diagram img {
  margin: 0;
}

/* ========================================
   深色模式适配
   ======================================== */

[data-theme="dark"] .markdown-body.astro-theme {
  color: var(--text-primary);
}

[data-theme="dark"] .markdown-body.astro-theme h1,
[data-theme="dark"] .markdown-body.astro-theme h2 {
  border-bottom-color: #30363d;
}

[data-theme="dark"] .markdown-body.astro-theme a {
  color: #60a5fa;
}

[data-theme="dark"] .markdown-body.astro-theme a:hover {
  color: #93c5fd;
}

[data-theme="dark"] .markdown-body.astro-theme li::marker {
  color: #9ca3af;
}

[data-theme="dark"] .markdown-body.astro-theme blockquote {
  border-left-color: #3b3b3b;
  color: #9ca3af;
}

[data-theme="dark"] .markdown-body.astro-theme pre {
  background: #161b22;
  border-color: #30363d;
}

[data-theme="dark"] .markdown-body.astro-theme pre code {
  color: #c9d1d9;
}

[data-theme="dark"] .markdown-body.astro-theme code:not(.hljs) {
  background: rgba(110, 118, 129, 0.4);
  color: #c9d1d9;
}

[data-theme="dark"] .markdown-body.astro-theme thead {
  border-bottom-color: #30363d;
}

[data-theme="dark"] .markdown-body.astro-theme th {
  border-color: #30363d;
  background: #161b22;
}

[data-theme="dark"] .markdown-body.astro-theme td {
  border-color: #30363d;
}

[data-theme="dark"] .markdown-body.astro-theme tbody tr {
  border-bottom-color: #30363d;
}

[data-theme="dark"] .markdown-body.astro-theme tbody tr:nth-child(even) {
  background: #161b22;
}

[data-theme="dark"] .markdown-body.astro-theme hr {
  background: #30363d;
}

[data-theme="dark"] .markdown-body.astro-theme del {
  color: #6b7280;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.tip {
  background: rgba(46, 160, 67, 0.15);
  border-color: #238636;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.tip .custom-container-title {
  color: #3fb950;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.tip .custom-container-content {
  color: #7ee787;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.warning {
  background: rgba(210, 153, 34, 0.15);
  border-color: #9e6a03;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.warning .custom-container-title {
  color: #d29922;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.warning .custom-container-content {
  color: #e3b341;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.danger {
  background: rgba(248, 81, 73, 0.15);
  border-color: #da3633;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.danger .custom-container-title {
  color: #f85149;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.danger .custom-container-content {
  color: #ffa198;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.info {
  background: rgba(56, 139, 253, 0.15);
  border-color: #1f6feb;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.info .custom-container-title {
  color: #58a6ff;
}

[data-theme="dark"] .markdown-body.astro-theme .custom-container.info .custom-container-content {
  color: #79c0ff;
}

[data-theme="dark"] .markdown-body.astro-theme .plantuml-diagram {
  background: #161b22;
  border-color: #30363d;
}

/* 深色模式代码高亮 - GitHub Dark 风格 */
[data-theme="dark"] .markdown-body.astro-theme .hljs {
  background: #161b22;
  color: #c9d1d9;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-comment,
[data-theme="dark"] .markdown-body.astro-theme .hljs-quote {
  color: #8b949e;
  font-style: italic;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-keyword,
[data-theme="dark"] .markdown-body.astro-theme .hljs-selector-tag,
[data-theme="dark"] .markdown-body.astro-theme .hljs-addition {
  color: #ff7b72;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-number,
[data-theme="dark"] .markdown-body.astro-theme .hljs-string,
[data-theme="dark"] .markdown-body.astro-theme .hljs-meta .hljs-meta-string,
[data-theme="dark"] .markdown-body.astro-theme .hljs-regexp,
[data-theme="dark"] .markdown-body.astro-theme .hljs-link {
  color: #a5d6ff;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-title,
[data-theme="dark"] .markdown-body.astro-theme .hljs-section {
  color: #d2a8ff;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-attr,
[data-theme="dark"] .markdown-body.astro-theme .hljs-variable,
[data-theme="dark"] .markdown-body.astro-theme .hljs-template-variable,
[data-theme="dark"] .markdown-body.astro-theme .hljs-type {
  color: #ffa657;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-symbol,
[data-theme="dark"] .markdown-body.astro-theme .hljs-bullet,
[data-theme="dark"] .markdown-body.astro-theme .hljs-meta,
[data-theme="dark"] .markdown-body.astro-theme .hljs-selector-id,
[data-theme="dark"] .markdown-body.astro-theme .hljs-selector-class,
[data-theme="dark"] .markdown-body.astro-theme .hljs-built_in {
  color: #79c0ff;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-deletion {
  color: #ffdcd7;
  background-color: #67060c;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-addition {
  color: #aff5b4;
  background-color: #033a16;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-emphasis {
  font-style: italic;
}

[data-theme="dark"] .markdown-body.astro-theme .hljs-strong {
  font-weight: 700;
}
```

- [ ] **Step 2: 验证 CSS 文件创建成功**

```bash
ls -la src/styles/markdown/astro.css
```

Expected: 文件存在，大小约 10KB+

---

## Task 2: 导入 Astro 主题样式

**Files:**
- Modify: `src/styles/markdown/index.css:1-11`

- [ ] **Step 1: 修改 index.css，添加 astro.css 导入**

在 `src/styles/markdown/index.css` 中添加 `@import './astro.css';`：

```css
/* Markdown 样式入口 */

/* 导入 highlight.js 代码高亮主题 */
@import 'highlight.js/styles/atom-one-light.css';

/* 导入基础样式 */
@import './base.css';

/* 导入主题样式 */
@import './vuepress.css';
@import './github.css';
@import './astro.css';
```

- [ ] **Step 2: 验证导入顺序正确**

确保导入顺序为：base.css → vuepress.css → github.css → astro.css，这样 astro 主题可以正确覆盖基础样式。

---

## Task 3: 扩展 settings store 主题选项

**Files:**
- Modify: `src/stores/settings.js:6,9,19`

- [ ] **Step 1: 修改 settings.js，新增 astro 主题**

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  // Markdown 渲染主题：'vuepress' | 'github' | 'astro'
  const markdownTheme = ref('vuepress')

  // 切换 Markdown 主题
  function setMarkdownTheme(theme) {
    if (['vuepress', 'github', 'astro'].includes(theme)) {
      markdownTheme.value = theme
    }
  }

  return {
    markdownTheme,
    setMarkdownTheme
  }
})
```

- [ ] **Step 2: 验证 store 定义正确**

检查文件内容确保：
- `markdownTheme` 注释包含 'astro'
- `setMarkdownTheme` 函数的数组包含 'astro'

---

## Task 4: 提交代码

- [ ] **Step 1: 查看变更状态**

```bash
git status
```

Expected: 显示 astro.css（新文件）、index.css（修改）、settings.js（修改）

- [ ] **Step 2: 提交变更**

```bash
git add src/styles/markdown/astro.css src/styles/markdown/index.css src/stores/settings.js
git commit -m "$(cat <<'EOF'
feat(markdown): add astro theme

新增 Astro 风格的 Markdown 渲染主题，复刻 Tailwind Typography 的视觉效果：
- 样式特点：简洁标题、圆角代码块、细边框引用块、隔行表格
- 支持深色模式，使用 GitHub Dark 风格代码高亮
- 用户可在设置中切换 vuepress/github/astro 三种主题

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: 验证提交成功**

```bash
git log -1 --oneline
```

Expected: 显示最新提交记录

---

## Task 5: 测试验证

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd frontend && npm run dev
```

- [ ] **Step 2: 手动测试主题切换**

1. 打开浏览器访问 http://localhost:3001
2. 进入任意文章详情页
3. 打开开发者工具，在 Console 执行：
   ```javascript
   const settings = window.__PINIA_STATE__?.settings || localStorage.getItem('settings')
   // 手动切换到 astro 主题测试
   ```
4. 或者检查设置面板是否有主题切换选项（如有 UI）

- [ ] **Step 3: 检查样式渲染效果**

在浏览器开发者工具中：
1. 检查文章内容容器是否有 `astro-theme` 类
2. 检查标题是否有底部边框
3. 检查代码块是否有圆角和边框
4. 切换深色模式验证适配效果

---

## 验收标准

- [ ] astro.css 文件创建成功，样式完整
- [ ] index.css 正确导入 astro.css
- [ ] settings.js 包含 'astro' 主题选项
- [ ] 主题切换功能正常工作
- [ ] 深色模式正确适配
- [ ] 代码已提交