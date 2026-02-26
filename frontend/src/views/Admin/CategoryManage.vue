<template>
  <div class="category-manage">
    <div class="manage-card">
      <div class="card-header">
        <h3 class="card-title">分类管理</h3>
        <div class="card-actions">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增分类
          </el-button>
          <el-button
            v-if="isDirty"
            type="success"
            @click="saveAllChanges"
            :loading="savingSort"
          >
            <el-icon><Finished /></el-icon>
            保存排序
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="showSortHelp"
        :closable="false"
        title="拖拽提示"
        type="info"
        description="点击并拖拽行左侧的图标来重新排序，排序完成后点击「保存排序」按钮"
        style="margin-bottom: 16px;"
      />

      <!-- 手动拖拽模式的列表实现 -->
      <div
        class="draggable-container"
        v-loading="loading"
      >
        <!-- 表格头部 -->
        <div class="table-header">
          <div class="header-cell" style="width: 50px;"></div>
          <div class="header-cell" style="width: 80px;">ID</div>
          <div class="header-cell" style="flex: 1;">分类名称</div>
          <div class="header-cell" style="flex: 1.5;">描述</div>
          <div class="header-cell" style="width: 180px;">创建时间</div>
          <div class="header-cell" style="width: 200px;">操作</div>
        </div>

        <!-- 拖拽列表 -->
        <div
          class="draggable-list"
          @dragover.prevent
          @drop="handleDrop"
        >
          <div
            v-for="(item, index) in categories"
            :key="item.id"
            class="draggable-row"
            :class="{
              'drag-over-top': dragOverIndex === index && dragOverPosition === 'top',
              'drag-over-bottom': dragOverIndex === index && dragOverPosition === 'bottom',
              'drag-source': dragSourceIndex === index
            }"
            draggable="true"
            @dragstart="handleDragStart($event, index)"
            @dragend="handleDragEnd"
            @dragover="handleDragOver($event, index)"
            @dragenter="handleDragEnter($event, index)"
          >
            <div class="row-content">
              <div class="row-cell" style="width: 50px;">
                <el-icon class="drag-handle" :class="{ dragging: isDragging }">
                  <Rank />
                </el-icon>
              </div>
              <div class="row-cell" style="width: 80px;">{{ item.id }}</div>
              <div class="row-cell" style="flex: 1;">{{ item.name }}</div>
              <div class="row-cell" style="flex: 1.5;" :title="item.description">{{ item.description }}</div>
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
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入分类名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
            maxlength="200"
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Plus, Rank, Finished } from '@element-plus/icons-vue'
import { getCategoryList, createCategory, updateCategory, deleteCategory, updateCategorySort } from '@/api/category'

// 状态管理
const loading = ref(false)
const savingSort = ref(false)
const submitting = ref(false)
const categories = ref([])
const originalCategories = ref([]) // 用于对比是否有变化
const dialogVisible = ref(false)
const isEdit = ref(false)
const isDragging = ref(false)
const dragSourceIndex = ref(-1) // 拖拽源索引
const dragOverIndex = ref(-1) // 拖拽悬停索引
const dragOverPosition = ref('') // 拖拽悬停位置 ('top' 或 'bottom')
const formRef = ref(null)

// 用于检测是否需要保存排序
const isDirty = computed(() => {
  return JSON.stringify(categories.value.map(c => c.id)) !==
         JSON.stringify(originalCategories.value.map(c => c.id))
})

// 用于显示帮助提示
const showSortHelp = ref(false)
setTimeout(() => {
  showSortHelp.value = true
}, 1000)

const formData = reactive({
  id: null,
  name: '',
  description: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '描述不能超过 200 个字符', trigger: 'blur' }
  ]
}

// 日期格式化函数
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr // 如果无法解析日期，则返回原值

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const fetchCategories = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
    originalCategories.value = JSON.parse(JSON.stringify(res.data || [])) // 保存原始状态副本
  } catch (error) {
    console.error('获取分类失败:', error)
    ElMessage.error('获取分类失败')
  } finally {
    loading.value = false
  }
}

// 拖拽开始
const handleDragStart = (event, index) => {
  isDragging.value = true
  dragSourceIndex.value = index
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', index.toString())

  // 添加拖拽图像
  event.dataTransfer.setDragImage(event.target.closest('.draggable-row'), 0, 0)
}

// 拖拽结束
const handleDragEnd = () => {
  isDragging.value = false
  dragSourceIndex.value = -1
  dragOverIndex.value = -1
  dragOverPosition.value = ''
}

// 拖拽悬停
const handleDragOver = (event, index) => {
  event.preventDefault()

  // 计算鼠标在元素中的位置
  const rect = event.currentTarget.getBoundingClientRect()
  const midpoint = (rect.top + rect.bottom) / 2
  const position = event.clientY < midpoint ? 'top' : 'bottom'

  dragOverIndex.value = index
  dragOverPosition.value = position
}

// 拖拽进入
const handleDragEnter = (event, index) => {
  event.preventDefault()
}

