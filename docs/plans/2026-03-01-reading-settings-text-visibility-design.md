# 阅读设置选项文字显示优化设计

**日期**: 2026-03-01
**状态**: 已确认

## 问题描述

个人中心「阅读设置」中的两个主题选项（VuePress / GitHub）在选中状态下文字显示不清晰：

- 主题名称 (`.theme-name`) 使用 `var(--text-primary)`
- 主题描述 (`.theme-desc`) 使用 `var(--text-tertiary)` (灰色 `#9CA3AF`)

当选中某个主题时，Element Plus 会将按钮背景变为蓝色 (primary color)，但描述文字仍保持灰色，导致对比度不足、难以阅读。

**影响范围**: 深色模式和浅色模式均存在此问题

## 解决方案

利用 Element Plus 自动添加的 `.is-active` 类，通过 CSS 深度选择器将选中状态下的文字统一变为白色。

### 技术实现

**修改文件**: `frontend/src/views/User/Profile.vue`

**修改内容**: 在 `<style scoped>` 部分添加以下 CSS：

```css
/* 选中状态下的文字颜色 */
:deep(.el-radio-button.is-active .theme-name),
:deep(.el-radio-button.is-active .theme-desc) {
  color: white;
}
```

**修改位置**: 在 `.theme-desc` 样式（约第 287-291 行）之后

### 方案优势

1. **纯 CSS 实现** - 无需修改模板或 JavaScript 逻辑
2. **兼容性好** - 所有现代浏览器均支持
3. **响应式** - 自动响应选中状态变化
4. **一致性** - 与 Element Plus 设计模式一致

## 验收标准

- [ ] 浅色模式下，选中的主题选项文字为白色，清晰可读
- [ ] 深色模式下，选中的主题选项文字为白色，清晰可读
- [ ] 未选中的选项文字保持原有颜色不变
- [ ] 切换主题时文字颜色正确响应