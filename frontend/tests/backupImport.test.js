import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import vm from 'node:vm'

const page = await readFile(new URL('../src/views/Knowledge/Backup.vue', import.meta.url), 'utf8')
const config = await readFile(new URL('../../backend/src/main/resources/application.yml', import.meta.url), 'utf8')
const nginx = await readFile(new URL('../../docker/nginx.conf', import.meta.url), 'utf8')
const pick = page.match(/function pickFile\(file\) \{[\s\S]*?\n\}/)?.[0]
const importAction = page.match(/async function importNow\(\) \{[\s\S]*?\n\}/)?.[0]
const warning = page.match(/const restoreWarning = ('[^']*')/)?.[1]
const limit = page.match(/const MAX_IMPORT_BYTES = ([^\n]+)/)?.[1]

function setup(confirm = async () => {}) {
  const messages = []
  const uploads = []
  const context = {
    picked: { value: null }, fileList: { value: [] }, importing: { value: false },
    ElMessage: { warning: message => messages.push(message), success: () => {} },
    ElMessageBox: { confirm }, importBackup: async file => uploads.push(file), loadList: () => {}
  }
  vm.createContext(context)
  vm.runInContext(`const MAX_IMPORT_BYTES = ${limit}; const restoreWarning = ${warning}; ${pick}\n${importAction}`, context)
  return { context, messages, uploads }
}

test('client limit matches server multipart limit', () => {
  assert.match(config, /max-file-size:\s*100MB/)
  assert.match(config, /max-request-size:\s*101MB/)
  assert.equal(vm.runInNewContext(limit), 100 * 1024 * 1024)
  assert.match(page, /不超过 100 MiB/)
})

test('nginx grants the larger limit only to backup import and preserves the API path', () => {
  const location = path => nginx.match(new RegExp(`location ${path} \\{([^}]+)\\}`))?.[1]
  const backup = location('= /api/backup/import')
  const image = location('= /api/upload/image')
  const api = location('/api/')
  assert.match(backup, /client_max_body_size\s+101m;/)
  assert.match(image, /client_max_body_size\s+6m;/)
  assert.match(api, /client_max_body_size\s+20m;/)
  assert.match(backup, /proxy_pass\s+http:\/\/qblog_backend;/)
  assert.match(image, /proxy_pass\s+http:\/\/qblog_backend;/)
})

test('accepts the boundary and rejects oversized, empty and invalid files without stale selections', async () => {
  const { context, messages, uploads } = setup()
  const valid = { name: 'backup.ZIP', size: 100 * 1024 * 1024 }
  context.pickFile({ raw: valid })
  assert.equal(context.picked.value, valid)
  await context.importNow()
  assert.equal(uploads[0], valid)
  for (const raw of [{ name: 'large.zip', size: 100 * 1024 * 1024 + 1 }, { name: 'empty.zip', size: 0 }, { name: 'bad.txt', size: 1 }]) {
    context.pickFile({ raw: valid })
    context.pickFile({ raw })
    assert.equal(context.picked.value, null)
    assert.equal(context.fileList.value.length, 0)
    await context.importNow()
  }
  assert.equal(messages.length, 3)
  assert.equal(uploads.length, 1)
})

test('both restore paths warn about v2, v3 and same-name uploaded files', () => {
  const text = vm.runInNewContext(warning)
  assert.match(text, /v2.*不修改知识库/)
  assert.match(text, /v3.*知识库/)
  assert.match(text, /覆盖同名文件/)
  assert.match(page, /async function restoreBackupFile[\s\S]*?ElMessageBox\.confirm\(\s*restoreWarning/)
  assert.match(importAction, /ElMessageBox\.confirm\(\s*restoreWarning/)
})

test('cancel or a changed selection during confirmation cannot import', async () => {
  let resolveConfirm
  const { context, uploads } = setup(() => new Promise(resolve => { resolveConfirm = resolve }))
  context.pickFile({ raw: { name: 'first.zip', size: 1 } })
  const pending = context.importNow()
  context.pickFile({ raw: { name: 'other.zip', size: 1 } })
  resolveConfirm()
  await pending
  assert.equal(uploads.length, 0)
  const cancelled = setup(async () => { throw new Error('cancel') })
  cancelled.context.pickFile({ raw: { name: 'a.zip', size: 1 } })
  await cancelled.context.importNow()
  assert.equal(cancelled.uploads.length, 0)
})
