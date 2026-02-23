<template>
  <div class="article-editor">
    <!-- 顶部操作栏 -->
    <header class="editor-header">
      <div class="header-left">
        <el-button link @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h1 class="page-title">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
      </div>
      <div class="header-right">
        <el-button @click="handleSaveDraft" :loading="saving">
          <el-icon><Download /></el-icon>
          存草稿
        </el-button>
        <el-button type="primary" @click="handleSubmit" :loading="publishing">
          <el-icon><Check /></el-icon>
          发布文章
        </el-button>
      </div>
    </header>

    <div class="editor-body">
      <!-- 左侧表单区 -->
      <div class="form-panel">
        <!-- 标题输入 -->
        <div class="title-section">
          <input
            v-model="articleForm.title"
            class="title-input"
            placeholder="输入文章标题..."
            maxlength="200"
          />
          <span class="char-count">{{ articleForm.title.length }}/200</span>
        </div>

        <!-- 元信息 -->
        <div class="meta-section">
          <div class="meta-row">
            <label>分类</label>
            <el-select v-model="articleForm.categoryId" placeholder="选择分类" class="meta-select">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </div>
          <div class="meta-row">
            <label>标签</label>
            <el-select
              v-model="articleForm.tagIds"
              multiple
              placeholder="添加标签"
              allow-create
              filterable
              class="meta-select"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              />
            </el-select>
          </div>
        </div>

        <!-- 封面图 -->
        <div class="cover-section">
          <label class="section-label">封面图片</label>
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
        </div>

        <!-- 摘要 -->
        <div class="summary-section">
          <label class="section-label">文章摘要</label>
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="3"
            placeholder="写一段文章摘要..."
            maxlength="500"
            show-word-limit
            class="summary-input"
          />
        </div>
      </div>

      <!-- 编辑器区域 -->
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
            <el-button link @click="insertText('- ', '列表项')" title="列表">
              <el-icon><List /></el-icon>
            </el-button>
            <el-button link @click="insertText('[', '](url)')" title="链接">
              <el-icon><LinkIcon /></el-icon>
            </el-button>
            <el-button link @click="insertText('![', '](url)')" title="图片">
              <el-icon><PictureIcon /></el-icon>
            </el-button>
          </div>
          <div class="toolbar-right">
            <el-button link :class="{ active: showPreview }" @click="togglePreview">
              <el-icon><View /></el-icon>
              {{ showPreview ? '编辑' : '预览' }}
            </el-button>
            <el-divider direction="vertical" />
            <el-button link :class="{ active: isFullscreen }" @click="toggleFullscreen" title="全屏">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 编辑区 -->
        <div class="editor-container" :class="{ 'show-preview': showPreview }">
          <div class="editor-wrapper">
            <textarea
              ref="editorRef"
              v-model="articleForm.content"
              class="markdown-editor"
              placeholder="开始写作，支持 Markdown 语法..."
              @scroll="handleScroll"
            />
          </div>
          <div v-if="showPreview" class="preview-wrapper" ref="previewRef">
            <div class="preview-content" v-html="renderedContent"></div>
          </div>
        </div>

        <!-- 底部状态栏 -->
        <div class="editor-footer">
          <span class="footer-tip">
            <el-icon><InfoFilled /></el-icon>
            支持 Markdown 语法
          </span>
          <span class="word-count">
            {{ wordCount }} 字
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import {
  ArrowLeft,
  Download,
  Check,
  Plus,
  Document,
  List,
  Link as LinkIcon,
  Picture as PictureIcon,
  View,
  InfoFilled,
  Edit as EditIcon,
  FullScreen
} from '@element-plus/icons-vue'
import { createArticle, updateArticle, getArticleDetail } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const route = useRoute()
const router = useRouter()
const md = new MarkdownIt({
  breaks: true,
  linkify: true,
  typographer: true
})

const editorRef = ref(null)
const previewRef = ref(null)
const isEdit = ref(false)
const categories = ref([])
const tags = ref([])
const showPreview = ref(false)
const saving = ref(false)
const publishing = ref(false)
const isFullscreen = ref(false)
const editorPanelRef = ref(null)

const articleForm = reactive({
  id: null,
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  categoryId: null,
  tagIds: [],
  status: 1
})

const renderedContent = computed(() => {
  return articleForm.content ? md.render(articleForm.content) : ''
})

const wordCount = computed(() => {
  return articleForm.content.replace(/\s/g, '').length
})

const goBack = () => {
  router.back()
}

