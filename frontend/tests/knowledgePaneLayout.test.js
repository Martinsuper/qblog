import test from 'node:test'
import assert from 'node:assert/strict'
import { paneLayout, paneResizeLimit, clampPaneWidth } from '../src/utils/knowledgePaneLayout.js'

test('分栏保留正文最小宽度并限制首栏', () => {
  assert.deepEqual(paneLayout(700, [360, 560], [true, true]), { widths: [252, 200], reader: 240 })
  assert.equal(paneResizeLimit(0, 700, [true, true], [252, 200]), 252)
  assert.equal(clampPaneWidth(0, 999, 700, [true, true], [252, 200]), 252)
  assert.equal(clampPaneWidth(1, 0, 1000, [true, true], [190, 330]), 200)
})

test('收起和恢复保留首选宽度', () => {
  const preferred = [240, 400]
  assert.deepEqual(paneLayout(1000, preferred, [false, true]).widths, [0, 400])
  assert.deepEqual(paneLayout(1000, preferred, [true, false]).widths, [240, 0])
  assert.deepEqual(paneLayout(1000, preferred, [false, false]).widths, [0, 0])
  assert.deepEqual(paneLayout(1000, preferred, [true, true]).widths, preferred)
})

test('缩窗不改变首选宽度，极小宽度不会出现负值', () => {
  const preferred = [300, 500]
  assert.deepEqual(paneLayout(100, preferred, [true, true]), { widths: [0, 0], reader: 92 })
  assert.deepEqual(paneLayout(1400, preferred, [true, true]).widths, preferred)
  assert.equal(clampPaneWidth(0, 100, 100, [true, true], [0, 0]), 0)
})
