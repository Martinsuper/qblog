<template>
  <div>
    <div class="card p-6">
      <div class="flex items-center justify-between mb-5">
        <h3 class="text-lg font-semibold" style="color: var(--text-primary)">文章管理</h3>
        <el-button type="primary" @click="$router.push('/admin/create')">
          <el-icon><Plus /></el-icon> 写文章
        </el-button>
      </div>

      <el-form :inline="true" :model="searchForm" class="mb-5">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索文章标题" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable>
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <span>{{ row.category?.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="commentCount" label="评论" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/admin/create?id=${row.id}`)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="flex justify-end mt-5"
        @current-change="fetchArticles"
        @size-change="fetchArticles"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArticleList, deleteArticle } from '@/api/article'
import { getCategoryList } from '@/api/category'

const loading = ref(false)
const tableData = ref([])
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  categoryId: null
})

const fetchArticles = async () => {
  loading.value = true
  try {
    const res = await getArticleList({
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchArticles()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  handleSearch()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteArticle(id)
      ElMessage.success('删除成功')
      fetchArticles()
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

onMounted(() => {
  fetchArticles()
  fetchCategories()
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}
</style>
