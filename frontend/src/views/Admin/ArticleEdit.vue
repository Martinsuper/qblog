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
        <el-button :type="showPreview ? 'primary' : 'default'" @click="togglePreview">
          <el-icon><View /></el-icon>
          {{ showPreview ? '关闭预览' : '分屏预览' }}
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
            <el-icon><Document /></el-icon>
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
      <div class="editor-container" :class="{ 'split-mode': showPreview }">
        <!-- 编辑区 -->
        <div class="editor-wrapper">
          <textarea
            ref="editorRef"
            v-model="articleForm.content"
            class="markdown-editor"
            placeholder="开始写作，支持 Markdown 语法..."
            @input="handleEditorInput"
            @keydown="handleEditorKeydown"
            @scroll="handleScroll"
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
        <!-- 预览区（分屏时显示） -->
        <div v-if="showPreview" class="preview-wrapper" ref="previewRef">
          <div :class="markdownClass" v-html="renderedContent"></div>
        </div>
      </div>

      <!-- 底部状态栏 -->
      <div class="editor-footer">
        <span class="footer-tip">
          <el-icon><InfoFilled /></el-icon>
          支持 Markdown 语法 · 输入 <kbd>/</kbd> 唤起快捷命令
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
  Link as LinkIcon,
  Picture as PictureIcon,
  View,
  InfoFilled,
  Edit as EditIcon,
  FullScreen,
  Connection,
  MessageBox,
  Grid,
  Edit
} from '@element-plus/icons-vue'
import { createArticle, updateArticle, getArticleDetail } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const route = useRoute()
const router = useRouter()
const { render, getThemeClass } = useMarkdown()

const editorRef = ref(null)
const previewRef = ref(null)
const commandMenuRef = ref(null)
const isEdit = ref(false)
const categories = ref([])
const tags = ref([])
const showPreview = ref(false)
const saving = ref(false)
const publishing = ref(false)
const isFullscreen = ref(false)
const editorPanelRef = ref(null)

// 新增状态
const showSettingsModal = ref(false)
const autoSaveStatus = ref('')

// 自动保存
const AUTO_SAVE_INTERVAL = 60000 // 60 秒
let autoSaveTimer = null

