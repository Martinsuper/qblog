<template>
  <div class="article-detail-page">
    <article v-if="article" class="article-content">
      <el-card>
        <!-- 文章头部 -->
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          
          <div class="article-meta">
            <div class="author-info">
              <el-avatar :size="32" :src="article.author?.avatar">
                {{ article.author?.nickname?.charAt(0) }}
              </el-avatar>
              <span class="author-name">{{ article.author?.nickname || '管理员' }}</span>
            </div>
            <div class="meta-items">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ article.publishTime }}
              </span>
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ article.viewCount }}
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                {{ article.likeCount }}
              </span>
              <span class="meta-item">
                <el-icon><ChatDotRound /></el-icon>
                {{ article.commentCount }}
              </span>
            </div>
          </div>
        </div>

        <!-- 封面图 -->
        <div v-if="article.coverImage" class="article-cover">
          <img :src="article.coverImage" alt="封面图" />
        </div>

        <!-- 文章内容 -->
        <div class="article-body markdown-body" v-html="renderedContent"></div>

        <!-- 标签 -->
        <div v-if="article.tags && article.tags.length > 0" class="article-tags">
          <el-tag
            v-for="tag in article.tags"
            :key="tag.id"
            size="small"
            style="margin-right: 10px; margin-bottom: 10px;"
          >
            {{ tag.name }}
          </el-tag>
        </div>

        <!-- 操作按钮 -->
        <div class="article-actions">
          <el-button type="primary" @click="handleLike">
            <el-icon><Star /></el-icon>
            点赞 ({{ article.likeCount }})
          </el-button>
          <el-button @click="handleFavorite">
            <el-icon>
              <StarFilled v-if="article.isFavorited" />
              <Star v-else />
            </el-icon>
            收藏
          </el-button>
          <el-button @click="handleShare">
            <el-icon><Share /></el-icon>
            分享
          </el-button>
        </div>
      </el-card>

      <!-- 评论区 -->
      <el-card class="comment-section">
        <h3 class="comment-title">评论 ({{ commentCount }})</h3>
        
        <div class="comment-editor">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="4"
            placeholder="写下你的评论..."
          />
          <el-button type="primary" class="mt-3" @click="submitComment">
            发表评论
          </el-button>
        </div>

        <div class="comment-list">
          <el-empty v-if="commentCount === 0" description="暂无评论" />
        </div>
      </el-card>
    </article>

    <el-empty v-else description="文章不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import plantumlEncoder from 'plantuml-encoder'
import { getArticleDetail, likeArticle } from '@/api/article'

const route = useRoute()

// PlantUML 渲染函数
const renderPlantUML = (code) => {
  try {
    const encoded = plantumlEncoder.encode(code)
    return `<div class="plantuml-diagram"><img src="https://www.plantuml.com/plantuml/svg/${encoded}" alt="PlantUML Diagram" /></div>`
  } catch (error) {
    console.error('PlantUML 渲染失败:', error)
    return `<div class="plantuml-error">PlantUML 渲染失败</div>`
  }
}

// 创建 Markdown 实例
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function(str, lang) {
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

// 重写代码块渲染，支持 PlantUML
const defaultFenceRenderer = md.renderer.rules.fence
md.renderer.rules.fence = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const info = token.info ? token.info.trim() : ''
  
  // 检查是否是 PlantUML 代码块
  if (info === 'plantuml' || info === 'puml') {
    return renderPlantUML(token.content)
  }
  
  // 默认代码块渲染
  return defaultFenceRenderer(tokens, idx, options, env, self)
}

const article = ref(null)
const commentContent = ref('')
const commentCount = ref(0)

const renderedContent = computed(() => {
  if (!article.value?.content) {
    console.log('没有内容')
    return '<p>暂无内容</p>'
  }
  const content = article.value.content
  console.log('原始内容:', content)
  console.log('渲染后内容:', md.render(content))
  return md.render(content)
})

