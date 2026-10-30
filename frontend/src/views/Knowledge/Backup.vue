<template>
  <SettingsLayout
    title="备份与导出"
    description="管理服务器备份，或将笔记导出到本地。"
  >
    <div class="backup-page">
      <nav
        class="tabs"
        aria-label="备份与导出页面"
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

      <template v-if="tab === 'backups'">
        <section>
          <div class="heading">
            <div><h2>手动备份</h2><p>生成 v3 快照：博客、账号、知识库与上传文件都会打包。</p></div><div class="actions">
              <el-input
                v-model="description"
                maxlength="50"
                placeholder="备份说明（可选）"
                style="width:220px"
              /><el-button
                type="primary"
                :loading="creating"
                @click="createNow"
              >
                立即备份
              </el-button>
            </div>
          </div><el-alert
            title="恢复会覆盖备份所包含的数据；v2 旧备份不含知识库，恢复时不会修改知识库。上传文件按备份中的文件覆盖同名文件。"
            type="info"
            :closable="false"
          />
        </section>
        <section>
          <div class="heading">
            <div><h2>备份列表</h2><p>保留在服务器上，超过保留份数的旧备份会自动清理。</p></div><el-button
              :loading="loading"
              @click="loadList"
            >
              刷新
            </el-button>
          </div><el-table
            v-loading="loading"
            :data="backups"
            empty-text="暂无备份"
          >
            <el-table-column
              label="创建时间"
              min-width="160"
            >
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column><el-table-column
              prop="filename"
              label="文件"
              min-width="220"
              show-overflow-tooltip
            /><el-table-column
              prop="formattedSize"
              label="大小"
              width="100"
            /><el-table-column
              prop="description"
              label="说明"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                {{ row.description || '手动备份' }}
              </template>
            </el-table-column><el-table-column
              label="操作"
              width="190"
            >
              <template #default="{ row }">
                <el-button
                  link
                  @click="downloadBackupFile(row)"
                >
                  下载
                </el-button><el-button
                  link
                  type="warning"
                  @click="restoreBackupFile(row)"
                >
                  恢复
                </el-button><el-button
                  link
                  type="danger"
                  @click="removeBackup(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table><div v-if="listError">
            加载失败。<el-button
              link
              @click="loadList"
            >
              重试
            </el-button>
          </div>
        </section>
        <section>
          <h2>从文件导入</h2><p>上传备份 zip 并立即恢复，文件不超过 100 MiB（104,857,600 字节；服务器或代理可能有更严格限制）。支持 v2 和 v3：v2 仅恢复博客及账号数据，不修改知识库；v3 还会覆盖知识库。备份中的上传文件会覆盖同名文件。</p><div class="actions">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".zip"
              :file-list="fileList"
              :on-change="pickFile"
              :on-remove="() => { picked = null; fileList = [] }"
            >
              <el-button>选择备份文件 (.zip)</el-button>
            </el-upload><el-button
              type="warning"
              :disabled="!picked"
              :loading="importing"
              @click="importNow"
            >
              导入并恢复
            </el-button>
          </div>
        </section>
      </template>

      <template v-else-if="tab === 'schedule'">
        <section>
          <div class="heading">
            <div><h2>自动备份</h2><p>按计划在服务器上自动创建全库快照，保留最近若干份。</p></div>
          </div><el-form
            v-if="schedule"
            label-position="top"
            class="schedule-form"
          >
            <el-form-item label="自动备份">
              <el-switch v-model="schedule.enabled" />
            </el-form-item>
            <el-form-item label="备份频率">
              <el-radio-group
                v-model="schedule.frequency"
                :disabled="!schedule.enabled"
              >
                <el-radio-button value="hourly">
                  每小时
                </el-radio-button><el-radio-button value="daily">
                  每天
                </el-radio-button><el-radio-button value="weekly">
                  每周
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <template v-if="schedule.enabled && schedule.frequency !== 'hourly'">
              <el-form-item label="开始时间">
                <el-time-picker
                  v-model="schedule.time"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择时间"
                />
              </el-form-item>
              <el-form-item
                v-if="schedule.frequency === 'weekly'"
                label="星期"
              >
                <el-select
                  v-model="schedule.dayOfWeek"
                  style="width:140px"
                >
                  <el-option
                    v-for="(name, index) in weekdays"
                    :key="index"
                    :value="index"
                    :label="name"
                  />
                </el-select>
              </el-form-item>
            </template>
            <el-form-item
              v-if="schedule.enabled && schedule.frequency === 'hourly'"
              label="每小时第几分钟"
            >
              <el-input-number
                v-model="schedule.minute"
                :min="0"
                :max="59"
              />
            </el-form-item>
            <el-form-item label="保留份数（超过会删除最旧的）">
              <el-input-number
                v-model="schedule.keepCount"
                :min="1"
                :max="30"
              />
            </el-form-item>
            <el-button
              type="primary"
              :loading="saving"
              @click="saveSchedule"
            >
              保存设置
            </el-button>
          </el-form><div
            v-else-if="scheduleError"
            role="alert"
          >
            备份设置加载失败，无法保存。<el-button
              link
              @click="loadSchedule"
            >
              重试
            </el-button>
          </div><p v-else>
            正在加载备份设置…
          </p>
        </section>
      </template>

      <template v-else-if="tab === 'export'">
        <section>
          <div class="heading">
            <div><h2>全部笔记 · Markdown 包</h2><p>按目录层级整理的 zip：每篇笔记一个 .md，Frontmatter 保留标题、标签、收藏和时间，可直接导入 Obsidian、Typora 等工具。已删除的笔记不会导出。</p></div><el-button
              type="primary"
              :loading="exporting === 'markdown'"
              @click="exportMarkdown"
            >
              导出 Markdown 包
            </el-button>
          </div>
        </section>
        <section>
          <div class="heading">
            <div><h2>JSON 数据快照</h2><p>完整保真的结构化数据（含目录结构与已删除笔记），适合留档或程序化处理。恢复整站请用「备份列表」中的快照。</p></div><el-button
              :loading="exporting === 'json'"
              @click="exportJson"
            >
              导出 JSON 快照
            </el-button>
          </div>
        </section>
        <el-alert
          title="导出内容不含访问令牌等敏感数据。笔记中的图片以引用链接保留，如需连同图片备份请使用整库备份 zip。"
          type="info"
          :closable="false"
        />
      </template>
    </div>
  </SettingsLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import SettingsLayout from '@/components/SettingsLayout.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createBackup, getBackupList, downloadBackup, restoreBackup,
  deleteBackup, importBackup, getBackupSettings, updateBackupSettings
} from '@/api/backup'
import { exportNotesMarkdown, exportKnowledgeSnapshot } from '@/api/knowledge'
import { scheduleFromSettings, schedulePayload } from '@/utils/backupSchedule'

