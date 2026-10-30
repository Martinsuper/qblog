<template>
  <div
    class="seven"
    :class="{ 'focus-mode': focusMode }"
    :data-density="density"
    :data-color="color"
    @keydown="focusSearchShortcut"
    @click="menuOpen = false; closeMenu()"
    @contextmenu.self.prevent="closeMenu()"
  >
    <header>
      <router-link
        class="brand"
        to="/"
        aria-label="知识库首页"
      >
        <span class="brand-mark">Q</span>QBlog
      </router-link>
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
          class="avatar"
          title="工作区菜单"
          aria-label="工作区菜单"
          :aria-expanded="menuOpen"
          @click="menuOpen = !menuOpen"
        >
          <KnowledgeIcon name="layers" />
        </button>
        <div
          v-if="menuOpen"
          class="workspace-menu"
          @keydown.esc="menuOpen = false"
        >
          <router-link to="/settings/appearance">
            外观与布局
          </router-link>
          <button @click="menuOpen = false; $emit('manage-folder')">
            新建目录
          </button>
          <router-link to="/admin/backup">
            备份与导出
          </router-link>
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
      ref="appElement"
      class="app"
      :class="{ 'tree-collapsed': !paneOpen[0], 'list-collapsed': !paneOpen[1] }"
      :style="paneStyle"
      :data-view="mobileView"
    >
      <aside
        id="knowledge-tree-pane"
        class="sidebar"
        aria-label="知识库导航"
      >
        <div class="section-label">
          笔记
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
          目录 <div class="directory-actions">
            <button
              class="icon-btn"
              title="筛选目录"
              aria-label="筛选目录"
              :aria-expanded="treeSearchOpen"
              @click="treeSearchOpen = !treeSearchOpen; treeSearch = ''"
            >
              <KnowledgeIcon name="search" />
            </button><button
              class="icon-btn"
              title="折叠或展开全部目录"
              aria-label="折叠或展开全部目录"
              style="width:22px;height:22px"
              @click="$emit('collapse-all')"
            >
              <KnowledgeIcon name="fold" />
            </button>
          </div>
        </div>
        <div
          v-if="treeSearchOpen"
          class="tree-filter"
        >
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
            @contextmenu.prevent="openFolderMenu($event, folder)"
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
        <button
          class="new-folder"
          aria-label="新建目录"
          @click="$emit('manage-folder')"
        >
          <KnowledgeIcon name="plus" />新建目录
        </button>
      </aside>
      <div
        v-if="paneOpen[0]"
        class="pane-splitter splitter-tree"
        role="separator"
        tabindex="0"
        aria-label="调整目录宽度"
        aria-orientation="vertical"
        aria-controls="knowledge-tree-pane"
        :aria-valuemin="Math.min(150, paneMax(0))"
        :aria-valuemax="paneMax(0)"
        :aria-valuenow="Math.round(layout.widths[0])"
        @pointerdown="startResize($event, 0)"
        @pointermove="moveResize"
        @pointerup="endResize"
        @pointercancel="endResize"
        @lostpointercapture="endResize"
        @keydown="resizeKey($event, 0)"
      />
      <button
        class="pane-toggle toggle-tree"
        :aria-expanded="paneOpen[0]"
        aria-controls="knowledge-tree-pane"
        :aria-label="`${paneOpen[0] ? '收起' : '恢复'}目录栏`"
        :title="`${paneOpen[0] ? '收起' : '恢复'}目录栏`"
        @click="togglePane(0)"
      >
        <KnowledgeIcon name="chevron" />
      </button>
      <section
        id="knowledge-list-pane"
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
            placeholder="搜索当前目录…"
            aria-label="搜索当前笔记列表"
            @input="$emit('list-search', $event.target.value)"
          ></label>
          <div class="list-controls">
            <button
              class="filter-toggle"
              :class="{ on: tag || source }"
              :aria-expanded="filtersOpen"
              @click="filtersOpen = !filtersOpen"
            >
              筛选与排序{{ tag || source ? ' · 已启用' : '' }}
            </button>
            <button
              class="density-toggle"
              :aria-pressed="density === 'compact'"
              @click="toggleDensity"
            >
              {{ density === 'compact' ? '紧凑' : '标准' }}
            </button>
          </div>
          <div
            v-if="filtersOpen"
            class="filters"
          >
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
            <button
              class="sort-button"
              title="切换排序"
              @click="$emit('sort')"
            >
              {{ sort === 'title' ? '标题 A–Z' : '最近更新 ↓' }}
            </button>
          </div>
          <div
            v-if="search || listSearch || tag || source"
            class="active-filters"
            aria-label="当前搜索与筛选"
          >
            <button
              v-if="search"
              @click="$emit('search', '')"
            >
              全局：{{ search }} ×
            </button>
            <button
              v-if="listSearch"
              @click="$emit('list-search', '')"
            >
              目录内：{{ listSearch }} ×
            </button>
            <button
              v-if="tag"
              @click="$emit('tag', '')"
            >
              标签：{{ tag }} ×
            </button>
            <button
              v-if="source"
              @click="$emit('source', '')"
            >
              来源：{{ source === 'mcp' ? 'MCP' : '手动' }} ×
            </button>
          </div>
        </div>
        <div
          class="note-list"
          :aria-busy="loadingList"
          @keydown="navigateNotes"
        >
          <div
            v-if="listError"
            class="empty"
            role="alert"
          >
            无法加载笔记列表<p class="feedback-description">
              请检查网络连接后重试，已打开的笔记不会被清除。
            </p><p>
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
            role="status"
            aria-live="polite"
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
              @contextmenu.prevent="openNoteMenu($event, note)"
              @keydown.shift.f10.prevent="openKeyboardNoteMenu($event, note)"
            >
              <h3>
                <span class="note-title">{{ note.title || '未命名笔记' }}</span><span
                  v-if="note.favorite"
                  class="note-star"
                >★</span><time
                  class="note-date"
                  :datetime="note.updateTime || undefined"
                >{{ shortDate(note.updateTime) }}</time>
              </h3><div
                v-if="note.summary"
                class="excerpt"
              >
                {{ note.summary }}
              </div>
            </button><div
              v-if="!notes.length"
              class="empty"
              role="status"
            >
              <strong>{{ emptyFeedback.title }}</strong>
              <p class="feedback-description">
                {{ emptyFeedback.description }}
              </p>
              <button
                v-if="emptyFeedback.action === 'clear'"
                class="btn"
                @click="clearFilters"
              >
                清除搜索与筛选
              </button>
              <button
                v-else-if="emptyFeedback.action === 'all'"
                class="btn"
                @click="$emit('scope', 'all')"
              >
                查看全部笔记
              </button>
              <button
                v-else
                class="btn"
                @click="$emit('new-note')"
              >
                新建笔记
              </button>
            </div>
          </template>
        </div>
        <div
          v-if="total > pageSize"
          class="list-foot"
        >
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
          </template>
        </div>
      </section>
      <div
        v-if="paneOpen[1]"
        class="pane-splitter splitter-list"
        role="separator"
        tabindex="0"
        aria-label="调整笔记列表宽度"
        aria-orientation="vertical"
        aria-controls="knowledge-list-pane"
        :aria-valuemin="Math.min(200, paneMax(1))"
        :aria-valuemax="paneMax(1)"
        :aria-valuenow="Math.round(layout.widths[1])"
        @pointerdown="startResize($event, 1)"
        @pointermove="moveResize"
        @pointerup="endResize"
        @pointercancel="endResize"
        @lostpointercapture="endResize"
        @keydown="resizeKey($event, 1)"
      />
      <button
        class="pane-toggle toggle-list"
        :aria-expanded="paneOpen[1]"
        aria-controls="knowledge-list-pane"
        :aria-label="`${paneOpen[1] ? '收起' : '恢复'}笔记列表栏`"
        :title="`${paneOpen[1] ? '收起' : '恢复'}笔记列表栏`"
        @click="togglePane(1)"
      >
        <KnowledgeIcon name="chevron" />
      </button>
      <section
        ref="readerPanel"
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
              v-if="headings.length"
              class="icon-btn"
              :class="{ on: infoOpen }"
              title="本文目录"
              aria-label="本文目录"
              :aria-expanded="infoOpen || narrowExpanded"
              @click="toggleInfo"
            >
              <KnowledgeIcon name="panel" />
            </button><button
              class="icon-btn"
              title="专注模式：隐藏界面并进入全屏（Esc 退出）"
              aria-label="专注模式"
              @click="toggleFocusMode"
            >
              <KnowledgeIcon name="focus" />
            </button>
            <el-popover
              ref="noteMenu"
              placement="bottom-end"
              trigger="click"
              :width="180"
            >
              <template #reference>
                <button
                  class="icon-btn"
                  title="笔记操作"
                  aria-label="笔记操作"
                >
                  ⋯
                </button>
              </template>
              <div
                class="note-actions"
                @click="noteMenu?.hide()"
              >
                <button @click="$emit('move')">
                  移动到目录
                </button>
                <button @click="$emit('tags')">
                  编辑标签
                </button>
                <button @click="recordOpen = true">
                  笔记信息
                </button>
                <button @click="$emit('download-draft')">
                  下载 Markdown
                </button>
                <button
                  class="danger"
                  :disabled="loadingNote || deletingNote"
                  aria-label="删除当前笔记"
                  @click="$emit('delete')"
                >
                  {{ deletingNote ? '删除中…' : '删除笔记' }}
                </button>
              </div>
            </el-popover>
          </template>
        </div>
        <div
          ref="readerElement"
          class="reader-scroll"
        >
          <div
            v-if="loadingNote"
            class="empty"
            role="status"
          >
            正在打开文档…
          </div><div
            v-else-if="noteError"
            class="empty"
            role="alert"
          >
            <strong>无法打开笔记</strong>
            <p class="feedback-description">
              笔记可能已被删除，或网络暂时不可用。当前草稿仍保留。
            </p>
            <button
              class="btn"
              @click="$emit('retry-note')"
            >
              重试打开
            </button>
            <button
              v-if="active"
              class="btn"
              @click="$emit('dismiss-note-error')"
            >
              返回当前笔记
            </button>
          </div><div
            v-else-if="!active"
            class="empty"
          >
            <strong>选择一篇笔记</strong>
            <p class="feedback-description">
              从列表打开笔记，或新建一篇开始记录。
            </p>
            <button
              class="btn"
              @click="$emit('new-note')"
            >
              新建笔记
            </button>
          </div>
          <div
            v-else
            class="reading-area"
          >
            <article class="document">
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
                  :toolbar-target="readerPanel"
                  @update:model-value="contentInput"
                  @headings="headings = $event"
                />
              </div>
            </article>
            <aside
              v-if="headings.length && (infoOpen || narrowExpanded)"
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
            </aside>
          </div>
        </div>
        <div
          v-if="active"
          class="doc-end"
        >
          <span
            role="status"
            aria-live="polite"
          >{{ saveStatus || '自动保存已开启' }}</span>
        </div>
      </section>
    </main>
    <div
      v-if="focusMode"
      class="focus-exit"
    >
      <button
        class="btn"
        :title="nativeFullscreen ? '还原窗口大小' : '进入浏览器全屏'"
        @click="toggleNativeFullscreen"
      >
        <KnowledgeIcon name="expand" />{{ nativeFullscreen ? '还原' : '全屏' }}
      </button><button
        class="btn"
        title="退出专注（Esc）"
        @click="exitFocusMode"
      >
        <KnowledgeIcon name="x" />退出专注
      </button>
    </div>
    <div
      v-if="focusMode"
      class="focus-status"
      role="status"
      aria-live="polite"
    >
      {{ saveStatus || '编辑中 · 停顿后自动保存' }}
    </div>
    <el-dialog
      v-model="recordOpen"
      title="笔记信息"
      width="430px"
      class="kb-dialog"
    >
      <dl
        v-if="active"
        class="note-details"
      >
        <dt>目录</dt><dd>{{ folderName(active.folderId) }}</dd>
        <dt>来源</dt><dd>{{ active.source === 'mcp' ? 'MCP 导入' : '手动创建' }}</dd>
        <dt>更新时间</dt><dd>{{ fullDate(active.updateTime) }}</dd>
        <dt>版本</dt><dd>{{ active.version ?? '—' }}</dd>
        <dt>字符数</dt><dd>{{ draft.content.replace(/\s/g, '').length.toLocaleString() }}</dd>
        <template v-if="active.sourceTool">
          <dt>写入工具</dt><dd>{{ active.sourceTool }}</dd>
        </template>
      </dl><template #footer>
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
    <div
      v-if="contextMenu"
      ref="contextMenuElement"
      class="context-menu"
      role="menu"
      :aria-label="contextMenu.type === 'folder' ? `目录操作：${contextMenu.folder.name}` : `笔记操作：${contextMenu.note.title}`"
      :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
      @keydown="contextMenuKeydown"
      @click.stop
      @contextmenu.prevent
    >
      <template v-if="contextMenu.type === 'folder'">
        <div class="menu-title">
          {{ contextMenu.folder.name }}
        </div>
        <button
          role="menuitem"
          @click="folderAction('create-child')"
        >
          <KnowledgeIcon name="plus" />新建子目录
        </button>
        <button
          role="menuitem"
          @click="folderAction('edit')"
        >
          <KnowledgeIcon name="edit" />重命名 / 移动
        </button>
        <button
          role="menuitem"
          class="danger"
          @click="folderAction('delete')"
        >
          <KnowledgeIcon name="x" />删除空目录
        </button>
      </template>
      <template v-else>
        <div class="menu-title">
          {{ contextMenu.note.title || '未命名笔记' }}
        </div>
        <button
          role="menuitem"
          @click="noteAction('open')"
        >
          <KnowledgeIcon name="chevron" />打开笔记
        </button>
        <button
          role="menuitem"
          @click="noteAction('favorite')"
        >
          <KnowledgeIcon name="star" />{{ contextMenu.note.favorite ? '取消收藏' : '收藏笔记' }}
        </button>
        <button
          role="menuitem"
          @click="noteAction('move')"
        >
          <KnowledgeIcon name="folder" />移动目录
        </button>
        <button
          role="menuitem"
          class="danger"
          @click="noteAction('delete')"
        >
          <KnowledgeIcon name="x" />删除笔记
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { paneLayout, paneResizeLimit, clampPaneWidth, PANE_LIMITS } from '../utils/knowledgePaneLayout'
import { readKnowledgeDensity, saveKnowledgeDensity } from '../utils/knowledgeDensity'
import { noteListFeedback } from '../utils/knowledgeFeedback'
import KnowledgeIcon from './KnowledgeIcon.vue'
import KnowledgeEditor from './KnowledgeEditor.vue'
const props = defineProps({
  color: String, folders: { type: Array, default: () => [] }, flatFolders: { type: Array, default: () => [] },
  allFolders: { type: Array, default: () => [] }, collapsed: { type: Set, default: () => new Set() },
  counts: { type: Object, default: () => ({}) }, notes: { type: Array, default: () => [] },
  scope: [String, Number], scopeTitle: String, search: String, listSearch: String, tag: String, source: String, sort: String,
  active: Object, draft: { type: Object, required: true }, mobileView: String,
  total: Number, page: Number, pageSize: Number, loadingList: Boolean, listError: Boolean,
  loadingNote: Boolean, noteError: Boolean, deletingNote: Boolean, saveStatus: String, saveFailed: Boolean, conflicted: Boolean, folderName: Function
})
const emit = defineEmits(['appearance', 'logout', 'scope', 'search', 'list-search', 'tag', 'source', 'sort', 'view', 'collapse', 'collapse-all', 'manage-folder', 'folder-action', 'note-action', 'new-note', 'open-note', 'retry-list', 'retry-note', 'dismiss-note-error', 'page', 'favorite', 'move', 'tags', 'dirty', 'save', 'download-draft', 'reload-conflict', 'filter-tag', 'delete', 'reader-element'])
const treeSearch = ref(''), treeSearchOpen = ref(false), menuOpen = ref(false), recordOpen = ref(false), filtersOpen = ref(false)
const density = ref('standard')
onMounted(() => { try { density.value = readKnowledgeDensity(window.localStorage) } catch { /* Storage access can be blocked. */ } })
function toggleDensity() {
  density.value = density.value === 'compact' ? 'standard' : 'compact'
  try { saveKnowledgeDensity(window.localStorage, density.value) } catch { /* Keep the in-memory preference. */ }
}
const contextMenu = ref(null), noteMenu = ref(null), contextMenuElement = ref(null)
const emptyFeedback = computed(() => noteListFeedback(props))
function clearFilters() { for (const event of ['search', 'list-search', 'tag', 'source']) emit(event, '') }
function navigateNotes(event) {
  if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return
  const items = [...event.currentTarget.querySelectorAll('.note-item')]
  const index = items.indexOf(event.target)
  if (index < 0) return
  event.preventDefault()
  const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : Math.max(0, Math.min(items.length - 1, index + (event.key === 'ArrowDown' ? 1 : -1)))
  items[next]?.focus()
}
function openKeyboardNoteMenu(event, note) {
  const rect = event.currentTarget.getBoundingClientRect()
  showMenu({ clientX: rect.left + 20, clientY: rect.top + 20 }, { type: 'note', note })
  nextTick(() => contextMenuElement.value?.querySelector('button')?.focus())
}
function contextMenuKeydown(event) {
  if (event.key === 'Escape') { event.preventDefault(); const trigger = menuTrigger; closeMenu(); trigger?.focus(); return }
  const items = [...event.currentTarget.querySelectorAll('button')]
  const index = items.indexOf(document.activeElement)
  if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
    event.preventDefault()
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : (index + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length
    items[next]?.focus()
  }
}
let menuTrigger = null
// 右键菜单锚定鼠标位置，并向视口内收拢避免被裁剪；点击空白处 / Esc / 滚轮 / 窗口失焦都会关闭。
function openFolderMenu(event, folder) { showMenu(event, { type: 'folder', folder }) }
function openNoteMenu(event, note) { showMenu(event, { type: 'note', note }) }
function showMenu(event, base) {
  menuTrigger = document.activeElement
  const width = 172, height = base.type === 'folder' ? 130 : 160
  const x = Math.max(8, Math.min(event.clientX, window.innerWidth - width - 8))
  const y = Math.max(8, Math.min(event.clientY, window.innerHeight - height - 8))
  contextMenu.value = { ...base, x, y }
}
function folderAction(action) { const menu = contextMenu.value; closeMenu(); if (menu?.folder) emit('folder-action', { action, folder: menu.folder }) }
function noteAction(action) { const menu = contextMenu.value; closeMenu(); if (menu?.note) emit('note-action', { action, note: menu.note }) }
function closeMenu() { contextMenu.value = null }
const focusMode = ref(false), nativeFullscreen = ref(false)
const appElement = ref(null), readerPanel = ref(null), paneOpen = ref([true, true]), paneWidths = ref([190, 330]), appWidth = ref(1000)
const layout = computed(() => paneLayout(appWidth.value, paneWidths.value, paneOpen.value))
const paneStyle = computed(() => ({ '--tree-width': `${layout.value.widths[0]}px`, '--list-width': `${layout.value.widths[1]}px` }))
const paneMax = index => Math.round(paneResizeLimit(index, appWidth.value, paneOpen.value, layout.value.widths))
function resizePane(index, value) { const next = [...paneWidths.value]; next[index] = clampPaneWidth(index, value, appWidth.value, paneOpen.value, layout.value.widths); paneWidths.value = next }
function togglePane(index) { const next = [...paneOpen.value]; next[index] = !next[index]; paneOpen.value = next }
let drag = null, appObserver
function startResize(event, index) { if (event.button !== 0) return; drag = { index, id: event.pointerId, x: event.clientX, width: layout.value.widths[index] }; event.currentTarget.setPointerCapture(event.pointerId); event.preventDefault() }
function moveResize(event) { if (drag && drag.id === event.pointerId) resizePane(drag.index, drag.width + event.clientX - drag.x) }
function endResize(event) { if (drag?.id === event.pointerId) drag = null }
function resizeKey(event, index) { const step = event.shiftKey ? 40 : 10; const current = layout.value.widths[index]; let value; if (event.key === 'ArrowLeft') value = current - step; else if (event.key === 'ArrowRight') value = current + step; else if (event.key === 'Home') value = PANE_LIMITS[index].min; else if (event.key === 'End') value = Infinity; else return; event.preventDefault(); resizePane(index, value) }
onMounted(() => { appObserver = new ResizeObserver(entries => { appWidth.value = entries[0].contentRect.width }); appObserver.observe(appElement.value) })
let ownFullscreenExit = false
const infoOpen = ref(false), narrowExpanded = ref(false), readerElement = ref(null), titleElement = ref(null), globalInput = ref(null), headings = ref([])
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
function resizeTitle() { if (titleElement.value) { titleElement.value.style.height = 'auto'; titleElement.value.style.height = titleElement.value.scrollHeight + 'px' } }
function titleInput(event) { props.draft.title = event.target.value; resizeTitle(); emit('dirty') }
function contentInput(value) { props.draft.content = value; emit('dirty') }
function scrollToHeading(index) { readerElement.value?.querySelectorAll('.tiptap h1, .tiptap h2, .tiptap h3')[index]?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
function toggleInfo() { if (window.innerWidth <= 1100) narrowExpanded.value = !narrowExpanded.value; else infoOpen.value = !infoOpen.value }
function requestNativeFullscreen() { try { document.documentElement.requestFullscreen?.()?.catch(() => {}) } catch { /* 请求被拒绝或环境不支持时，仅保留隐藏界面的专注态 */ } }
function toggleFocusMode() { focusMode.value = true; requestNativeFullscreen() }
function exitFocusMode() { focusMode.value = false; leaveNativeFullscreen() }
function leaveNativeFullscreen() { if (!document.fullscreenElement) return; ownFullscreenExit = true; document.exitFullscreen().catch(() => {}) }
function toggleNativeFullscreen() { if (document.fullscreenElement) leaveNativeFullscreen(); else requestNativeFullscreen() }
function handleFocusShortcut(event) {
  if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.key.toLowerCase() === 'f') { event.preventDefault(); focusMode.value ? exitFocusMode() : toggleFocusMode(); return }
  // 原生全屏由浏览器接管 Esc；斜杠菜单打开时 Esc 只负责关菜单
  if (event.key === 'Escape' && focusMode.value && !document.fullscreenElement && !document.querySelector('.slash-menu')) exitFocusMode()
}
function handleFullscreenChange() {
  nativeFullscreen.value = Boolean(document.fullscreenElement)
  // 自己点「还原」触发的退出保持专注态；用户按 Esc 退出全屏则一并退出专注
  const ownExit = ownFullscreenExit
  ownFullscreenExit = false
  if (!document.fullscreenElement && focusMode.value && !ownExit) focusMode.value = false
}
window.addEventListener('keydown', handleFocusShortcut)
window.addEventListener('fullscreenchange', handleFullscreenChange)
function focusSearchShortcut(event) { if (event.key === '/' && !event.target.closest('input,textarea,select,button,[role=dialog],[contenteditable=true]')) { event.preventDefault(); globalInput.value?.focus() } if (event.key === 'Escape') { menuOpen.value = false; closeMenu() } }
window.addEventListener('scroll', closeMenu, true)
window.addEventListener('blur', closeMenu)
watch(readerElement, element => emit('reader-element', element), { immediate: true })
watch(() => [props.active?.id, props.draft.title, props.loadingNote], () => nextTick(resizeTitle), { immediate: true })
watch(() => props.active?.id, () => { headings.value = []; recordOpen.value = false })
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
onBeforeUnmount(() => {
  appObserver?.disconnect()
  titleObserver?.disconnect()
  window.removeEventListener('scroll', closeMenu, true)
  window.removeEventListener('blur', closeMenu)
  window.removeEventListener('keydown', handleFocusShortcut)
  window.removeEventListener('fullscreenchange', handleFullscreenChange)
  if (focusMode.value && document.fullscreenElement) document.exitFullscreen().catch(() => {})
})
</script>

