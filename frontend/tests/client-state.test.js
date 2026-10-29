import test from 'node:test'
import assert from 'node:assert/strict'
import { getCachedUserInfo } from '../src/utils/auth.js'
import { createLatestRequest } from '../src/utils/latestRequest.js'

for (const raw of ['{', 'null', '[]', '"admin"', '{"role":99}', '{}']) {
  test(`invalid user cache ${raw} falls back safely and is removed`, () => {
    let removed = false
    const storage = { getItem: () => raw, removeItem: () => { removed = true } }
    assert.equal(getCachedUserInfo(storage), null)
    assert.equal(removed, true)
  })
}

test('valid user cache is preserved', () => {
  const storage = { getItem: () => '{"id":1,"role":1}', removeItem: () => assert.fail('removed') }
  assert.deepEqual(getCachedUserInfo(storage), { id: 1, role: 1 })
})

test('unavailable storage does not crash cache parsing', () => {
  const storage = { getItem: () => { throw new Error('denied') }, removeItem: () => { throw new Error('denied') } }
  assert.equal(getCachedUserInfo(storage), null)
})

test('out-of-order responses and stale finally callbacks cannot replace current state', async () => {
  const requests = createLatestRequest()
  let resolveOld
  const oldResponse = new Promise(resolve => { resolveOld = resolve })
  const oldCurrent = requests.start()
  const state = { value: null, loading: true }
  const old = oldResponse.then(value => { if (oldCurrent()) state.value = value })
    .finally(() => { if (oldCurrent()) state.loading = false })
  const current = requests.start()
  if (current()) state.value = 'new article and related results'
  resolveOld('old article')
  await old
  assert.equal(state.value, 'new article and related results')
  assert.equal(state.loading, true)
  requests.invalidate()
  assert.equal(current(), false)
})
