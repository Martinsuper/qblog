<template>
  <main class="tokens-page"><header><router-link to="/">← 返回知识库</router-link><h1>MCP 接入与 API Token</h1><p>浏览器使用账号密码。自动化工具使用独立凭证，不需要你的登录密码。</p></header><section class="tokens-guide"><h2>接入地址</h2><code>{{ endpoint }}</code><p>传输方式：Streamable HTTP（JSON 响应，协议 2025-03-26）。请求头：</p><pre>Authorization: Bearer &lt;你的 Token&gt;
Content-Type: application/json
Accept: application/json, text/event-stream</pre><p>可用工具：notes_search、notes_get、notes_create、notes_update。搜索为全文关键词查询，尚未配置向量索引或 AI 问答服务。</p></section><section><div class="tokens-heading"><h2>访问凭证</h2><el-button type="primary" @click="openCreate">新建 Token</el-button></div><el-alert title="目录范围仅包含明确选择的目录，不自动包含子目录。写权限允许创建和更新，不授予删除或账号管理权限。" type="info" :closable="false" /><el-table v-loading="loading" :data="tokens" style="width:100%;margin-top:15px"><el-table-column prop="name" label="名称" min-width="130" /><el-table-column label="权限" min-width="100"><template #default="{ row }">{{ row.scopes?.join(' / ') }}</template></el-table-column><el-table-column label="范围" min-width="170"><template #default="{ row }">{{ scopeLabel(row.directoryScope) }}</template></el-table-column><el-table-column label="有效期" min-width="180"><template #default="{ row }">{{ formatDate(row.expiresAt) }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{ row }">{{ row.revokedAt ? '已撤销' : Date.parse(row.expiresAt) <= Date.now() ? '已过期' : '有效' }}</template></el-table-column><el-table-column width="90"><template #default="{ row }"><el-button type="danger" link :disabled="!!row.revokedAt" @click="revoke(row)">撤销</el-button></template></el-table-column></el-table><div v-if="error" style="padding:15px;color:#b05757">加载失败。<el-button link @click="load">重试</el-button></div></section>
    <el-dialog v-model="createOpen" title="新建访问 Token" width="500px"><el-form label-position="top"><el-form-item label="名称"><el-input v-model="form.name" maxlength="80" placeholder="例如：Claude 技术资料写入" /></el-form-item><el-form-item label="有效天数（最多 365 天）"><el-input-number v-model="form.days" :min="1" :max="365" /></el-form-item><el-form-item label="允许操作"><el-checkbox-group v-model="form.scopes"><el-checkbox label="READ">读取与搜索</el-checkbox><el-checkbox label="WRITE">创建与更新</el-checkbox></el-checkbox-group></el-form-item><el-form-item label="目录范围"><el-checkbox v-model="form.all">全部目录（包括未来新增目录及收件箱）</el-checkbox><template v-if="!form.all"><el-checkbox v-model="form.inbox">收件箱</el-checkbox><el-select v-model="form.directoryIds" multiple placeholder="选择精确目录，不包含子目录" style="width:100%"><el-option v-for="f in folders" :key="f.id" :label="f.path" :value="f.id" /></el-select></template></el-form-item></el-form><template #footer><el-button @click="createOpen = false">取消</el-button><el-button type="primary" :loading="creating" @click="create">创建</el-button></template></el-dialog>
    <el-dialog v-model="secretOpen" title="请立即保存 Token" width="580px" :close-on-click-modal="false" @closed="secret = ''"><el-alert title="完整凭证仅展示一次。关闭后无法再次查看；丢失请撤销并重新创建。" type="warning" :closable="false" /><textarea :value="secret" readonly aria-label="新建 Token 完整凭证" class="token-secret" /><el-button @click="copySecret">复制 Token</el-button><p>不要放入源码仓库、聊天记录或公开配置。服务器仅保存凭证摘要。</p><template #footer><el-button type="primary" @click="secretOpen = false">我已安全保存</el-button></template></el-dialog>
  </main>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApiTokens, createApiToken, revokeApiToken, listFolders } from '@/api/knowledge'
