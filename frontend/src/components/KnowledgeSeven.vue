<template>
  <div
    class="seven"
    :data-color="color"
    @keydown="focusSearchShortcut"
    @click="menuOpen = false"
  >
    <header>
      <router-link
        class="brand"
        to="/"
        aria-label="知识库首页"
      >
        <span class="brand-mark">y</span>YOUNOTE <small>知识库</small>
      </router-link>
      <div class="workspace">
        个人工作区 <span class="slash">/</span><span class="workspace-name">知识库</span>
      </div>
      <label class="global-search"><span><KnowledgeIcon name="search" /></span><input
        ref="globalInput"
        type="search"
        :value="search"
        placeholder="搜索整个知识库…"
        aria-label="全文搜索整个知识库"
        @input="$emit('search', $event.target.value)"
      ><kbd>/</kbd></label>
      <div
        class="header-right"
        @click.stop
      >
        <button
          class="btn"
          @click="assistantOpen = true"
        >
          <KnowledgeIcon name="spark" />知识助手
        </button>
        <button
          class="avatar"
          title="工作区菜单"
          aria-label="工作区菜单"
          :aria-expanded="menuOpen"
          @click="menuOpen = !menuOpen"
        >
          YN
        </button>
        <div
          v-if="menuOpen"
          class="workspace-menu"
          @keydown.esc="menuOpen = false"
        >
          <button @click="menuOpen = false; $emit('appearance')">
            外观与布局
          </button>
          <button @click="menuOpen = false; $emit('manage-folder')">
            新建目录
          </button>
          <router-link to="/settings/tokens">
            MCP 接入
          </router-link>
          <router-link to="/user/profile">
            账号与密码
          </router-link>
          <button @click="$emit('logout')">
            退出登录
          </button>
        </div>
      </div>
    </header>
    <div class="demo-bar">
      <span><b>方案 G · 目录型知识库</b><span class="extra">&#12288;/&#12288;以阅读、检索与整理为中心</span></span><span>私人知识工作区 · 已连接服务器&#12288; <button
        class="appearance-link"
        @click="$emit('appearance')"
      >外观与布局 ↗</button></span>
    </div>
    <nav
      class="mobile-nav"
      aria-label="切换工作区域"
    >
      <button
        v-for="(label, view) in viewLabels"
        :key="view"
        :class="{ active: mobileView === view }"
        @click="$emit('view', view)"
      >
        {{ label }}
      </button>
    </nav>
    <main
      class="app"
      :data-view="mobileView"
    >
      <aside
        class="sidebar"
        aria-label="知识库导航"
      >
        <div class="section-label">
          工作区 <span>PRIVATE</span>
        </div>
        <button
          v-for="item in scopes"
          :key="item.value"
          class="nav-row"
          :class="{ active: scope === item.value }"
          @click="$emit('scope', item.value)"
        >
          <KnowledgeIcon :name="item.icon" />{{ item.label }}<span class="count">{{ counts[item.value] ?? '—' }}</span>
        </button>
        <hr>
        <div class="section-label">
          知识目录 <button
            class="icon-btn"
            title="折叠或展开全部目录"
            aria-label="折叠或展开全部目录"
            style="width:22px;height:22px"
            @click="$emit('collapse-all')"
          >
            <KnowledgeIcon name="fold" />
          </button>
        </div>
        <div class="tree-filter">
          <input
            v-model="treeSearch"
            type="search"
            placeholder="筛选目录…"
            aria-label="筛选知识目录"
          >
        </div>
        <nav
          class="folder-tree"
          aria-label="多级知识目录"
        >
          <div
            v-for="folder in displayedFolders"
            :key="folder.id"
            class="tree-row"
            :class="{ active: String(scope) === String(folder.id), child: folder.depth > 0 }"
            :style="{ '--depth': folder.depth }"
            @contextmenu.prevent="$emit('manage-folder', folder)"
          >
            <button
              v-if="folder.hasChildren"
              class="tree-toggle"
              :class="{ open: !collapsed.has(folder.id) }"
              :aria-expanded="!collapsed.has(folder.id)"
              :aria-label="`${collapsed.has(folder.id) ? '展开' : '折叠'}${folder.name}`"
              @click="$emit('collapse', folder.id)"
            >
              <KnowledgeIcon name="chevron" />
            </button><span
              v-else
              class="tree-toggle"
            />
            <button
              class="tree-name"
              @click="$emit('scope', folder.id)"
            >
              <KnowledgeIcon name="folder" /><span class="folder-label">{{ folder.name }}</span><span class="count">{{ folder.count }}</span>
            </button>
            <button
              class="tree-manage"
              title="管理目录"
              :aria-label="`管理目录 ${folder.name}`"
              @click="$emit('manage-folder', folder)"
            >
              ⋯
            </button>
          </div>
        </nav>
        <div class="sidebar-bottom">
          <div class="local-info">
            <strong><span class="storage-dot" />私人知识工作区</strong><br>笔记安全保存到服务器<br>{{ counts.all ?? 0 }} 篇笔记 · {{ folders.length }} 个目录<br><button
              class="about-link"
              @click="$emit('appearance')"
            >
              外观与布局 ↗
            </button>
          </div>
        </div>
      </aside>
      <section
        class="list-pane"
        aria-label="笔记列表"
      >
        <div class="list-head">
          <div class="list-title-row">
            <h2>{{ scopeTitle }} <span>{{ total }}</span></h2><button
              class="icon-btn"
              title="新建笔记"
              aria-label="新建笔记"
              @click="$emit('new-note')"
            >
              <KnowledgeIcon name="plus" />
            </button>
          </div>
          <label class="list-search"><KnowledgeIcon name="search" /><input
            type="search"
            :value="listSearch"
            placeholder="在当前目录中搜索正文…"
            aria-label="搜索当前笔记列表"
            @input="$emit('list-search', $event.target.value)"
          ></label>
          <div class="filters">
            <select
              :value="tag"
              aria-label="按标签筛选"
              @change="$emit('tag', $event.target.value)"
            >
              <option value="">
                全部标签
              </option><option
                v-for="value in tagOptions"
                :key="value"
                :value="value"
              >
                {{ value }}
              </option>
            </select><select
              :value="source"
              aria-label="按来源筛选"
              @change="$emit('source', $event.target.value)"
            >
              <option value="">
                全部来源
              </option><option value="manual">
                手动创建
              </option><option value="mcp">
                MCP 导入
              </option>
            </select>
          </div>
        </div>
        <div class="list-caption">
          <span>{{ total }} 篇笔记</span><button
            class="sort-button"
            title="切换排序"
            @click="$emit('sort')"
          >
            {{ sort === 'title' ? '标题排序 A–Z' : '最近更新 ↓' }}
          </button>
        </div>
        <div class="note-list">
          <div
            v-if="listError"
            class="empty"
          >
            加载失败，请重试。<p>
              <button
                class="btn"
                @click="$emit('retry-list')"
              >
                重新加载
              </button>
            </p>
          </div>
          <div
            v-else-if="loadingList"
            class="empty"
          >
            正在加载笔记…
          </div>
          <template v-else>
            <button
              v-for="note in notes"
              :key="note.id"
              class="note-item"
              :class="{ selected: active?.id === note.id }"
              :aria-pressed="active?.id === note.id"
              @click="$emit('open-note', note.id)"
            >
              <h3>
                {{ note.title }} <span
                  v-if="note.favorite"
                  class="note-star"
                >☆</span>
              </h3><div class="excerpt">
                {{ note.summary || '暂无摘要' }}
              </div><div class="note-meta">
                <span
                  class="pill"
                  :class="{ 'source-mcp': note.source === 'mcp' }"
                >{{ note.source === 'mcp' ? 'MCP 导入' : '手动' }}</span><span>{{ (note.tags || []).slice(0, 2).join(' · ') }}</span><span class="date">{{ shortDate(note.updateTime) }}</span>
              </div>
            </button><div
              v-if="!notes.length"
              class="empty"
            >
              没有匹配的笔记<p>
                <button
                  class="btn"
                  @click="$emit('new-note')"
                >
                  写下第一篇笔记
                </button>
              </p>
            </div>
          </template>
        </div>
        <div class="list-foot">
          <template v-if="total > pageSize">
            <button
              :disabled="page <= 1 || loadingList"
              aria-label="上一页"
              @click="$emit('page', -1)"
            >
              ←
            </button> {{ page }} / {{ Math.ceil(total / pageSize) }} <button
              :disabled="page * pageSize >= total || loadingList"
              aria-label="下一页"
              @click="$emit('page', 1)"
            >
              →
            </button>
          </template><template v-else>
            正文参与搜索 · 标签 / 来源可组合筛选
          </template>
        </div>
      </section>
      <section
        ref="readerElement"
        class="reader"
        aria-label="笔记正文"
      >
        <div class="reader-toolbar">
          <div class="doc-crumb">
            {{ active ? folderName(active.folderId) : '知识库' }}
          </div><template v-if="active">
            <button
              class="icon-btn"
              :class="{ on: draft.favorite }"
              :title="draft.favorite ? '取消收藏' : '收藏笔记'"
              :aria-label="draft.favorite ? '取消收藏' : '收藏笔记'"
              :aria-pressed="draft.favorite"
              @click="$emit('favorite')"
            >
              <KnowledgeIcon name="star" />
            </button><button
              class="btn"
              @click="$emit('move')"
            >
              <KnowledgeIcon name="folder" />移动
            </button><button
              class="icon-btn"
              :class="{ on: infoOpen }"
              title="切换正文目录与来源信息"
              aria-label="切换正文目录与来源信息"
              :aria-expanded="infoOpen"
              @click="toggleInfo"
            >
              <KnowledgeIcon name="panel" />
            </button>
          </template>
        </div>
        <div
          v-if="loadingNote"
          class="empty"
        >
          正在打开文档…
        </div><div
          v-else-if="!active"
          class="empty"
        >
          从左侧打开一篇笔记，开始阅读和整理。
        </div>
        <div
          v-else
          class="reading-area"
        >
          <article class="document">
            <div class="eyebrow">
              KNOWLEDGE / {{ leafFolder(active.folderId) }}
            </div>
            <textarea
              ref="titleElement"
              class="document-title"
              rows="1"
              :value="draft.title"
              maxlength="120"
              aria-label="笔记标题"
              @input="titleInput"
              @keydown.enter.prevent
            />
            <div class="doc-properties">
              <span>{{ active.source === 'mcp' ? 'MCP 导入' : '手动创建' }}</span><span class="divider">/</span><span>更新于 {{ fullDate(active.updateTime) }}</span><span class="divider">/</span><span>{{ draft.content.replace(/\s/g, '').length.toLocaleString() }} 字符</span><span class="divider">/</span><span class="status pending">索引未配置</span>
            </div>
            <div class="tags">
              <button
                v-for="value in draft.tags"
                :key="value"
                class="tag-btn"
                @click="$emit('filter-tag', value)"
              >
                # {{ value }}
              </button><button
                class="tag-btn tag-edit"
                title="编辑标签"
                aria-label="编辑标签"
                @click="$emit('tags')"
              >
                <KnowledgeIcon name="plus" />
              </button>
            </div>
            <div
              v-if="conflicted"
              class="save-alert"
              role="alert"
            >
              此笔记已被其他窗口或 MCP 更新，自动保存已暂停。当前草稿仍保留。<div>
                <button
                  class="btn"
                  @click="$emit('download-draft')"
                >
                  下载当前草稿
                </button><button
                  class="btn"
                  @click="$emit('reload-conflict')"
                >
                  载入最新版本
                </button>
              </div>
            </div>
            <div
              v-else-if="saveFailed"
              class="save-alert"
              role="alert"
            >
              {{ saveStatus }}<button
                class="btn"
                @click="$emit('save')"
              >
                重试保存
              </button>
            </div>
            <div class="markdown">
              <KnowledgeEditor
                :key="active.id"
                :model-value="draft.content"
                quiet-toolbar
                @update:model-value="contentInput"
                @headings="headings = $event"
              />
            </div>
            <div class="doc-end">
              <span>YOUNOTE · 让知识有序可寻</span><span
                role="status"
                aria-live="polite"
              >{{ saveStatus || '直接编辑 · 停顿后自动保存' }}</span>
            </div>
          </article>
          <aside
            v-show="infoOpen || narrowExpanded"
            class="info-rail"
            :class="{ expanded: narrowExpanded }"
            aria-label="正文目录与来源"
          >
            <div class="rail-title">
              本文目录
            </div><nav
              class="toc"
              aria-label="本文目录"
            >
              <button
                v-for="(heading, index) in headings"
                :key="index"
                :class="{ nested: heading.level > 2 }"
                @click="scrollToHeading(index)"
              >
                {{ heading.text }}
              </button><span
                v-if="!headings.length"
                class="tiny muted"
              >暂无小标题</span>
            </nav>
            <div class="source-block">
              <div class="rail-title">
                来源与记录
              </div><strong>{{ active.source === 'mcp' ? 'MCP 导入' : '手动创建' }}</strong><p>目录：{{ folderName(active.folderId) }}<br>内容类型：私人笔记</p><p><span class="status pending">索引未配置</span></p><br><div class="rail-title">
                最近更新记录
              </div><p>{{ fullDate(active.updateTime) }}<br>版本 {{ active.version }}</p><p
                v-if="active.sourceTool"
                class="mono"
              >
                工具：{{ active.sourceTool }}
              </p><button
                class="record-actions"
                @click="recordOpen = true"
              >
                笔记管理 ↗
              </button>
            </div>
          </aside>
        </div>
      </section>
    </main>
    <el-dialog
      v-model="assistantOpen"
      title="知识助手"
      width="560px"
      class="kb-dialog"
    >
      <p>AI 服务尚未配置。你可以先浏览和整理已有笔记。</p><div class="assistant-summary">
        {{ counts.all ?? 0 }} 篇笔记 · {{ counts.inbox ?? 0 }} 篇待归档 · {{ counts.favorites ?? 0 }} 篇收藏
      </div><div class="assistant-buttons">
        <el-button @click="assistantOpen = false; $emit('scope', 'inbox')">
          查看待归档笔记
        </el-button><el-button @click="assistantOpen = false; $emit('source', 'mcp')">
          检查 MCP 来源
        </el-button>
      </div><template #footer>
        <el-button @click="assistantOpen = false">
          返回阅读
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="recordOpen"
      title="笔记管理"
      width="430px"
      class="kb-dialog"
    >
      <p>笔记保存在服务器。自动保存会检测与其他窗口或 MCP 的版本冲突。</p><template #footer>
        <el-button @click="recordOpen = false; $emit('download-draft')">
          下载 Markdown
        </el-button><el-button
          type="danger"
          plain
          @click="recordOpen = false; $emit('delete')"
        >
          删除笔记
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, onBeforeUnmount } from 'vue'
import KnowledgeIcon from './KnowledgeIcon.vue'
import KnowledgeEditor from './KnowledgeEditor.vue'
const props = defineProps({
  color: String, folders: { type: Array, default: () => [] }, flatFolders: { type: Array, default: () => [] },
  allFolders: { type: Array, default: () => [] }, collapsed: { type: Set, default: () => new Set() },
  counts: { type: Object, default: () => ({}) }, notes: { type: Array, default: () => [] },
  scope: [String, Number], scopeTitle: String, search: String, listSearch: String, tag: String, source: String, sort: String,
  active: Object, draft: { type: Object, required: true }, mobileView: String,
  total: Number, page: Number, pageSize: Number, loadingList: Boolean, listError: Boolean,
  loadingNote: Boolean, saveStatus: String, saveFailed: Boolean, conflicted: Boolean, folderName: Function
})
const emit = defineEmits(['appearance', 'logout', 'scope', 'search', 'list-search', 'tag', 'source', 'sort', 'view', 'collapse', 'collapse-all', 'manage-folder', 'new-note', 'open-note', 'retry-list', 'page', 'favorite', 'move', 'tags', 'dirty', 'save', 'download-draft', 'reload-conflict', 'filter-tag', 'delete', 'reader-element'])
const treeSearch = ref(''), menuOpen = ref(false), assistantOpen = ref(false), recordOpen = ref(false)
const infoOpen = ref(true), narrowExpanded = ref(false), readerElement = ref(null), titleElement = ref(null), globalInput = ref(null), headings = ref([])
const scopes = [{ value: 'all', label: '全部笔记', icon: 'layers' }, { value: 'inbox', label: '收件箱', icon: 'inbox' }, { value: 'favorites', label: '我的收藏', icon: 'star' }]
const viewLabels = { tree: '目录', list: '笔记列表', reader: '正文阅读' }
const displayedFolders = computed(() => {
  const term = treeSearch.value.trim().toLowerCase()
  if (!term) return props.flatFolders
  const matches = props.allFolders.filter(f => f.path.toLowerCase().includes(term))
  const ids = new Set(matches.map(f => f.id))
  for (const f of matches) { let parent = f.parentId; while (parent) { ids.add(parent); parent = props.allFolders.find(item => item.id === parent)?.parentId } }
  return props.allFolders.filter(f => ids.has(f.id))
})
const tagOptions = computed(() => [...new Set([props.tag, ...props.notes.flatMap(n => n.tags || []), ...(props.draft.tags || [])].filter(Boolean))].sort((a, b) => a.localeCompare(b, 'zh-CN')))
function fullDate(value) { return value ? String(value).slice(0, 10) : '—' }
function shortDate(value) { return value ? String(value).slice(5, 10).replace('-', '/') : '' }
function leafFolder(id) { return props.folderName(id).split(' / ').at(-1) }
function resizeTitle() { if (titleElement.value) { titleElement.value.style.height = 'auto'; titleElement.value.style.height = titleElement.value.scrollHeight + 'px' } }
function titleInput(event) { props.draft.title = event.target.value; resizeTitle(); emit('dirty') }
function contentInput(value) { props.draft.content = value; emit('dirty') }
function scrollToHeading(index) { readerElement.value?.querySelectorAll('.tiptap h1, .tiptap h2, .tiptap h3')[index]?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
function toggleInfo() { if (window.innerWidth <= 1100) narrowExpanded.value = !narrowExpanded.value; else infoOpen.value = !infoOpen.value }
function focusSearchShortcut(event) { if (event.key === '/' && !event.target.closest('input,textarea,[contenteditable=true]')) { event.preventDefault(); globalInput.value?.focus() } if (event.key === 'Escape') menuOpen.value = false }
watch(readerElement, element => emit('reader-element', element), { immediate: true })
watch(() => [props.active?.id, props.draft.title, props.loadingNote], () => nextTick(resizeTitle), { immediate: true })
watch(() => props.active?.id, () => { headings.value = [] })
let titleObserver
watch(titleElement, element => {
  titleObserver?.disconnect()
  if (!element) return
  let width = -1
  titleObserver = new ResizeObserver(entries => {
    const nextWidth = entries[0].contentRect.width
    if (nextWidth !== width) { width = nextWidth; resizeTitle() }
  })
  titleObserver.observe(element)
})
onBeforeUnmount(() => titleObserver?.disconnect())
</script>

<style scoped src="../styles/knowledge-seven.css"></style>
<style scoped>
.seven { width:100%; height:100dvh; overflow:hidden; color-scheme:light; }
.seven .brand { color:var(--blue); }
.workspace-name { color:#7a899e; }
.header-right { position:relative; }
.appearance-link { color:#6e7e93; padding:0; font-size:10px; }
.workspace-menu { position:absolute; top:39px; right:0; z-index:12; width:160px; padding:6px; background:var(--panel); border:1px solid var(--line); border-radius:6px; box-shadow:0 8px 24px #20334b22; }
.workspace-menu :is(button,a) { display:block; width:100%; text-align:left; padding:8px 10px; color:var(--ink); font-size:12px; }
.folder-label { min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.tree-row { padding-left:calc(var(--depth) * 18px); position:relative; }
.tree-row.child::before { content:''; position:absolute; top:0; bottom:0; left:calc(var(--depth) * 18px - 3px); border-left:1px solid #dfe5ed; }
.tree-manage { position:absolute; right:23px; opacity:0; background:var(--soft); border-radius:3px; }
.tree-row:hover .tree-manage,.tree-row:focus-within .tree-manage { opacity:1; }
.new-folder { display:flex; align-items:center; gap:7px; padding:8px 10px; margin-top:6px; color:var(--muted); font-size:11px; }
.about-link { padding:5px 0; color:#6e85a2; font-size:10px; }
.note-star { color:#b29970; font-size:11px; }
.sort-button { font-size:10px; padding:2px; border-radius:3px; }
.document-title { display:block; width:100%; min-height:39px; resize:none; overflow:hidden; padding:0; border:0; outline:none; background:transparent; color:#25344a; font:630 26px/1.5 var(--font); letter-spacing:-.7px; margin:0 0 14px; border-radius:0; }
.document-title:focus-visible { outline:none; }
.tag-edit { opacity:0; padding:3px 5px; }
.tag-edit svg { width:12px; height:12px; }
.tags:hover .tag-edit,.tags:focus-within .tag-edit { opacity:1; }
.tags:empty { min-height:0; }
.record-actions { margin-top:12px; padding:0; color:#6e85a2; font-size:10px; }
.toc .nested { padding-left:10px; }
.save-alert { color:#a65c66; padding:14px; border:1px solid #e4bdc5; border-radius:4px; font-size:11px; margin:14px 0; }
.save-alert .btn { margin:8px 8px 0 0; }
.assistant-summary { padding:14px; background:#f0f4fa; color:#647b99; }
.assistant-buttons { display:flex; flex-wrap:wrap; gap:8px; margin-top:18px; }
.markdown :deep(*) { box-sizing:border-box; }
.markdown :deep(.tiptap) { min-height:220px; outline:none; font:13px/1.95 var(--font); color:#4c5a6f; overflow-wrap:anywhere; }
.markdown :deep(.tiptap p) { margin:12px 0 17px; }
.markdown :deep(.tiptap h2) { font-size:17px; font-weight:600; color:#33445c; padding:4px 0 9px; border-bottom:1px solid #e8edf3; margin:28px 0 15px; scroll-margin-top:70px; }
.markdown :deep(.tiptap h3) { font-size:14px; color:#3a4e68; font-weight:600; margin:23px 0 10px; scroll-margin-top:70px; }
.markdown :deep(.tiptap h2:first-child) { margin-top:0; }
.markdown :deep(.tiptap strong) { font-weight:600; color:#3a4a60; }
.markdown :deep(.tiptap ul),.markdown :deep(.tiptap ol) { padding-left:19px; margin:13px 0 19px; list-style:disc; }
.markdown :deep(.tiptap ol) { list-style:decimal; }
.markdown :deep(.tiptap li) { padding-left:4px; margin:5px 0; }
.markdown :deep(.tiptap li p) { margin:0; }
.markdown :deep(.tiptap blockquote) { margin:19px 0; border-left:3px solid #9fb5d3; background:#f5f8fc; padding:11px 15px; font-size:12px; color:#647b99; }
.markdown :deep(.tiptap blockquote p) { margin:0; }
.markdown :deep(.tiptap table) { border-collapse:collapse; width:100%; font-size:11px; margin:19px 0 24px; line-height:1.8; }
.markdown :deep(.tiptap table) { display:block; overflow-x:auto; }
.markdown :deep(.tiptap .tableWrapper) { overflow-x:auto; margin:19px 0 24px; }
.markdown :deep(.tiptap .tableWrapper table) { margin:0; }
.markdown :deep(.tiptap th) { font-weight:500; background:#f3f6fa; color:#576b88; }
.markdown :deep(.tiptap th),.markdown :deep(.tiptap td) { border:1px solid #e1e7ef; padding:9px 12px; text-align:left; min-width:90px; }
.markdown :deep(.tiptap td p),.markdown :deep(.tiptap th p) { margin:0; }
.markdown :deep(.tiptap code) { font:11px/1.7 ui-monospace,SFMono-Regular,Consolas,monospace; background:#f0f3f7; border-radius:3px; padding:2px 5px; color:#526b8b; }
.markdown :deep(.tiptap pre) { background:#f5f7fa; border:1px solid #e5eaf1; border-radius:5px; padding:17px; overflow-x:auto; margin:18px 0 22px; }
.markdown :deep(.tiptap pre code) { padding:0; background:none; white-space:pre; color:#57718e; line-height:1.85; }
.markdown :deep(.tiptap img) { max-width:100%; height:auto; }
.markdown :deep(.tiptap .selectedCell) { background:#eaf0f9; }
.markdown :deep(.knowledge-editor) { position:relative; }
.markdown :deep(.format-bar) { position:absolute; bottom:100%; left:0; z-index:3; align-items:center; gap:3px; display:none; padding:5px; border:1px solid var(--line); background:var(--panel); border-radius:5px; box-shadow:0 3px 14px #20334b16; }
.markdown :deep(.knowledge-editor:focus-within .format-bar) { display:flex; }
.markdown :deep(.format-bar button) { width:27px; height:27px; font-size:11px; border-radius:3px; }
.markdown :deep(.format-bar button:hover),.markdown :deep(.format-bar button.active) { background:var(--blue-bg); }
.markdown :deep(.format-bar>span) { display:none; }
@media(min-width:1700px) { .document-title { font-size:29px; } .markdown :deep(.tiptap) { font-size:14px; } }
@media(max-width:1260px) { .document-title { font-size:23px; min-height:34.5px; } }
@media(max-width:900px) { .document-title { font-size:22px; min-height:33px; } }
@media(prefers-reduced-motion:reduce) { * { scroll-behavior:auto!important; transition:none!important; } }
</style>

<style scoped>
.seven[data-color='dark'] { color-scheme:dark; --bg:#151d28; --panel:#1c2633; --ink:#dce5f1; --muted:#98a8bd; --line:#334257; --blue:#9abded; --blue-bg:#2b405d; --soft:#222f40; --green:#8ab6a1; }
.seven[data-color='dark'] :is(header,.reader,.reader-toolbar,.btn,.global-search kbd,.list-search input) { background:var(--panel); color:var(--ink); border-color:var(--line); }
.seven[data-color='dark'] :is(.demo-bar,.sidebar,.global-search input) { background:var(--bg); border-color:var(--line); }
.seven[data-color='dark'] .list-pane { background:var(--panel); }
.seven[data-color='dark'] :is(.nav-row.active,.tree-row.active,.note-item.selected,.note-item:hover) { background:var(--blue-bg); color:var(--blue); }
.seven[data-color='dark'] :is(.note-item h3,.document-title,.workspace-name,.nav-row,.tree-name,.toc button,.source-block strong) { color:var(--ink); }
.seven[data-color='dark'] :is(.filters select,.tree-filter input,.pill,.tag-btn) { background:var(--soft); border-color:var(--line); color:var(--muted); }
.seven[data-color='dark'] :deep(.tiptap) { color:var(--ink); }
.seven[data-color='dark'] :deep(.tiptap h2),.seven[data-color='dark'] :deep(.tiptap h3),.seven[data-color='dark'] :deep(.tiptap strong) { color:var(--ink); border-color:var(--line); }
.seven[data-color='dark'] :deep(.tiptap th),.seven[data-color='dark'] :deep(.tiptap td) { color:var(--ink); border-color:var(--line); }
.seven[data-color='dark'] :deep(.tiptap pre),.seven[data-color='dark'] :deep(.tiptap th),.seven[data-color='dark'] :deep(.tiptap code),.seven[data-color='dark'] :deep(.tiptap blockquote) { background:var(--soft); color:var(--muted); border-color:var(--line); }
</style>
