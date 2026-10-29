/** Prepare the original prototype's sample data for visual-only component QA.
 * No server requests, authentication changes or database writes are performed.
 * Run from the frontend directory: node tests/visual-seven/prepare.mjs
 * With Vite running, open /tests/visual-seven/index.html.
 */
import fs from 'node:fs/promises'
import vm from 'node:vm'
const source = await fs.readFile(new URL('../../../docs/ui-prototypes/07-knowledge.html', import.meta.url), 'utf8')
const start = source.indexOf('const folders='), end = source.indexOf('const STORAGE_KEY=')
if (start < 0 || end <= start) throw new Error('Prototype fixture boundaries were not found')
const data = vm.runInNewContext(source.slice(start, end) + ';({folders,seed})', {}, { timeout: 1000 })
let id = 1
const ids = new Map()
const folders = data.folders.map(parent => {
  const folder = { id: id++, name: parent.name, parentId: null, sort: 0 }
  ids.set(parent.id, folder.id)
  folder.children = parent.children.map(child => {
    const item = { id: id++, name: child.name, parentId: folder.id, sort: 0, children: [] }
    ids.set(child.id, item.id)
    return item
  })
  return folder
})
const notes = data.seed.map((note, index) => ({
  id: index + 1, title: note.title, content: note.body, summary: note.summary,
  folderId: ids.get(note.folder) || null, tags: note.tags, favorite: note.favorite,
  source: note.source, sourceTool: note.tool, version: 0,
  createTime: note.updated, updateTime: note.updated, indexStatus: 'not_configured'
})).sort((a, b) => Date.parse(b.updateTime) - Date.parse(a.updateTime))
for (const folder of folders) {
  for (const child of folder.children) child.count = notes.filter(note => note.folderId === child.id).length
  folder.count = folder.children.reduce((total, child) => total + child.count, 0)
}
await fs.writeFile(new URL('./fixture.json', import.meta.url), JSON.stringify({ folders, notes }))
console.log(`Prepared ${notes.length} sample notes for visual QA only.`)
