// 总览仪表盘的纯计算：全部由笔记数组与目录树推导，不依赖后端聚合接口。
// 收件箱指未归档笔记（folderId 为空），与目录树的「未分类」一致。

export function dashboardStats(notes) {
  const list = notes || []
  return {
    total: list.length,
    favorites: list.filter(note => note.favorite).length,
    mcp: list.filter(note => note.source === 'mcp').length,
    inbox: list.filter(note => note.folderId == null).length
  }
}

const byUpdatedDesc = (a, b) => String(b.updateTime || '').localeCompare(String(a.updateTime || ''))

export function inboxQueue(notes, limit = 8) {
  return (notes || []).filter(note => note.folderId == null).sort(byUpdatedDesc).slice(0, limit)
}

export function recentNotes(notes, limit = 8) {
  return [...(notes || [])].sort(byUpdatedDesc).slice(0, limit)
}

export function tagCounts(notes, limit = 12) {
  const counts = new Map()
  for (const note of notes || []) for (const tag of note.tags || []) if (tag) counts.set(tag, (counts.get(tag) || 0) + 1)
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'zh-CN'))
    .slice(0, limit)
    .map(([name, count]) => ({ name, count }))
}

// 顶级目录条形图：计数为该目录子树内的全部笔记；收件箱（未归档）固定显示一行。
export function folderDistribution(folders, notes) {
  const list = notes || []
  const rows = (folders || []).map(folder => {
    const ids = new Set()
    const walk = items => { for (const item of items || []) { ids.add(item.id); walk(item.children) } }
    walk([folder])
    return { name: folder.name, count: list.filter(note => note.folderId != null && ids.has(note.folderId)).length }
  })
  rows.push({ name: '收件箱', count: list.filter(note => note.folderId == null).length })
  return rows
}
