# Article Editor Page Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor ArticleEdit.vue to adopt a minimal focused layout with modal-based settings and toggle preview mode.

**Architecture:** Single-file component refactor. Remove left sidebar, move settings to modal dialog, implement toggle preview instead of split-pane. Preserve all existing functionality (slash commands, PlantUML, containers, toolbar, fullscreen).

**Tech Stack:** Vue 3 Composition API, Element Plus components (el-dialog, el-button, el-select, el-upload, el-input), SCSS

---

## File Structure

All changes in single file:

| File | Changes |
|------|---------|
| `frontend/src/views/Admin/ArticleEdit.vue` | Complete layout refactor - remove sidebar, add modal, toggle preview |

---

## Task 1: Set Up New Layout Structure

**Files:**
- Modify: `frontend/src/views/Admin/ArticleEdit.vue`

This task establishes the new component structure without changing functionality.

- [ ] **Step 1: Update template structure**

Replace the current `<template>` section with the new minimal layout structure. Keep all script logic unchanged.

```vue
<template>
  <div class="article-editor">
    <!-- 顶部导航栏 -->
    <header class="editor-header">
      <div class="header-left">
        <el-button link @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h1 class="page-title">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
        <span v-if="autoSaveStatus" class="auto-save-status">{{ autoSaveStatus }}</span>
      </div>
      <div class="header-right">
        <el-button v-if="!showPreview" @click="togglePreview">
          <el-icon><View /></el-icon>
          预览
        </el-button>
        <el-button v-else @click="togglePreview" type="primary">
          <el-icon><Edit /></el-icon>
          返回编辑
        </el-button>
        <el-button @click="handleSaveDraft" :loading="saving">
          <el-icon><Document /></el-icon>
          存草稿
        </el-button>
        <el-button type="primary" @click="openSettingsModal">
          <el-icon><Check /></el-icon>
          发布
        </el-button>
      </div>
    </header>

    <!-- 标题区域 -->
    <div class="title-area">
      <div class="title-input-wrapper">
        <input
          v-model="articleForm.title"
          class="title-input"
          placeholder="请输入文章标题（5-100字）"
          maxlength="100"
        />
        <span class="char-count">{{ articleForm.title.length }}/100</span>
      </div>
      <div class="meta-tags">
        <el-tag
          v-if="selectedCategory"
          class="meta-tag"
          @click="openSettingsModal"
        >
          📁 {{ selectedCategory.name }}
        </el-tag>
        <el-tag
          v-for="tag in selectedTags"
          :key="tag.id"
          class="meta-tag"
          @click="openSettingsModal"
        >
          🏷️ {{ tag.name }}
        </el-tag>
        <span v-if="!selectedCategory && selectedTags.length === 0" class="meta-empty" @click="openSettingsModal">
          点击设置分类和标签
        </span>
      </div>
    </div>

    <!-- 编辑器面板 -->
    <div class="editor-panel" ref="editorPanelRef">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button link @click="insertText('# ', '标题')" title="标题">
            <el-icon><Document /></el-icon>
          </el-button>
          <el-button link @click="insertText('**', '**')" title="加粗">
            <el-icon><EditIcon /></el-icon>
          </el-button>
          <el-button link @click="insertText('*', '*')" title="斜体">
            <el-icon><EditIcon /></el-icon>
          </el-button>
          <el-button link @click="insertText('[', '](url)')" title="链接">
            <el-icon><LinkIcon /></el-icon>
          </el-button>
          <el-button link @click="insertText('![', '](url)')" title="图片">
            <el-icon><PictureIcon /></el-icon>
          </el-button>
          <el-button link @click="insertCodeBlock()" title="代码块">
            <el-icon><CodeIcon /></el-icon>
          </el-button>
          <el-button link @click="insertTable()" title="表格">
            <el-icon><Grid /></el-icon>
          </el-button>
          <el-divider direction="vertical" />
          <el-dropdown trigger="click">
            <el-button link title="插入图表">
              <el-icon><Connection /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="insertPlantUML('sequence')">时序图</el-dropdown-item>
                <el-dropdown-item @click="insertPlantUML('class')">类图</el-dropdown-item>
                <el-dropdown-item @click="insertPlantUML('usecase')">用例图</el-dropdown-item>
                <el-dropdown-item @click="insertPlantUML('activity')">活动图</el-dropdown-item>
                <el-dropdown-item @click="insertPlantUML('component')">组件图</el-dropdown-item>
                <el-dropdown-item @click="insertPlantUML('deployment')">部署图</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click">
            <el-button link title="插入容器">
              <el-icon><MessageBox /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="insertContainer('tip')">💡 提示容器</el-dropdown-item>
                <el-dropdown-item @click="insertContainer('warning')">⚠️ 警告容器</el-dropdown-item>
                <el-dropdown-item @click="insertContainer('danger')">🚨 危险容器</el-dropdown-item>
                <el-dropdown-item @click="insertContainer('info')">ℹ️ 信息容器</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="toolbar-right">
          <el-button link :class="{ active: isFullscreen }" @click="toggleFullscreen" title="全屏">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 编辑/预览区域 -->
      <div class="editor-container">
        <!-- 编辑模式 -->
        <div v-if="!showPreview" class="editor-wrapper">
          <textarea
            ref="editorRef"
            v-model="articleForm.content"
            class="markdown-editor"
            placeholder="开始写作，支持 Markdown 语法..."
            @input="handleEditorInput"
            @keydown="handleEditorKeydown"
          />
          <!-- 快捷命令菜单 -->
          <div
            v-if="showCommandMenu"
            class="command-menu"
            :style="commandMenuStyle"
            ref="commandMenuRef"
          >
            <div class="command-menu-header">
              <span class="command-menu-title">快捷命令</span>
              <span class="command-menu-hint">↑↓ 选择 · Enter 确认 · Esc 关闭</span>
            </div>
            <div class="command-menu-list">
              <div
                v-for="(cmd, index) in filteredCommands"
                :key="cmd.name"
                class="command-item"
                :class="{ active: index === selectedCommandIndex }"
                @click="executeCommand(cmd)"
                @mouseenter="selectedCommandIndex = index"
              >
                <span class="command-icon">{{ cmd.icon }}</span>
                <div class="command-info">
                  <span class="command-name">{{ cmd.label }}</span>
                  <span class="command-desc">{{ cmd.description }}</span>
                </div>
                <span class="command-shortcut">/{{ cmd.name }}</span>
              </div>
              <div v-if="filteredCommands.length === 0" class="command-empty">
                未找到匹配的命令
              </div>
            </div>
          </div>
        </div>
        <!-- 预览模式 -->
        <div v-else class="preview-wrapper">
          <div :class="markdownClass" v-html="renderedContent"></div>
        </div>
      </div>

      <!-- 底部状态栏 -->
      <div class="editor-footer">
        <span class="footer-tip">
          <el-icon><InfoFilled /></el-icon>
          支持 Markdown 语法 · 输入 <kbd>/</kbd> 呼起快捷命令
        </span>
        <span class="word-count">
          {{ wordCount }} 字
        </span>
      </div>
    </div>

    <!-- 设置模态框 -->
    <el-dialog
      v-model="showSettingsModal"
      title="文章设置"
      width="480px"
      :close-on-click-modal="false"
      class="settings-dialog"
    >
      <el-form label-position="top">
        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            action="/api/v1/upload/image"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
          >
            <img v-if="articleForm.coverImage" :src="articleForm.coverImage" class="cover-img" />
            <div v-else class="cover-placeholder">
              <el-icon class="cover-icon"><Plus /></el-icon>
              <span>上传封面</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="articleForm.categoryId" placeholder="选择分类" class="full-width">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="articleForm.tagIds"
            multiple
            placeholder="添加标签"
            allow-create
            filterable
            class="full-width"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文章摘要">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="4"
            placeholder="写一段文章摘要..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeSettingsModal">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">
          保存并发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 2: Add new state variables to script**

Add these new refs to the script setup section (around line 264):

```javascript
// 新增状态
const showPreview = ref(false)
const showSettingsModal = ref(false)
const autoSaveStatus = ref('')
const CodeIcon = ref(null) // 添加代码图标引用，如果 Element Plus 没有，使用 Document