const startAutoSave = () => {
  if (autoSaveTimer) return

  autoSaveTimer = setInterval(async () => {
      // 标题和内容都不为空才自动保存
      if (!articleForm.title.trim() || !articleForm.content.trim()) return

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

// 计算属性：已选分类和标签
const selectedCategory = computed(() => {
  if (!articleForm.categoryId) return null
  return categories.value.find(c => c.id === articleForm.categoryId)
})

const selectedTags = computed(() => {
  if (!articleForm.tagIds || articleForm.tagIds.length === 0) return []
  return tags.value.filter(t => articleForm.tagIds.includes(t.id))
})

// 快捷命令菜单状态
const showCommandMenu = ref(false)
const commandMenuStyle = ref({ top: '0px', left: '0px' })
const commandQuery = ref('')
const selectedCommandIndex = ref(0)
const commandStartPosition = ref(0)

// 快捷命令配置
const commands = [
  // 标题
  { name: 'h1', label: '一级标题', description: '大标题', icon: 'H1', action: () => insertText('# ', '') },
  { name: 'h2', label: '二级标题', description: '中标题', icon: 'H2', action: () => insertText('## ', '') },
  { name: 'h3', label: '三级标题', description: '小标题', icon: 'H3', action: () => insertText('### ', '') },
  { name: 'h4', label: '四级标题', description: '更小标题', icon: 'H4', action: () => insertText('#### ', '') },
  { name: 'h5', label: '五级标题', description: '小标题', icon: 'H5', action: () => insertText('##### ', '') },
  { name: 'h6', label: '六级标题', description: '最小标题', icon: 'H6', action: () => insertText('###### ', '') },
  // 文本格式
  { name: 'code', label: '代码块', description: '插入代码块', icon: '💻', action: () => insertCodeBlock() },
  { name: 'inline', label: '行内代码', description: '行内代码', icon: '`', action: () => insertText('`', '`') },
  { name: 'bold', label: '加粗', description: '粗体文字', icon: 'B', action: () => insertText('**', '**') },
  { name: 'italic', label: '斜体', description: '斜体文字', icon: 'I', action: () => insertText('*', '*') },
  { name: 'strike', label: '删除线', description: '删除线文字', icon: 'S', action: () => insertText('~~', '~~') },
  // 列表
  { name: 'list', label: '无序列表', description: '项目列表', icon: '•', action: () => insertText('- ', '') },
  { name: 'olist', label: '有序列表', description: '编号列表', icon: '1.', action: () => insertText('1. ', '') },
  { name: 'task', label: '任务列表', description: '待办事项', icon: '☑', action: () => insertText('- [ ] ', '') },
  // 引用和分割线
  { name: 'quote', label: '引用', description: '引用块', icon: '"', action: () => insertText('> ', '') },
  { name: 'hr', label: '分割线', description: '水平分割线', icon: '—', action: () => insertText('\n---\n', '') },
  // 链接和媒体
  { name: 'link', label: '链接', description: '超链接', icon: '🔗', action: () => insertText('[', '](url)') },
  { name: 'image', label: '图片', description: '插入图片', icon: '🖼', action: () => insertText('![', '](url)') },
  // 表格
  { name: 'table', label: '表格', description: 'Markdown 表格', icon: '📊', action: () => insertTable() },
  // 容器
  { name: 'tip', label: '提示容器', description: '提示信息框', icon: '💡', action: () => insertContainer('tip') },
  { name: 'warning', label: '警告容器', description: '警告信息框', icon: '⚠️', action: () => insertContainer('warning') },
  { name: 'danger', label: '危险容器', description: '危险警告框', icon: '🚨', action: () => insertContainer('danger') },
  { name: 'info', label: '信息容器', description: '信息提示框', icon: 'ℹ️', action: () => insertContainer('info') },
  // 图表
  { name: 'plantuml', label: 'PlantUML', description: 'UML 图表', icon: '📈', action: () => insertPlantUML('sequence') },
  { name: 'sequence', label: '时序图', description: '时序图', icon: '⏱', action: () => insertPlantUML('sequence') },
  { name: 'class', label: '类图', description: '类图', icon: '📦', action: () => insertPlantUML('class') },
]

// 过滤后的命令列表
const filteredCommands = computed(() => {
  if (!commandQuery.value) return commands
  const query = commandQuery.value.toLowerCase()
  return commands.filter(cmd =>
    cmd.name.includes(query) ||
    cmd.label.toLowerCase().includes(query) ||
    cmd.description.toLowerCase().includes(query)
  )
})

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
}

// 滚动同步
const handleScroll = () => {
  if (!showPreview.value || !editorRef.value || !previewRef.value) return

  const editorElement = editorRef.value
  const previewElement = previewRef.value

  // 计算滚动比例
  const scrollRatio = editorElement.scrollTop / (editorElement.scrollHeight - editorElement.clientHeight)

  // 同步预览区滚动
  previewElement.scrollTop = scrollRatio * (previewElement.scrollHeight - previewElement.clientHeight)
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
  fetchCategories()
  fetchTags()
  fetchArticle()
  startAutoSave()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscKey)
  document.documentElement.classList.remove('editor-fullscreen')
  stopAutoSave()
})

// 处理编辑器输入事件
const handleEditorInput = (e) => {
  const textarea = editorRef.value
  if (!textarea) return

  const cursorPos = textarea.selectionStart
  const textBeforeCursor = articleForm.content.substring(0, cursorPos)

  // 查找当前行中最近的 / 符号
  const lastNewLine = textBeforeCursor.lastIndexOf('\n')
  const currentLine = textBeforeCursor.substring(lastNewLine + 1)

  // 检查是否有 / 触发命令菜单
  const slashIndex = currentLine.lastIndexOf('/')

  if (slashIndex !== -1) {
    // 确保斜杠后面没有空格（避免误触发）
    const textAfterSlash = currentLine.substring(slashIndex + 1)
    if (!textAfterSlash.includes(' ')) {
      commandStartPosition.value = lastNewLine + 1 + slashIndex
      commandQuery.value = textAfterSlash
      selectedCommandIndex.value = 0
      updateCommandMenuPosition()
      showCommandMenu.value = true
      return
    }
  }

  // 没有 / 则关闭菜单
  showCommandMenu.value = false
}

// 更新命令菜单位置
const updateCommandMenuPosition = () => {
  const textarea = editorRef.value
  if (!textarea) return

  // 简单定位：使用光标位置估算
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = articleForm.content.substring(0, cursorPos)
  const lines = textBeforeCursor.split('\n')
  const currentLineIndex = lines.length - 1
  const currentCol = lines[lines.length - 1].length

  // 基于行数和列数估算位置
  const lineHeight = 25 // 与 CSS 中的 line-height 1.8 和 font-size 14px 对应
  const charWidth = 8.4 // 等宽字体大约宽度

  const top = Math.min(currentLineIndex * lineHeight + 60, textarea.clientHeight - 300)
  const left = Math.min(currentCol * charWidth + 20, textarea.clientWidth - 250)

  commandMenuStyle.value = {
    top: `${top}px`,
    left: `${left}px`
  }
}