const togglePreview = () => {
  showPreview.value = !showPreview.value
  if (showPreview.value) {
    nextTick(() => {
      syncScroll()
    })
  }
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    document.documentElement.classList.add('editor-fullscreen')
  } else {
    document.documentElement.classList.remove('editor-fullscreen')
  }
}

// 监听 ESC 键退出全屏
const handleEscKey = (e) => {
  if (e.key === 'Escape' && isFullscreen.value) {
    isFullscreen.value = false
    document.documentElement.classList.remove('editor-fullscreen')
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscKey)
  document.documentElement.classList.remove('editor-fullscreen')
})

const handleScroll = () => {
  if (showPreview.value && previewRef.value) {
    syncScroll()
  }
}

const syncScroll = () => {
  if (!editorRef.value || !previewRef.value) return
  const ratio = previewRef.value.scrollHeight / editorRef.value.scrollHeight
  previewRef.value.scrollTop = editorRef.value.scrollTop * ratio
}

const insertText = (before, after = '') => {
  const textarea = editorRef.value
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = articleForm.content.substring(start, end) || '文本'
  
  const newText = articleForm.content.substring(0, start) + before + selectedText + after + articleForm.content.substring(end)
  articleForm.content = newText
  
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + before.length, start + before.length + selectedText.length)
  })
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchTags = async () => {
  try {
    const res = await getTagList()
    tags.value = res.data || []
  } catch (error) {
    console.error('获取标签失败:', error)
  }
}

const fetchArticle = async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    articleForm.id = id
    try {
      const res = await getArticleDetail(id)
      const data = res.data
      Object.assign(articleForm, {
        id: data.id,
        title: data.title,
        summary: data.summary,
        content: data.content,
        coverImage: data.coverImage,
        categoryId: data.category?.id,
        tagIds: data.tags?.map(t => t.id) || []
      })
    } catch (error) {
      console.error('获取文章失败:', error)
    }
  }
}

const handleCoverSuccess = (response) => {
  if (response.code === 200) {
    articleForm.coverImage = response.data.url
    ElMessage.success('封面上传成功')
  }
}

const handleSubmit = async () => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  try {
    publishing.value = true
    articleForm.status = 1
    if (isEdit.value) {
      await updateArticle(articleForm.id, articleForm)
      ElMessage.success('文章更新成功')
    } else {
      await createArticle(articleForm)
      ElMessage.success('文章发布成功')
    }
    router.push('/admin/articles')
  } catch (error) {
    console.error('发布文章失败:', error)
  } finally {
    publishing.value = false
  }
}

const handleSaveDraft = async () => {
  try {
    saving.value = true
    articleForm.status = 0
    if (isEdit.value) {
      await updateArticle(articleForm.id, articleForm)
    } else {
      await createArticle(articleForm)
    }
    ElMessage.success('草稿保存成功')
  } catch (error) {
    console.error('保存草稿失败:', error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchCategories()
  fetchTags()
  fetchArticle()
})
</script>

<style lang="scss" scoped>
.article-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-primary);
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

  .form-panel {
    display: none !important;
  }

  .editor-body {
    padding: 0 !important;
    gap: 0 !important;
    height: 100vh !important;
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

  .editor-wrapper,
  .preview-wrapper {
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
}
</style>

<style lang="scss" scoped>
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
}

.header-right {
  display: flex;
  gap: 12px;
}

// 主体区域
.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  gap: 20px;
  padding: 20px;
}

// 左侧表单区
.form-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
  padding-right: 8px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 3px;
  }
}

// 标题输入
.title-section {
  position: relative;

  .title-input {
    width: 100%;
    padding: 14px 16px;
    font-size: 20px;
    font-weight: 600;
    border: 1px solid var(--border-color);
    border-radius: var(--border-radius);
    background: var(--bg-secondary);
    color: var(--text-primary);
    outline: none;
    transition: all var(--transition-fast);

    &:focus {
      border-color: var(--color-primary);
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }

    &::placeholder {
      color: var(--text-tertiary);
    }
  }

  .char-count {
    position: absolute;
    right: 12px;
    bottom: -20px;
    font-size: 12px;
    color: var(--text-tertiary);
  }
}

// 元信息
.meta-section {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .meta-row {
    display: flex;
    align-items: center;
    gap: 12px;

    label {
      width: 50px;
      font-size: 14px;
      color: var(--text-secondary);
      font-weight: 500;
    }

    .meta-select {
      flex: 1;
    }
  }
}

// 封面图
.cover-section {
  .section-label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 12px;
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

// 摘要
.summary-section {
  .section-label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 12px;
  }

  .summary-input {
    :deep(.el-textarea__inner) {
      border-radius: var(--border-radius);
      font-size: 14px;
      line-height: 1.6;
      resize: none;
    }
  }
}

