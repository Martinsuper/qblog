<template>
  <div class="backup-manage" v-loading="loading">
    <!-- 自动备份设置 -->
    <el-card shadow="hover" class="mb-4">
      <template #header>
        <div class="card-header">
          <span class="card-title">⚙️ 自动备份设置</span>
          <el-tag :type="settings.enabled ? 'success' : 'info'" size="small">
            {{ settings.enabled ? '已启用' : '已禁用' }}
          </el-tag>
        </div>
      </template>

      <el-form :model="settingsForm" label-width="120px" size="default">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="启用自动备份">
              <el-switch
                v-model="settingsForm.enabled"
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="备份频率">
              <el-select v-model="settingsForm.frequency" placeholder="请选择备份频率" style="width: 100%">
                <el-option label="每小时" value="hourly" />
                <el-option label="每天" value="daily" />
                <el-option label="每周" value="weekly" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" v-if="settingsForm.frequency !== 'hourly'">
            <el-form-item label="备份时间">
              <el-time-picker
                v-model="backupTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" v-if="settingsForm.frequency === 'weekly'">
            <el-form-item label="备份星期">
              <el-select v-model="settingsForm.dayOfWeek" placeholder="选择星期" style="width: 100%">
                <el-option label="周日" :value="0" />
                <el-option label="周一" :value="1" />
                <el-option label="周二" :value="2" />
                <el-option label="周三" :value="3" />
                <el-option label="周四" :value="4" />
                <el-option label="周五" :value="5" />
                <el-option label="周六" :value="6" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="保留数量">
              <el-input-number
                v-model="settingsForm.keepCount"
                :min="1"
                :max="30"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="备份类型">
              <el-select v-model="settingsForm.backupType" placeholder="选择备份类型" style="width: 100%">
                <el-option label="仅数据库" value="database" />
                <el-option label="完整备份" value="full" disabled title="开发中" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="saveSettings" :loading="savingSettings">
            保存设置
          </el-button>
          <el-text type="info" style="margin-left: 12px" v-if="settings.nextBackupTime">
            下次备份时间：{{ settings.nextBackupTime }}
          </el-text>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 备份操作区 -->
    <el-card shadow="hover" class="mb-4">
      <template #header>
        <div class="card-header">
          <span class="card-title">📦 备份管理</span>
          <div class="card-actions">
            <el-button type="primary" @click="handleCreateBackup" :loading="creatingBackup">
              <el-icon><Plus /></el-icon>
              立即备份
            </el-button>
            <el-button @click="triggerImport">
              <el-icon><Upload /></el-icon>
              导入备份
            </el-button>
            <input
              ref="importInputRef"
              type="file"
              accept=".sql,.zip"
              style="display: none"
              @change="handleImportFile"
            />
          </div>
        </div>
      </template>

      <!-- 备份列表 -->
      <el-table :data="backups" style="width: 100%" v-loading="loading">
        <el-table-column prop="filename" label="文件名" min-width="200" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'database' ? 'primary' : 'success'" size="small">
              {{ row.type === 'database' ? '数据库' : '完整' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="formattedSize" label="大小" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDownload(row)">
              下载
            </el-button>
            <el-button link type="success" size="small" @click="handleRestore(row)" :loading="row.restoring">
              恢复
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="backups.length === 0" description="暂无备份记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import {
  createBackup,
  getBackupList,
  downloadBackup,
  restoreBackup,
  deleteBackup,
  importBackup,
  getBackupSettings,
  updateBackupSettings
} from '@/api/backup'

// 加载状态
const loading = ref(false)
const creatingBackup = ref(false)
const savingSettings = ref(false)

// 备份列表
const backups = ref([])

// 设置相关
const settings = ref({})
const settingsForm = reactive({
  enabled: true,
  frequency: 'daily',
  hour: 2,
  minute: 0,
  dayOfWeek: 0,
  keepCount: 7,
  backupType: 'database'
})

// 备份时间选择器
const backupTime = ref('02:00')

// 导入文件输入框引用
const importInputRef = ref(null)

// 获取备份列表
const fetchBackups = async () => {
  loading.value = true
  try {
    const res = await getBackupList()
    backups.value = res.data || []
  } catch (error) {
    console.error('获取备份列表失败:', error)
    ElMessage.error('获取备份列表失败')
  } finally {
    loading.value = false
  }
}

// 获取备份设置
const fetchSettings = async () => {
  try {
    const res = await getBackupSettings()
    settings.value = res.data || {}

    // 填充表单
    settingsForm.enabled = res.data?.enabled ?? true
    settingsForm.frequency = res.data?.frequency ?? 'daily'
    settingsForm.hour = res.data?.hour ?? 2
    settingsForm.minute = res.data?.minute ?? 0
    settingsForm.dayOfWeek = res.data?.dayOfWeek ?? 0
    settingsForm.keepCount = res.data?.keepCount ?? 7
    settingsForm.backupType = res.data?.backupType ?? 'database'

    // 设置时间选择器
    const hour = String(settingsForm.hour).padStart(2, '0')
    const minute = String(settingsForm.minute).padStart(2, '0')
    backupTime.value = `${hour}:${minute}`
  } catch (error) {
    console.error('获取备份设置失败:', error)
  }
}

// 监听备份时间变化
watch(backupTime, (newTime) => {
  if (newTime) {
    const [hour, minute] = newTime.split(':').map(Number)
    settingsForm.hour = hour
    settingsForm.minute = minute
  }
})

// 保存设置
const saveSettings = async () => {
  savingSettings.value = true
  try {
    await updateBackupSettings(settingsForm)
    ElMessage.success('设置保存成功')
    await fetchSettings()
  } catch (error) {
    console.error('保存设置失败:', error)
    ElMessage.error('保存设置失败')
  } finally {
    savingSettings.value = false
  }
}

// 创建备份
const handleCreateBackup = async () => {
  creatingBackup.value = true
  try {
    await createBackup('database', '手动备份')
    ElMessage.success('备份创建成功')
    await fetchBackups()
  } catch (error) {
    console.error('创建备份失败:', error)
    ElMessage.error('创建备份失败')
  } finally {
    creatingBackup.value = false
  }
}

// 下载备份
const handleDownload = async (row) => {
  try {
    const res = await downloadBackup(row.id)

    // 创建下载链接
    const blob = new Blob([res.data], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载开始')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 恢复备份
const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复备份"${row.filename}"吗？恢复后当前数据将被覆盖！`,
      '警告：恢复操作不可逆',
      {
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 标记为恢复中
    row.restoring = true

    await restoreBackup(row.id)

    ElMessage.success('备份恢复成功，请刷新页面查看')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复备份失败:', error)
      ElMessage.error('恢复备份失败')
    }
  } finally {
    row.restoring = false
  }
}

// 删除备份
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除备份"${row.filename}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteBackup(row.id)
    ElMessage.success('删除成功')
    await fetchBackups()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 触发导入
const triggerImport = () => {
  importInputRef.value?.click()
}

// 处理导入文件
const handleImportFile = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    await importBackup(file)
    ElMessage.success('备份导入成功')
    await fetchBackups()
  } catch (error) {
    console.error('导入备份失败:', error)
    ElMessage.error('导入备份失败')
  } finally {
    // 清空输入框，允许重复选择同一文件
    event.target.value = ''
  }
}

onMounted(() => {
  fetchBackups()
  fetchSettings()
})
</script>

<style lang="scss" scoped>
.backup-manage {
  padding: 0;
}

.mb-4 {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-actions {
  display: flex;
  gap: 8px;
}

:deep(.el-card) {
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-fast);

  &:hover {
    box-shadow: var(--shadow-md);
  }

  .el-card__header {
    padding: var(--spacing-md) var(--spacing-lg);
    border-bottom: 1px solid var(--border-color);
    background: var(--bg-secondary);
  }

  .el-card__body {
    padding: var(--spacing-lg);
  }
}

:deep(.el-table) {
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--bg-tertiary);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--border-color);

  .el-table__header th {
    font-weight: 600;
    font-size: 13px;
  }

  .el-table__row {
    td {
      padding: 12px 0;
      border-bottom: 1px solid var(--border-color);
    }

    &:hover {
      background: var(--bg-tertiary) !important;
    }
  }
}
</style>
