import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse } from '@vue/compiler-sfc'
import { parse as parseTemplate } from '@vue/compiler-dom'

test('目录树对每个目录只执行一次循环', () => {
  const source = readFileSync(new URL('../src/components/KnowledgeSeven.vue', import.meta.url), 'utf8')
  const template = parse(source).descriptor.template.content
  const ast = parseTemplate(template)
  let folderLoops = 0
  function visit(node) {
    if (node.props?.some(prop => prop.type === 7 && prop.name === 'for' && prop.exp?.content.includes('folder in displayedFolders'))) folderLoops++
    node.children?.forEach(visit)
  }
  visit(ast)
  assert.equal(folderLoops, 1)
})
