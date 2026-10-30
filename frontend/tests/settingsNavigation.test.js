import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('settings share navigation with an accessible current page marker', () => {
  const layout = read('components/SettingsLayout.vue')
  for (const path of ['/settings/appearance', '/settings/backup', '/settings/tokens', '/user/profile']) assert.ok(layout.includes(path))
  assert.match(layout, /aria-current/)
  assert.match(layout, /aria-label="工作区设置"/)
  for (const path of ['views/Knowledge/Appearance.vue', 'views/Knowledge/Backup.vue', 'views/Knowledge/Tokens.vue', 'views/User/Profile.vue']) assert.match(read(path), /<SettingsLayout/)
})

test('appearance settings preserve existing preferences and authenticated routes', () => {
  const appearance = read('views/Knowledge/Appearance.vue')
  assert.match(appearance, /readAppearance/)
  assert.match(appearance, /saveAppearance/)
  assert.match(appearance, /saveKnowledgeDensity/)
  assert.match(appearance, /setMarkdownTheme/)
  assert.match(read('router/index.js'), /path: '\/settings\/appearance'[\s\S]*?requiresAuth: true, requiresAdmin: true, standalone: true/)
})

test('settings remove nonfunctional profile save and placeholder connection status', () => {
  const profile = read('views/User/Profile.vue')
  assert.doesNotMatch(profile, /handleUpdate|功能开发中/)
  assert.match(profile, /fetchUserInfo/)
  assert.match(profile, /await changePassword/)
  const tokens = read('views/Knowledge/Tokens.vue')
  assert.doesNotMatch(tokens, /尚未测试连接|WORKSPACE SETTINGS/)
  assert.match(tokens, /完整凭证仅展示一次/)
  assert.match(read('views/Knowledge/Backup.vue'), /恢复会覆盖/)
})
