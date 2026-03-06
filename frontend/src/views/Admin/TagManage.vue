<template>
  <div class="tag-manage">
    <div class="manage-card">
      <div class="card-header">
        <h3 class="card-title">标签管理</h3>
        <div class="card-actions">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增标签
          </el-button>
        </div>
      </div>

      <!-- 表格 -->
      <div class="table-container" v-loading="loading">
        <!-- 表格头部 -->
        <div class="table-header">
          <div class="header-cell" style="width: 80px;">ID</div>
          <div class="header-cell" style="flex: 1;">标签名称</div>
          <div class="header-cell" style="width: 120px;">文章数量</div>
          <div class="header-cell" style="width: 180px;">创建时间</div>
          <div class="header-cell" style="width: 200px;">操作</div>
        </div>

        <!-- 表格内容 -->
        <div class="table-body">
          <div
            v-for="item in tags"
            :key="item.id"
            class="table-row"
          >
            <div class="row-cell" style="width: 80px;">{{ item.id }}</div>
            <div class="row-cell" style="flex: 1;">{{ item.name }}</div>
            <div class="row-cell" style="width: 120px;">{{ item.articleCount || 0 }}</div>
            <div class="row-cell" style="width: 180px;">{{ formatDate(item.createTime) }}</div>
            <div class="row-cell actions" style="width: 200px;">
              <el-button link type="primary" @click="handleEdit(item)">
                编辑
              </el-button>
              <el-button link type="danger" @click="handleDelete(item)">
                删除
              </el-button>
            </div>
          </div>

          <div v-if="!loading && tags.length === 0" class="empty-data">
            暂无标签数据
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑标签' : '新增标签'"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入标签名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTagList, createTag, updateTag, deleteTag } from '@/api/tag'

// 状态管理
const loading = ref(false)
const submitting = ref(false)
const tags = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const formData = reactive({
  id: null,
  name: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
}

// 日期格式化函数
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const fetchTags = async () => {
  loading.value = true
  try {
    const res = await getTagList()
    tags.value = res.data || []
  } catch (error) {
    console.error('获取标签失败:', error)
    ElMessage.error('获取标签失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  formData.id = row.id
  formData.name = row.name
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除标签"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteTag(row.id)
    ElMessage.success('删除成功')
    await fetchTags()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      submitting.value = true
      const data = {
        name: formData.name
      }

      if (isEdit.value) {
        await updateTag(formData.id, data)
        ElMessage.success('更新成功')
      } else {
        await createTag(data)
        ElMessage.success('创建成功')
      }

      dialogVisible.value = false
      await fetchTags()
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleDialogClose = () => {
  formData.id = null
  formData.name = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style lang="scss" scoped>
.tag-manage {
  padding: 0;
}

.manage-card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-tertiary);

  .card-actions {
    display: flex;
    gap: 12px;
  }
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.table-container {
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  overflow: hidden;
  margin: var(--spacing-lg);
}

.table-header {
  display: flex;
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 13px;
  color: var(--text-secondary);
}

.header-cell {
  padding: 12px var(--spacing-md);
  display: flex;
  align-items: center;
  border-right: 1px solid var(--border-color);
  box-sizing: border-box;

  &:last-child {
    border-right: none;
  }
}

.table-body {
  min-height: 100px;
}

.table-row {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  transition: all 0.2s ease;
  background: var(--bg-primary);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--bg-tertiary);
  }
}

.row-cell {
  padding: 14px var(--spacing-md);
  display: flex;
  align-items: center;
  border-right: 1px solid var(--border-color);
  box-sizing: border-box;
  word-break: break-word;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:last-child {
    border-right: none;
  }

  &.actions {
    display: flex;
    gap: 8px;
    justify-content: center;
  }
}

.empty-data {
  padding: 40px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
}
</style>