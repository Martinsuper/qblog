<template>
  <SettingsLayout
    title="外观与布局"
    description="偏好仅保存在当前浏览器，修改后立即生效。"
  >
    <section class="appearance-section">
      <h2>知识库布局</h2>
      <div class="choice-grid">
        <button
          v-for="item in layouts"
          :key="item.value"
          :aria-pressed="appearance.layout === item.value"
          @click="update('layout', item.value)"
        >
          <strong>{{ item.label }}</strong><span>{{ item.description }}</span>
        </button>
      </div>
    </section>
    <section class="appearance-section">
      <h2>配色</h2>
      <div class="choice-row">
        <button
          v-for="(label, value) in colors"
          :key="value"
          :aria-pressed="appearance.color === value"
          @click="update('color', value)"
        >
          {{ label }}
        </button>
      </div>
    </section>
    <section class="appearance-section">
      <h2>笔记列表密度</h2>
      <div class="choice-row">
        <button
          v-for="(label, value) in densities"
          :key="value"
          :aria-pressed="density === value"
          @click="setDensity(value)"
        >
          {{ label }}
        </button>
      </div>
      <p>适用于默认目录知识库布局。</p>
    </section>
    <section class="appearance-section">
      <h2>博客文章渲染风格</h2>
      <div class="choice-row">
        <button
          v-for="name in ['vuepress', 'github', 'astro']"
          :key="name"
          :aria-pressed="settings.markdownTheme === name"
          @click="settings.setMarkdownTheme(name)"
        >
          {{ themeNames[name] }}
        </button>
      </div>
    </section>
  </SettingsLayout>
</template>

<script setup>
import { reactive, ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import SettingsLayout from '@/components/SettingsLayout.vue'
import { readAppearance, saveAppearance } from '@/utils/knowledgePreferences'
import { readKnowledgeDensity, saveKnowledgeDensity } from '@/utils/knowledgeDensity'
import { useSettingsStore } from '@/stores/settings'
const appearance = reactive(readAppearance()), settings = useSettingsStore(), density = ref('standard')
const layouts = [
  { value: 'knowledge', label: '目录知识库', description: '紧凑三栏，直接编辑' },
  { value: 'calm', label: '留白三栏', description: '宽松布局，专注阅读' },
  { value: 'paper', label: '纸间卡片', description: '暖色卡片，展开正文' },
  { value: 'library', label: '笔记书架', description: '按目录浏览主题' }
]
const colors = { system: '跟随系统', light: '浅色', dark: '深色' }, densities = { standard: '标准 · 标题与摘要', compact: '紧凑 · 仅标题' }
const themeNames = { vuepress: 'VuePress', github: 'GitHub', astro: 'Astro' }
const media = matchMedia('(prefers-color-scheme: dark)'), systemDark = ref(media.matches)
const resolved = computed(() => appearance.color === 'system' ? (systemDark.value ? 'dark' : 'light') : appearance.color)
watch(resolved, color => { document.documentElement.dataset.theme = color; document.documentElement.classList.toggle('dark', color === 'dark') }, { immediate: true })
function update(key, value) { appearance[key] = value; if (!saveAppearance(appearance)) ElMessage.warning('浏览器存储不可用，偏好无法记住') }
function setDensity(value) { density.value = value; try { saveKnowledgeDensity(localStorage, value) } catch { /* In-memory preference remains usable. */ } }
function systemChanged(event) { systemDark.value = event.matches }
onMounted(() => { try { density.value = readKnowledgeDensity(localStorage) } catch { /* Use standard density. */ } media.addEventListener('change', systemChanged) })
onBeforeUnmount(() => media.removeEventListener('change', systemChanged))
</script>

<style scoped>
.appearance-section{border:1px solid var(--border-color);background:var(--bg-secondary)}
h2{font-size:15px;font-weight:600;margin:0 0 14px}
p{font-size:12px;color:var(--text-secondary);margin:10px 0 0}
.choice-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}
.choice-row{display:flex;flex-wrap:wrap;gap:8px}
button{border:1px solid var(--border-color);background:transparent;color:var(--text-primary);padding:10px 14px;border-radius:5px;text-align:left;cursor:pointer;font:inherit;min-height:40px}
button[aria-pressed='true']{border-color:var(--color-primary);color:var(--color-primary);background:var(--bg-tertiary)}
button:focus-visible{outline:2px solid var(--color-primary);outline-offset:2px}
strong,span{display:block}strong{font-weight:600}span{font-size:12px;color:var(--text-secondary);margin-top:5px}
@media(max-width:450px){.choice-grid{grid-template-columns:1fr}}
</style>
