<template>
  <SettingsLayout
    title="API 与 MCP"
    description="管理工具访问权限、客户端配置与调用记录。"
  >
    <div class="tokens-page">
      <nav
        class="tabs"
        aria-label="API 与 MCP 页面"
      >
        <button
          v-for="item in tabs"
          :key="item.id"
          :class="{ active: tab === item.id }"
          @click="tab = item.id"
        >
          {{ item.label }}
        </button>
      </nav>
      <template v-if="tab === 'overview'">
        <section>
          <div class="tokens-heading">
            <h2>连接 MCP 服务</h2>
          </div><p>使用独立访问令牌连接 AI 客户端，无需提供网站登录密码。</p><div class="endpoint">
            <code>{{ endpoint }}</code><el-button @click="copyText(endpoint)">
              复制地址
            </el-button>
          </div><div class="actions">
            <el-button
              type="primary"
              @click="openCreate"
            >
              创建访问令牌
            </el-button><el-button @click="configOpen = true">
              查看客户端配置
            </el-button>
          </div>
        </section><p>
          首次接入？<el-button
            link
            type="primary"
            @click="tab = 'guide'"
          >
            查看安装与使用
          </el-button>
        </p><el-alert
          title="访问令牌仅在创建时完整显示一次。请妥善保存，并按用途分别创建。"
          type="info"
          :closable="false"
        />
      </template>
      <template v-if="tab === 'tokens'">
        <div class="tokens-heading">
          <div><h2>访问令牌</h2><p>为不同工具创建独立令牌，随时查看或撤销访问。</p></div><el-button
            type="primary"
            @click="openCreate"
          >
            创建令牌
          </el-button>
        </div><el-alert
          title="目录范围仅包含明确选择的目录，不自动包含子目录。写权限允许创建和更新，不授予删除或账号管理权限。"
          type="info"
          :closable="false"
        /><el-table
          v-loading="loading"
          :data="tokens"
          style="width:100%;margin-top:15px"
        >
          <el-table-column
            prop="name"
            label="名称"
            min-width="130"
          /><el-table-column
            label="权限"
            min-width="150"
          >
            <template #default="{ row }">
              {{ row.scopes?.map(s => s === 'READ' ? '知识库读取' : s === 'WRITE' ? '知识库写入' : s === 'ARTICLE_READ' ? '文章读取' : s === 'ARTICLE_WRITE' ? '文章写入' : s === 'ARTICLE_PUBLISH' ? '文章发布' : s === 'ARTICLE_DELETE' ? '文章删除' : s).join('、') }}
            </template>
          </el-table-column><el-table-column
            label="范围"
            min-width="170"
          >
            <template #default="{ row }">
              {{ scopeLabel(row.directoryScope) }}
            </template>
          </el-table-column><el-table-column
            label="创建时间"
            min-width="160"
          >
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column><el-table-column
            label="到期时间"
            min-width="160"
          >
            <template #default="{ row }">
              {{ formatDate(row.expiresAt) }}
            </template>
          </el-table-column><el-table-column
            label="状态"
            width="90"
          >
            <template #default="{ row }">
              {{ row.revokedAt ? '已撤销' : Date.parse(row.expiresAt) <= Date.now() ? '已过期' : '有效' }}
            </template>
          </el-table-column><el-table-column width="80">
            <template #default="{ row }">
              <el-button
                type="danger"
                link
                :disabled="!!row.revokedAt"
                @click="revoke(row)"
              >
                撤销
              </el-button>
            </template>
          </el-table-column>
        </el-table><div v-if="error">
          加载失败。<el-button
            link
            @click="load"
          >
            重试
          </el-button>
        </div>
      </template>
      <template v-if="tab === 'tools'">
        <div class="section-title">
          <h2>可用工具</h2><p>查看 MCP 客户端可调用的能力及其所需权限。</p>
        </div><div class="group-label">
          知识库 · 4 个工具
        </div><section class="tool-list">
          <div
            v-for="tool in tools"
            :key="tool.name"
            class="tool-row"
          >
            <code>{{ tool.name }}</code><span>{{ tool.description }}</span><small>需要知识库{{ tool.scope }}</small>
          </div>
        </section><div class="group-label">
          知识目录 · 4 个工具
        </div><section class="tool-list">
          <div
            v-for="tool in folderTools"
            :key="tool.name"
            class="tool-row"
          >
            <code>{{ tool.name }}</code><span>{{ tool.description }}</span><small>{{ tool.scope }}</small>
          </div>
        </section><div class="group-label">
          博客文章 · 6 个工具
        </div><section class="tool-list">
          <div
            v-for="tool in articleTools"
            :key="tool.name"
            class="tool-row"
          >
            <code>{{ tool.name }}</code><span>{{ tool.description }}</span><small>{{ tool.scope }}</small>
          </div>
        </section>
      </template>
      <template v-if="tab === 'history'">
        <div class="section-title">
          <h2>调用记录</h2><p>查看工具的调用结果，帮助排查连接问题。</p>
        </div><el-button @click="loadCalls">
          刷新
        </el-button><p v-if="callsError">
          加载失败，请检查数据库迁移后重试。
        </p><el-table
          :data="calls"
          empty-text="暂无调用记录"
        >
          <el-table-column
            label="时间"
            min-width="160"
          >
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column><el-table-column
            prop="tokenName"
            label="令牌"
            min-width="120"
          /><el-table-column
            prop="toolName"
            label="工具"
            min-width="140"
          /><el-table-column
            label="结果"
            width="90"
          >
            <template #default="{ row }">
              {{ row.success ? '成功' : '失败' }}
            </template>
          </el-table-column><el-table-column
            prop="durationMs"
            label="耗时（毫秒）"
            width="120"
          />
        </el-table><p>仅显示最近 100 条，不记录请求正文或令牌。</p>
      </template>
      <template v-if="tab === 'guide'">
        <div class="section-title">
          <h2>MCP 安装与使用</h2><p>连接支持 Streamable HTTP 和自定义请求头的 MCP 客户端。</p>
        </div>
        <section>
          <h3>1. 创建令牌</h3><p>在“访问令牌”中创建令牌，选择需要的权限和目录范围。完整令牌只显示一次，请及时保存。</p><el-button @click="openCreate">
            创建访问令牌
          </el-button>
        </section>
        <section>
          <h3>2. 添加到 MCP 客户端</h3><p>在客户端的 MCP 服务配置中添加以下示例，替换占位令牌；不同客户端的配置格式可能不同。</p><pre>{{ configExample }}</pre><el-button @click="copyText(configExample)">
            复制配置示例
          </el-button><p>服务地址必须能从客户端访问；远程使用时请配置 HTTPS。不要把令牌提交到代码仓库。</p>
        </section>
        <section><h3>3. 测试与使用</h3><p>重启或重新连接客户端，确认它列出了 notes_search、notes_get、notes_create 和 notes_update，以及目录工具 folders_list、folders_create、folders_update、folders_delete。文章工具包括 articles_search、articles_get、articles_create、articles_update、articles_publish 和 articles_delete；调用时需要相应的文章权限。可以尝试搜索授权目录中的笔记或创建文章草稿。</p><p>写入需要写权限和目标目录权限；目录授权不包含子目录，收件箱需要单独授权。目录的新建、重命名、移动和删除仅向「全部目录」授权的令牌开放，且只能删除空目录。更新笔记或文章前应读取最新版本；文章版本冲突时，请重新读取并合并更改。</p></section>
        <section><h3>连接失败怎么办</h3><p>检查地址及网络、令牌是否过期或撤销、权限和目录范围，以及客户端是否支持自定义 Authorization 请求头。浏览器客户端还需要在服务器配置允许的来源。</p></section>
      </template>
      <el-dialog
        v-model="configOpen"
        title="客户端配置"
        width="min(580px, 90vw)"
      >
        <p>使用 Streamable HTTP 连接，并在请求头中提供访问令牌：</p><pre>{{ configExample }}</pre><p>将示例中的令牌替换为创建时保存的完整令牌。</p><template #footer>
          <el-button @click="copyText(configExample)">
            复制配置
          </el-button><el-button
            type="primary"
            @click="configOpen = false"
          >
            完成
          </el-button>
        </template>
      </el-dialog>
      <el-dialog
        v-model="createOpen"
        title="新建访问 Token"
        width="500px"
      >
        <el-form label-position="top">
          <el-form-item label="名称">
            <el-input
              v-model="form.name"
              maxlength="80"
              placeholder="例如：Claude 技术资料写入"
            />
          </el-form-item><el-form-item label="有效天数（最多 365 天）">
            <el-input-number
              v-model="form.days"
              :min="1"
              :max="365"
            />
          </el-form-item><el-form-item label="允许操作">
            <el-checkbox-group v-model="form.scopes">
              <el-checkbox label="READ">
                读取与搜索
              </el-checkbox><el-checkbox label="WRITE">
                创建与更新
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item><el-form-item label="文章权限">
            <el-checkbox-group v-model="form.scopes">
              <el-checkbox label="ARTICLE_READ">
                读取文章
              </el-checkbox><el-checkbox label="ARTICLE_WRITE">
                创建和编辑草稿
              </el-checkbox><el-checkbox label="ARTICLE_PUBLISH">
                发布文章（修改已发布文章还需写入权限）
              </el-checkbox><el-checkbox label="ARTICLE_DELETE">
                删除文章
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item><el-form-item
            v-if="form.scopes.includes('READ') || form.scopes.includes('WRITE')"
            label="目录范围"
          >
            <el-checkbox v-model="form.all">
              全部目录（包括未来新增目录及收件箱）
            </el-checkbox><template v-if="!form.all">
              <el-checkbox v-model="form.inbox">
                收件箱
              </el-checkbox><el-select
                v-model="form.directoryIds"
                multiple
                placeholder="选择精确目录，不包含子目录"
                style="width:100%"
              >
                <el-option
                  v-for="f in folders"
                  :key="f.id"
                  :label="f.path"
                  :value="f.id"
                />
              </el-select>
            </template>
          </el-form-item>
        </el-form><template #footer>
          <el-button @click="createOpen = false">
            取消
          </el-button><el-button
            type="primary"
            :loading="creating"
            @click="create"
          >
            创建
          </el-button>
        </template>
      </el-dialog>
      <el-dialog
        v-model="secretOpen"
        title="请立即保存 Token"
        width="580px"
        :close-on-click-modal="false"
        @closed="secret = ''"
      >
        <el-alert
          title="完整凭证仅展示一次。关闭后无法再次查看；丢失请撤销并重新创建。"
          type="warning"
          :closable="false"
        /><textarea
          :value="secret"
          readonly
          aria-label="新建 Token 完整凭证"
          class="token-secret"
        /><el-button @click="copySecret">
          复制 Token
        </el-button><p>不要放入源码仓库、聊天记录或公开配置。服务器仅保存凭证摘要。</p><template #footer>
          <el-button
            type="primary"
            @click="secretOpen = false"
          >
            我已安全保存
          </el-button>
        </template>
      </el-dialog>
    </div>
  </SettingsLayout>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import SettingsLayout from '@/components/SettingsLayout.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApiTokens, createApiToken, revokeApiToken, listFolders, listMcpCalls } from '@/api/knowledge'
