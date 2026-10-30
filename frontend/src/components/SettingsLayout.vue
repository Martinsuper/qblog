<template>
  <div class="settings-layout">
    <header class="settings-header">
      <router-link
        to="/"
        class="settings-back"
      >
        ← 返回知识库
      </router-link>
      <span>设置</span>
    </header>
    <div class="settings-workspace">
      <nav
        class="settings-nav"
        aria-label="工作区设置"
      >
        <router-link
          v-for="item in items"
          :key="item.path"
          :to="item.path"
          :aria-current="route.path === item.path ? 'page' : undefined"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <main class="settings-content">
        <div class="settings-title">
          <h1>{{ title }}</h1><p v-if="description">
            {{ description }}
          </p>
        </div>
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { onMounted, onBeforeUnmount } from 'vue'
import { readAppearance } from '@/utils/knowledgePreferences'
defineProps({ title: { type: String, required: true }, description: String })
const route = useRoute()
const media = matchMedia('(prefers-color-scheme: dark)')
function applyAppearance() {
  const appearance = readAppearance()
  const color = appearance.color === 'system' ? (media.matches ? 'dark' : 'light') : appearance.color
  document.documentElement.dataset.theme = color
  document.documentElement.classList.toggle('dark', color === 'dark')
}
onMounted(() => { applyAppearance(); media.addEventListener('change', applyAppearance) })
onBeforeUnmount(() => media.removeEventListener('change', applyAppearance))
const items = [
  { path: '/settings/appearance', label: '外观与布局' },
  { path: '/admin/backup', label: '备份与导出' },
  { path: '/settings/tokens', label: 'API 与 MCP' },
  { path: '/user/profile', label: '账号与密码' }
]
</script>

<style scoped>
.settings-layout{min-height:100dvh;background:var(--bg-primary);color:var(--text-primary);font-size:13px}
.settings-header{height:48px;display:flex;align-items:center;gap:24px;padding:0 24px;border-bottom:1px solid var(--border-color);background:var(--bg-secondary)}
.settings-back{color:var(--color-primary);text-decoration:none}
.settings-header span{color:var(--text-secondary)}
.settings-workspace{display:grid;grid-template-columns:170px minmax(0,1fr);max-width:1240px;margin:0 auto;padding:24px;gap:28px}
.settings-nav{display:flex;flex-direction:column;gap:4px;align-self:start;position:sticky;top:24px}
.settings-nav a{padding:10px 12px;border-radius:5px;text-decoration:none;color:var(--text-secondary);white-space:nowrap}
.settings-nav a:hover{background:var(--bg-tertiary)}
.settings-nav a[aria-current='page']{background:var(--bg-tertiary);color:var(--color-primary);font-weight:600}
.settings-content{min-width:0;max-width:1000px}
.settings-title{margin:0 0 20px}
.settings-title h1{font-size:22px;margin:0;line-height:1.5;font-weight:600}
.settings-title p{color:var(--text-secondary);margin:6px 0 0;line-height:1.7}
.settings-content :deep(.tabs){margin:0 0 18px;gap:22px}
.settings-content :deep(section){padding:18px;margin-bottom:16px;border-radius:6px;box-shadow:none}
.settings-content :deep(.section-title){margin:20px 0 12px}
@media(max-width:700px){
 .settings-header{padding:0 16px}
 .settings-workspace{display:block;padding:16px}
 .settings-nav{position:static;flex-direction:row;overflow-x:auto;gap:2px;margin:0 -4px 20px}
 .settings-nav a{padding:10px;font-size:13px}
 .settings-title h1{font-size:20px}
 .settings-content :deep(section){padding:16px}
 .settings-content :deep(.heading),.settings-content :deep(.tokens-heading){flex-wrap:wrap;gap:12px}
}
</style>
