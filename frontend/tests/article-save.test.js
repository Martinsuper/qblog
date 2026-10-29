import test from 'node:test'
import assert from 'node:assert/strict'
import { createArticleSaveQueue } from '../src/utils/articleSaveQueue.js'

const deferred = () => {
  let resolve, reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

function fixture(persist = async () => {}) {
  const form = { title: 'published', content: 'body', status: 1, tagIds: [1] }
  const writes = []
  const queue = createArticleSaveQueue({
    getSnapshot: () => structuredClone(form),
    persist: async (snapshot) => { writes.push(snapshot); await persist(snapshot) },
    onSaved: (snapshot) => { form.status = snapshot.status }
  })
  queue.markSaved()
  return { form, writes, queue }
}

test('unchanged published form is not automatically saved', async () => {
  const { writes, queue } = fixture()
  assert.equal(await queue.save({ automatic: true }), false)
  assert.deepEqual(writes, [])
})

test('automatic save preserves publication status and skips acknowledged content', async () => {
  const { form, writes, queue } = fixture()
  form.content = 'updated'
  assert.equal(await queue.save({ automatic: true }), true)
  assert.equal(writes[0].status, 1)
  assert.equal(await queue.save({ automatic: true }), false)
})

test('manual publish waits for automatic write; automatic duplicates are skipped', async () => {
  const gate = deferred()
  const { form, writes, queue } = fixture(async () => { if (writes.length === 1) await gate.promise })
  form.status = 0
  form.content = 'draft'
  const automatic = queue.save({ automatic: true })
  await Promise.resolve()
  const publish = queue.save({ status: 1 })
  assert.equal(await queue.save({ automatic: true }), false)
  assert.equal(writes.length, 1)
  // Payloads must not be live references to the editable form.
  form.content = 'latest'
  assert.equal(writes[0].content, 'draft')
  gate.resolve()
  await Promise.all([automatic, publish])
  assert.deepEqual(writes.map(w => w.status), [0, 1])
  assert.equal(writes[1].content, 'latest')
  assert.equal(form.status, 1)
})

test('edits made during a write remain dirty for the next automatic save', async () => {
  const gate = deferred()
  const { form, writes, queue } = fixture(async () => { if (writes.length === 1) await gate.promise })
  form.content = 'first'
  const saving = queue.save({ automatic: true })
  await Promise.resolve()
  form.content = 'second'
  gate.resolve()
  await saving
  await queue.save({ automatic: true })
  assert.deepEqual(writes.map(w => w.content), ['first', 'second'])
})

test('failed write stays dirty and does not poison the write queue', async () => {
  let fail = true
  const { form, writes, queue } = fixture(async () => { if (fail) throw new Error('network') })
  form.content = 'changed'
  await assert.rejects(queue.save({ automatic: true }), /network/)
  fail = false
  assert.equal(await queue.save({ automatic: true }), true)
  assert.equal(writes.length, 2)
})

test('disposing cancels queued writes without issuing another request', async () => {
  const { form, writes, queue } = fixture()
  form.content = 'changed'
  const pending = queue.save({ automatic: true })
  queue.dispose()
  assert.equal(await pending, false)
  assert.equal(writes.length, 0)
})