// 计算属性：已选分类和标签
const selectedCategory = computed(() => {
  if (!articleForm.categoryId) return null
  return categories.value.find(c => c.id === articleForm.categoryId)
})

const selectedTags = computed(() => {
  if (!articleForm.tagIds || articleForm.tagIds.length === 0) return []
  return tags.value.filter(t => articleForm.tagIds.includes(t.id))
})
```

- [ ] **Step 3: Add new methods**

Add these new methods to the script (before onMounted):

```javascript
// 切换预览模式
const togglePreview = () => {
  showPreview.value = !showPreview.value
}

// 打开设置模态框
const openSettingsModal = () => {
  showSettingsModal.value = true
}

// 关闭设置模态框
const closeSettingsModal = () => {
  showSettingsModal.value = false
}

// 发布文章（从模态框调用）
const handlePublish = async () => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }
  
  closeSettingsModal()
  
  try {
    publishing.value = true
    articleForm.status = 1
    if (isEdit.value && articleForm.id) {
      await updateArticle(articleForm.id, articleForm)
      ElMessage.success('文章更新成功')
    } else {
      await createArticle(articleForm)
      ElMessage.success('文章发布成功')
    }
    router.push('/admin/articles')
  } catch (error) {
    console.error('发布文章失败:', error)
    ElMessage.error('发布文章失败：' + (error.message || '未知错误'))
  } finally {
    publishing.value = false
  }
}
```

- [ ] **Step 4: Add Grid icon import**

Add Grid icon to the imports (around line 236):

```javascript
import {
  ArrowLeft,
  Check,
  Plus,
  Document,
  List,
  Link as LinkIcon,
  Picture as PictureIcon,
  View,
  InfoFilled,
  Edit as EditIcon,
  FullScreen,
  Connection,
  MessageBox,
  Grid  // 新增
} from '@element-plus/icons-vue'
```

- [ ] **Step 5: Verify the component still works**

Run: `cd frontend && npm run dev`
Open: http://localhost:3001/admin/create
Expected: Page loads without errors, new layout structure visible (header, title area, editor panel)

- [ ] **Step 6: Commit structure changes**

```bash
git add frontend/src/views/Admin/ArticleEdit.vue
git commit -m "refactor(editor): update ArticleEdit template structure for minimal layout

