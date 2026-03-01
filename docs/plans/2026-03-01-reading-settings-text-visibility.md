# 阅读设置文字显示优化 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复个人中心阅读设置选项在选中状态下文字不清晰的问题

**Architecture:** 通过 CSS 深度选择器利用 Element Plus 的 `.is-active` 类，将选中状态下的主题名称和描述文字变为白色

**Tech Stack:** Vue 3, Element Plus, CSS

---

### Task 1: 添加选中状态文字颜色样式

**Files:**
- Modify: `frontend/src/views/User/Profile.vue:291-292` (在 `.theme-desc` 样式后添加新规则)

**Step 1: 添加 CSS 规则**

在 `<style scoped>` 部分，`.theme-desc` 样式之后、`:deep(.el-radio-button__inner)` 之前添加：

```css
/* 选中状态下的文字颜色 */
:deep(.el-radio-button.is-active .theme-name),
:deep(.el-radio-button.is-active .theme-desc) {
  color: white;
}
```

**Step 2: 验证修改**

1. 启动前端开发服务器: `cd frontend && npm run dev`
2. 登录系统，进入个人中心
3. 在阅读设置中切换 VuePress 和 GitHub 选项
4. 确认选中状态下的主题名称和描述文字均为白色，清晰可读

**Step 3: 提交**

```bash
git add frontend/src/views/User/Profile.vue
git commit -m "fix: 修复阅读设置选中状态文字不清晰问题

- 选中状态下主题名称和描述文字变为白色
- 解决蓝色背景上灰色文字对比度不足的问题

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```