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
          <el-icon><Document /></el-icon>
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
            <el-divider direction="vertical" />
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
            <div :class="markdownClass" v-html="renderedContent"></div>
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
import { useMarkdown } from '@/composables/useMarkdown'
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
  MessageBox
} from '@element-plus/icons-vue'
import { createArticle, updateArticle, getArticleDetail } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const route = useRoute()
const router = useRouter()
const { render, getThemeClass } = useMarkdown()

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

// 渲染后的内容
const renderedContent = computed(() => {
  return render(articleForm.content)
})

// Markdown 样式类
const markdownClass = computed(() => getThemeClass())

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

  // 保存当前滚动位置
  const scrollTop = textarea.scrollTop

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = articleForm.content.substring(start, end) || '文本'

  const newText = articleForm.content.substring(0, start) + before + selectedText + after + articleForm.content.substring(end)
  articleForm.content = newText

  nextTick(() => {
    textarea.focus()
    // 恢复滚动位置
    textarea.scrollTop = scrollTop
    textarea.setSelectionRange(start + before.length, start + before.length + selectedText.length)
  })
}

// PlantUML 模板
const plantUMLTemplates = {
  sequence: `@startuml
用户 -> 系统: 请求
系统 --> 用户: 响应
@enduml`,
  class: `@startuml
class User {
  +id: Long
  +name: String
  +login()
}
class Article {
  +id: Long
  +title: String
  +content: String
}
User --> Article: 发布
@enduml`,
  usecase: `@startuml
:user: as 用户
:系统: as 系统

用户 --> (登录)
用户 --> (查看文章)
用户 --> (发表评论)
(登录) --> 系统
@enduml`,
  activity: `@startuml
start
:接收请求;
if (验证通过？) then (是)
  :处理业务;
  stop
else (否)
  :返回错误;
  stop
endif
@enduml`,
  component: `@startuml
[前端] --> [API 网关]
[API 网关] --> [服务 A]
[API 网关] --> [服务 B]
[服务 A] --> [(数据库 A)]
[服务 B] --> [(数据库 B)]
@enduml`,
  deployment: `@startuml
node "服务器" {
  component "应用服务" as app
  database "数据库" as db
}

internet --> app
app --> db
@enduml`
}

const insertPlantUML = (type) => {
  const template = plantUMLTemplates[type] || plantUMLTemplates.sequence
  const textarea = editorRef.value
  if (!textarea) return

  // 保存当前滚动位置
  const scrollTop = textarea.scrollTop

  const start = textarea.selectionStart
  const end = textarea.selectionEnd

  // 在光标位置插入 PlantUML 代码块
  const plantumlBlock = `\n\`\`\`plantuml\n${template}\n\`\`\`\n`

  const newText = articleForm.content.substring(0, start) + plantumlBlock + articleForm.content.substring(end)
  articleForm.content = newText

  nextTick(() => {
    textarea.focus()
    // 恢复滚动位置
    textarea.scrollTop = scrollTop
    // 将光标定位到代码块开始位置（在 plantuml 关键字后面）
    const newCursorPos = start + 12 // \n\`\`\`plantuml\n 的长度
    textarea.setSelectionRange(newCursorPos, newCursorPos)
  })
}

// 插入容器
const insertContainer = (type) => {
  const textarea = editorRef.value
  if (!textarea) return

  const scrollTop = textarea.scrollTop
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = articleForm.content.substring(start, end) || '在这里写内容'

  const containerBlock = `\n::: ${type}\n${selectedText}\n:::\n`

  const newText = articleForm.content.substring(0, start) + containerBlock + articleForm.content.substring(end)
  articleForm.content = newText

  nextTick(() => {
    textarea.focus()
    textarea.scrollTop = scrollTop
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
  // 使用 query 参数获取 ID（编辑时 URL 为 /admin/create?id=123）
  const id = route.query.id
  if (id) {
    isEdit.value = true
    articleForm.id = Number(id)
    try {
      const res = await getArticleDetail(id)

      const data = res.data

      if (!data) {
        ElMessage.warning('未获取到文章数据')
        return
      }

      // 直接逐个字段赋值，确保响应式更新
      articleForm.id = data.id || Number(id)
      articleForm.title = data.title || ''
      articleForm.summary = data.summary || ''
      articleForm.content = data.content || ''
      articleForm.coverImage = data.coverImage || ''
      articleForm.categoryId = data.categoryId || data.category?.id || null
      articleForm.tagIds = data.tagIds || data.tags?.map(t => t.id) || []
      articleForm.status = data.status || 1

      // 验证内容是否真的填充了
      if (data.content && articleForm.content === '') {
        console.error('❌ 内容字段填充失败！')
      }
    } catch (error) {
      console.error('获取文章失败:', error)
      ElMessage.error('获取文章失败：' + (error.message || '未知错误'))
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
    // 如果有 ID 且是编辑模式，才调用更新接口
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

const handleSaveDraft = async () => {
  try {
    saving.value = true
    articleForm.status = 0
    // 如果有 ID 且是编辑模式，才调用更新接口
    if (isEdit.value && articleForm.id) {
      await updateArticle(articleForm.id, articleForm)
    } else {
      // 否则调用创建接口
      await createArticle(articleForm)
    }
    ElMessage.success('草稿保存成功')
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败：' + (error.message || '未知错误'))
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

  // 确保 dropdown 菜单在全屏模式下也能正常显示
  .el-dropdown__popper {
    z-index: 10000 !important;
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

    .markdown-body {
      padding: 20px;
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