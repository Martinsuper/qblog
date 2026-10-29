import test from 'node:test'
import assert from 'node:assert/strict'
import { createNoteAutosave } from '../src/utils/noteAutosave.js'

test('reverting while save is in flight remains dirty until final snapshot is saved', async () => {
  let draft = 'A', release, server = 'A'
  const q = createNoteAutosave({ snapshot: () => draft, persist: async value => { await new Promise(r => { release = r }); server = value; return {} }, onSaved() {}, onError() {} })
  draft = 'B'; const b = q.flush(); await Promise.resolve(); draft = 'A'
  assert.equal(q.isDirty(), true)
  release(); await b; assert.equal(q.isDirty(), true); assert.equal(server, 'B')
  const a = q.flush(); await Promise.resolve(); release(); await a
  assert.equal(server, 'A'); assert.equal(q.isDirty(), false)
})
test('disposed in-flight save cannot call old document callbacks', async () => {
  let draft = 'A', release, callbacks = 0
  const q = createNoteAutosave({ snapshot: () => draft, persist: async () => { await new Promise(r => { release = r }); return {} }, onSaved() { callbacks++ }, onError() { callbacks++ } })
  draft = 'B'; const pending = q.flush(); await Promise.resolve(); q.dispose(); release(); await pending
  assert.equal(callbacks, 0)
})