// 处理编辑器键盘事件
const handleEditorKeydown = (e) => {
  if (!showCommandMenu.value) return

  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      selectedCommandIndex.value = Math.min(
        selectedCommandIndex.value + 1,
        filteredCommands.value.length - 1
      )
      scrollToSelectedCommand()
      break
    case 'ArrowUp':
      e.preventDefault()
      selectedCommandIndex.value = Math.max(selectedCommandIndex.value - 1, 0)
      scrollToSelectedCommand()
      break
    case 'Enter':
      e.preventDefault()
      if (filteredCommands.value.length > 0) {
        executeCommand(filteredCommands.value[selectedCommandIndex.value])
      }
      break
    case 'Escape':
      e.preventDefault()
      showCommandMenu.value = false
      break
  }
}

// 滚动到选中的命令
const scrollToSelectedCommand = () => {
  nextTick(() => {
    const menuList = document.querySelector('.command-menu-list')
    const activeItem = document.querySelector('.command-item.active')
    if (menuList && activeItem) {
      activeItem.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  })
}

// 执行命令
const executeCommand = (cmd) => {
  // 先移除命令文本（包括 / 和查询内容）
  const textarea = editorRef.value
  if (!textarea) return

  const start = commandStartPosition.value
  const end = start + 1 + commandQuery.value.length // +1 是 /

  // 移除命令文本
  articleForm.content =
    articleForm.content.substring(0, start) +
    articleForm.content.substring(end)

  // 关闭菜单
  showCommandMenu.value = false

  // 更新光标位置
  nextTick(() => {
    textarea.selectionStart = start
    textarea.selectionEnd = start
    // 执行命令动作
    cmd.action()
  })
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

// 插入代码块
const insertCodeBlock = () => {
  const textarea = editorRef.value
  if (!textarea) return

  const scrollTop = textarea.scrollTop
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = articleForm.content.substring(start, end) || '代码'

  const codeBlock = '\n```\n' + selectedText + '\n```\n'
  const newText = articleForm.content.substring(0, start) + codeBlock + articleForm.content.substring(end)
  articleForm.content = newText

  nextTick(() => {
    textarea.focus()
    textarea.scrollTop = scrollTop
    const newCursorPos = start + 5 // 定位到代码块内部
    textarea.setSelectionRange(newCursorPos, newCursorPos + selectedText.length)
  })
}

// 插入表格
const insertTable = () => {
  const textarea = editorRef.value
  if (!textarea) return

  const scrollTop = textarea.scrollTop
  const start = textarea.selectionStart

  const table = `
| 列1 | 列2 | 列3 |
|-----|-----|-----|
| 内容 | 内容 | 内容 |
`
  const newText = articleForm.content.substring(0, start) + table + articleForm.content.substring(start)
  articleForm.content = newText

  nextTick(() => {
    textarea.focus()
    textarea.scrollTop = scrollTop
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
      // 修复：确保 tagIds 正确提取并过滤无效值，转换为数字类型
      articleForm.tagIds = (data.tags || [])
        .map(t => Number(t.id))
        .filter(id => !isNaN(id) && id > 0)
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

const handleSaveDraft = async () => {
  // 验证标题和内容
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  try {
    saving.value = true
    articleForm.status = 0
    // 如果有 ID 且是编辑模式，才调用更新接口
    if (isEdit.value && articleForm.id) {
      await updateArticle(articleForm.id, articleForm)
    } else {
      // 否则调用创建接口
      const res = await createArticle(articleForm)
      // 新建草稿后保存返回的 ID，后续更新使用
      if (res.data?.id) {
        articleForm.id = res.data.id
        isEdit.value = true
      }
    }
    ElMessage.success('草稿保存成功')
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败：' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
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
</script>

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
  display: flex;
  overflow: hidden;
  position: relative;

  // 分屏模式
  &.split-mode {
    .editor-wrapper {
      width: 50%;
      border-right: 1px solid var(--border-color);
    }

    .markdown-editor {
      overflow-y: auto;
    }
  }

  .editor-wrapper {
    flex: 1;
    height: 100%;
    overflow: hidden;
    position: relative;
    display: flex;
    flex-direction: column;

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
      overflow-y: auto;

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
    width: 50%;
    height: 100%;
    overflow-y: auto;
    background: var(--bg-primary);
    padding: 20px 32px;

    .markdown-body {
      max-width: 100%;
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