const base = new URL(import.meta.env.VITE_API_BASE_URL || '/api', location.origin)
const endpoint = `${base.href.replace(/\/$/, '')}/mcp`
const tokens = ref([]), folders = ref([]), loading = ref(false), error = ref(false), createOpen = ref(false), creating = ref(false), secretOpen = ref(false), secret = ref('')
const form = reactive({ name: '', days: 30, scopes: ['READ'], all: false, inbox: false, directoryIds: [] })
function flattenFolders(items, path = '') {
  return items.flatMap(item => {
    const full = path ? `${path} / ${item.name}` : item.name
    return [{ id: item.id, path: full }, ...flattenFolders(item.children || [], full)]
  })
}
async function load() {
  loading.value = true; error.value = false
  try {
    const [t, f] = await Promise.all([listApiTokens(), listFolders()])
    tokens.value = t.data; folders.value = flattenFolders(f.data || [])
  } catch { error.value = true } finally { loading.value = false }
}
function openCreate() { Object.assign(form, { name: '', days: 30, scopes: ['READ'], all: false, inbox: false, directoryIds: [] }); createOpen.value = true }
async function create() { if (!form.name.trim() || !form.scopes.length || (!form.all && !form.inbox && !form.directoryIds.length)) { ElMessage.warning('请填写名称、权限及明确的目录范围'); return } creating.value = true; try { const res = await createApiToken({ name: form.name.trim(), expiresAt: new Date(Date.now() + form.days * 86400000).toISOString(), scopes: [...form.scopes], directoryScope: { all: form.all, inbox: !form.all && form.inbox, directoryIds: form.all ? [] : [...form.directoryIds] } }); secret.value = res.data.secret; createOpen.value = false; secretOpen.value = true; await load() } finally { creating.value = false } }
async function revoke(row) { await ElMessageBox.confirm(`撤销「${row.name}」后，工具将立即失去访问权限。`, '撤销访问凭证'); await revokeApiToken(row.id); await load() }
function scopeLabel(scope) { if (!scope) return '无范围'; if (scope.all) return '全部目录'; return [...(scope.inbox ? ['收件箱'] : []), ...(scope.directoryIds || []).map(id => folders.value.find(f => String(f.id) === String(id))?.path || `目录 ${id}`)].join('、') }
function formatDate(value) { return value ? new Date(value).toLocaleString() : '—' }
async function copySecret() { try { await navigator.clipboard.writeText(secret.value); ElMessage.success('已复制') } catch { ElMessage.warning('无法访问剪贴板，请手动复制上方凭证') } }
onMounted(load)
</script>
<style scoped>
.tokens-page{max-width:1100px;margin:0 auto;padding:35px 25px 60px;color:var(--text-primary);font-size:13px}.tokens-page header{margin-bottom:28px}.tokens-page header a{color:var(--color-primary);font-size:12px}.tokens-page h1{font-size:25px;margin:16px 0 8px}.tokens-page h2{font-size:16px;margin:0 0 15px}.tokens-page p{color:var(--text-secondary);line-height:1.8}.tokens-page section{background:var(--bg-secondary);border:1px solid var(--border-color);border-radius:9px;padding:24px;margin-bottom:24px}.tokens-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:17px}.tokens-page code,.tokens-page pre{display:block;background:var(--bg-tertiary);padding:13px 16px;border-radius:5px;overflow:auto;font-size:12px}.token-secret{margin:18px 0;width:100%;padding:12px;min-height:90px;overflow-wrap:anywhere;border:1px solid var(--border-color);background:var(--bg-tertiary);color:var(--text-primary);resize:none}@media(max-width:600px){.tokens-page{padding:20px 14px}.tokens-page section{padding:17px}}
</style>
