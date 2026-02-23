# 前端样式优化实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 全面重构qblog前端样式，引入UnoCSS原子化CSS框架，实现响应式布局和暗色主题支持

**Architecture:** UnoCSS + CSS变量主题系统 + Element Plus自定义主题，内容优先设计理念

**Tech Stack:** Vue 3, Vite, UnoCSS, Element Plus, SCSS

---

## Task 1: 安装UnoCSS依赖

**Files:**
- Modify: `frontend/package.json`

**Step 1: 安装UnoCSS**

```bash
cd frontend && npm install -D unocss @unocss/preset-uno @unocss/preset-attributify @unocss/preset-icons
```

**Step 2: 验证安装**

Run: `cd frontend && npm ls unocss`
Expected: unocss@x.x.x

---

## Task 2: 配置UnoCSS

**Files:**
- Create: `frontend/uno.config.js`
- Modify: `frontend/vite.config.js`

**Step 1: 创建UnoCSS配置文件**

```js
// frontend/uno.config.js
import { defineConfig, presetUno, presetAttributify, presetIcons } from 'unocss'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({
      scale: 1.2,
      warn: true,
    }),
  ],
  shortcuts: {
    'btn': 'px-4 py-2 rounded inline-block bg-primary text-white cursor-pointer hover:bg-primary-600 disabled:cursor-default disabled:bg-gray-600 disabled:opacity-50',
    'card': 'bg-secondary rounded-lg shadow-sm p-4',
    'text-primary': 'text-gray-800 dark:text-gray-100',
    'text-secondary': 'text-gray-600 dark:text-gray-400',
  },
  theme: {
    colors: {
      primary: '#3b82f6',
      secondary: '#6b7280',
    },
  },
  rules: [
    ['text-balance', { 'text-wrap': 'balance' }],
  ],
})
```

**Step 2: 修改Vite配置**

先读取现有vite.config.js，然后添加UnoCSS插件。

**Step 3: 验证配置**

Run: `cd frontend && npm run build`
Expected: 构建成功

---

## Task 3: 创建CSS变量主题系统

**Files:**
- Create: `frontend/src/styles/variables.css`
- Create: `frontend/src/styles/reset.css`
- Create: `frontend/src/styles/typography.css`
- Create: `frontend/src/styles/index.css`

**Step 1: 创建变量文件**

```css
/* frontend/src/styles/variables.css */
:root {
  /* 颜色 */
  --color-primary: #3b82f6;
  --color-primary-light: #60a5fa;
  --color-primary-dark: #2563eb;
  
  /* 背景色 */
  --bg-primary: #fafafa;
  --bg-secondary: #ffffff;
  --bg-tertiary: #f5f5f5;
  
  /* 文字色 */
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  
  /* 边框 */
  --border-color: #e5e7eb;
  --border-radius: 8px;
  
  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
  
  /* 间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  
  /* 字体 */
  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  --font-mono: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  
  /* 过渡 */
  --transition-fast: 150ms ease;
  --transition-normal: 250ms ease;
  --transition-slow: 350ms ease;
}

/* 暗色主题 */
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary: #111827;
    --bg-secondary: #1f2937;
    --bg-tertiary: #374151;
    
    --text-primary: #f9fafb;
    --text-secondary: #d1d5db;
    --text-tertiary: #9ca3af;
    
    --border-color: #374151;
    
    --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.3);
    --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.4);
    --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.5);
  }
}
```

**Step 2: 创建重置样式**

```css
/* frontend/src/styles/reset.css */
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  font-family: var(--font-sans);
  background-color: var(--bg-primary);
  color: var(--text-primary);
  line-height: 1.6;
}

a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--color-primary-light);
}

img {
  max-width: 100%;
  height: auto;
}

button {
  font-family: inherit;
}

ul, ol {
  list-style: none;
}
```

**Step 3: 创建排版样式**

