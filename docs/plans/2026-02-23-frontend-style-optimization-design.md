# 前端样式全面优化设计文档

## 概述

对qblog前端进行全面样式重构，采用内容优先设计理念，提升阅读体验和视觉美感。

## 技术方案

### CSS框架
- **UnoCSS**：原子化CSS框架，比Tailwind更轻量，Vite友好
- **CSS变量**：实现主题系统和暗色模式
- **Element Plus自定义主题**：覆盖默认变量

### 目录结构
```
frontend/src/
├── styles/
│   ├── variables.css      # CSS变量定义
│   ├── reset.css          # 样式重置
│   ├── typography.css     # 排版样式
│   ├── utilities.css      # 工具类
│   └── index.css          # 入口
```

## 配色方案

### 亮色主题
```css
--bg-primary: #fafafa        /* 页面背景 */
--bg-secondary: #ffffff      /* 卡片背景 */
--text-primary: #1f2937      /* 主文字 */
--text-secondary: #6b7280    /* 次文字 */
--accent: #3b82f6            /* 强调色 */
--border: #e5e7eb            /* 边框 */
```

### 暗色主题
```css
--bg-primary: #111827
--bg-secondary: #1f2937
--text-primary: #f9fafb
--text-secondary: #9ca3af
```

## 页面布局

### 首页
- 容器最大宽：1000px
- 文章列表区：650px
- 侧边栏：280px
- 移动端：侧边栏移至底部

### 文章详情页
- 最大宽：720px（最佳阅读宽度）
- 行高：1.75
- 字号：16px
- 段落间距增大

### 响应式断点
```
sm: 640px   /* 手机横屏 */
md: 768px   /* 平板 */
lg: 1024px  /* 桌面 */
xl: 1280px  /* 大屏 */
```

## 新增功能

1. **阅读进度条** - 文章页顶部显示
2. **回到顶部按钮** - 滚动200px后显示
3. **页面切换过渡** - 路由切换动画
4. **图片懒加载** - 加载占位符

## 动效原则

- 过渡时间：200-300ms
- hover效果：轻微位移 + 阴影变化
- 避免花哨动画干扰阅读

## 需要修改的页面

1. `Home/Index.vue` - 首页
2. `Article/Detail.vue` - 文章详情
3. `Article/List.vue` - 文章列表
4. `User/Login.vue` - 登录
5. `User/Register.vue` - 注册
6. `User/Profile.vue` - 个人资料
7. `User/Favorites.vue` - 收藏
8. `Admin/Dashboard.vue` - 后台仪表盘
9. `Admin/ArticleManage.vue` - 文章管理
10. `Admin/ArticleEdit.vue` - 文章编辑
11. `Category/Index.vue` - 分类页
12. `Tag/Index.vue` - 标签页
