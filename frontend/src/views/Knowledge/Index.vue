<template>
  <div
    :class="{ kb: appearance.layout !== 'knowledge' }"
    :data-layout="appearance.layout"
    :data-color="resolvedColor"
  >
    <KnowledgeSeven
      v-if="appearance.layout === 'knowledge'"
      :color="resolvedColor"
      :folders="folders"
      :flat-folders="flatFolders"
      :all-folders="allFlatFolders"
      :collapsed="collapsed"
      :counts="workspaceCounts"
      :notes="notes"
      :scope="scope"
      :scope-title="scopeTitle"
      :search="search"
      :list-search="localSearch"
      :tag="tagFilter"
      :source="sourceFilter"
      :sort="sortOrder"
      :active="active"
      :draft="draft"
      :mobile-view="mobileView"
      :total="total"
      :page="page"
      :page-size="pageSize"
      :loading-list="loadingList"
      :list-error="listError"
      :loading-note="loadingNote"
      :save-status="saveStatus"
      :save-failed="saveFailed"
      :conflicted="conflicted"
      :folder-name="folderName"
      @appearance="settingsOpen = true"
      @logout="logout"
      @scope="selectScope"
      @view="mobileView = $event"
      @search="search = $event; scheduleSearch()"
      @list-search="localSearch = $event; scheduleSearch()"
      @tag="tagFilter = $event; resetAndLoad()"
      @source="sourceFilter = $event; resetAndLoad()"
      @sort="toggleSort"
      @collapse="toggleFolder"
      @collapse-all="toggleAllFolders"
      @manage-folder="openFolderDialog"
      @new-note="newNote"
      @open-note="openNote"
      @retry-list="loadNotes"
      @page="changePage"
      @favorite="toggleFavorite"
      @move="moveOpen = true"
      @tags="tagsOpen = true"
      @dirty="markDirty"
      @save="saveNow"
      @download-draft="downloadDraft"
      @reload-conflict="reloadConflict"
      @filter-tag="tagFilter = $event; resetAndLoad()"
      @delete="deleteCurrent"
      @reader-element="readerScroll = $event"
    />
    <template v-else>
      <header class="kb-top">
        <a
          class="kb-brand"
          href="/"
        ><span class="kb-logo">y</span><strong>YOUNOTE</strong><small>知识库</small></a><span
          v-if="appearance.layout === 'knowledge'"
          class="kb-breadcrumb"
        >个人工作区 <i>/</i> 知识库</span><input
          v-model="search"
          class="kb-global-search"
          placeholder="搜索标题、正文和标签…"
          aria-label="全文搜索"
          @input="scheduleSearch"
        ><button @click="settingsOpen = true">
          ◧ 外观与布局
        </button><router-link to="/settings/tokens">
          MCP 接入
        </router-link><button @click="logout">
          退出
        </button>
      </header>
      <div class="kb-notice">
        <span>{{ layoutNames[appearance.layout] }} · {{ total }} 篇匹配笔记</span><span>私人空间 · 登录后访问 · AI 索引未配置</span>
      </div>
      <nav class="kb-mobile">
        <button @click="mobileView = 'tree'">
          目录
        </button><button @click="mobileView = 'list'">
          笔记
        </button><button
          :disabled="!active"
          @click="mobileView = 'reader'; readerOpen = true"
        >
          正文
        </button>
      </nav>
      <main
        class="kb-workspace"
        :data-view="mobileView"
        :class="{ 'reader-open': readerOpen }"
      >
        <aside class="kb-sidebar">
          <div class="kb-sidebar-title">
            工作区 <span>PRIVATE</span>
          </div><button
            :class="{ active: scope === 'all' }"
            @click="selectScope('all')"
          >
            ▤ 全部笔记
          </button><button
            :class="{ active: scope === 'inbox' }"
            @click="selectScope('inbox')"
          >
            ▱ 收件箱
          </button><button
            :class="{ active: scope === 'favorites' }"
            @click="selectScope('favorites')"
          >
            ☆ 我的收藏
          </button><div class="kb-sidebar-title">
            知识目录 <button
              title="新建目录"
              @click="openFolderDialog()"
            >
              ＋
            </button>
          </div><input
            v-if="appearance.layout === 'knowledge'"
            v-model="folderSearch"
            class="kb-folder-search"
            placeholder="筛选目录..."
            aria-label="筛选知识目录"
          ><div
            v-for="folder in flatFolders.filter(f => !folderSearch || f.name.toLowerCase().includes(folderSearch.toLowerCase()))"
            :key="folder.id"
            class="kb-folder-row"
            :style="{ paddingLeft: `${folder.depth * 13}px` }"
          >
            <button
              v-if="folder.hasChildren"
              class="kb-collapse"
              :aria-expanded="!collapsed.has(folder.id)"
              @click="toggleFolder(folder.id)"
            >
              {{ collapsed.has(folder.id) ? '›' : '⌄' }}
            </button><span
              v-else
              class="kb-collapse"
            >·</span><button
              class="kb-folder"
              :class="{ active: String(scope) === String(folder.id) }"
              @click="selectScope(folder.id)"
            >
              {{ folder.name }}<small>{{ folder.count ?? '' }}</small>
            </button><button
              title="管理目录"
              class="kb-folder-menu"
              @click="openFolderDialog(folder)"
            >
              ⋯
            </button>
          </div><div class="kb-sidebar-foot">
            笔记存储于服务器<br>外观偏好仅存本机<br><router-link to="/user/profile">
              账号与密码
            </router-link>
          </div>
        </aside>
        <section
          v-if="appearance.layout === 'library'"
          class="kb-shelf"
        >
          <div><strong>我的知识书架</strong><small>一本一主题 · 笔记作为章节</small></div><div class="kb-books">
            <button
              v-for="(folder, i) in topFolders"
              :key="folder.id"
              class="kb-book"
              :class="{ active: String(scope) === String(folder.id) }"
              :style="{ '--book': bookColors[i % bookColors.length] }"
              @click="selectScope(folder.id)"
            >
              <small>VOL. {{ String(i + 1).padStart(2, '0') }}</small><strong>{{ folder.name }}</strong><span>{{ folder.count ?? 0 }} 篇笔记</span>
            </button><button
              class="kb-book new-book"
              @click="openFolderDialog()"
            >
              ＋ 新建笔记本
            </button>
          </div>
        </section>
        <section class="kb-list">
          <div class="kb-list-head">
            <div>
              <h2>{{ scopeTitle }} <small>{{ total }}</small></h2><button
                title="新建笔记"
                @click="newNote"
              >
                ＋
              </button>
            </div><input
              v-if="appearance.layout === 'knowledge'"
              v-model="search"
              class="kb-list-search"
              placeholder="在当前目录中搜索正文..."
              aria-label="搜索笔记列表"
              @input="scheduleSearch"
            ><div class="kb-filters">
              <select
                v-model="sourceFilter"
                aria-label="写入来源"
                @change="resetAndLoad"
              >
                <option value="">
                  所有来源
                </option><option value="manual">
                  手动
                </option><option value="mcp">
                  MCP 写入
                </option>
              </select><input
                v-model="tagFilter"
                placeholder="标签筛选"
                aria-label="标签筛选"
                @change="resetAndLoad"
              >
            </div><div
              v-if="appearance.layout === 'knowledge'"
              class="kb-list-count"
            >
              <span>{{ total }} 篇笔记</span><span>最近更新 ↓</span>
            </div>
          </div><div
            v-if="listError"
            class="kb-empty"
          >
            加载失败，请重试。<button @click="loadNotes">
              重试
            </button>
          </div><div
            v-else-if="loadingList"
            class="kb-empty"
          >
            正在加载笔记…
          </div><div
            v-else
            class="kb-note-list"
          >
            <button
              v-for="note in notes"
              :key="note.id"
              class="kb-note"
              :class="{ selected: active?.id === note.id }"
              @click="openNote(note.id)"
            >
              <small>{{ folderName(note.folderId) }}</small><h3>{{ note.title }} <span v-if="note.favorite">★</span></h3><p>{{ note.summary || '暂无摘要' }}</p><div><span>{{ note.source === 'mcp' ? 'MCP 写入' : '手动' }}</span><span>{{ (note.tags || []).slice(0, 2).join(' · ') }}</span><time>{{ formatDate(note.updateTime) }}</time></div>
            </button><div
              v-if="!notes.length"
              class="kb-empty"
            >
              没有匹配的笔记。<br><button @click="newNote">
                写下第一篇笔记
              </button>
            </div>
          </div><footer class="kb-pagination">
            <button
              :disabled="page === 1 || loadingList"
              @click="changePage(-1)"
            >
              ←
            </button><span>{{ page }} / {{ Math.max(1, Math.ceil(total / pageSize)) }} · {{ total }} 篇</span><button
              :disabled="page * pageSize >= total || loadingList"
              @click="changePage(1)"
            >
              →
            </button>
          </footer>
        </section>
        <section class="kb-reader">
          <header>
            <button
              v-if="appearance.layout === 'paper'"
              title="关闭正文"
              @click="closeReader"
            >
              ✕
            </button><span>{{ active ? folderName(active.folderId) : '知识库' }}</span><small
              role="status"
              :class="{ error: saveFailed }"
            >{{ saveStatus }}</small><button
              v-if="saveFailed && !conflicted"
              @click="saveNow"
            >
              重试
            </button><button
              v-if="active"
              title="收藏笔记"
              @click="toggleFavorite"
            >
              {{ draft.favorite ? '★' : '☆' }}
            </button><button
              v-if="active"
              @click="moveOpen = true"
            >
              移动
            </button><button
              v-if="active && appearance.layout !== 'knowledge'"
              @click="editing = !editing"
            >
              {{ editing ? '完成编辑' : '编辑' }}
            </button><button
              v-if="active"
              title="笔记属性"
              @click="propertiesOpen = !propertiesOpen"
            >
              ⓘ
            </button>
          </header><div
            v-if="loadingNote"
            class="kb-empty"
          >
            正在打开文档…
          </div><div
            v-else-if="!active"
            class="kb-empty"
          >
            从左侧打开一篇笔记，开始阅读和整理。
          </div><div
            v-else
            ref="readerScroll"
            class="kb-reading"
          >
            <article>
              <div class="kb-eyebrow">
                KNOWLEDGE / {{ folderName(active.folderId) }}
              </div><input
                v-model="draft.title"
                class="kb-doc-title"
                aria-label="笔记标题"
                maxlength="120"
                :readonly="!canEdit"
                @input="markDirty"
              ><div class="kb-doc-meta">
                <span>{{ active.source === 'mcp' ? 'MCP 写入' : '手动记录' }}</span><span>{{ formatDate(active.updateTime) }}</span><span>{{ draft.content.length }} 字符</span><span>索引未配置</span>
              </div><div class="kb-doc-tags">
                <span
                  v-for="tag in draft.tags"
                  :key="tag"
                ># {{ tag }}</span><button @click="tagsOpen = true">
                  编辑标签
                </button>
              </div><div
                v-if="conflicted"
                class="kb-conflict"
              >
                此笔记已在其他窗口或 MCP 中更新，自动保存已暂停。你的修改仍保留在编辑器内。<button @click="downloadDraft">
                  下载当前草稿
                </button><button @click="reloadConflict">
                  放弃本地修改并载入最新
                </button>
              </div><KnowledgeEditor
                :key="active.id"
                v-model="draft.content"
                :readonly="!canEdit"
                @update:model-value="markDirty"
              /><footer>YOUNOTE · {{ canEdit ? '直接编辑，停顿后自动保存' : '阅读模式' }} · Ctrl / ⌘ S 保存</footer>
            </article><aside
              v-if="appearance.layout === 'knowledge' && !propertiesOpen"
              class="kb-document-aside"
            >
              <div class="kb-aside-title">
                本文目录
              </div><nav
                v-if="documentHeadings.length"
                aria-label="本文目录"
              >
                <button
                  v-for="(heading, index) in documentHeadings"
                  :key="index"
                  :class="{ nested: heading.level > 2 }"
                  @click="scrollToHeading(index)"
                >
                  {{ heading.text }}
                </button>
              </nav><p v-else>
                添加标题后显示目录
              </p><div class="kb-aside-details">
                <div class="kb-aside-title">
                  来源与记录
                </div><strong>{{ active.source === 'mcp' ? 'MCP 写入' : '手动创建' }}</strong><p>目录：{{ folderName(active.folderId) }}<br>更新于：{{ formatDate(active.updateTime) }}<br>版本：{{ active.version }}</p><p>索引未配置</p>
              </div>
            </aside><aside
              v-if="propertiesOpen"
              class="kb-properties"
            >
              <strong>笔记属性</strong><dl><dt>目录</dt><dd>{{ folderName(active.folderId) }}</dd><dt>来源</dt><dd>{{ active.sourceTool || '手动记录' }}</dd><dt>版本</dt><dd>{{ active.version }}</dd><dt>索引</dt><dd>未配置（非已索引）</dd></dl><button @click="deleteCurrent">
                删除笔记
              </button>
            </aside>
          </div>
        </section>
      </main>
    </template>
    <el-dialog
      v-model="settingsOpen"
      title="外观与布局"
      width="620px"
      class="kb-dialog"
    >
      <p>同一份知识库，四种浏览方式。切换不复制笔记。</p><div class="kb-layout-choices">
        <button
          v-for="(name, value) in layoutNames"
          :key="value"
          :class="{ active: appearance.layout === value }"
          @click="setLayout(value)"
        >
          <span>{{ layoutIcons[value] }}</span><strong>{{ name }}</strong><small>{{ layoutDescriptions[value] }}</small>
        </button>
      </div><h4>配色模式</h4><div class="kb-color-choices">
        <button
          v-for="(name, value) in colorNames"
          :key="value"
          :class="{ active: appearance.color === value }"
          @click="setColor(value)"
        >
          {{ name }}
        </button>
      </div><template #footer>
        <el-button @click="settingsOpen = false">
          完成
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="folderDialog"
      :title="folderEditing ? '管理目录' : '新建目录'"
      width="430px"
    >
      <el-form label-position="top">
        <el-form-item label="目录名称">
          <el-input
            v-model="folderForm.name"
            maxlength="80"
          />
        </el-form-item><el-form-item label="上级目录">
          <el-select
            v-model="folderForm.parentId"
            clearable
            placeholder="根目录"
          >
            <el-option
              v-for="folder in allFlatFolders.filter(f => f.id !== folderEditing?.id)"
              :key="folder.id"
              :label="folder.path"
              :value="folder.id"
            />
          </el-select>
        </el-form-item>
      </el-form><template #footer>
        <el-button
          v-if="folderEditing"
          type="danger"
          plain
          @click="deleteFolder"
        >
          删除空目录
        </el-button><el-button
          type="primary"
          @click="submitFolder"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="moveOpen"
      title="移动笔记"
      width="420px"
    >
      <el-select
        v-model="moveTarget"
        clearable
        placeholder="收件箱（无目录）"
      >
        <el-option
          v-for="folder in allFlatFolders"
          :key="folder.id"
          :label="folder.path"
          :value="folder.id"
        />
      </el-select><template #footer>
        <el-button
          type="primary"
          @click="moveCurrent"
        >
          确认移动
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="tagsOpen"
      title="笔记标签"
      width="420px"
    >
      <el-select
        v-model="draft.tags"
        multiple
        filterable
        allow-create
        default-first-option
        :multiple-limit="8"
        style="width:100%"
      >
        <el-option
          v-for="tag in draft.tags"
          :key="tag"
          :label="tag"
          :value="tag"
        />
      </el-select><template #footer>
        <el-button @click="tagsOpen = false; markDirty()">
          完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import KnowledgeEditor from '@/components/KnowledgeEditor.vue'