import { formatDateTime } from '@/composables/useTime'
const tab = ref('overview'), configOpen = ref(false)
const tabs = [{ id: 'overview', label: '接入概览' }, { id: 'tokens', label: '访问令牌' }, { id: 'tools', label: '可用工具' }, { id: 'history', label: '调用记录' }, { id: 'guide', label: '安装与使用' }]
const tools = [{ name: 'notes_search', description: '按条件搜索笔记', scope: '读取' }, { name: 'notes_get', description: '获取笔记内容', scope: '读取' }, { name: 'notes_create', description: '创建笔记', scope: '写入' }, { name: 'notes_update', description: '更新已有笔记', scope: '写入' }]
const folderTools = [{ name: 'folders_list', description: '列出授权范围内可见的目录树', scope: '需要知识库读取' }, { name: 'folders_create', description: '新建目录（可指定上级，null 为根目录）', scope: '需要知识库写入 + 全部目录授权' }, { name: 'folders_update', description: '重命名、排序或移动目录（不允许形成循环）', scope: '需要知识库写入 + 全部目录授权' }, { name: 'folders_delete', description: '删除空目录（不含笔记与子目录）', scope: '需要知识库写入 + 全部目录授权' }]
const articleTools = [{ name: 'articles_search', description: '搜索文章', scope: '文章读取' }, { name: 'articles_get', description: '读取文章与版本', scope: '文章读取' }, { name: 'articles_create', description: '创建草稿', scope: '文章写入' }, { name: 'articles_update', description: '更新文章', scope: '文章写入；已发布文章还需发布权限' }, { name: 'articles_publish', description: '发布文章', scope: '文章发布' }, { name: 'articles_delete', description: '删除文章', scope: '文章删除' }]
const base = new URL(import.meta.env.VITE_API_BASE_URL || '/api', location.origin)
const endpoint = `${base.href.replace(/\/$/, '')}/mcp`
const configExample = JSON.stringify({ mcpServers: { qblog: { url: endpoint, headers: { Authorization: 'Bearer <你的 Token>' } } } }, null, 2)
const calls = ref([]), callsError = ref(false)
const tokens = ref([]), folders = ref([]), loading = ref(false), error = ref(false), createOpen = ref(false), creating = ref(false), secretOpen = ref(false), secret = ref('')
const form = reactive({ name: '', days: 30, scopes: ['READ'], all: false, inbox: false, directoryIds: [] })
function flattenFolders(items, path = '') {
  return items.flatMap(item => {
    const full = path ? `${path} / ${item.name}` : item.name
    return [{ id: item.id, path: full }, ...flattenFolders(item.children || [], full)]
  })
}
async function loadCalls() { try { const res = await listMcpCalls(); calls.value = res.data; callsError.value = false } catch { callsError.value = true } }
async function load() {
  loading.value = true; error.value = false
  try {
    const [t, f] = await Promise.all([listApiTokens(), listFolders()])
    tokens.value = t.data; folders.value = flattenFolders(f.data || [])
  } catch { error.value = true } finally { loading.value = false }
}
function openCreate() { Object.assign(form, { name: '', days: 30, scopes: ['READ'], all: false, inbox: false, directoryIds: [] }); createOpen.value = true }
async function create() { if (!form.name.trim() || !form.scopes.length || ((form.scopes.includes('READ') || form.scopes.includes('WRITE')) && !form.all && !form.inbox && !form.directoryIds.length)) { ElMessage.warning('请填写名称、权限及明确的目录范围'); return } creating.value = true; try { const res = await createApiToken({ name: form.name.trim(), expiresAt: new Date(Date.now() + form.days * 86400000).toISOString(), scopes: [...form.scopes], directoryScope: { all: form.all, inbox: !form.all && form.inbox, directoryIds: form.all ? [] : [...form.directoryIds] } }); secret.value = res.data.secret; createOpen.value = false; secretOpen.value = true; await load() } finally { creating.value = false } }
async function revoke(row) { await ElMessageBox.confirm(`撤销「${row.name}」后，工具将立即失去访问权限。`, '撤销访问凭证'); await revokeApiToken(row.id); await load() }
function scopeLabel(scope) { if (!scope) return '无范围'; if (scope.all) return '全部目录'; return [...(scope.inbox ? ['收件箱'] : []), ...(scope.directoryIds || []).map(id => folders.value.find(f => String(f.id) === String(id))?.path || `目录 ${id}`)].join('、') }
function formatDate(value) { return formatDateTime(value) || '—' }
async function copyText(value) { try { await navigator.clipboard.writeText(value); ElMessage.success('已复制') } catch { ElMessage.warning('无法访问剪贴板，请手动复制') } }
async function copySecret() { try { await navigator.clipboard.writeText(secret.value); ElMessage.success('已复制') } catch { ElMessage.warning('无法访问剪贴板，请手动复制上方凭证') } }
onMounted(() => { load(); loadCalls() })
</script>
<style scoped>
.tokens-page{color:var(--text-primary);font-size:13px}.tokens-page header{margin-bottom:28px}.tokens-page header a{color:var(--color-primary);font-size:12px}.tokens-page h1{font-size:25px;margin:16px 0 8px}.tokens-page h2{font-size:16px;margin:0 0 15px}.tokens-page p{color:var(--text-secondary);line-height:1.8}.tokens-page section{background:var(--bg-secondary);border:1px solid var(--border-color);border-radius:9px;padding:24px;margin-bottom:24px}.tokens-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:17px}.tokens-page code,.tokens-page pre{display:block;background:var(--bg-tertiary);padding:13px 16px;border-radius:5px;overflow:auto;font-size:12px}.token-secret{margin:18px 0;width:100%;padding:12px;min-height:90px;overflow-wrap:anywhere;border:1px solid var(--border-color);background:var(--bg-tertiary);color:var(--text-primary);resize:none}@media(max-width:640px){.tokens-page section{padding:16px}}
.eyebrow{font-size:11px;letter-spacing:1.5px;color:var(--text-tertiary);margin-top:25px}.tabs{display:flex;gap:28px;border-bottom:1px solid var(--border-color);margin:30px 0 25px;overflow:auto}.tabs button{white-space:nowrap;border:0;background:none;color:var(--text-secondary);padding:0 0 13px;cursor:pointer}.tabs button.active{color:var(--color-primary);border-bottom:2px solid var(--color-primary);font-weight:600}.tokens-heading h2,.section-title h2{margin:0}.tokens-heading p,.section-title p{margin:5px 0 0;font-size:12px}.status{font-size:12px;color:#9a8661;background:#faf6eb;padding:5px 10px;border-radius:20px}.endpoint{display:flex;gap:10px;align-items:center}.endpoint code{flex:1;min-width:0}.actions{display:flex;margin-top:18px}.section-title{margin:32px 0 16px}.steps{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}.steps small,.group-label{color:var(--text-tertiary)}.steps h3,.tokens-page h3{font-size:14px;margin:14px 0 6px}.steps p{font-size:12px;margin:0}.group-label{margin:22px 0 10px}.tool-list{padding:0!important}.tool-row{display:flex;gap:18px;align-items:center;padding:15px 20px;border-bottom:1px solid var(--border-color)}.tool-row:last-child{border-bottom:0}.tool-row code{width:140px;padding:0;background:none}.tool-row span{flex:1}.tool-row small{color:var(--text-secondary)}.empty{text-align:center;padding:50px!important}pre{white-space:pre-wrap;word-break:break-all}@media(max-width:640px){.steps{grid-template-columns:1fr}.tabs{gap:20px}.endpoint{align-items:stretch;flex-direction:column}.tool-row{flex-wrap:wrap}.tool-row small{width:100%}}</style>
