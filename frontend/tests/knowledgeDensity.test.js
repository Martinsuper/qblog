import test from 'node:test'
import assert from 'node:assert/strict'
import { readKnowledgeDensity, saveKnowledgeDensity } from '../src/utils/knowledgeDensity.js'

test('density defaults to standard and rejects unknown preferences', () => {
  for (const value of [null, '', 'invalid', 'standard']) {
    assert.equal(readKnowledgeDensity({ getItem: () => value }), 'standard')
  }
  assert.equal(readKnowledgeDensity({ getItem: () => 'compact' }), 'compact')
})

test('density persists across reads and normalizes invalid writes', () => {
  const values = new Map()
  const storage = { getItem: key => values.get(key), setItem: (key, value) => values.set(key, value) }
  saveKnowledgeDensity(storage, 'compact')
  assert.equal(readKnowledgeDensity(storage), 'compact')
  saveKnowledgeDensity(storage, 'invalid')
  assert.equal(readKnowledgeDensity(storage), 'standard')
})

test('unavailable storage does not break the workspace', () => {
  const storage = { getItem() { throw new Error('blocked') }, setItem() { throw new Error('quota') } }
  assert.equal(readKnowledgeDensity(storage), 'standard')
  assert.doesNotThrow(() => saveKnowledgeDensity(storage, 'compact'))
  assert.equal(readKnowledgeDensity(), 'standard')
})
