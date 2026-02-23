<template>
  <div>
    <div class="card p-6">
      <div class="flex items-center justify-between mb-5">
        <h3 class="text-lg font-semibold" style="color: var(--text-primary)">
          {{ isEdit ? '编辑文章' : '创建文章' }}
        </h3>
        <el-button type="primary" @click="handleSubmit">发布</el-button>
      </div>

      <el-form :model="articleForm" label-width="80px">
        <el-form-item label="标题">
          <el-input
            v-model="articleForm.title"
            placeholder="请输入文章标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="articleForm.categoryId" placeholder="请选择分类">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="articleForm.tagIds"
            multiple
            placeholder="请选择标签"
            allow-create
            filterable
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="封面图">
          <el-upload
            class="cover-uploader"
            action="/api/v1/upload/image"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
          >
            <img v-if="articleForm.coverImage" :src="articleForm.coverImage" class="cover" />
            <el-icon v-else class="uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="摘要">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容">
          <div class="editor-container">
            <div class="editor">
              <el-input
                v-model="articleForm.content"
                type="textarea"
                :rows="20"
                placeholder="使用 Markdown 格式编写内容"
              />
            </div>
            <div class="preview">
              <div class="preview-content prose" v-html="renderedContent"></div>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit">发布</el-button>
          <el-button @click="handleSaveDraft">保存草稿</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { createArticle, updateArticle, getArticleDetail } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const route = useRoute()
const router = useRouter()
const md = new MarkdownIt()

const isEdit = ref(false)
const categories = ref([])
const tags = ref([])

const articleForm = reactive({
  id: null,
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  categoryId: null,
  tagIds: [],
  status: 1
})

const renderedContent = computed(() => {
  return articleForm.content ? md.render(articleForm.content) : ''
})

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchTags = async () => {
  try {
    const res = await getTagList()
    tags.value = res.data || []
  } catch (error) {
    console.error('获取标签失败:', error)
  }
}

const fetchArticle = async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    articleForm.id = id
    try {
      const res = await getArticleDetail(id)
      const data = res.data
      Object.assign(articleForm, {
        id: data.id,
        title: data.title,
        summary: data.summary,
        content: data.content,
        coverImage: data.coverImage,
        categoryId: data.category?.id,
        tagIds: data.tags?.map(t => t.id) || []
      })
    } catch (error) {
      console.error('获取文章失败:', error)
    }
  }
}

const handleCoverSuccess = (response) => {
  if (response.code === 200) {
    articleForm.coverImage = response.data.url
  }
}

const handleSubmit = async () => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  try {
    articleForm.status = 1
    if (isEdit.value) {
      await updateArticle(articleForm.id, articleForm)
      ElMessage.success('文章更新成功')
    } else {
      await createArticle(articleForm)
      ElMessage.success('文章发布成功')
    }
    router.push('/admin/articles')
  } catch (error) {
    console.error('发布文章失败:', error)
  }
}

const handleSaveDraft = async () => {
  try {
    articleForm.status = 0
    if (isEdit.value) {
      await updateArticle(articleForm.id, articleForm)
    } else {
      await createArticle(articleForm)
    }
    ElMessage.success('草稿保存成功')
  } catch (error) {
    console.error('保存草稿失败:', error)
  }
}

onMounted(() => {
  fetchCategories()
  fetchTags()
  fetchArticle()
})
</script>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.cover-uploader {
  border: 1px dashed var(--border-color);
  border-radius: var(--border-radius);
  cursor: pointer;
  width: 200px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color var(--transition-fast);
}

.cover-uploader:hover {
  border-color: var(--color-primary);
}

.cover-uploader .cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--border-radius);
}

.cover-uploader .uploader-icon {
  font-size: 28px;
  color: var(--text-tertiary);
}

.editor-container {
  display: flex;
  gap: 20px;
  height: 500px;
}

.editor-container .editor,
.editor-container .preview {
  flex: 1;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow-y: auto;
}

.editor-container .preview {
  background: var(--bg-secondary);
}

.editor-container .preview .preview-content {
  padding: 20px;
}
</style>