import KnowledgeSeven from '@/components/KnowledgeSeven.vue'
import * as api from '@/api/knowledge'
import { useUserStore } from '@/stores/user'
import { readAppearance, saveAppearance } from '@/utils/knowledgePreferences'
import { createNoteAutosave } from '@/utils/noteAutosave'
import '@/styles/knowledge.css'
const router = useRouter(), route = useRoute(), user = useUserStore()
const appearance = reactive(readAppearance()), systemDark = ref(matchMedia('(prefers-color-scheme: dark)').matches)
const resolvedColor = computed(() => appearance.color === 'system' ? (systemDark.value ? 'dark' : 'light') : appearance.color)
const layoutNames = { calm: '01 · 留白三栏', paper: '03 · 纸间卡片', library: '06 · 笔记书架', knowledge: '07 · 目录知识库' }
const layoutIcons = { calm: '▥ ▤ ☰', paper: '▦ ▦ ▦', library: '▥ ▥ ▥', knowledge: '☷ ▤ ≡' }
const layoutDescriptions = { calm: '留白阅读，三栏并排', paper: '暖色卡片，展开正文', library: '目录成书，笔记成章', knowledge: '紧凑目录，直接编辑' }, colorNames = { system: '跟随系统', light: '浅色', dark: '深色' }, bookColors = ['#365b68', '#7b8465', '#ad855b', '#85718a', '#6d8794']
const folderSearch = ref(''), localSearch = ref(''), sortOrder = ref('updated')
const workspaceCounts = reactive({ all: 0, inbox: 0, favorites: 0 })
const settingsOpen = ref(false), mobileView = ref('list'), readerOpen = ref(false), editing = ref(false), propertiesOpen = ref(false)
const folders = ref([]), notes = ref([]), total = ref(0), page = ref(1), pageSize = 50, scope = ref('all'), search = ref(''), tagFilter = ref(''), sourceFilter = ref(''), collapsed = reactive(new Set())
const loadingList = ref(false), listError = ref(false), loadingNote = ref(false), active = ref(null), draft = reactive({ title: '', content: '', tags: [], favorite: false, folderId: null }), saveStatus = ref(''), saveFailed = ref(false), conflicted = ref(false), readerScroll = ref(null)
const folderDialog = ref(false), folderEditing = ref(null), folderForm = reactive({ name: '', parentId: null }), moveOpen = ref(false), moveTarget = ref(null), tagsOpen = ref(false)
let loadGeneration = 0, noteGeneration = 0, autosave = null, saveTimer, searchTimer, composing = false
const scrollPositions = new Map(), colorMedia = matchMedia('(prefers-color-scheme: dark)')
const documentHeadings = computed(() => {
  let fenced = false
  return draft.content.split('\n').flatMap(line => {
    if (/^\s*(```|~~~)/.test(line)) { fenced = !fenced; return [] }
    const match = !fenced && line.match(/^\s{0,3}(#{1,3})\s+(.+?)\s*#*\s*$/)
    return match ? [{ level: match[1].length, text: match[2].replace(/[*_`~]/g, '') }] : []
  })
})
function scrollToHeading(index) {
  readerScroll.value?.querySelectorAll('.tiptap h1, .tiptap h2, .tiptap h3')[index]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
const canEdit = computed(() => !loadingNote.value && (appearance.layout === 'knowledge' || editing.value))
const folderArray = computed(() => { const out = []; function visit(items, parentId = null) { for (const f of items) { out.push({ ...f, parentId: f.parentId ?? parentId }); if (f.children?.length) visit(f.children, f.id) } } visit(folders.value); return [...new Map(out.map(f => [f.id, f])).values()] })
function flatten(showCollapsed = true) { const out = [], visited = new Set(); function walk(parent, depth, path) { for (const f of folderArray.value.filter(f => String(f.parentId ?? '') === String(parent ?? ''))) { if (visited.has(f.id)) continue; visited.add(f.id); const full = path ? `${path} / ${f.name}` : f.name; out.push({ ...f, path: full, depth, hasChildren: folderArray.value.some(c => String(c.parentId) === String(f.id)) }); if (!showCollapsed || !collapsed.has(f.id)) walk(f.id, depth + 1, full) } } walk(null, 0, ''); return out }
const allFlatFolders = computed(() => flatten(false)), flatFolders = computed(() => flatten()), topFolders = computed(() => folderArray.value.filter(f => !f.parentId)), scopeTitle = computed(() => scope.value === 'all' ? '全部笔记' : scope.value === 'inbox' ? '待归档' : scope.value === 'favorites' ? '收藏笔记' : folderName(scope.value))
function folderName(id) { return id ? (allFlatFolders.value.find(f => String(f.id) === String(id))?.path || '未分类') : '收件箱' }
function toggleFolder(id) { collapsed.has(id) ? collapsed.delete(id) : collapsed.add(id) }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' }
async function loadFolders() { const res = await api.listFolders(); folders.value = res.data || []; await loadCounts() }
async function loadCounts() {
  const values = await Promise.all([api.listNotes({ size: 1 }), api.listNotes({ size: 1, inbox: true }), api.listNotes({ size: 1, favorite: true })])
  ;['all', 'inbox', 'favorites'].forEach((key, index) => { workspaceCounts[key] = values[index].data.total })
}
function toggleAllFolders() {
  const parents = allFlatFolders.value.filter(f => f.hasChildren)
  if (parents.every(f => collapsed.has(f.id))) collapsed.clear()
  else parents.forEach(f => collapsed.add(f.id))
}
function toggleSort() { sortOrder.value = sortOrder.value === 'updated' ? 'title' : 'updated'; resetAndLoad() }
async function loadNotes() {
  const generation = ++loadGeneration; loadingList.value = true; listError.value = false
  try { const res = await api.listNotes({ current: page.value, size: pageSize, search: search.value || undefined, query: appearance.layout === 'knowledge' ? localSearch.value || undefined : undefined, tag: tagFilter.value || undefined, source: sourceFilter.value || undefined, sort: sortOrder.value, folderId: typeof scope.value === 'number' || /^\d+$/.test(scope.value) ? scope.value : undefined, inbox: scope.value === 'inbox' || undefined, favorite: scope.value === 'favorites' || undefined }); if (generation !== loadGeneration) return; notes.value = res.data.records; total.value = res.data.total; if (page.value > 1 && !notes.value.length) { page.value--; return loadNotes() } } catch { if (generation === loadGeneration) listError.value = true } finally { if (generation === loadGeneration) loadingList.value = false }
}
function scheduleSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(resetAndLoad, 250) }
function resetAndLoad() { page.value = 1; loadNotes() }
async function selectScope(value) { if (appearance.layout === 'knowledge') { search.value = ''; localSearch.value = '' } scope.value = value; page.value = 1; mobileView.value = 'list'; readerOpen.value = false; await loadNotes() }
function changePage(delta) { page.value += delta; loadNotes() }
function snapshot() { return { title: draft.title, content: draft.content, tags: [...draft.tags], favorite: draft.favorite, folderId: draft.folderId ?? null } }
function setupAutosave() {
  autosave?.dispose(); const id = active.value.id
  autosave = createNoteAutosave({ snapshot, persist: async value => { if (!value.title.trim()) throw new Error('标题不能为空'); const res = await api.updateNote(id, { ...value, version: active.value.version }); return res.data }, onSaved: (saved, written) => { active.value = saved; saveFailed.value = false; saveStatus.value = JSON.stringify(snapshot()) === JSON.stringify(written) ? '已保存到服务器' : '仍有修改待保存'; const item = notes.value.find(n => n.id === id); if (item) Object.assign(item, saved, { content: undefined }); if (JSON.stringify(snapshot()) !== JSON.stringify(written)) scheduleSave() }, onError: (error, conflict) => { saveFailed.value = true; conflicted.value = conflict; saveStatus.value = conflict ? '版本冲突 · 自动保存暂停' : `保存失败 · ${error.message || '请重试'}` } })
}
function markDirty() { if (!active.value) return; saveStatus.value = '未保存修改'; saveFailed.value = false; if (!conflicted.value && !composing) scheduleSave() }
function scheduleSave() { clearTimeout(saveTimer); saveTimer = setTimeout(saveNow, 800) }
async function saveNow() { clearTimeout(saveTimer); if (!autosave || !autosave.isDirty()) return true; if (conflicted.value) return false; saveStatus.value = '正在保存…'; return autosave.flush() }
async function safeLeave() { while (autosave?.isDirty()) { if (!await saveNow()) return false } return true }
async function openNote(id) {
  if (loadingNote.value) return
  if (active.value?.id === id) { readerOpen.value = true; mobileView.value = 'reader'; return }
  if (!await safeLeave()) { ElMessage.warning('当前笔记未保存，请先重试或下载草稿'); return }
  if (active.value && readerScroll.value) scrollPositions.set(active.value.id, readerScroll.value.scrollTop)
  const generation = ++noteGeneration; loadingNote.value = true
  try { const res = await api.getNote(id); if (generation !== noteGeneration) return; active.value = res.data; Object.assign(draft, { title: res.data.title, content: res.data.content || '', tags: [...(res.data.tags || [])], favorite: res.data.favorite, folderId: res.data.folderId ?? null }); conflicted.value = false; saveFailed.value = false; saveStatus.value = '已载入服务器版本'; editing.value = false; readerOpen.value = true; mobileView.value = 'reader'; moveTarget.value = draft.folderId; setupAutosave(); await router.replace({ query: { ...route.query, note: id } }); await nextTick(); if (readerScroll.value) readerScroll.value.scrollTop = scrollPositions.get(id) || 0 } finally { if (generation === noteGeneration) loadingNote.value = false }
}
async function newNote() { if (!await safeLeave()) return; const res = await api.createNote({ title: '未命名笔记', content: '', tags: [], favorite: false, folderId: typeof scope.value === 'number' || /^\d+$/.test(scope.value) ? Number(scope.value) : null }); await loadNotes(); await loadFolders(); await openNote(res.data.id); editing.value = true }
async function toggleFavorite() { draft.favorite = !draft.favorite; markDirty(); if (await saveNow()) await loadCounts() }
async function closeReader() { if (!await safeLeave()) return; readerOpen.value = false; mobileView.value = 'list' }
async function setLayout(layout) { if (!await safeLeave()) return; const scroll = readerScroll.value?.scrollTop || 0; appearance.layout = layout; persistAppearance(); await nextTick(); if (readerScroll.value) readerScroll.value.scrollTop = scroll }
function setColor(color) { appearance.color = color; persistAppearance() }
function persistAppearance() { if (!saveAppearance(appearance)) ElMessage.warning('浏览器存储不可用，外观偏好无法记住') }
function openFolderDialog(folder = null) { folderEditing.value = folder; folderForm.name = folder?.name || ''; folderForm.parentId = folder?.parentId ?? null; folderDialog.value = true }
async function submitFolder() { if (!folderForm.name.trim()) return; const data = { name: folderForm.name.trim(), parentId: folderForm.parentId || null }; if (folderEditing.value) await api.updateFolder(folderEditing.value.id, data); else await api.createFolder(data); folderDialog.value = false; await loadFolders() }
async function deleteFolder() { await ElMessageBox.confirm('只允许删除没有子目录和笔记的目录。确认删除？', '删除目录'); await api.removeFolder(folderEditing.value.id); folderDialog.value = false; if (scope.value === folderEditing.value.id) await selectScope('all'); await loadFolders() }
async function moveCurrent() { draft.folderId = moveTarget.value ?? null; markDirty(); if (await saveNow()) { moveOpen.value = false; await loadFolders(); await loadNotes() } }
async function deleteCurrent() { if (!await safeLeave()) return; await ElMessageBox.confirm(`删除「${active.value.title}」？`, '删除笔记'); await api.removeNote(active.value.id, active.value.version); autosave?.dispose(); autosave = null; active.value = null; readerOpen.value = false; mobileView.value = 'list'; await router.replace({ query: {} }); await loadNotes(); await loadFolders() }
function downloadDraft() { const blob = new Blob([`# ${draft.title}\n\n${draft.content}`], { type: 'text/markdown;charset=utf-8' }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `${draft.title.replace(/[\\/:*?"<>|]/g, '_') || '笔记'}.md`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000) }
async function reloadConflict() { await ElMessageBox.confirm('将放弃当前未保存修改。建议先下载草稿。', '载入最新版本'); const id = active.value.id; autosave?.dispose(); autosave = null; active.value = null; await openNote(id) }
async function logout() { if (!await safeLeave()) return; user.userLogout(); await router.replace('/login') }
function beforeUnload(event) { if (autosave?.isDirty()) { event.preventDefault(); event.returnValue = '' } }
function keydown(event) { if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 's') { event.preventDefault(); saveNow() } }
function compositionStart() { composing = true; clearTimeout(saveTimer) }
function compositionEnd() { composing = false; if (autosave?.isDirty()) scheduleSave() }
function systemChanged(event) { systemDark.value = event.matches }
watch(resolvedColor, value => { document.documentElement.dataset.theme = value; document.documentElement.classList.toggle('dark', value === 'dark') }, { immediate: true })
watch(() => route.query.note, async id => { if (id && String(active.value?.id) !== String(id) && !loadingNote.value) await openNote(id) })
onBeforeRouteUpdate(async to => { if (String(to.query.note || '') !== String(active.value?.id || '')) return safeLeave(); return true })
onBeforeRouteLeave(async to => { if (to.path === route.path) return true; return safeLeave() })
onMounted(async () => { window.addEventListener('beforeunload', beforeUnload); window.addEventListener('keydown', keydown); document.addEventListener('compositionstart', compositionStart); document.addEventListener('compositionend', compositionEnd); colorMedia.addEventListener('change', systemChanged); await Promise.all([loadFolders(), loadNotes()]); if (route.query.note) await openNote(route.query.note); else if (appearance.layout === 'knowledge' && notes.value.length) await openNote(notes.value[0].id) })
onBeforeUnmount(() => { clearTimeout(saveTimer); clearTimeout(searchTimer); autosave?.dispose(); window.removeEventListener('beforeunload', beforeUnload); window.removeEventListener('keydown', keydown); document.removeEventListener('compositionstart', compositionStart); document.removeEventListener('compositionend', compositionEnd); colorMedia.removeEventListener('change', systemChanged) })
</script>
