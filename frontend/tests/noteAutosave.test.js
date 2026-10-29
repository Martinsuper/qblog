import test from 'node:test'
import assert from 'node:assert/strict'
import { createNoteAutosave } from '../src/utils/noteAutosave.js'
import { readAppearance, saveAppearance } from '../src/utils/knowledgePreferences.js'

test('autosave serializes writes and preserves edits made during a request', async () => {
  let draft = { title: 'one' }, release, version = 0
  const seen = []
  const queue = createNoteAutosave({ snapshot: () => draft, persist: async value => { seen.push({ ...value, version }); await new Promise(r => { release = r }); return { version: ++version } }, onSaved: () => {}, onError: () => {} })
  draft = { title: 'two' }; const first = queue.flush(); await Promise.resolve()
  draft = { title: 'three' }; release(); await first
  assert.equal(queue.isDirty(), true)
  const second = queue.flush(); await Promise.resolve(); release(); await second
  assert.deepEqual(seen, [{ title: 'two', version: 0 }, { title: 'three', version: 1 }]); assert.equal(queue.isDirty(), false)
})
test('conflicts block automatic overwrites and preserve dirty draft', async () => {
  let draft = 'one', calls = 0, conflict
  const queue = createNoteAutosave({ snapshot: () => draft, persist: async () => { calls++; throw { response: { status: 409 } } }, onSaved: () => {}, onError: (_, blocked) => { conflict = blocked } })
  draft = 'two'; assert.equal(await queue.flush(), false); assert.equal(conflict, true); await queue.flush(); assert.equal(calls, 1); assert.equal(queue.isDirty(), true)
})
test('network failure can retry and is not acknowledged', async () => {
  let draft = 'a', calls = 0
  const queue = createNoteAutosave({ snapshot: () => draft, persist: async () => { if (++calls === 1) throw Error('offline'); return {} }, onSaved: () => {}, onError: () => {} })
  draft = 'b'; await queue.flush(); assert.equal(queue.isDirty(), true); await queue.flush(); assert.equal(queue.isDirty(), false)
})
test('appearance rejects corrupt or unknown preferences', () => {
  assert.deepEqual(readAppearance({ getItem: () => '{' }), { layout: 'knowledge', color: 'light' })
  assert.deepEqual(readAppearance({ getItem: () => '{"layout":"paper","color":"dark"}' }), { layout: 'paper', color: 'dark' })
  assert.equal(saveAppearance({}, { setItem() { throw Error('denied') } }), false)
})
