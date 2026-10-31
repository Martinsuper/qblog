import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'

const source = readFileSync(new URL('../src/views/Knowledge/Index.vue', import.meta.url), 'utf8')
const loadSource = source.slice(source.indexOf('async function loadNotes()'), source.indexOf('function scheduleSearch()'))
const ref = value => ({ value })
function setup(listNotes, scopeValue = 'all') {
  const context = {
    loadGeneration: 0, loadingList: ref(false), listError: ref(false),
    appearance: { layout: 'knowledge' }, pageSize: 2, BULK_PAGE_SIZE: 2, notes: ref([]), total: ref(0),
    scope: ref(scopeValue),
    search: ref('Vue'), localSearch: ref('组合式'), tagFilter: ref('前端'), sourceFilter: ref('mcp'),
    sortOrder: ref('title'), api: { listNotes }
  }
  return { context, load: runInNewContext(`${loadSource}\nloadNotes`, context) }
}

test('knowledge search forwards combined filters on every page and collects all tree results', async () => {
  const calls = []
  const { context, load } = setup(async params => {
    calls.push(params)
    return { data: { total: 3, records: params.current === 1 ? [{ id: 1 }, { id: 2 }] : [{ id: 3 }] } }
  })
  await load()
  assert.equal(calls.length, 2)
  for (const params of calls) {
    assert.equal(params.search, 'Vue')
    assert.equal(params.query, '组合式')
    assert.equal(params.tag, '前端')
    assert.equal(params.source, 'mcp')
    assert.equal(params.sort, 'title')
    assert.equal(params.favorite, undefined)
  }
  assert.equal(context.notes.value.length, 3)
  assert.equal(context.total.value, 3)
  assert.equal(context.loadingList.value, false)
})

test('favorites scope forwards the favorite filter on every page', async () => {
  const calls = []
  const { load } = setup(async params => {
    calls.push(params)
    return { data: { total: 1, records: [{ id: 1 }] } }
  }, 'favorites')
  await load()
  assert.equal(calls.length, 1)
  assert.equal(calls[0].favorite, true)
})

test('a newer search wins when an earlier request finishes late', async () => {
  let finishOld
  let calls = 0
  const { context, load } = setup(() => ++calls === 1
    ? new Promise(resolve => { finishOld = resolve })
    : Promise.resolve({ data: { total: 1, records: [{ id: 2 }] } }))
  const old = load()
  await load()
  finishOld({ data: { total: 1, records: [{ id: 1 }] } })
  await old
  assert.equal(context.notes.value[0].id, 2)
  assert.equal(context.loadingList.value, false)
})

test('a failed search keeps the existing results available for retry', async () => {
  const { context, load } = setup(async () => { throw new Error('offline') })
  context.notes.value = [{ id: 7 }]
  await load()
  assert.equal(context.notes.value[0].id, 7)
  assert.equal(context.listError.value, true)
  assert.equal(context.loadingList.value, false)
})