// 拖拽放置
const handleDrop = async (event) => {
  event.preventDefault()
  const sourceIndexStr = event.dataTransfer.getData('text/plain')
  if (!sourceIndexStr) {
    resetDragState()
    return
  }

  const sourceIndex = parseInt(sourceIndexStr)

  if (isNaN(sourceIndex) || sourceIndex === dragOverIndex.value) {
    // 相同位置不需要移动
    resetDragState()
    return
  }

  // 执行移动操作
  const newCategories = [...categories.value]
  const [movedItem] = newCategories.splice(sourceIndex, 1)

  // 根据拖拽位置计算目标索引
  let targetIndex = dragOverIndex.value
  // 如果是从上方移动到下方
  if (sourceIndex < dragOverIndex.value) {
    // 如果拖拽到目标行的下半部分，插入到目标行后面
    if (dragOverPosition.value === 'bottom') {
      targetIndex += 1
    }
    // 如果拖拽到目标行的上半部分，插入到目标行位置
  }
  // 如果是从下方移动到上方
  else if (sourceIndex > dragOverIndex.value) {
    // 如果拖拽到目标行的上半部分，插入到目标行位置
    if (dragOverPosition.value === 'top') {
      // targetIndex 不变，就是当前位置
    }
    // 如果拖拽到目标行的下半部分，插入到目标行后面
    else if (dragOverPosition.value === 'bottom') {
      targetIndex += 1
    }
  }

  newCategories.splice(targetIndex, 0, movedItem)
  categories.value = newCategories

  // 显示拖拽成功的通知
  const draggedItem = movedItem
  ElNotification({
    title: '拖拽成功',
    message: `分类 "${draggedItem.name}" 已重新排序`,
    type: 'success',
    duration: 2000
  })

  // 自动保存排序到后端
  await saveSortAutomatically()

  resetDragState()
}

// 重置拖拽状态
const resetDragState = () => {
  dragSourceIndex.value = -1
  dragOverIndex.value = -1
  dragOverPosition.value = ''
  isDragging.value = false
}

// 自动保存排序到后端
const saveSortAutomatically = async () => {
  if (!isDirty.value) {
    return // 如果没有变化，则不保存
  }

  try {
    savingSort.value = true

    // 发送新的排序顺序到后端
    const ids = categories.value.map(item => item.id)
    await updateCategorySort(ids)

    // 更新原始状态为当前状态
    originalCategories.value = JSON.parse(JSON.stringify(categories.value))

    // 只显示一次成功提示（避免与拖拽成功提示重复）
    console.log('排序已自动保存到后端')
  } catch (error) {
    console.error('自动保存排序失败:', error)
    ElMessage.error('排序保存失败，请手动保存')

    // 恢复原始排序
    categories.value = JSON.parse(JSON.stringify(originalCategories.value))
  } finally {
    savingSort.value = false
  }
}

// 保存所有排序更改 - 批量保存
const saveAllChanges = async () => {
  if (!isDirty.value) {
    ElMessage.info('没有排序变化需要保存')
    return
  }

  try {
    savingSort.value = true

    // 发送新的排序顺序到后端
    const ids = categories.value.map(item => item.id)
    await updateCategorySort(ids)

    // 更新原始状态为当前状态
    originalCategories.value = JSON.parse(JSON.stringify(categories.value))

    ElMessage.success('排序保存成功')
  } catch (error) {
    console.error('排序保存失败:', error)
    ElMessage.error('排序保存失败，请重试')

    // 询问用户是否恢复原始状态
    try {
      await ElMessageBox.confirm(
        '排序保存失败，是否恢复原始排序？',
        '保存失败',
        {
          confirmButtonText: '恢复',
          cancelButtonText: '保持当前',
          type: 'warning'
        }
      )
      // 恢复原始排序
      categories.value = JSON.parse(JSON.stringify(originalCategories.value))
    } catch {
      // 用户选择不恢复，则什么都不做
    }
  } finally {
    savingSort.value = false
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
  formData.description = row.description || ''
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteCategory(row.id)
    ElMessage.success('删除成功')

    // 重新获取数据以确保排序正确
    await fetchCategories()
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
        name: formData.name,
        description: formData.description
      }

      if (isEdit.value) {
        await updateCategory(formData.id, data)
        ElMessage.success('更新成功')
      } else {
        await createCategory(data)
        ElMessage.success('创建成功')
      }

      dialogVisible.value = false
      await fetchCategories() // 重新获取分类列表以确保排序正确
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
  formData.description = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.category-manage {
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

.draggable-container {
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  overflow: hidden;
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

.draggable-list {
  min-height: 100px;
}

.draggable-row {
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

  &.drag-over-top {
    border-top: 2px dashed var(--color-primary);
  }

  &.drag-over-bottom {
    border-bottom: 2px dashed var(--color-primary);
  }

  &.drag-source {
    opacity: 0.5;
  }
}

.row-content {
  display: flex;
  width: 100%;
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

.drag-handle {
  cursor: grab;
  color: var(--text-tertiary);
  transition: color 0.2s ease;

  &:hover {
    color: var(--text-primary);
  }

  &:active {
    cursor: grabbing;
  }

  &.dragging {
    color: var(--color-primary);
  }
}
</style>