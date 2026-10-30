import test from 'node:test'
import assert from 'node:assert/strict'
import { COMMANDS, filterCommands } from '../src/utils/slashCommands.js'

test('empty or blank query returns the full catalog', () => {
  assert.equal(filterCommands('').length, COMMANDS.length)
  assert.equal(filterCommands('   ').length, COMMANDS.length)
  assert.equal(filterCommands(null).length, COMMANDS.length)
})

test('commands are matched by Chinese label', () => {
  const headings = filterCommands('标题')
  assert.equal(headings.length, 3)
  assert.deepEqual(headings.map(item => item.id), ['heading1', 'heading2', 'heading3'])
  assert.equal(filterCommands('列表').length, 3)
  assert.deepEqual(filterCommands('待办').map(item => item.id), ['taskList'])
})

test('commands are matched by pinyin and english keywords', () => {
  assert.deepEqual(filterCommands('bt').map(item => item.id), ['heading1', 'heading2', 'heading3'])
  assert.deepEqual(filterCommands('code').map(item => item.id), ['codeBlock'])
  assert.deepEqual(filterCommands('todo').map(item => item.id), ['taskList'])
  assert.deepEqual(filterCommands('hr').map(item => item.id), ['horizontalRule'])
  assert.deepEqual(filterCommands('TABLE').map(item => item.id), ['table'])
})

test('unmatched queries return an empty list', () => {
  assert.deepEqual(filterCommands('zzzz'), [])
})

test('every command exposes render data and a runnable action', () => {
  const ids = COMMANDS.map(item => item.id)
  assert.equal(new Set(ids).size, COMMANDS.length)
  for (const item of COMMANDS) {
    assert.ok(item.glyph && item.label && item.hint, `${item.id} missing render data`)
    assert.equal(typeof item.run, 'function')
  }
})