- Add new header with preview/draft/publish buttons
- Add title area with meta tags display
- Add settings modal dialog placeholder
- Add toggle preview mode state

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: Implement New Styles

**Files:**
- Modify: `frontend/src/views/Admin/ArticleEdit.vue` (style section)

- [ ] **Step 1: Replace the entire style section**

Replace all `<style>` sections with new minimal layout styles:

```scss
<style lang="scss" scoped>
.article-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-primary);
}

// 顶部 header
.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;

  .back-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    color: var(--text-secondary);

    &:hover {
      background: var(--bg-tertiary);
      color: var(--text-primary);
    }
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .auto-save-status {
    font-size: 12px;
    color: var(--text-tertiary);
  }
}

.header-right {
  display: flex;
  gap: 12px;
}

// 标题区域
.title-area {
  padding: 24px 32px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);

  .title-input-wrapper {
    position: relative;

    .title-input {
      width: 100%;
      padding: 12px 0;
      font-size: 24px;
      font-weight: 600;
      border: none;
      border-bottom: 2px solid var(--border-color);
      background: transparent;
      color: var(--text-primary);
      outline: none;
      transition: border-color var(--transition-fast);

      &:focus {
        border-bottom-color: var(--color-primary);
      }

      &::placeholder {
        color: var(--text-tertiary);
      }
    }

    .char-count {
      position: absolute;
      right: 0;
      bottom: -20px;
      font-size: 12px;
      color: var(--text-tertiary);
    }
  }

  .meta-tags {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 20px;

    .meta-tag {
      cursor: pointer;
      transition: all var(--transition-fast);

      &:hover {
        transform: scale(1.05);
      }
    }

    .meta-empty {
      font-size: 13px;
      color: var(--text-tertiary);
      cursor: pointer;

      &:hover {
        color: var(--color-primary);
      }
    }
  }
}

// 编辑器面板
.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  overflow: hidden;
}

// 工具栏
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);

  .toolbar-left {
    display: flex;
    gap: 4px;

    .el-button {
      width: 34px;
      height: 34px;
      padding: 0;
      border-radius: 6px;
      color: var(--text-secondary);
      transition: all var(--transition-fast);

      &:hover {
        background: var(--bg-secondary);
        color: var(--color-primary);
      }

      .el-icon {
        font-size: 18px;
      }
    }
  }

  .toolbar-right {
    .el-button {
      color: var(--text-secondary);

      &.active {
        color: var(--color-primary);
        background: rgba(59, 130, 246, 0.1);
      }

      &:hover {
        color: var(--color-primary);
      }
    }
  }
}

// 编辑器容器
.editor-container {
  flex: 1;
  overflow: hidden;
  position: relative;

  .editor-wrapper {
    height: 100%;
    overflow: hidden;
    position: relative;

    .markdown-editor {
      width: 100%;
      height: 100%;
      padding: 20px 32px;
      border: none;
      outline: none;
      resize: none;
      font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
      font-size: 14px;
      line-height: 1.8;
      background: #1a1a2e;
      color: #d4d4d4;

      &::placeholder {
        color: #6b6b8d;
      }
    }

    // 快捷命令菜单
    .command-menu {
      position: absolute;
      z-index: 100;
      width: 280px;
      max-height: 320px;
      background: var(--bg-secondary);
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius);
      box-shadow: var(--shadow-lg);
      overflow: hidden;

      .command-menu-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 12px;
        background: var(--bg-tertiary);
        border-bottom: 1px solid var(--border-color);
        font-size: 12px;

        .command-menu-title {
          font-weight: 600;
          color: var(--text-primary);
        }

        .command-menu-hint {
          color: var(--text-tertiary);
          font-size: 11px;
        }
      }

      .command-menu-list {
        max-height: 270px;
        overflow-y: auto;

        &::-webkit-scrollbar {
          width: 4px;
        }

        &::-webkit-scrollbar-thumb {
          background: var(--border-color);
          border-radius: 2px;
        }
      }

      .command-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 12px;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover,
        &.active {
          background: rgba(59, 130, 246, 0.1);
        }

        &.active {
          .command-name {
            color: var(--color-primary);
          }
        }

        .command-icon {
          width: 28px;
          height: 28px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: var(--bg-tertiary);
          border-radius: 6px;
          font-size: 14px;
          font-weight: 600;
          color: var(--text-primary);
        }

        .command-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 2px;
          min-width: 0;

          .command-name {
            font-size: 13px;
            font-weight: 500;
            color: var(--text-primary);
          }

          .command-desc {
            font-size: 11px;
            color: var(--text-tertiary);
          }
        }

        .command-shortcut {
          font-size: 11px;
          color: var(--text-tertiary);
          font-family: 'SF Mono', Monaco, monospace;
          background: var(--bg-tertiary);
          padding: 2px 6px;
          border-radius: 4px;
        }
      }

      .command-empty {
        padding: 20px;
        text-align: center;
        color: var(--text-tertiary);
        font-size: 13px;
      }
    }
  }

  .preview-wrapper {
    height: 100%;
    overflow-y: auto;
    background: var(--bg-primary);
    padding: 32px;

    .markdown-body {
      max-width: 800px;
      margin: 0 auto;
    }
  }
}

// 底部状态栏
.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--bg-tertiary);
  border-top: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-secondary);

  .footer-tip {
    display: flex;
    align-items: center;
    gap: 6px;

    .el-icon {
      font-size: 14px;
    }

    kbd {
      display: inline-block;
      padding: 2px 6px;
      font-family: inherit;
      font-size: 12px;
      font-weight: 500;
      background: var(--bg-tertiary);
      border: 1px solid var(--border-color);
      border-radius: 4px;
      box-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
    }
  }

  .word-count {
    font-weight: 500;
  }
}

// 设置模态框样式
.settings-dialog {
  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }

  :deep(.el-form-item__label) {
    font-weight: 500;
    color: var(--text-primary);
  }

  .full-width {
    width: 100%;
  }

  .cover-uploader {
    width: 100%;
    height: 160px;
    border: 2px dashed var(--border-color);
    border-radius: var(--border-radius);
    cursor: pointer;
    transition: all var(--transition-fast);
    background: var(--bg-tertiary);
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover {
      border-color: var(--color-primary);
      background: rgba(59, 130, 246, 0.05);
    }

    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: var(--border-radius);
    }

    .cover-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      color: var(--text-tertiary);

      .cover-icon {
        font-size: 40px;
      }

      span {
        font-size: 13px;
      }
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .editor-header {
    padding: 0 16px;
  }

  .header-left .page-title {
    font-size: 16px;
  }

  .header-right .el-button span {
    display: none;
  }

  .title-area {
    padding: 16px;

    .title-input {
      font-size: 18px;
    }
  }

  .editor-container .editor-wrapper .markdown-editor {
    padding: 16px;
  }

  .settings-dialog {
    width: 90% !important;
  }
}
</style>

<style lang="scss">
// 全屏模式 - 不使用 scoped
.editor-fullscreen {
  .article-editor {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background: var(--bg-primary);
  }

  .editor-header {
    display: none !important;
  }

  .title-area {
    display: none !important;
  }

  .editor-panel {
    width: 100% !important;
    height: 100vh !important;
    border-radius: 0 !important;
    border: none !important;
    box-shadow: none !important;
  }

  .editor-container {
    height: calc(100vh - 41px) !important;
  }

  .editor-wrapper {
    height: 100% !important;
  }

  .markdown-editor {
    height: 100% !important;
  }

  .toolbar-right {
    .el-button:last-child {
      color: #409EFF !important;
      background: rgba(64, 158, 255, 0.1) !important;
    }
  }

  .el-dropdown__popper {
    z-index: 10000 !important;
  }

  .command-menu {
    z-index: 10001 !important;
  }
}
</style>
```