const fetchArticle = async () => {
  try {
    const res = await getArticleDetail(route.params.id)
    article.value = res.data
    commentCount.value = res.data.commentCount || 0
    console.log('文章数据:', article.value)
  } catch (error) {
    console.error('获取文章失败:', error)
    ElMessage.error('获取文章失败')
  }
}

const handleLike = async () => {
  try {
    await likeArticle(article.value.id)
    article.value.likeCount++
    ElMessage.success('点赞成功')
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

const handleFavorite = () => {
  ElMessage.info('收藏功能开发中...')
}

const handleShare = () => {
  ElMessage.info('分享功能开发中...')
}

const submitComment = () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  ElMessage.info('评论功能开发中...')
}

onMounted(() => {
  fetchArticle()
})
</script>

<style lang="scss" scoped>
.article-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.article-content {
  margin-bottom: 20px;
}

.article-header {
  margin-bottom: 30px;

  .article-title {
    font-size: 28px;
    font-weight: bold;
    color: #333;
    margin-bottom: 20px;
    line-height: 1.4;
  }

  .article-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 15px;
    color: #666;
    font-size: 14px;

    .author-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .author-name {
        font-weight: 500;
      }
    }

    .meta-items {
      display: flex;
      gap: 15px;
      flex-wrap: wrap;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.article-cover {
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    max-height: 400px;
    object-fit: cover;
  }
}

.article-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;

  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin-top: 24px;
    margin-bottom: 16px;
    font-weight: 600;
    line-height: 1.25;
    color: #333;
  }

  :deep(h1) {
    font-size: 24px;
    border-bottom: 1px solid #eaecef;
    padding-bottom: 0.3em;
  }

  :deep(h2) {
    font-size: 20px;
    border-bottom: 1px solid #eaecef;
    padding-bottom: 0.3em;
  }

  :deep(h3) {
    font-size: 16px;
  }

  :deep(p) {
    margin-bottom: 16px;
  }

  :deep(code) {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    background: #f6f8fa;
    padding: 0.2em 0.4em;
    border-radius: 3px;
    font-size: 85%;
  }

  :deep(pre) {
    background: #f6f8fa;
    padding: 16px;
    border-radius: 6px;
    overflow-x: auto;
    margin-bottom: 16px;

    code {
      background: none;
      padding: 0;
      font-size: 14px;
      line-height: 1.45;
    }
  }

  // PlantUML 图表样式
  :deep(.plantuml-diagram) {
    margin: 24px 0;
    padding: 20px;
    background: #fff;
    border: 1px solid #eaecef;
    border-radius: 6px;
    text-align: center;
    display: flex;
    justify-content: center;
    align-items: center;

    img {
      max-width: 100%;
      height: auto;
    }
  }

  :deep(.plantuml-error) {
    margin: 16px 0;
    padding: 12px 16px;
    background: #fef0f0;
    border: 1px solid #fde2e2;
    border-radius: 6px;
    color: #f56c6c;
    font-size: 14px;
  }

  :deep(a) {
    color: #409eff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(ul), :deep(ol) {
    padding-left: 2em;
    margin-bottom: 16px;
  }

  :deep(li) {
    margin-bottom: 8px;
  }

  :deep(blockquote) {
    padding: 0 1em;
    color: #6a737d;
    border-left: 0.25em solid #dfe2e5;
    margin-bottom: 16px;
  }

  :deep(img) {
    max-width: 100%;
    box-sizing: border-box;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin-bottom: 16px;

    th, td {
      border: 1px solid #dfe2e5;
      padding: 6px 13px;
    }

    tr:nth-child(2n) {
      background-color: #f6f8fa;
    }
  }
}

.article-tags {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eaecef;
}

.article-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eaecef;
  display: flex;
  gap: 10px;
}

.comment-section {
  margin-top: 20px;

  .comment-title {
    font-size: 18px;
    margin-bottom: 20px;
    color: #333;
  }

  .comment-editor {
    margin-bottom: 20px;
  }
}

.mt-3 {
  margin-top: 12px;
}
</style>