// 编辑器面板
.editor-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
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
  display: flex;
  overflow: hidden;
  position: relative;

  &.show-preview {
    .editor-wrapper {
      width: 50%;
      border-right: 1px solid var(--border-color);
    }

    .preview-wrapper {
      display: block;
      width: 50%;
    }
  }

  .editor-wrapper {
    flex: 1;
    overflow: hidden;
    position: relative;

    .markdown-editor {
      width: 100%;
      height: 100%;
      padding: 20px;
      border: none;
      outline: none;
      resize: none;
      font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
      font-size: 14px;
      line-height: 1.8;
      background: transparent;
      color: var(--text-primary);

      &::placeholder {
        color: var(--text-tertiary);
      }
    }
  }

  .preview-wrapper {
    display: none;
    overflow-y: auto;
    background: var(--bg-primary);

    .preview-content {
      padding: 20px;
      font-size: 15px;
      line-height: 1.8;
      color: var(--text-primary);

      :deep(h1) {
        font-size: 2em;
        margin: 1.5em 0 0.5em;
        font-weight: 700;
        border-bottom: 1px solid var(--border-color);
        padding-bottom: 0.3em;
      }

      :deep(h2) {
        font-size: 1.5em;
        margin: 1.5em 0 0.5em;
        font-weight: 600;
        border-bottom: 1px solid var(--border-color);
        padding-bottom: 0.3em;
      }

      :deep(h3) {
        font-size: 1.25em;
        margin: 1.2em 0 0.5em;
        font-weight: 600;
      }

      :deep(h4),
      :deep(h5),
      :deep(h6) {
        font-size: 1em;
        margin: 1em 0 0.5em;
        font-weight: 600;
      }

      :deep(p) {
        margin: 1em 0;
      }

      :deep(a) {
        color: var(--color-primary);
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }

      :deep(code) {
        background: var(--bg-tertiary);
        padding: 2px 6px;
        border-radius: 4px;
        font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
        font-size: 0.9em;
        color: #e83e8c;
      }

      :deep(pre) {
        background: #1e1e1e;
        color: #d4d4d4;
        padding: 16px;
        border-radius: var(--border-radius);
        overflow-x: auto;
        margin: 1em 0;
        font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
        font-size: 13px;
        line-height: 1.6;

        code {
          background: transparent;
          padding: 0;
          color: inherit;
        }
      }

      :deep(blockquote) {
        margin: 1em 0;
        padding: 12px 16px;
        border-left: 4px solid var(--color-primary);
        background: var(--bg-tertiary);
        color: var(--text-secondary);
        border-radius: 0 var(--border-radius) var(--border-radius) 0;

        p {
          margin: 0;
        }
      }

      :deep(ul),
      :deep(ol) {
        padding-left: 2em;
        margin: 1em 0;

        li {
          margin: 0.5em 0;
        }
      }

      :deep(img) {
        max-width: 100%;
        border-radius: var(--border-radius);
        margin: 1em 0;
      }

      :deep(hr) {
        border: none;
        border-top: 1px solid var(--border-color);
        margin: 2em 0;
      }

      :deep(table) {
        width: 100%;
        border-collapse: collapse;
        margin: 1em 0;
        overflow: hidden;
        border-radius: var(--border-radius);

        th,
        td {
          border: 1px solid var(--border-color);
          padding: 10px 14px;
          text-align: left;
        }

        th {
          background: var(--bg-tertiary);
          font-weight: 600;
        }

        tr:nth-child(even) {
          background: var(--bg-tertiary);
        }
      }
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
  }

  .word-count {
    font-weight: 500;
  }
}

// 响应式
@media (max-width: 1024px) {
  .editor-body {
    flex-direction: column;
    padding: 12px;
  }

  .form-panel {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    overflow: visible;
  }

  .title-section {
    width: 100%;
  }

  .meta-section {
    width: calc(50% - 10px);
  }

  .cover-section,
  .summary-section {
    width: calc(50% - 10px);
  }
}

@media (max-width: 768px) {
  .editor-header {
    padding: 0 16px;
  }

  .page-title {
    font-size: 16px !important;
  }

  .header-right .el-button span {
    display: none;
  }

  .meta-section,
  .cover-section,
  .summary-section {
    width: 100%;
  }

  .editor-container.show-preview {
    flex-direction: column;

    .editor-wrapper,
    .preview-wrapper {
      width: 100%;
    }

    .editor-wrapper {
      border-right: none;
      border-bottom: 1px solid var(--border-color);
    }
  }
}
</style>