- [ ] **Step 2: Verify styles are applied correctly**

Run: `cd frontend && npm run dev`
Open: http://localhost:3001/admin/create
Expected: 
- Header displays correctly with buttons
- Title area shows large input with underline style
- Editor has dark background (#1a1a2e)
- Preview toggle works (clicking "预览" shows white preview area)

- [ ] **Step 3: Commit style changes**

```bash
git add frontend/src/views/Admin/ArticleEdit.vue
git commit -m "style(editor): implement minimal focused layout styles

- Header with preview/draft/publish buttons
- Large title input with underline separator
- Dark editor background for focus
- Toggle preview mode styling
- Settings modal dialog styles
- Responsive breakpoints for mobile

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: Implement Auto-Save Feature

**Files:**
- Modify: `frontend/src/views/Admin/ArticleEdit.vue`

- [ ] **Step 1: Add auto-save logic**

Add auto-save timer and status update in script section:

```javascript
// 自动保存
const AUTO_SAVE_INTERVAL = 60000 // 60秒
let autoSaveTimer = null

const startAutoSave = () => {
  if (autoSaveTimer) return
  
  autoSaveTimer = setInterval(async () => {
    if (!articleForm.title.trim() && !articleForm.content.trim()) return
    
    try {
      articleForm.status = 0
      if (isEdit.value && articleForm.id) {
        await updateArticle(articleForm.id, articleForm)
      } else {
        const res = await createArticle(articleForm)
        if (res.data?.id) {
          articleForm.id = res.data.id
          isEdit.value = true
        }
      }
      const now = new Date()
      autoSaveStatus.value = `自动保存 · ${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
    } catch (error) {
      console.error('自动保存失败:', error)
    }
  }, AUTO_SAVE_INTERVAL)
}

const stopAutoSave = () => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
}
```

- [ ] **Step 2: Start auto-save on mount**

Add to onMounted:

```javascript
onMounted(() => {
  fetchCategories()
  fetchTags()
  fetchArticle()
  startAutoSave()  // 新增
})
```

- [ ] **Step 3: Stop auto-save on unmount**

Add onUnmounted hook:

```javascript
onUnmounted(() => {
  document.removeEventListener('keydown', handleEscKey)
  document.documentElement.classList.remove('editor-fullscreen')
  stopAutoSave()  // 新增
})
```

- [ ] **Step 4: Import onUnmounted**

Add onUnmounted to Vue imports (line 231):

```javascript
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
```

- [ ] **Step 5: Verify auto-save works**

Run: `cd frontend && npm run dev`
Open: http://localhost:3001/admin/create
Test:
1. Type some content
2. Wait 60 seconds
3. Check if "自动保存 · XX:XX" appears in header
4. Refresh page - content should be restored (if draft was saved)

- [ ] **Step 6: Commit auto-save implementation**

```bash
git add frontend/src/views/Admin/ArticleEdit.vue
git commit -m "feat(editor): add auto-save functionality

- Auto-save every 60 seconds when content exists
- Display save time in header status
- Save as draft (status: 0)
- Update article ID after first save for updates

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: Fix Fullscreen Mode for New Layout

**Files:**
- Modify: `frontend/src/views/Admin/ArticleEdit.vue`

- [ ] **Step 1: Update fullscreen toggle logic**

The fullscreen CSS already hides header and title-area. Ensure the toggleFullscreen function works:

```javascript
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    document.documentElement.classList.add('editor-fullscreen')
  } else {
    document.documentElement.classList.remove('editor-fullscreen')
  }
}
```

This function already exists, just verify it works with new layout.

- [ ] **Step 2: Verify fullscreen mode**

Run: `cd frontend && npm run dev`
Open: http://localhost:3001/admin/create
Test:
1. Click fullscreen button in toolbar right side
2. Expected: Header and title area disappear, editor fills entire screen
3. Press ESC key
4. Expected: Exit fullscreen, header and title area reappear

- [ ] **Step 3: Commit fullscreen verification**

```bash
git add frontend/src/views/Admin/ArticleEdit.vue
git commit -m "fix(editor): ensure fullscreen mode works with new layout

- CSS rules hide header and title-area in fullscreen
- ESC key handler already implemented

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: Final Verification and Cleanup

**Files:**
- Modify: `frontend/src/views/Admin/ArticleEdit.vue`

- [ ] **Step 1: Remove unused code**

Remove any remaining references to the old sidebar layout:
- Remove `handleScroll` function (no longer needed for sync scroll)
- Remove `syncScroll` function
- Remove `previewRef` ref (not used in toggle preview)
- Remove `showPreview` logic that relied on split-pane

- [ ] **Step 2: Complete end-to-end test**

Run: `cd frontend && npm run dev`
Test all features:

| Feature | Test Steps | Expected Result |
|---------|-----------|-----------------|
| Header | View header area | Back button, title, preview/draft/publish buttons visible |
| Title | Type title | Large input, char count shows, underline on focus |
| Meta tags | Select category/tags | Tags appear below title, clickable |
| Editor | Type content | Dark background, monospace font, / commands work |
| Toolbar | Click toolbar buttons | Insert markdown syntax correctly |
| Preview | Click "预览" | Switch to white preview area |
| Return edit | Click "返回编辑" | Back to dark editor |
| Fullscreen | Click fullscreen button | Header/title hide, editor full screen |
| ESC exit | Press ESC in fullscreen | Exit fullscreen |
| Settings modal | Click "发布" | Modal appears with cover/category/tag/summary |
| Publish | Fill modal, click "保存并发布" | Article published, redirect to list |
| Draft | Click "存草稿" | Save as draft, no modal |
| Auto-save | Wait 60s with content | "自动保存 · XX:XX" appears |

- [ ] **Step 3: Fix any issues found**

If any feature doesn't work, fix the code and re-test.

- [ ] **Step 4: Final commit**

```bash
git add frontend/src/views/Admin/ArticleEdit.vue
git commit -m "refactor(editor): complete ArticleEdit minimal layout refactor

- Remove old sidebar layout
- Remove sync scroll logic (not needed for toggle preview)
- All features verified working:
  - Header with preview/draft/publish
  - Large title input with meta tags
  - Toggle preview mode
  - Settings modal for publish
  - Auto-save functionality
  - Fullscreen mode
  - Slash commands
  - Toolbar formatting

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Summary

This plan refactors ArticleEdit.vue into a minimal focused layout:

1. **Task 1**: Establish new template structure
2. **Task 2**: Implement styles for minimal layout
3. **Task 3**: Add auto-save functionality
4. **Task 4**: Verify fullscreen mode
5. **Task 5**: Final verification and cleanup

All existing functionality preserved: slash commands, PlantUML, containers, toolbar, fullscreen.