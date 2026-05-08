---
name: Astro Markdown 主题设计
description: 为 QBlog 新增 Astro 风格的 Markdown 渲染主题
type: project
---

# Astro Markdown 主题设计

## 背景

QBlog 目前支持两种 Markdown 渲染主题：vuepress 和 github。用户希望新增一个 "astro" 主题，渲染效果参考 https://fblog.younote.top/posts/maven-guide.md/，该网站使用 Tailwind Typography (`prose` 类) 实现。

## 目标

新增 `astro` 主题选项，复刻 Tailwind Typography 的视觉风格，保持与现有主题切换架构一致。

## 实现方案

采用纯 CSS 方案，不引入新依赖，复用现有的 `markdown-it` + `highlight.js` 渲染器。

## 文件变更

### 新增文件

- `src/styles/markdown/astro.css` — Astro 主题样式

### 修改文件

- `src/styles/markdown/index.css` — 导入 `astro.css`
- `src/stores/settings.js` — 新增 `astro` 主题选项

## 样式设计

### 元素样式对照表

| 元素 | Astro 主题样式 |
|------|----------------|
| **标题** | 简洁，H1/H2 有微妙底部边框（`border-bottom: 1px solid #e5e7eb`），无左侧锚点 |
| **段落** | 行高 1.75，`margin-bottom: 1.25em` |
| **代码块** | 圆角 `6px`，浅色背景 `#f6f8fa`，边框 `1px solid #d0d7de` |
| **行内代码** | 圆角 `6px`，背景 `rgba(175,184,193,0.2)`，文字颜色继承 |
| **引用块** | 左侧细灰线 `4px solid #d0d7de`，无背景色，文字灰色 `#6b7280` |
| **表格** | 无外边框，单元格边框 `1px solid #d0d7de`，隔行背景 `#f6f8fa` |
| **链接** | 颜色 `#3b82f6`，hover 下划线 |
| **列表** | `list-style: disc/decimal`，间距 `margin: 1.25em 0` |
| **图片** | 圆角 `6px`，居中，`margin: 2em 0` |
| **自定义容器** | 简洁边框风格，无左侧粗线 |

### 深色模式适配

使用 `[data-theme="dark"]` 选择器：

| 元素 | 深色模式样式 |
|------|-------------|
| **标题边框** | `#30363d` |
| **代码块背景** | `#161b22`，边框 `#30363d` |
| **行内代码** | 背景 `rgba(110,118,129,0.4)` |
| **引用块边框** | `#3b3b3b` |
| **表格边框** | `#30363d`，隔行背景 `#161b22` |
| **文字颜色** | 主文字 `#f9fafb`，次要文字 `#d1d5db` |

### 代码高亮

继续使用 highlight.js，保持 Atom One Light/Dark 主题，与 Shiki 的 github-dark 视觉接近。

## 用户交互

在设置面板中，用户可选择三种主题：
- vuepress
- github
- astro（新增）

选择后实时切换，无需刷新页面。

## 技术细节

### CSS 类名

使用 `.markdown-body.astro-theme` 作为选择器前缀。

### 渲染器

复用 `useMarkdown` composable，`getThemeClass()` 返回对应的类名。

## 成功标准

1. astro 主题渲染效果接近 Tailwind Typography
2. 深色模式正确适配
3. 主题切换流畅无闪烁
4. 与现有 vuepress/github 主题共存无冲突