```css
/* frontend/src/styles/typography.css */
h1, h2, h3, h4, h5, h6 {
  color: var(--text-primary);
  font-weight: 600;
  line-height: 1.3;
}

h1 { font-size: 2rem; margin-bottom: var(--spacing-lg); }
h2 { font-size: 1.5rem; margin-bottom: var(--spacing-md); }
h3 { font-size: 1.25rem; margin-bottom: var(--spacing-md); }
h4 { font-size: 1.125rem; margin-bottom: var(--spacing-sm); }

p {
  margin-bottom: var(--spacing-md);
}

.prose {
  max-width: 720px;
  font-size: 1rem;
  line-height: 1.75;
  color: var(--text-primary);
}

.prose p {
  margin-bottom: 1.25em;
}

.prose h2 {
  margin-top: 2em;
  margin-bottom: 0.75em;
}

.prose h3 {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
}

.prose pre {
  background: var(--bg-tertiary);
  padding: var(--spacing-md);
  border-radius: var(--border-radius);
  overflow-x: auto;
  margin: var(--spacing-md) 0;
}

.prose code {
  font-family: var(--font-mono);
  font-size: 0.875em;
}

.prose blockquote {
  border-left: 4px solid var(--color-primary);
  padding-left: var(--spacing-md);
  margin: var(--spacing-md) 0;
  color: var(--text-secondary);
}

.prose ul, .prose ol {
  margin: var(--spacing-md) 0;
  padding-left: var(--spacing-lg);
}

.prose li {
  margin-bottom: var(--spacing-xs);
}

.prose img {
  border-radius: var(--border-radius);
  margin: var(--spacing-lg) 0;
}
```

**Step 4: 创建入口文件**

```css
/* frontend/src/styles/index.css */
@import './variables.css';
@import './reset.css';
@import './typography.css';

#app {
  min-height: 100vh;
}

.container {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

@media (max-width: 768px) {
  .container {
    padding: 0 var(--spacing-sm);
  }
}
```

---

## Task 4: 更新main.js引入样式

**Files:**
- Modify: `frontend/src/main.js`

**Step 1: 添加样式导入和UnoCSS**

```js
// frontend/src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

import 'virtual:uno.css'
import './styles/index.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
```

---

## Task 5: 重构首页 Home/Index.vue

**Files:**
- Modify: `frontend/src/views/Home/Index.vue`

**Step 1: 重写模板和样式**

使用UnoCSS原子类重构，保持原有功能逻辑不变。

---

## Task 6: 重构文章详情页 Article/Detail.vue

**Files:**
- Modify: `frontend/src/views/Article/Detail.vue`
- Create: `frontend/src/components/ReadingProgress.vue`
- Create: `frontend/src/components/BackToTop.vue`

**Step 1: 创建阅读进度条组件**

**Step 2: 创建回到顶部组件**

**Step 3: 重构详情页**

---

## Task 7: 重构其他页面

**Files:**
- Modify: `frontend/src/views/Article/List.vue`
- Modify: `frontend/src/views/User/Login.vue`
- Modify: `frontend/src/views/User/Register.vue`
- Modify: `frontend/src/views/User/Profile.vue`
- Modify: `frontend/src/views/User/Favorites.vue`
- Modify: `frontend/src/views/Admin/Dashboard.vue`
- Modify: `frontend/src/views/Admin/ArticleManage.vue`
- Modify: `frontend/src/views/Admin/ArticleEdit.vue`
- Modify: `frontend/src/views/Category/Index.vue`
- Modify: `frontend/src/views/Tag/Index.vue`

**Step 1: 逐个页面重构，应用新样式系统**

---

## Task 8: 清理旧样式文件

**Files:**
- Delete: `frontend/src/assets/styles/index.scss`

**Step 1: 确认新样式工作正常后删除旧文件**

---

## Task 9: 验证和测试

**Step 1: 本地开发测试**

Run: `cd frontend && npm run dev`
Expected: 页面正常显示，样式正确

**Step 2: 构建测试**

Run: `cd frontend && npm run build`
Expected: 构建成功，无错误

**Step 3: 预览测试**

Run: `cd frontend && npm run preview`
Expected: 生产环境样式正常