const tabs = [
  { id: 'backups', label: '备份列表' },
  { id: 'schedule', label: '定时设置' },
  { id: 'export', label: '导出数据' }
]
const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
// 与默认 spring.servlet.multipart.max-file-size 和备份服务限制保持一致。
const MAX_IMPORT_BYTES = 100 * 1024 * 1024
const restoreWarning = '恢复不可撤销：v3 将替换博客、账号和知识库数据；v2 旧备份仅替换博客及账号数据，不修改知识库。备份中的上传文件会覆盖同名文件。确定继续吗？'

const tab = ref('backups')
const backups = ref([])
const loading = ref(false)
const listError = ref(false)
const creating = ref(false)
const description = ref('')
const picked = ref(null)
const importing = ref(false)
const fileList = ref([])
const schedule = ref(null)
const scheduleError = ref(false)
const saving = ref(false)
const exporting = ref('')

const formatTime = value => (value ? new Date(value).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '')

function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const stamp = () => {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}-${p(d.getHours())}${p(d.getMinutes())}`
}

async function loadList() {
  loading.value = true
  listError.value = false
  try {
    const { data } = await getBackupList()
    if (!Array.isArray(data)) throw new Error('备份列表数据无效')
    backups.value = data
  } catch {
    listError.value = true
  } finally {
    loading.value = false
  }
}

async function createNow() {
  creating.value = true
  try {
    await createBackup('json', description.value || undefined)
    ElMessage.success('备份已创建')
    description.value = ''
    loadList()
  } catch { /* 提示已由请求层统一处理 */ } finally {
    creating.value = false
  }
}

async function downloadBackupFile(row) {
  const blob = await downloadBackup(row.id)
  saveBlob(blob, row.filename)
}

async function restoreBackupFile(row) {
  try {
    await ElMessageBox.confirm(
      restoreWarning,
      '恢复备份',
      { type: 'warning', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await restoreBackup(row.id)
    ElMessage.success('恢复完成')
  } catch { /* 提示已由请求层统一处理 */ }
}

async function removeBackup(row) {
  try {
    await ElMessageBox.confirm(`删除备份「${row.filename}」？此操作不可撤销。`, '删除备份', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch { return }
  try {
    await deleteBackup(row.id)
    loadList()
  } catch { /* 提示已由请求层统一处理 */ }
}

function pickFile(file) {
  const raw = file.raw
  picked.value = null
  if (!raw?.name?.toLowerCase().endsWith('.zip')) {
    ElMessage.warning('仅支持 .zip 备份文件')
    fileList.value = []
    return
  }
  if (!Number.isFinite(raw.size) || raw.size <= 0 || raw.size > MAX_IMPORT_BYTES) {
    ElMessage.warning('备份文件必须大于 0 且不超过 100 MiB')
    fileList.value = []
    return
  }
  picked.value = raw
  fileList.value = [file]
}

async function importNow() {
  const file = picked.value
  if (!file || importing.value) return
  try {
    await ElMessageBox.confirm(
      restoreWarning,
      '导入备份',
      { type: 'warning', confirmButtonText: '确认导入', cancelButtonText: '取消' }
    )
  } catch { return }
  if (picked.value !== file || importing.value) return
  importing.value = true
  try {
    await importBackup(file)
    ElMessage.success('导入完成，数据已恢复')
    picked.value = null
    fileList.value = []
    loadList()
  } catch { /* 提示已由请求层统一处理 */ } finally {
    importing.value = false
  }
}

async function loadSchedule() {
  schedule.value = null
  scheduleError.value = false
  try {
    const { data } = await getBackupSettings()
    schedule.value = scheduleFromSettings(data)
  } catch {
    scheduleError.value = true
  }
}

async function saveSchedule() {
  if (!schedule.value || saving.value) return
  let payload
  try {
    payload = schedulePayload(schedule.value)
  } catch (error) {
    ElMessage.error(error.message)
    return
  }
  saving.value = true
  try {
    await updateBackupSettings(payload)
    ElMessage.success('备份设置已更新')
  } catch { /* 提示已由请求层统一处理 */ } finally {
    saving.value = false
  }
}

async function exportMarkdown() {
  exporting.value = 'markdown'
  try {
    saveBlob(await exportNotesMarkdown(), `知识库笔记导出-${stamp()}.zip`)
    ElMessage.success('Markdown 包已生成')
  } catch { /* 提示已由请求层统一处理 */ } finally {
    exporting.value = ''
  }
}

async function exportJson() {
  exporting.value = 'json'
  try {
    saveBlob(await exportKnowledgeSnapshot(), `知识库JSON快照-${stamp().slice(0, 8)}.json`)
    ElMessage.success('JSON 快照已生成')
  } catch { /* 提示已由请求层统一处理 */ } finally {
    exporting.value = ''
  }
}

onMounted(() => {
  loadList()
  loadSchedule()
})
</script>

<style scoped>
.backup-page{color:var(--text-primary);font-size:13px}
.backup-page header{margin-bottom:28px}
.backup-page header a{color:var(--color-primary);font-size:12px}
.backup-page h1{font-size:25px;margin:16px 0 8px}
.backup-page h2{font-size:16px;margin:0 0 4px}
.backup-page p{color:var(--text-secondary);line-height:1.8}
.backup-page section{background:var(--bg-secondary);border:1px solid var(--border-color);border-radius:9px;padding:24px;margin-bottom:24px}
.heading{display:flex;justify-content:space-between;align-items:flex-start;gap:14px;margin-bottom:16px}
.heading p{margin:4px 0 10px;font-size:12px}
.actions{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.eyebrow{font-size:11px;letter-spacing:1.5px;color:var(--text-tertiary);margin-top:25px}
.tabs{display:flex;gap:28px;border-bottom:1px solid var(--border-color);margin:30px 0 25px;overflow:auto}
.tabs button{white-space:nowrap;border:0;background:none;color:var(--text-secondary);padding:0 0 13px;cursor:pointer}
.tabs button.active{color:var(--color-primary);border-bottom:2px solid var(--color-primary);font-weight:600}
.schedule-form{max-width:420px}
.schedule-form :deep(.el-form-item){margin-bottom:16px}
@media(max-width:640px){.backup-page section{padding:16px}}
</style>