<style scoped src="../styles/knowledge-seven.css"></style>
<style scoped>
.seven { width:100%; height:100dvh; overflow:hidden; color-scheme:light; }
.seven .brand { color:var(--blue); }
.workspace-name { color:#7a899e; }
.header-right { position:relative; }
.reader-toolbar .delete-note { color:#b34b55; }
.new-folder { border:1px dashed var(--line); border-radius:5px; }
@media(hover:none) { .tree-manage { opacity:1; } }
.context-menu { position:fixed; z-index:60; width:172px; padding:5px; background:var(--panel); border:1px solid var(--line); border-radius:8px; box-shadow:0 10px 28px #20334b26; }
.context-menu .menu-title { padding:6px 10px 5px; border-bottom:1px solid var(--line); margin-bottom:4px; color:var(--muted); font-size:10px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.context-menu :is(button,a) { display:flex; align-items:center; gap:8px; width:100%; text-align:left; padding:8px 10px; color:var(--ink); font-size:12px; border-radius:5px; }
.context-menu button:hover { background:var(--soft); }
.context-menu button.danger { color:#b34b55; }
.context-menu svg { width:13px; height:13px; flex:none; }
.seven[data-color='dark'] .context-menu { background:var(--panel); box-shadow:0 10px 28px #00000055; }
.seven[data-color='dark'] .context-menu .menu-title { border-color:var(--line); }
.seven[data-color='dark'] .context-menu button.danger { color:#d98a92; }
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
/* 内容可编辑，但键盘用户需要可见的焦点位置（用内发光代替 outline，避免突兀） */
.document-title:focus-visible { outline:none; box-shadow:0 1px 0 0 var(--blue,#3564a9); }
.tag-edit { opacity:0; padding:3px 5px; }
.tag-edit svg { width:12px; height:12px; }
.tags:hover .tag-edit,.tags:focus-within .tag-edit { opacity:1; }
.tags:empty { min-height:0; }
.record-actions { margin-top:12px; padding:0; color:#6e85a2; font-size:10px; }
.toc .nested { padding-left:10px; }
.save-alert { color:#a65c66; padding:14px; border:1px solid #e4bdc5; border-radius:4px; font-size:11px; margin:14px 0; }
.save-alert .btn { margin:8px 8px 0 0; }
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
.markdown :deep(.format-bar) { position:absolute; bottom:calc(100% + 6px); left:0; z-index:3; }
@media(min-width:1700px) { .document-title { font-size:29px; } .markdown :deep(.tiptap) { font-size:14px; } }
@media(max-width:1260px) { .document-title { font-size:23px; min-height:34.5px; } }
@media(max-width:900px) { .document-title { font-size:22px; min-height:33px; } }
@media(prefers-reduced-motion:reduce) { * { scroll-behavior:auto!important; transition:none!important; } }
</style>

<style scoped>
.seven[data-color='dark'] { color-scheme:dark; --bg:#151d28; --panel:#1c2633; --ink:#dce5f1; --muted:#98a8bd; --line:#334257; --blue:#9abded; --blue-bg:#2b405d; --soft:#222f40; --green:#8ab6a1; }
.seven[data-color='dark'] :is(header,.reader,.reader-toolbar,.btn,.global-search kbd,.list-search input) { background:var(--panel); color:var(--ink); border-color:var(--line); }
.seven[data-color='dark'] :is(.sidebar,.global-search input) { background:var(--bg); border-color:var(--line); }
.seven[data-color='dark'] .list-pane { background:var(--panel); }
.seven[data-color='dark'] :is(.nav-row.active,.tree-row.active,.note-item.selected,.note-item:hover) { background:var(--blue-bg); color:var(--blue); }
.seven[data-color='dark'] :is(.note-item h3,.document-title,.workspace-name,.nav-row,.tree-name,.toc button,.source-block strong) { color:var(--ink); }
.seven[data-color='dark'] :is(.filters select,.tree-filter input,.pill,.tag-btn) { background:var(--soft); border-color:var(--line); color:var(--muted); }
.seven[data-color='dark'] :deep(.tiptap) { color:var(--ink); }
.seven[data-color='dark'] :deep(.tiptap h2),.seven[data-color='dark'] :deep(.tiptap h3),.seven[data-color='dark'] :deep(.tiptap strong) { color:var(--ink); border-color:var(--line); }
.seven[data-color='dark'] :deep(.tiptap th),.seven[data-color='dark'] :deep(.tiptap td) { color:var(--ink); border-color:var(--line); }
.seven[data-color='dark'] :deep(.tiptap pre),.seven[data-color='dark'] :deep(.tiptap th),.seven[data-color='dark'] :deep(.tiptap code),.seven[data-color='dark'] :deep(.tiptap blockquote) { background:var(--soft); color:var(--muted); border-color:var(--line); }
</style>

<style scoped>
/* 专注模式：隐藏整个工作台界面，仅保留标题与正文 */
.seven.focus-mode header,.seven.focus-mode .mobile-nav,.seven.focus-mode .sidebar,.seven.focus-mode .list-pane,.seven.focus-mode .reader-toolbar,.seven.focus-mode .info-rail,.seven.focus-mode .eyebrow,.seven.focus-mode .doc-properties,.seven.focus-mode .tags,.seven.focus-mode .doc-end { display:none }
.seven.focus-mode .app { height:100dvh; grid-template-columns:minmax(0,1fr); }
.seven.focus-mode .reader { display:block; background:var(--bg); }
.seven.focus-mode .reading-area { max-width:none; justify-content:center; padding-top:9vh; }
.seven.focus-mode .document { flex:none; width:min(730px,100%); margin:0 auto; }
.seven.focus-mode .document-title { font-size:30px; }
.seven.focus-mode :deep(.tiptap) { font-size:14.5px; line-height:2; }
.seven.focus-mode :deep(.tiptap h2) { font-size:19px; }
.seven.focus-mode :deep(.tiptap h3) { font-size:15.5px; }
/* 专注模式下格式工具栏悬浮顶部居中，避免元信息行隐藏后遮挡标题 */
.seven.focus-mode .markdown :deep(.format-bar) { position:fixed; top:12px; bottom:auto; left:50%; transform:translateX(-50%); box-shadow:0 6px 20px #20334b26; }
@media(max-width:900px) {
  .seven.focus-mode .markdown :deep(.format-bar) { top:auto; bottom:14px; }
  .seven.focus-mode .focus-status { bottom:56px; }
}
.focus-exit { position:fixed; top:14px; right:16px; z-index:40; display:flex; gap:7px; opacity:.4; transition:opacity .18s; }
.focus-exit:hover,.focus-exit:focus-within { opacity:1; }
.focus-exit .btn { box-shadow:0 4px 16px #20334b1a; }
.focus-status { position:fixed; right:16px; bottom:12px; z-index:40; padding:4px 10px; border-radius:20px; background:var(--panel); border:1px solid var(--line); color:var(--muted); font-size:10px; opacity:.75; }
.seven.focus-mode .pane-toggle,.seven.focus-mode .pane-splitter { display:none; }
.seven.focus-mode .reader { display:flex; grid-column:1; grid-row:1; margin:0; border:0; border-radius:0; }
.seven :deep(.reader-format-bar) { display:flex; position:absolute; z-index:4; left:50%; bottom:52px; transform:translateX(-50%); max-width:calc(100% - 20px); overflow-x:auto; }
.seven.focus-mode :deep(.reader-format-bar) { bottom:14px; }
@media(max-width:620px) { .seven :deep(.reader-format-bar) { bottom:calc(52px + env(safe-area-inset-bottom)); } .seven.focus-mode :deep(.reader-format-bar) { bottom:14px; } }
.seven[data-color='dark'] .note-item { background:var(--panel); border-color:var(--line); }
.seven[data-color='dark'] .note-item.selected { background:var(--blue-bg); }
</style>
